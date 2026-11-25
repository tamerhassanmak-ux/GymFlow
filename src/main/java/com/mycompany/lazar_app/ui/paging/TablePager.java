package com.mycompany.lazar_app.ui.paging;

import com.mycompany.lazar_app.LanguageManager;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Database paging with bounded work, stale-result protection and no UI-thread queries. */
public final class TablePager<T> {
    private final TableView<T> table;
    private final BiFunction<Integer, Integer, Callable<PageResult<T>>> requests;
    private final Consumer<PageResult<T>> applied;
    private final Consumer<Throwable> failed;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "table-page-loader");
        thread.setDaemon(true);
        return thread;
    });
    private final Button previous = new Button(text("paging.previous"));
    private final Button next = new Button(text("paging.next"));
    private final Label position = new Label();
    private final Label loading = new Label();
    private final ComboBox<Integer> sizes = new ComboBox<>();
    private Task<PageResult<T>> active;
    private int index;
    private int pageSize = 50;
    private long total;
    private long revision;
    private boolean disposed;

    public TablePager(TableView<T> table, HBox host,
                      BiFunction<Integer, Integer, Callable<PageResult<T>>> requests,
                      Consumer<PageResult<T>> applied, Consumer<Throwable> failed) {
        this.table = table;
        this.requests = requests;
        this.applied = applied;
        this.failed = failed;
        sizes.getItems().setAll(25, 50, 100);
        sizes.setValue(pageSize);
        previous.getStyleClass().add("secondary-button");
        next.getStyleClass().add("secondary-button");
        host.getStyleClass().add("table-pagination");
        host.setSpacing(12);
        host.setAlignment(Pos.CENTER_LEFT);
        position.setWrapText(true);
        position.setMinWidth(0);
        position.getStyleClass().add("pagination-position");
        loading.setWrapText(true);
        loading.setMinWidth(0);
        loading.visibleProperty().bind(loading.textProperty().isNotEmpty());
        loading.managedProperty().bind(loading.visibleProperty());
        previous.setMinWidth(Region.USE_PREF_SIZE);
        next.setMinWidth(Region.USE_PREF_SIZE);
        sizes.setPrefWidth(80);sizes.setMinWidth(70);
        previous.setId("pagingPrevious");
        next.setId("pagingNext");
        sizes.setId("pagingSize");
        com.mycompany.lazar_app.ui.ResponsiveRow navigation = new com.mycompany.lazar_app.ui.ResponsiveRow();
        navigation.setSpacing(10);navigation.getChildren().setAll(previous, next);
        navigation.setAlignment(Pos.CENTER_LEFT);
        com.mycompany.lazar_app.ui.ResponsiveRow pageSizeControls = new com.mycompany.lazar_app.ui.ResponsiveRow();
        pageSizeControls.setSpacing(10);
        Label sizeCaption = new Label(text("paging.size"));
        sizeCaption.setWrapText(true);
        sizeCaption.setMinWidth(0);
        pageSizeControls.getChildren().setAll(sizeCaption, sizes);
        pageSizeControls.setAlignment(Pos.CENTER_LEFT);
        com.mycompany.lazar_app.ui.ResponsiveRow controls = new com.mycompany.lazar_app.ui.ResponsiveRow();
        controls.setSpacing(12);controls.getChildren().setAll(position, navigation, pageSizeControls, loading);
        controls.setMinWidth(0);
        controls.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(controls, Priority.ALWAYS);
        host.setMinWidth(0);
        host.setMinHeight(Region.USE_PREF_SIZE);
        host.getChildren().setAll(controls);
        // ResponsiveRow measures its height from the width supplied by its parent,
        // avoiding stale getWidth()-based bindings during a resize.
        previous.setOnAction(e -> { if (index > 0) { index--; reload(); } });
        next.setOnAction(e -> { if (((long) index + 1) * pageSize < total) { index++; reload(); } });
        sizes.valueProperty().addListener((o, before, value) -> {
            if (value != null && value != pageSize) { pageSize = value; first(); }
        });
        table.setSortPolicy(view -> { first(); return true; });
        table.sceneProperty().addListener((o, before, after) -> {
            if (before != null && after == null) dispose();
        });
        updateNavigation(false);
    }

    public void first() { index = 0; reload(); }

    public void clear() {
        revision++;
        if (active != null) active.cancel();
        index = 0;
        total = 0;
        table.getItems().clear();
        loading.setText("");
        updateNavigation(false);
    }

    public void reload() {
        if (disposed) return;
        long requestRevision = ++revision;
        if (active != null) active.cancel();
        // Capture dates/search/sort on the JavaFX thread before starting IO.
        Callable<PageResult<T>> request = requests.apply(index, pageSize);
        Task<PageResult<T>> task = new Task<>() {
            @Override protected PageResult<T> call() throws Exception { return request.call(); }
        };
        active = task;
        loading.setText(text("paging.loading"));
        updateNavigation(true);
        task.setOnSucceeded(e -> {
            if (disposed || requestRevision != revision) return;
            PageResult<T> result = task.getValue();
            index = result.pageIndex();
            total = result.total();
            table.getItems().setAll(result.items());
            table.scrollTo(0);
            loading.setText("");
            updateNavigation(false);
            applied.accept(result);
        });
        task.setOnFailed(e -> {
            if (disposed || requestRevision != revision) return;
            // Clear stale rows so old search results cannot masquerade as the new filter.
            table.getItems().clear();
            total = 0;
            index = 0;
            loading.setText(text("paging.error"));
            updateNavigation(false);
            failed.accept(task.getException());
        });
        worker.submit(task);
    }

    private void updateNavigation(boolean busy) {
        long pages = Math.max(1, (total + pageSize - 1) / pageSize);
        long first = total == 0 ? 0 : (long) index * pageSize + 1;
        long last = Math.min(total, ((long) index + 1) * pageSize);
        String description=String.format(text("paging.position"), index + 1, pages, first, last, total);
        position.setText(total == 0 ? text("paging.empty") : description);
        position.setAccessibleText(position.getText());
        position.setTooltip(new Tooltip(position.getText()));
        previous.setDisable(busy || index == 0);
        next.setDisable(busy || ((long) index + 1) * pageSize >= total);
        // Rows remain visible during loading but cannot be acted on with a stale filter.
        table.setDisable(busy);
    }

    private void dispose() {
        disposed = true;
        revision++;
        if (active != null) active.cancel();
        worker.shutdownNow();
    }

    private static String text(String key) { return LanguageManager.getBundle().getString(key); }
}

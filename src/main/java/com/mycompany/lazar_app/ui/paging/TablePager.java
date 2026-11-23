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
import javafx.scene.layout.FlowPane;
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
        previous.setId("pagingPrevious");
        next.setId("pagingNext");
        sizes.setId("pagingSize");
        HBox navigation = new HBox(10, previous, position, next);
        navigation.setAlignment(Pos.CENTER_LEFT);
        HBox pageSizeControls = new HBox(10, new Label(text("paging.size")), sizes);
        pageSizeControls.setAlignment(Pos.CENTER_LEFT);
        FlowPane controls = new FlowPane(12, 10, navigation, pageSizeControls, loading);
        controls.setMinWidth(0);
        controls.setMaxWidth(Double.MAX_VALUE);
        controls.setPrefWrapLength(760);
        HBox.setHgrow(controls, Priority.ALWAYS);
        host.setMinWidth(0);
        host.setMinHeight(Region.USE_PREF_SIZE);
        host.getChildren().setAll(controls);
        // HBox can measure a FlowPane at its preferred wrap width. Explicitly
        // measure the real footer width so a second line never falls below it.
        host.prefHeightProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(() -> {
            double width = host.getWidth() > 0
                    ? host.getWidth() - host.getInsets().getLeft() - host.getInsets().getRight()
                    : controls.getPrefWrapLength();
            return controls.prefHeight(Math.max(1, width))
                    + host.getInsets().getTop() + host.getInsets().getBottom();
        }, host.widthProperty(), host.insetsProperty(), position.textProperty(), loading.textProperty(),
                position.fontProperty()));
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
        position.setText(String.format(text("paging.position"), index + 1, pages, first, last, total));
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

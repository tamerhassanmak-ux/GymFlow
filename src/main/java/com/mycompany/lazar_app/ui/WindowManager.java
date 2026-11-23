package com.mycompany.lazar_app.ui;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.ui.dialog.DialogStyler;

import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.Node;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.Dialog;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.layout.VBox;
import javafx.beans.binding.Bindings;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * Central window sizing / positioning policy for GymFlow.
 *
 * IMPORTANT:
 * Do not call stage.show(), stage.showAndWait(), sizeToScene(),
 * setWidth(), setHeight() or setMaximized() directly for normal
 * application windows. Use this class instead.
 *
 * This avoids inconsistent window sizes on Linux/Ubuntu window managers.
 */
public final class WindowManager {

    private static final double DEFAULT_MIN_WIDTH = 850.0;
    private static final double DEFAULT_MIN_HEIGHT = 600.0;

    /*
     * Standard size for all normal small GymFlow dialogs/messages.
     * Keeping this in one place prevents Linux/Ubuntu from opening
     * alerts at inconsistent sizes depending on their text.
     */
    public static final double STANDARD_DIALOG_WIDTH = 960.0;
    public static final double STANDARD_DIALOG_HEIGHT = 680.0;

    private WindowManager() {
    }


    // =========================================================
    // CREATE STANDARD MODAL STAGE
    // =========================================================

    public static Stage createModalStage(
            Parent root,
            String title,
            Window owner
    ) {

        if (root == null) {
            throw new IllegalArgumentException("Root cannot be null.");
        }

        applyOrientation(root);

        Scene scene =
                new Scene(root);

        var css =
                WindowManager.class.getResource(
                        "/css/app.css"
                );

        if (css != null) {
            scene.getStylesheets()
                    .add(css.toExternalForm());
        }

        Stage stage =
                new Stage();

        stage.setTitle(
                title == null ? "" : title
        );

        stage.setScene(scene);

        stage.initModality(
                Modality.APPLICATION_MODAL
        );

        if (owner != null) {
            stage.initOwner(owner);
        }

        stage.setResizable(true);

        return stage;
    }


    // =========================================================
    // SHOW MAXIMIZED
    // =========================================================

    public static void showMaximizedAndWait(
            Stage stage
    ) {

        showMaximizedAndWait(
                stage,
                DEFAULT_MIN_WIDTH,
                DEFAULT_MIN_HEIGHT
        );
    }


    public static void showMaximizedAndWait(
            Stage stage,
            double requestedMinWidth,
            double requestedMinHeight
    ) {

        // Retained for existing controllers: all modal forms now follow the
        // same fixed window policy instead of opening maximized.
        showResponsiveAndWait(stage, STANDARD_DIALOG_WIDTH, STANDARD_DIALOG_HEIGHT);
    }

    /** Preferred dimensions are retained for source compatibility only. */
    public static void showResponsiveAndWait(
            Stage stage,
            double preferredWidth,
            double preferredHeight
    ) {
        showFixedStage(stage, true);
    }

    /** Non-blocking variant for controllers that retain the open modal stage. */
    public static void showResponsive(Stage stage) {
        showFixedStage(stage, false);
    }

    private static void showFixedStage(Stage stage, boolean wait) {
        if (stage == null || stage.getScene() == null) return;
        prepareModalContent(stage);
        Rectangle2D bounds = getVisualBounds(stage);
        double width = modalWidth(bounds);
        double height = modalHeight(bounds);
        Runnable geometry = () -> applyFixedModalGeometry(stage, bounds, width, height);
        geometry.run();
        javafx.event.EventHandler<WindowEvent> showing = event -> geometry.run();
        javafx.event.EventHandler<WindowEvent> shown = event -> {
            geometry.run();
            Platform.runLater(() -> { if (stage.isShowing()) geometry.run(); });
        };
        stage.addEventHandler(WindowEvent.WINDOW_SHOWING, showing);
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, shown);
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, new javafx.event.EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent event) {
                stage.removeEventHandler(WindowEvent.WINDOW_SHOWING, showing);
                stage.removeEventHandler(WindowEvent.WINDOW_SHOWN, shown);
                stage.removeEventHandler(WindowEvent.WINDOW_HIDDEN, this);
            }
        });
        if (wait) stage.showAndWait();
        else stage.show();
    }

    private static double modalWidth(Rectangle2D bounds) {
        return Math.min(STANDARD_DIALOG_WIDTH, Math.max(1, bounds.getWidth() - 36));
    }

    private static double modalHeight(Rectangle2D bounds) {
        return Math.min(STANDARD_DIALOG_HEIGHT, Math.max(1, bounds.getHeight() - 60));
    }

    private static void applyFixedModalGeometry(Stage stage, Rectangle2D bounds,
                                                 double width, double height) {
        stage.setMaximized(false);
        stage.setFullScreen(false);
        stage.setResizable(false);
        stage.setMinWidth(width);
        stage.setMaxWidth(width);
        stage.setMinHeight(height);
        stage.setMaxHeight(height);
        applyStableResponsiveSize(stage, bounds, width, height);
    }

    private static ScrollPane scrollableContent(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMinSize(0, 0);
        scroll.getStyleClass().add("gymflow-dialog-scroll");
        if (content instanceof Region region) {
            // Preserve the form's natural size. The viewport must scroll rather
            // than compress fields, labels or nested footer buttons to zero.
            region.setMinHeight(Region.USE_PREF_SIZE);
            region.setMaxHeight(Double.MAX_VALUE);
        }
        return scroll;
    }

    private static VBox wrappedMessage(Text text, DialogPane pane,
                                       javafx.beans.binding.StringExpression source) {
        // Explicit line breaks avoid bidi renderer clipping of implicitly
        // wrapped Arabic paragraphs. Unbreakable values remain scrollable.
        text.textProperty().bind(Bindings.createStringBinding(() -> {
            double width = Math.max(120, (pane.getWidth() > 56
                    ? pane.getWidth() : STANDARD_DIALOG_WIDTH) - 64);
            Text measure = new Text();
            measure.setFont(text.getFont());
            StringBuilder result = new StringBuilder();
            String value = source.get() == null ? "" : source.get();
            for (String paragraph : value.split("\\R", -1)) {
                if (!result.isEmpty()) result.append('\n');
                StringBuilder line = new StringBuilder();
                for (String word : paragraph.split("\\s+")) {
                    measure.setText(line.isEmpty() ? word : line + " " + word);
                    if (!line.isEmpty() && measure.getLayoutBounds().getWidth() > width) {
                        result.append(line).append('\n');
                        line.setLength(0);
                    }
                    if (!line.isEmpty()) line.append(' ');
                    line.append(word);
                }
                result.append(line);
            }
            return result.toString();
        }, source, pane.widthProperty(), text.fontProperty()));
        text.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        text.setTextAlignment(LanguageManager.isArabic()
                ? javafx.scene.text.TextAlignment.RIGHT : javafx.scene.text.TextAlignment.LEFT);
        VBox paragraphs = new VBox(text);
        paragraphs.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        paragraphs.setMinWidth(0);
        paragraphs.setAlignment(LanguageManager.isArabic()
                ? javafx.geometry.Pos.TOP_RIGHT : javafx.geometry.Pos.TOP_LEFT);
        return paragraphs;
    }

    private static void prepareModalContent(Stage stage) {
        Scene scene = stage.getScene();
        Parent content = scene.getRoot();
        if (content.getStyleClass().contains("gymflow-modal-viewport")) return;
        applyOrientation(content);
        ScrollPane viewport = scrollableContent(null);
        viewport.getStyleClass().add("gymflow-modal-viewport");
        viewport.setNodeOrientation(content.getNodeOrientation());
        if (content.getStyleClass().contains("dark-theme")) viewport.getStyleClass().add("dark-theme");
        scene.setRoot(viewport);
        viewport.setContent(content);
        if (content instanceof Region region) region.setMinWidth(Region.USE_PREF_SIZE);
        var css = WindowManager.class.getResource("/css/app.css");
        if (css != null && !scene.getStylesheets().contains(css.toExternalForm())) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }


    // =========================================================
    // APPLY STABLE RESPONSIVE WINDOW SIZE
    // =========================================================

    private static void applyStableResponsiveSize(
            Stage stage,
            Rectangle2D bounds,
            double width,
            double height
    ) {

        if (stage == null
                || bounds == null) {
            return;
        }

        stage.setWidth(
                width
        );

        stage.setHeight(
                height
        );

        centerInsideBounds(
                stage,
                bounds,
                width,
                height
        );
    }


    // =========================================================
    // SHOW MAXIMIZED NON-BLOCKING
    // =========================================================

    public static void showMaximized(
            Stage stage
    ) {

        if (stage == null) {
            return;
        }

        prepareRootForResize(stage);

        Rectangle2D bounds =
                getVisualBounds(stage);

        stage.setResizable(true);

        stage.setMinWidth(
                Math.min(
                        DEFAULT_MIN_WIDTH,
                        bounds.getWidth()
                )
        );

        stage.setMinHeight(
                Math.min(
                        DEFAULT_MIN_HEIGHT,
                        bounds.getHeight()
                )
        );

        applyVisualBounds(
                stage,
                bounds
        );

        stage.setOnShown(
                event ->
                        Platform.runLater(
                                () -> {
                                    Rectangle2D shownBounds =
                                            getVisualBounds(stage);

                                    applyVisualBounds(
                                            stage,
                                            shownBounds
                                    );

                                    stage.setMaximized(true);
                                }
                        )
        );

        stage.show();
    }


    // =========================================================
    // PREPARE ROOT
    // =========================================================

    private static void prepareRootForResize(
            Stage stage
    ) {

        if (stage.getScene() == null) {
            return;
        }

        Parent root =
                stage.getScene().getRoot();

        if (root instanceof Region region) {

            /*
             * Remove an accidental max-size cap from FXML/layout code.
             */
            region.setMaxWidth(
                    Double.MAX_VALUE
            );

            region.setMaxHeight(
                    Double.MAX_VALUE
            );
        }
    }


    // =========================================================
    // RTL / LTR
    // =========================================================

    private static void applyOrientation(
            Parent root
    ) {

        boolean arabic =
                LanguageManager.isArabic();

        root.setNodeOrientation(
                arabic
                        ? NodeOrientation.RIGHT_TO_LEFT
                        : NodeOrientation.LEFT_TO_RIGHT
        );

        root.getStyleClass()
                .removeAll(
                        "arabic",
                        "english"
                );

        root.getStyleClass()
                .add(
                        arabic
                                ? "arabic"
                                : "english"
                );
    }


    // =========================================================
    // VISUAL SCREEN BOUNDS
    // =========================================================

    private static Rectangle2D getVisualBounds(
            Stage stage
    ) {

        Window owner =
                stage.getOwner();

        if (owner != null
                && owner.isShowing()) {

            List<Screen> screens =
                    Screen.getScreensForRectangle(
                            owner.getX(),
                            owner.getY(),
                            Math.max(
                                    owner.getWidth(),
                                    1.0
                            ),
                            Math.max(
                                    owner.getHeight(),
                                    1.0
                            )
                    );

            if (!screens.isEmpty()) {

                return screens
                        .get(0)
                        .getVisualBounds();
            }
        }


        /*
         * If the stage already has a valid position, use its screen.
         */
        if (!Double.isNaN(stage.getX())
                && !Double.isNaN(stage.getY())) {

            List<Screen> screens =
                    Screen.getScreensForRectangle(
                            stage.getX(),
                            stage.getY(),
                            Math.max(
                                    stage.getWidth(),
                                    1.0
                            ),
                            Math.max(
                                    stage.getHeight(),
                                    1.0
                            )
                    );

            if (!screens.isEmpty()) {

                return screens
                        .get(0)
                        .getVisualBounds();
            }
        }


        return Screen
                .getPrimary()
                .getVisualBounds();
    }


    // =========================================================
    // APPLY FULL VISUAL BOUNDS
    // =========================================================

    private static void applyVisualBounds(
            Stage stage,
            Rectangle2D bounds
    ) {

        if (stage == null
                || bounds == null) {
            return;
        }

        stage.setX(
                bounds.getMinX()
        );

        stage.setY(
                bounds.getMinY()
        );

        stage.setWidth(
                bounds.getWidth()
        );

        stage.setHeight(
                bounds.getHeight()
        );
    }


    // =========================================================
    // CENTER
    // =========================================================

    private static void centerInsideBounds(
            Stage stage,
            Rectangle2D bounds,
            double width,
            double height
    ) {

        if (stage == null || bounds == null) {
            return;
        }

        /*
         * Prefer centering over the parent/owner window.
         *
         * This makes alerts and small forms feel visually attached to the
         * screen that opened them instead of appearing in the middle of the
         * monitor. It is especially important when the main GymFlow window is
         * not maximized or when multiple monitors are used.
         */
        Window owner =
                stage.getOwner();

        double x;
        double y;

        if (owner != null
                && owner.isShowing()
                && owner.getWidth() > 0
                && owner.getHeight() > 0) {

            x =
                    owner.getX()
                            + (
                            owner.getWidth()
                                    - width
                    ) / 2.0;

            y =
                    owner.getY()
                            + (
                            owner.getHeight()
                                    - height
                    ) / 2.0;

        } else {

            /*
             * Fallback: center on the usable area of the relevant screen.
             */
            x =
                    bounds.getMinX()
                            + (
                            bounds.getWidth()
                                    - width
                    ) / 2.0;

            y =
                    bounds.getMinY()
                            + (
                            bounds.getHeight()
                                    - height
                    ) / 2.0;
        }

        /*
         * Clamp the final position so the whole window remains visible.
         */
        double maxX =
                Math.max(
                        bounds.getMinX(),
                        bounds.getMaxX() - width
                );

        double maxY =
                Math.max(
                        bounds.getMinY(),
                        bounds.getMaxY() - height
                );

        x =
                Math.max(
                        bounds.getMinX(),
                        Math.min(
                                x,
                                maxX
                        )
                );

        y =
                Math.max(
                        bounds.getMinY(),
                        Math.min(
                                y,
                                maxY
                        )
                );

        stage.setX(x);
        stage.setY(y);
    }

    // =========================================================
    // RESPONSIVE JAVA FX DIALOG
    // =========================================================

    /**
     * Shows a JavaFX Dialog/Alert with a stable size on Linux/Ubuntu.
     *
     * Unlike setOnShown(), addEventHandler() is used so any existing
     * controller focus handler remains active.
     */
    public static <R> Optional<R> showResponsiveDialogAndWait(
            Dialog<R> dialog,
            Window owner,
            double preferredWidth,
            double preferredHeight
    ) {
        if (dialog == null) return Optional.empty();
        if (owner != null && dialog.getOwner() == null) dialog.initOwner(owner);

        final Window effectiveOwner = dialog.getOwner();
        Rectangle2D bounds = getVisualBoundsForOwner(effectiveOwner);
        DialogPane pane = dialog.getDialogPane();
        if (!pane.getStyleClass().contains("app-dialog")) {
            DialogStyler.style(dialog);
        }
        applyOrientation(pane);
        if (!pane.getStyleClass().contains("gymflow-responsive-dialog")) {
            pane.getStyleClass().add("gymflow-responsive-dialog");
        }

        // Preserve Alert's built-in content text. Clearing it and reparenting
        // its label can result in visually empty alerts on some JavaFX skins.
        if (dialog instanceof Alert alert) {
            String header = alert.getHeaderText();
            String body = alert.getContentText();
            if ((header == null || header.isBlank())
                    && (body == null || body.isBlank())
                    && pane.getContent() == null) {
                alert.setContentText(LanguageManager.isArabic()
                        ? "حدث خطأ غير متوقع. راجع سجل الأخطاء."
                        : "An unexpected error occurred. Check the error log.");
            }
            // An explicit content node allows long messages to scroll within
            // the standard frame, without changing the Alert's text property.
            if (pane.getContent() == null) {
                Text message = new Text();

                message.getStyleClass().add("gymflow-dialog-message-text");
                pane.setContent(wrappedMessage(message, pane, alert.contentTextProperty()));
            }
        }
        Node header = pane.getHeader();
        if (header == null && dialog.getHeaderText() != null && !dialog.getHeaderText().isBlank()) {
            Text heading = new Text();

            heading.getStyleClass().add("gymflow-dialog-heading-text");
            header = wrappedMessage(heading, pane, dialog.headerTextProperty());
        }
        if (header != null && !(header instanceof ScrollPane)) {
            ScrollPane headerScroll = scrollableContent(header);
            headerScroll.setMaxHeight(140);
            headerScroll.setPrefHeight(Region.USE_COMPUTED_SIZE);
            headerScroll.getStyleClass().add("gymflow-dialog-header-scroll");
            pane.setHeader(headerScroll);
        }
        Node content = pane.getContent();
        if (content != null && !(content instanceof ScrollPane)) {
            pane.setContent(scrollableContent(content));
        } else if (content instanceof ScrollPane scroll) {
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.setMinSize(0, 0);
        }

        // All modal dialogs use the same outer dimensions, regardless of text,
        // controller defaults, transparent/native decorations or prior size.
        final double width = modalWidth(bounds);
        final double height = modalHeight(bounds);
        dialog.setResizable(false);
        pane.setMinSize(0, 0);
        pane.setPrefSize(width, height);
        pane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        Runnable geometry = () -> {
            dialog.setWidth(width);
            dialog.setHeight(height);
            if (pane.getScene() != null && pane.getScene().getWindow() instanceof Stage stage) {
                applyFixedModalGeometry(stage, bounds, width, height);
            }
        };
        for (var type : pane.getButtonTypes()) {
            if (pane.lookupButton(type) instanceof Region button) {
                button.setMinHeight(Region.USE_PREF_SIZE);
            }
        }
        geometry.run();
        javafx.event.EventHandler<DialogEvent> showing = event -> geometry.run();
        javafx.event.EventHandler<DialogEvent> shown = event -> {
            geometry.run();
            Platform.runLater(() -> { if (dialog.isShowing()) geometry.run(); });
        };
        dialog.addEventHandler(DialogEvent.DIALOG_SHOWING, showing);
        dialog.addEventHandler(DialogEvent.DIALOG_SHOWN, shown);
        try {
            return dialog.showAndWait();
        } finally {
            dialog.removeEventHandler(DialogEvent.DIALOG_SHOWING, showing);
            dialog.removeEventHandler(DialogEvent.DIALOG_SHOWN, shown);
        }
    }


    // =========================================================
    // DIALOG OWNER SCREEN
    // =========================================================

    private static Rectangle2D getVisualBoundsForOwner(
            Window owner
    ) {

        if (owner != null
                && owner.isShowing()) {

            List<Screen> screens =
                    Screen.getScreensForRectangle(
                            owner.getX(),
                            owner.getY(),
                            Math.max(
                                    owner.getWidth(),
                                    1.0
                            ),
                            Math.max(
                                    owner.getHeight(),
                                    1.0
                            )
                    );

            if (!screens.isEmpty()) {

                return screens
                        .get(0)
                        .getVisualBounds();
            }
        }

        return Screen
                .getPrimary()
                .getVisualBounds();
    }



}

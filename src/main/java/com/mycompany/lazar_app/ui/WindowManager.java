package com.mycompany.lazar_app.ui;

import com.mycompany.lazar_app.LanguageManager;

import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.Node;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.Dialog;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
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
    public static final double STANDARD_DIALOG_WIDTH = 520.0;
    public static final double STANDARD_DIALOG_HEIGHT = 320.0;

    /*
     * Calls requesting a normal "small" dialog are normalized to the
     * standard size above. Larger custom dialogs keep their requested size.
     */
    private static final double SMALL_DIALOG_MAX_WIDTH = 650.0;
    private static final double SMALL_DIALOG_MAX_HEIGHT = 450.0;

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

        if (stage == null) {
            return;
        }

        prepareRootForResize(stage);

        Rectangle2D bounds =
                getVisualBounds(stage);

        double minWidth =
                Math.min(
                        requestedMinWidth,
                        bounds.getWidth()
                );

        double minHeight =
                Math.min(
                        requestedMinHeight,
                        bounds.getHeight()
                );

        stage.setResizable(true);

        stage.setMinWidth(
                Math.max(300.0, minWidth)
        );

        stage.setMinHeight(
                Math.max(250.0, minHeight)
        );

        /*
         * Fill the usable desktop BEFORE showing.
         *
         * This is important on Linux. If the window manager delays or
         * ignores the initial maximize request, the window is still
         * opened at a correct usable-screen size.
         */
        applyVisualBounds(
                stage,
                bounds
        );

        /*
         * Re-assert maximize AFTER the window becomes visible.
         *
         * Some Linux window managers do not reliably honour
         * setMaximized(true) before the first show.
         */
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

                                    stage.setMaximized(
                                            true
                                    );
                                }
                        )
        );

        /*
         * showAndWait MUST be called only once.
         */
        stage.showAndWait();
    }


    // =========================================================
    // SHOW RESPONSIVE DIALOG
    // =========================================================

    public static void showResponsiveAndWait(
            Stage stage,
            double preferredWidth,
            double preferredHeight
    ) {

        if (stage == null) {
            return;
        }

        prepareRootForResize(stage);

        Rectangle2D initialBounds =
                getVisualBounds(stage);


        /*
         * Calculate ONE deterministic opening size.
         *
         * IMPORTANT:
         * Do not recalculate the target from stage.getWidth()/getHeight()
         * after the first show. On Linux/Ubuntu the window manager may
         * temporarily report a much smaller decorated size during the
         * first frame. Using that temporary size was the reason a form
         * sometimes opened correctly and sometimes opened too small.
         */
        final double targetWidth =
                Math.min(
                        Math.max(
                                preferredWidth,
                                520.0
                        ),
                        initialBounds.getWidth() * 0.92
                );

        final double targetHeight =
                Math.min(
                        Math.max(
                                preferredHeight,
                                420.0
                        ),
                        initialBounds.getHeight() * 0.92
                );


        stage.setResizable(true);

        stage.setMinWidth(
                Math.min(
                        520.0,
                        targetWidth
                )
        );

        stage.setMinHeight(
                Math.min(
                        420.0,
                        targetHeight
                )
        );


        /*
         * Always start in normal-window mode.
         * This prevents an old maximize state from influencing a new form.
         */
        stage.setMaximized(false);
        stage.setFullScreen(false);


        applyStableResponsiveSize(
                stage,
                initialBounds,
                targetWidth,
                targetHeight
        );


        /*
         * Reapply BEFORE the operating system shows the window.
         */
        stage.addEventHandler(
                WindowEvent.WINDOW_SHOWING,
                event -> {

                    Rectangle2D showingBounds =
                            getVisualBounds(stage);

                    applyStableResponsiveSize(
                            stage,
                            showingBounds,
                            Math.min(
                                    targetWidth,
                                    showingBounds.getWidth() * 0.92
                            ),
                            Math.min(
                                    targetHeight,
                                    showingBounds.getHeight() * 0.92
                            )
                    );
                }
        );


        /*
         * Reapply once more AFTER the native Linux window is created.
         *
         * This is intentionally based on targetWidth/targetHeight,
         * NOT stage.getWidth()/stage.getHeight().
         */
        stage.addEventHandler(
                WindowEvent.WINDOW_SHOWN,
                event ->
                        Platform.runLater(
                                () -> {

                                    Rectangle2D shownBounds =
                                            getVisualBounds(stage);

                                    double finalWidth =
                                            Math.min(
                                                    targetWidth,
                                                    shownBounds.getWidth() * 0.92
                                            );

                                    double finalHeight =
                                            Math.min(
                                                    targetHeight,
                                                    shownBounds.getHeight() * 0.92
                                            );

                                    applyStableResponsiveSize(
                                            stage,
                                            shownBounds,
                                            finalWidth,
                                            finalHeight
                                    );
                                }
                        )
        );


        stage.showAndWait();
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

        Rectangle2D bounds = getVisualBoundsForOwner(owner);
        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("gymflow-responsive-dialog");

        // Keep JavaFX's built-in button bar OUTSIDE the scrolling area.
        // Long alert text and long form content scroll, while actions stay visible.
        if (dialog instanceof Alert alert) {
            String contentText = alert.getContentText();
            if (contentText != null && !contentText.isBlank()
                    && pane.getContent() == null) {
                Label message = new Label(contentText);
                message.setWrapText(true);
                message.setMaxWidth(Double.MAX_VALUE);
                alert.setContentText("");
                pane.setContent(message);
            }
        }
        Node content = pane.getContent();
        if (content != null && !(content instanceof ScrollPane)) {
            ScrollPane scroll = new ScrollPane(content);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(false);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.setPannable(true);
            scroll.getStyleClass().add("gymflow-dialog-scroll");
            pane.setContent(scroll);
        }

        boolean smallAlert = dialog instanceof Alert
                && preferredWidth <= SMALL_DIALOG_MAX_WIDTH
                && preferredHeight <= SMALL_DIALOG_MAX_HEIGHT;
        double desiredWidth = smallAlert ? STANDARD_DIALOG_WIDTH : preferredWidth;
        double desiredHeight = smallAlert ? STANDARD_DIALOG_HEIGHT : preferredHeight;
        final double width = Math.min(Math.max(desiredWidth, 360.0),
                Math.max(280.0, bounds.getWidth() - 36.0));
        final double targetHeight = Math.min(Math.max(desiredHeight, 300.0),
                Math.max(240.0, bounds.getHeight() - 60.0));

        dialog.setResizable(true);
        pane.setMinWidth(Math.min(300.0, width));
        pane.setPrefWidth(width);
        pane.setMaxWidth(Double.MAX_VALUE);
        pane.setMinHeight(Region.USE_COMPUTED_SIZE);
        pane.setPrefHeight(targetHeight);
        pane.setMaxHeight(Double.MAX_VALUE);

        dialog.addEventHandler(DialogEvent.DIALOG_SHOWN, event -> Platform.runLater(() -> {
            if (pane.getScene() == null || !(pane.getScene().getWindow() instanceof Stage stage)) return;
            Rectangle2D screen = getVisualBoundsForOwner(owner != null ? owner : stage);
            double safeWidth = Math.min(width, Math.max(280.0, screen.getWidth() - 36.0));
            double safeMaxHeight = Math.max(240.0, screen.getHeight() - 60.0);
            // CSS/layout can need more vertical room than the originally requested size.
            // Never force a smaller fixed height over the measured DialogPane.
            pane.applyCss();
            pane.layout();
            double measured = pane.prefHeight(safeWidth) + 32.0;
            double safeHeight = Math.min(safeMaxHeight, Math.max(targetHeight, measured));
            stage.setMaximized(false);
            stage.setFullScreen(false);
            stage.setResizable(true);
            stage.setMinWidth(Math.min(300.0, safeWidth));
            stage.setMinHeight(Math.min(220.0, safeHeight));
            stage.setWidth(safeWidth);
            stage.setHeight(safeHeight);
            centerInsideBounds(stage, screen, safeWidth, safeHeight);
        }));
        return dialog.showAndWait();
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

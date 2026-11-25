package com.mycompany.lazar_app;

import com.mycompany.lazar_app.database.DatabaseInitializer;
import com.mycompany.lazar_app.database.migration.MigrationRunner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class Lazar_APP extends Application {

    // =========================================================
    // APPLICATION START
    // =========================================================

    @Override
    public void start(Stage stage) {
        try (var font = Lazar_APP.class.getResourceAsStream("/fonts/NotoNaskhArabic-Regular.ttf")) {
            if (font != null) javafx.scene.text.Font.loadFont(font, 14);
        } catch (java.io.IOException ignored) { /* The platform fallback remains available. */ }

        javafx.scene.control.Label progress = new javafx.scene.control.Label("جاري تجهيز قاعدة البيانات… / Preparing database…");
        stage.setScene(new Scene(new javafx.scene.layout.StackPane(progress), 740, 240));
        stage.setTitle("GymFlow 1.0.7");
        com.mycompany.lazar_app.ui.WindowManager.showResponsive(stage, 740, 300);
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override protected Void call() throws Exception {
                com.mycompany.lazar_app.database.DatabaseStartup.prepare();
                return null;
            }
        };
        task.setOnSucceeded(event -> openApplication(stage));
        task.setOnFailed(event -> {
            com.mycompany.lazar_app.database.DatabaseDiagnostics.failure(task.getException());
            com.mycompany.lazar_app.ui.DatabaseRecoveryWindow.show(stage, task.getException(), () -> start(stage));
        });
        Thread worker = new Thread(task, "gymflow-startup");
        worker.setDaemon(true);
        worker.start();
    }

    private void openApplication(Stage stage) {
        // =====================================================
        // 3. LOAD APPLICATION UI
        // =====================================================

        try {

            // =================================================
            // LOAD DASHBOARD WITH LANGUAGE BUNDLE
            // =================================================

            FXMLLoader loader = new FXMLLoader(
                    Lazar_APP.class.getResource(
                            "/fxml/dashboard.fxml"
                    ),
                    LanguageManager.getBundle()
            );

            Parent root = loader.load();


            // =================================================
            // LANGUAGE + FONT STYLE
            // =================================================

            if (LanguageManager.isArabic()) {

                root.setNodeOrientation(
                        NodeOrientation.RIGHT_TO_LEFT
                );

                root.getStyleClass().add(
                        "arabic"
                );

            } else {

                root.setNodeOrientation(
                        NodeOrientation.LEFT_TO_RIGHT
                );

                root.getStyleClass().add(
                        "english"
                );
            }


            // =================================================
            // CREATE SCENE
            // =================================================

            Scene scene = new Scene(
                    root,
                    1500,
                    850
            );


            // =================================================
            // LOAD CSS
            // =================================================

            scene.getStylesheets().add(
                    Lazar_APP.class
                            .getResource(
                                    "/css/app.css"
                            )
                            .toExternalForm()
            );


            // =================================================
            // MAIN STAGE
            // =================================================

            stage.setTitle(
                    LanguageManager
                            .getBundle()
                            .getString(
                                    "app.title"
                            )
            );

            stage.setScene(scene);


            // =================================================
            // WINDOW SIZE
            // =================================================

            com.mycompany.lazar_app.ui.WindowManager.remember(stage, "main");
            com.mycompany.lazar_app.ui.WindowManager.showResponsive(stage, 1400, 850);
            com.mycompany.lazar_app.ui.WindowManager.enableFullScreen(stage);

            ApplicationBootstrap
                    .ensureSuperAdminConfigured(stage);


        } catch (Exception exception) {

            showApplicationError(exception);
        }
    }


    private void showApplicationError(Exception exception) {
        com.mycompany.lazar_app.database.DatabaseDiagnostics.failure(exception);
        // Startup UI/service failures use the same explicit recovery controls.
        // Retrying only reruns readiness; it never resubmits a financial operation.
        // The current stage remains alive while diagnostics are displayed.
        Stage stage = javafx.stage.Window.getWindows().stream()
                .filter(window -> window instanceof Stage && window.isShowing())
                .map(window -> (Stage)window).findFirst().orElseGet(Stage::new);
        com.mycompany.lazar_app.ui.DatabaseRecoveryWindow.show(stage, exception, () -> start(stage));
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {

        launch(args);
    }
}
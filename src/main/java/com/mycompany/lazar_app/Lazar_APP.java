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

        // =====================================================
        // 1. DATABASE INITIALIZATION
        // =====================================================

        try {

            /*
             * يقوم بتهيئة قاعدة البيانات والاتصال.
             *
             * SQLite هو الاختيار الافتراضي.
             *
             * يجب أن تكون مسؤولية DatabaseInitializer هنا:
             *
             * - قراءة إعدادات قاعدة البيانات.
             * - تجهيز الاتصال.
             * - إنشاء ملف SQLite عند الحاجة.
             *
             * إنشاء وتحديث جداول النظام أصبح مسؤولية
             * MigrationRunner.
             */

            DatabaseInitializer.initialize();


            // =================================================
            // 2. DATABASE MIGRATIONS
            // =================================================

            /*
             * يقوم بتشغيل الـ migrations غير المنفذة فقط.
             *
             * مثال:
             *
             * 001 create_members
             * 002 create_membership_plans
             * 003 create_member_memberships
             * 004 create_check_ins
             * 005 create_payments
             * 006 ensure_session_limit_columns
             *
             * الـ migrations المنفذة سابقًا سيتم تخطيها.
             */

            MigrationRunner.migrate();

        } catch (Exception exception) {

            exception.printStackTrace();

            showDatabaseError(exception);

            return;
        }


        // =====================================================
        // 3. LOAD APPLICATION UI
        // =====================================================

        try {

            // =================================================
            // LOAD DASHBOARD WITH LANGUAGE BUNDLE
            // =================================================

            FXMLLoader loader = new FXMLLoader(
                    Lazar_APP.class.getResource(
                            "dashboard.fxml"
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

            stage.setMinWidth(1200);
            stage.setMinHeight(700);

            stage.setMaximized(true);


            // =================================================
            // SHOW APPLICATION
            // =================================================

            stage.show();


            // =================================================
            // 4. FIRST RUN / SUPER ADMIN SETUP
            // =================================================

            /*
             * عند أول تشغيل فقط:
             *
             * إذا لم تكن هناك كلمة مرور Super Admin
             * ستظهر نافذة إنشاء كلمة المرور.
             *
             * بعد إنشائها لن تظهر النافذة مرة أخرى.
             */

            ApplicationBootstrap
                    .ensureSuperAdminConfigured(stage);


        } catch (Exception exception) {

            exception.printStackTrace();

            showApplicationError(exception);
        }
    }


    // =========================================================
    // DATABASE ERROR
    // =========================================================

    private void showDatabaseError(
            Exception exception) {

        Alert alert = new Alert(
                Alert.AlertType.ERROR
        );

        alert.setTitle(
                "Database Error"
        );

        alert.setHeaderText(
                "Unable to initialize database"
        );

        String message =
                exception.getMessage();

        if (message == null
                || message.isBlank()) {

            message =
                    exception
                            .getClass()
                            .getSimpleName();
        }

        alert.setContentText(
                message
        );

        alert.showAndWait();
    }


    // =========================================================
    // APPLICATION ERROR
    // =========================================================

    private void showApplicationError(
            Exception exception) {

        Alert alert = new Alert(
                Alert.AlertType.ERROR
        );

        alert.setTitle(
                "Application Error"
        );

        alert.setHeaderText(
                "Unable to start Lazar Gym"
        );

        String message =
                exception.getMessage();

        if (message == null
                || message.isBlank()) {

            message =
                    exception
                            .getClass()
                            .getSimpleName();
        }

        alert.setContentText(
                message
        );

        alert.showAndWait();
    }


    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {

        launch(args);
    }
}
package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.ApplicationBootstrap;
import com.mycompany.lazar_app.security.SuperAdminAuthService;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class DatabaseAuthController {

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;


    // =========================================================
    // VERIFY SUPER ADMIN
    // =========================================================

    @FXML
    private void handleVerify() {

        Stage authStage =
                (Stage) passwordField
                        .getScene()
                        .getWindow();


        // -----------------------------------------------------
        // FIRST TIME SETUP
        // -----------------------------------------------------

        if (!SuperAdminAuthService.isConfigured()) {

            try {

                ApplicationBootstrap
                        .ensureSuperAdminConfigured(
                                authStage.getOwner()
                        );

            } catch (Exception exception) {

                messageLabel.setText(
                        "يجب إعداد كلمة مرور السوبر أدمن أولًا."
                );

                return;
            }
        }


        // -----------------------------------------------------
        // READ PASSWORD
        // -----------------------------------------------------

        String password =
                passwordField.getText();


        if (password == null
                || password.isBlank()) {

            messageLabel.setText(
                    "أدخل كلمة مرور السوبر أدمن."
            );

            passwordField.requestFocus();

            return;
        }


        // -----------------------------------------------------
        // VERIFY PASSWORD
        // -----------------------------------------------------

        if (!SuperAdminAuthService.verify(password)) {

            messageLabel.setText(
                    "كلمة مرور السوبر أدمن غير صحيحة."
            );

            passwordField.clear();

            passwordField.requestFocus();

            return;
        }


        // -----------------------------------------------------
        // GET OWNER BEFORE CLOSING AUTH WINDOW
        // -----------------------------------------------------

        Window owner =
                authStage.getOwner();


        // -----------------------------------------------------
        // CLOSE AUTH WINDOW
        // -----------------------------------------------------

        authStage.close();


        // -----------------------------------------------------
        // OPEN DATABASE SETTINGS
        //
        // IMPORTANT:
        // Open it on the next JavaFX pulse.
        // -----------------------------------------------------

        Platform.runLater(
                () -> openDatabaseSettings(owner)
        );
    }


    // =========================================================
    // CANCEL
    // =========================================================

    @FXML
    private void handleCancel() {

        Stage stage =
                (Stage) passwordField
                        .getScene()
                        .getWindow();

        stage.close();
    }


    // =========================================================
    // OPEN DATABASE SETTINGS
    // =========================================================

    private void openDatabaseSettings(
            Window owner
    ) {

        try {

            // -------------------------------------------------
            // LOCATE FXML
            // -------------------------------------------------

            var resource =
                    getClass().getResource(
                            "/fxml/database-settings.fxml"
                    );


            if (resource == null) {

                throw new IllegalStateException(
                        "database-settings.fxml was not found."
                );
            }


            System.out.println(
                    "Loading database settings from: "
                            + resource
            );


            // -------------------------------------------------
            // LOAD FXML
            // -------------------------------------------------

            FXMLLoader loader =
                    new FXMLLoader(resource);


            Parent root =
                    loader.load();


            System.out.println(
                    "Database settings root loaded: "
                            + root.getClass().getName()
            );


            // -------------------------------------------------
            // CREATE STAGE
            // -------------------------------------------------

            Stage stage =
                    new Stage();


            stage.setTitle(
                    "Database Configuration"
            );


            // -------------------------------------------------
            // OWNER
            // -------------------------------------------------

            if (owner != null) {

                stage.initOwner(owner);
            }


            stage.initModality(
                    Modality.APPLICATION_MODAL
            );


            // -------------------------------------------------
            // SCENE
            // -------------------------------------------------

            Scene scene =
                    new Scene(
                            root,
                            1000,
                            700
                    );


            stage.setScene(scene);


            // -------------------------------------------------
            // WINDOW SETTINGS
            // -------------------------------------------------

            stage.setResizable(true);

            stage.setMinWidth(850);

            stage.setMinHeight(600);


            // -------------------------------------------------
            // SHOW
            // -------------------------------------------------

            stage.show();


            // -------------------------------------------------
            // CENTER AFTER SHOW
            // -------------------------------------------------

            stage.centerOnScreen();


            // -------------------------------------------------
            // DEBUG
            // -------------------------------------------------

            System.out.println(
                    "Database configuration window opened."
            );

            System.out.println(
                    "Root bounds: "
                            + root.getBoundsInParent()
            );

            System.out.println(
                    "Scene size: "
                            + scene.getWidth()
                            + " x "
                            + scene.getHeight()
            );


        } catch (IOException exception) {

            System.err.println(
                    "Failed to load database-settings.fxml"
            );

            exception.printStackTrace();

        } catch (Exception exception) {

            System.err.println(
                    "Failed to open Database Configuration"
            );

            exception.printStackTrace();
        }
    }
}
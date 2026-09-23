package com.mycompany.lazar_app;

import com.mycompany.lazar_app.security.SuperAdminAuthService;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

public final class ApplicationBootstrap {

    private ApplicationBootstrap() {
    }

    public static void ensureSuperAdminConfigured(
            Window owner
    ) {

        // Already configured
        if (SuperAdminAuthService.isConfigured()) {
            return;
        }

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            ApplicationBootstrap.class
                                    .getResource(
                                            "/fxml/super-admin-setup.fxml"
                                    )
                    );

            Parent root = loader.load();

            Stage stage = new Stage();

            stage.setTitle(
                    "Super Admin Setup"
            );

            stage.initModality(
                    Modality.APPLICATION_MODAL
            );

            if (owner != null) {
                stage.initOwner(owner);
            }

            stage.setScene(
                    new Scene(root)
            );

            stage.setResizable(false);

            stage.sizeToScene();

            stage.centerOnScreen();

            stage.showAndWait();

            /*
             * User must complete setup.
             */
            if (!SuperAdminAuthService.isConfigured()) {

                throw new IllegalStateException(
                        "Super Admin setup was not completed."
                );
            }

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Could not open Super Admin setup.",
                    exception
            );
        }
    }
}
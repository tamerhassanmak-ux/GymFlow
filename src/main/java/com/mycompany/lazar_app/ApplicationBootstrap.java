package com.mycompany.lazar_app;

import com.mycompany.lazar_app.license.ProgramLicenseGate;
import com.mycompany.lazar_app.security.SuperAdminAuthService;

import javafx.application.Platform;
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

    /**
     * Existing startup entry point retained for compatibility.
     *
     * It now performs BOTH:
     * 1. Super Admin first-time setup.
     * 2. Program license validation / renewal.
     *
     * Therefore existing code that already calls this method before
     * opening dashboard.fxml automatically receives the new license gate.
     */
    public static void ensureSuperAdminConfigured(
            Window owner
    ) {

        ensureSuperAdminPassword(owner);

        if (!ProgramLicenseGate.ensureValid(owner)) {
            Platform.exit();

            throw new IllegalStateException(
                    "GymFlow program license activation was cancelled or is invalid."
            );
        }
    }

    /**
     * More explicit alias for new startup code.
     */
    public static boolean ensureApplicationReady(
            Window owner
    ) {

        ensureSuperAdminPassword(owner);

        boolean allowed =
                ProgramLicenseGate.ensureValid(owner);

        if (!allowed) {
            Platform.exit();
        }

        return allowed;
    }

    private static void ensureSuperAdminPassword(
            Window owner
    ) {

        if (SuperAdminAuthService.isConfigured()) {
            return;
        }

        try {
            FXMLLoader loader =
                    new FXMLLoader(
                            ApplicationBootstrap.class.getResource(
                                    "/fxml/super-admin-setup.fxml"
                            )
                    );

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Super Admin Setup");
            stage.initModality(Modality.APPLICATION_MODAL);

            if (owner != null) {
                stage.initOwner(owner);
            }

            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.showAndWait();

            if (!SuperAdminAuthService.isConfigured()) {
                Platform.exit();

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

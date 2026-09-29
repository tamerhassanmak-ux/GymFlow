package com.mycompany.lazar_app.license;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.controller.ProgramLicenseSettingsController;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Startup gate for the GymFlow desktop application.
 *
 * The dashboard must not be opened until this gate returns true.
 */
public final class ProgramLicenseGate {

    private ProgramLicenseGate() {
    }

    public static boolean ensureValid(
            Window owner
    ) {

        ProgramLicenseStatus status =
                ProgramLicenseManager.evaluate();

        if (status.isAllowed()) {
            ProgramLicenseManager.recordSuccessfulOpen();
            return true;
        }

        return openActivationWindow(owner);
    }

    private static boolean openActivationWindow(
            Window owner
    ) {

        try {
            FXMLLoader loader =
                    new FXMLLoader(
                            ProgramLicenseGate.class.getResource(
                                    "/fxml/program-license-settings.fxml"
                            )
                    );

            Parent root = loader.load();

            ProgramLicenseSettingsController controller =
                    loader.getController();

            controller.setStartupLockMode(true);

            root.setNodeOrientation(
                    LanguageManager.isArabic()
                            ? NodeOrientation.RIGHT_TO_LEFT
                            : NodeOrientation.LEFT_TO_RIGHT
            );

            Stage stage = new Stage();

            stage.setTitle(
                    LanguageManager.isArabic()
                            ? "تفعيل ترخيص تشغيل GymFlow"
                            : "GymFlow Program License Activation"
            );

            stage.initModality(Modality.APPLICATION_MODAL);

            if (owner != null) {
                stage.initOwner(owner);
            }

            Scene scene = new Scene(root);

            var css =
                    ProgramLicenseGate.class.getResource(
                            "/css/app.css"
                    );

            if (css != null) {
                scene.getStylesheets().add(
                        css.toExternalForm()
                );
            }

            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(620);
            stage.setMinHeight(560);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.showAndWait();

            if (!controller.isActivated()) {
                return false;
            }

            ProgramLicenseStatus finalStatus =
                    ProgramLicenseManager.evaluate();

            if (finalStatus.isAllowed()) {
                ProgramLicenseManager.recordSuccessfulOpen();
                return true;
            }

            return false;

        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }
}

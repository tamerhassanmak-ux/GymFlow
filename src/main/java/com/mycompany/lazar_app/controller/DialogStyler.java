package com.mycompany.lazar_app.ui.dialog;

import javafx.geometry.NodeOrientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

/**
 * Applies the GymFlow visual system to existing JavaFX Dialog / Alert /
 * ChoiceDialog instances without changing their business logic.
 */
public final class DialogStyler {

    private DialogStyler() {
    }

    public static void style(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }

        DialogPane pane = dialog.getDialogPane();

        var css = DialogStyler.class.getResource("/css/app.css");
        if (css != null && !pane.getStylesheets().contains(css.toExternalForm())) {
            pane.getStylesheets().add(css.toExternalForm());
        }

        if (!pane.getStyleClass().contains("gymflow-native-dialog")) {
            pane.getStyleClass().add("gymflow-native-dialog");
        }

        if (dialog instanceof Alert alert) {
            String typeClass = switch (alert.getAlertType()) {
                case INFORMATION -> "gymflow-dialog-info";
                case WARNING -> "gymflow-dialog-warning";
                case ERROR -> "gymflow-dialog-error";
                case CONFIRMATION -> "gymflow-dialog-confirm";
                default -> "gymflow-dialog-info";
            };
            if (!pane.getStyleClass().contains(typeClass)) {
                pane.getStyleClass().add(typeClass);
            }
        } else if (!pane.getStyleClass().contains("gymflow-dialog-form")) {
            pane.getStyleClass().add("gymflow-dialog-form");
        }

        pane.setMinWidth(430);
        pane.setPrefWidth(Math.max(pane.getPrefWidth(), 470));

        // Style buttons according to their semantic ButtonData.
        for (ButtonType type : pane.getButtonTypes()) {
            if (!(pane.lookupButton(type) instanceof Button button)) {
                continue;
            }

            ButtonBar.ButtonData data = type.getButtonData();

            if (data == ButtonBar.ButtonData.CANCEL_CLOSE
                    || data == ButtonBar.ButtonData.NO) {
                addClass(button, "app-dialog-secondary-button");
            } else if (data == ButtonBar.ButtonData.OK_DONE
                    || data == ButtonBar.ButtonData.YES
                    || data == ButtonBar.ButtonData.FINISH
                    || data == ButtonBar.ButtonData.APPLY) {
                if (dialog instanceof Alert alert
                        && alert.getAlertType() == Alert.AlertType.CONFIRMATION) {
                    addClass(button, "app-dialog-primary-button");
                } else {
                    addClass(button, "app-dialog-primary-button");
                }
            }
        }

        // Respect the orientation already configured by each controller.
        if (pane.getNodeOrientation() == null) {
            pane.setNodeOrientation(NodeOrientation.INHERIT);
        }
    }

    private static void addClass(Button button, String styleClass) {
        if (!button.getStyleClass().contains(styleClass)) {
            button.getStyleClass().add(styleClass);
        }
    }
}

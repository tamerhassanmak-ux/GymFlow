package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.security.SuperAdminAuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class SuperAdminSetupController {

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private void handleSave() {

        String password = passwordField.getText();
        String confirmation = confirmPasswordField.getText();

        if (password == null || password.length() < 8) {
            messageLabel.setText(
                    "كلمة المرور يجب ألا تقل عن 8 أحرف."
            );
            return;
        }

        if (!password.equals(confirmation)) {
            messageLabel.setText(
                    "كلمتا المرور غير متطابقتين."
            );
            return;
        }

        try {

            SuperAdminAuthService.setPassword(password);

            Stage stage =
                    (Stage) passwordField
                            .getScene()
                            .getWindow();

            stage.close();

        } catch (Exception exception) {

            messageLabel.setText(
                    exception.getMessage()
            );
        }
    }
}
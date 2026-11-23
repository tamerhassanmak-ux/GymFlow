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
                    localized("كلمة المرور يجب ألا تقل عن 8 أحرف.", "Password must contain at least 8 characters.")
            );
            return;
        }

        if (!password.equals(confirmation)) {
            messageLabel.setText(
                    localized("كلمتا المرور غير متطابقتين.", "Passwords do not match.")
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
    private static String localized(String ar, String en) {
        return com.mycompany.lazar_app.LanguageManager.isArabic() ? ar : en;
    }
}
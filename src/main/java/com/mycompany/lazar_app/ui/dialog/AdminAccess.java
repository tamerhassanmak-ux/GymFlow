package com.mycompany.lazar_app.ui.dialog;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.security.AuthorizationService;
import com.mycompany.lazar_app.ui.WindowManager;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class AdminAccess {
    private AdminAccess() {}
    public static boolean ensure(Window owner) {
        if (AuthorizationService.currentRole() == AuthorizationService.Role.SUPER_ADMIN) return true;
        boolean ar = LanguageManager.isArabic();
        Dialog<ButtonType> dialog = new Dialog<>();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(ar ? "موافقة السوبر أدمن" : "Super Admin approval");
        PasswordField password = new PasswordField();
        password.setPromptText(ar ? "كلمة المرور" : "Password");
        Label message = new Label(ar ? "أدخل كلمة مرور السوبر أدمن للمتابعة." : "Enter the Super Admin password to continue.");
        dialog.getDialogPane().setContent(new VBox(10, message, password));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setNodeOrientation(ar ? javafx.geometry.NodeOrientation.RIGHT_TO_LEFT
                : javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!AuthorizationService.authenticate(password.getText())) {
                password.clear();
                message.setText(ar ? "كلمة المرور غير صحيحة." : "Incorrect password.");
                event.consume();
            }
        });
        var result = WindowManager.showResponsiveDialogAndWait(dialog, owner, 460, 280);
        password.clear();
        return result.isPresent() && result.get() == ButtonType.OK
                && AuthorizationService.currentRole() == AuthorizationService.Role.SUPER_ADMIN;
    }
    public static boolean ensure() {
        Window owner = Window.getWindows().stream().filter(Window::isFocused).findFirst().orElse(null);
        return ensure(owner);
    }
}

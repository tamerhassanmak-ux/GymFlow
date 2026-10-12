package com.mycompany.lazar_app.ui.dialog;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.ui.WindowManager;
import com.mycompany.lazar_app.model.Member;
import java.util.Optional;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/** Central professional dialog system for GymFlow / Lazar Gym. */
public final class AppDialog {

    public enum Type { SUCCESS, INFO, WARNING, ERROR, DANGER }

    private AppDialog() {}

    public static void success(Window owner, String title, String message) {
        show(owner, Type.SUCCESS, title, message, null, false);
    }

    public static void info(Window owner, String title, String message) {
        show(owner, Type.INFO, title, message, null, false);
    }

    public static void warning(Window owner, String title, String message) {
        show(owner, Type.WARNING, title, message, null, false);
    }

    public static void error(Window owner, String title, String message) {
        show(owner, Type.ERROR, title, message, null, false);
    }

    public static void duplicatePhone(Window owner, Member member) {
        boolean ar = LanguageManager.isArabic();
        String title = ar ? "رقم الهاتف مستخدم بالفعل" : "Phone Already Registered";
        String message = ar
                ? "لا يمكن متابعة الحفظ لأن رقم الهاتف مرتبط بعضو مسجل بالفعل."
                : "The phone number is already linked to an existing member.";

        VBox details = new VBox(7);
        details.getStyleClass().add("app-dialog-details");
        details.getChildren().addAll(
                detailRow(ar ? "اسم العضو" : "Member", safe(member.getName())),
                detailRow(ar ? "كود العضو" : "Member Code", safe(member.getMemberCode())),
                detailRow(ar ? "رقم الهاتف" : "Phone", safe(member.getPhone()))
        );
        show(owner, Type.WARNING, title, message, details, false);
    }

    public static boolean confirmDelete(Window owner, Member member) {
        boolean ar = LanguageManager.isArabic();
        String title = ar ? "حذف العضو؟" : "Delete member?";
        String message = ar
                ? "سيتم حذف بيانات العضو. تأكد من بيانات العضو قبل المتابعة."
                : "The member record will be deleted. Verify the member before continuing.";

        VBox details = new VBox(7);
        details.getStyleClass().add("app-dialog-details");
        details.getChildren().addAll(
                detailRow(ar ? "العضو" : "Member", safe(member.getName())),
                detailRow(ar ? "الكود" : "Code", safe(member.getMemberCode()))
        );
        return show(owner, Type.DANGER, title, message, details, true);
    }

    private static Label detailRow(String label, String value) {
        Label node = new Label(label + "   •   " + value);
        node.getStyleClass().add("app-dialog-detail-row");
        node.setWrapText(true);
        return node;
    }

    private static boolean show(Window owner, Type type, String title, String message,
                                Node extraContent, boolean confirmation) {
        boolean ar = LanguageManager.isArabic();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) dialog.initOwner(owner);

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().addAll("app-dialog", "app-dialog-" + type.name().toLowerCase());
        pane.setNodeOrientation(ar ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        /*
         * Keep every normal GymFlow message at the same professional size.
         * WindowManager will also enforce this on the native Stage.
         */
        // Responsive size is managed centrally, never lock the pane height.
        pane.setPrefWidth(WindowManager.STANDARD_DIALOG_WIDTH);
        pane.setPrefHeight(WindowManager.STANDARD_DIALOG_HEIGHT);

        var css = AppDialog.class.getResource("/css/app.css");
        if (css != null) pane.getStylesheets().add(css.toExternalForm());

        Label icon = new Label(iconFor(type));
        icon.getStyleClass().addAll("app-dialog-icon", "app-dialog-icon-" + type.name().toLowerCase());

        Label titleLabel = new Label(title == null ? "" : title);
        titleLabel.getStyleClass().add("app-dialog-title");
        titleLabel.setWrapText(true);

        Label messageLabel = new Label(message == null ? "" : message);
        messageLabel.getStyleClass().add("app-dialog-message");
        messageLabel.setWrapText(true);

        VBox textBox = new VBox(6, titleLabel, messageLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        HBox header = new HBox(15, icon, textBox);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-dialog-header");

        VBox body = new VBox(16, header);
        body.getStyleClass().add("app-dialog-body");
        if (extraContent != null) body.getChildren().add(extraContent);
        pane.setContent(body);

        ButtonType primary = confirmation
                ? new ButtonType(ar ? "حذف العضو" : "Delete Member", ButtonBar.ButtonData.OK_DONE)
                : new ButtonType(ar ? "تم" : "Done", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().add(primary);
        if (confirmation) {
            pane.getButtonTypes().add(new ButtonType(ar ? "إلغاء" : "Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));
        }

        Button primaryButton = (Button) pane.lookupButton(primary);
        primaryButton.getStyleClass().add(type == Type.DANGER ? "app-dialog-danger-button" : "app-dialog-primary-button");
        if (confirmation && pane.getButtonTypes().size() > 1) {
            Button cancel = (Button) pane.lookupButton(pane.getButtonTypes().get(1));
            cancel.getStyleClass().add("app-dialog-secondary-button");
        }

        /*
         * All AppDialog messages are shown through WindowManager so they use
         * one size policy and are centered over the owner window.
         */
        Optional<ButtonType> result =
                WindowManager.showResponsiveDialogAndWait(
                        dialog,
                        owner,
                        WindowManager.STANDARD_DIALOG_WIDTH,
                        WindowManager.STANDARD_DIALOG_HEIGHT
                );

        return confirmation
                && result.isPresent()
                && result.get() == primary;
    }

    private static String iconFor(Type type) {
        return switch (type) {
            case SUCCESS -> "✓";
            case INFO -> "i";
            case WARNING -> "!";
            case ERROR, DANGER -> "×";
        };
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }
}

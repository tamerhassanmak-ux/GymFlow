package com.mycompany.lazar_app.ui;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.database.*;
import com.mycompany.lazar_app.controller.DatabaseSettingsController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public final class DatabaseRecoveryWindow {
    private DatabaseRecoveryWindow() {}
    public static void show(Stage stage, Throwable failure, Runnable retry) {
        BorderPane root = new BorderPane();
        Label detail = new Label(DatabaseExceptionHandler.message(failure) + "\n" + DatabaseExceptionHandler.diagnostic(failure));
        detail.setWrapText(true);
        detail.setStyle("-fx-padding: 18; -fx-text-fill: #b91c1c;");
        root.setTop(detail);
        DatabaseSettingsController[] editor = new DatabaseSettingsController[1];
        java.util.List<javafx.scene.Node> configurationActions=new java.util.ArrayList<>();
        boolean arabic=LanguageManager.isArabic();
        try {
            FXMLLoader loader = new FXMLLoader(DatabaseRecoveryWindow.class.getResource("/fxml/database-settings.fxml"), LanguageManager.getBundle());
            javafx.scene.Parent form=loader.load();
            if(form instanceof BorderPane settings && settings.getBottom() instanceof Pane footer){
                configurationActions.addAll(footer.getChildren());footer.getChildren().clear();settings.setBottom(null);
            }
            root.setCenter(form);
            editor[0] = loader.getController();
            editor[0].setRecoveryMode();

        } catch (Exception error) {
            DatabaseDiagnostics.failure(error);
            root.setCenter(new Label("تعذر فتح الإعدادات. راجع دليل الاستعادة. / Configuration window unavailable; see recovery guide."));
        }
        Button again = new Button(arabic?"إعادة الاتصال":"Retry connection");
        again.setOnAction(event -> retry.run());
        if (editor[0] != null) editor[0].onBusy(again::setDisable);
        Button export = new Button(arabic?"تصدير التشخيص":"Export diagnostics");
        export.setOnAction(event -> {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setInitialFileName("GymFlow-diagnostics.txt");
            java.io.File file = chooser.showSaveDialog(stage);
            if (file == null) return;
            try {
                DatabaseDiagnostics.export(file.toPath());
                java.nio.file.Files.writeString(file.toPath(), DatabaseExceptionHandler.diagnostic(failure) + "\n",
                        java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception error) { detail.setText(DatabaseExceptionHandler.message(error)); }
        });
        Button exit = new Button(arabic?"خروج آمن":"Exit safely");
        exit.setOnAction(event -> javafx.application.Platform.exit());
        HBox actions = new ResponsiveRow();actions.setSpacing(12);
        actions.getChildren().addAll(configurationActions);actions.getChildren().addAll(again,export,exit);
        actions.setStyle("-fx-padding: 14;");
        root.setBottom(actions);
        root.setNodeOrientation(LanguageManager.isArabic() ? javafx.geometry.NodeOrientation.RIGHT_TO_LEFT : javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
        Scene scene = new Scene(root, 1000, 820);
        scene.getStylesheets().add(DatabaseRecoveryWindow.class.getResource("/css/app.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("GymFlow — إعداد قاعدة البيانات / Database recovery");
        WindowManager.showResponsive(stage, 1000, 820);
    }
}

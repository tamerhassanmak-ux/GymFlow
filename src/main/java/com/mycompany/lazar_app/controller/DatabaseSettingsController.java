package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.database.DatabaseConfig;
import com.mycompany.lazar_app.database.DatabaseConfigManager;
import com.mycompany.lazar_app.database.DatabaseManager;
import com.mycompany.lazar_app.database.DatabaseType;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

public class DatabaseSettingsController {

    @FXML private ComboBox<DatabaseType> databaseTypeCombo;
    @FXML private VBox sqlitePane;
    @FXML private VBox serverPane;
    @FXML private TextField sqlitePathField;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField databaseNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox encryptCheckBox;
    @FXML private Label statusLabel;
    @FXML private TextField trustStoreField;
    @FXML private PasswordField trustStorePasswordField;
    @FXML private ComboBox<String> trustStoreTypeCombo;
    @FXML private TextField rsaPublicKeyField;
    @FXML private Button testButton;
    @FXML private Button saveButton;
    private boolean busy;
    private boolean restartAfterSave = true;
    private java.util.function.Consumer<Boolean> busyListener = value -> {};
    public void setRecoveryMode() { restartAfterSave = false; }
    public void onBusy(java.util.function.Consumer<Boolean> listener) { busyListener = listener; listener.accept(busy); }
    private void setBusy(boolean value) {
        busy = value; testButton.setDisable(value); saveButton.setDisable(value); busyListener.accept(value);
    }
    @FXML private CheckBox platformCredentialCheckBox;


    @FXML
    public void initialize() {
        databaseTypeCombo.getItems().setAll(DatabaseType.values());
        databaseTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateVisibleFields(newValue);
            updateDefaultPort(oldValue, newValue);
        });
        trustStoreTypeCombo.getItems().setAll("PKCS12", "JKS");
        loadConfiguration();
    }

    private void loadConfiguration() {
        setBusy(true);
        javafx.concurrent.Task<DatabaseConfig> task = new javafx.concurrent.Task<>() {
            @Override protected DatabaseConfig call() { return DatabaseConfigManager.loadForEditing(); }
        };
        task.setOnSucceeded(event -> {
            applyConfiguration(task.getValue());
            setBusy(false);
        });
        task.setOnFailed(event -> {
            // Corrupt persisted settings never select a replacement database.
            hostField.setText("localhost"); portField.setText("3306");
            encryptCheckBox.setSelected(true); trustStoreTypeCombo.setValue("PKCS12");
            showFailure(task.getException());
            setBusy(false);
        });
        Thread worker = new Thread(task, "gymflow-config-load"); worker.setDaemon(true); worker.start();
    }

    private void applyConfiguration(DatabaseConfig c) {
        platformCredentialCheckBox.setSelected(c.isPlatformCredentialStorage());
        trustStoreField.setText(c.getTrustStore());
        trustStorePasswordField.setText(c.getTrustStorePassword());
        trustStoreTypeCombo.setValue(c.getTrustStoreType());
        rsaPublicKeyField.setText(c.getRsaPublicKey());
        databaseTypeCombo.setValue(c.getType());
        sqlitePathField.setText(c.getSqlitePath());
        hostField.setText(c.getHost());
        portField.setText(String.valueOf(c.getPort()));
        databaseNameField.setText(c.getDatabaseName());
        usernameField.setText(c.getUsername());
        passwordField.setText(c.getPassword());
        encryptCheckBox.setSelected(c.isEncryptConnection());
        updateVisibleFields(c.getType());
    }

    private void updateVisibleFields(DatabaseType type) {
        boolean sqlite = type == null || type == DatabaseType.SQLITE;
        sqlitePane.setVisible(sqlite);
        sqlitePane.setManaged(sqlite);
        serverPane.setVisible(!sqlite);
        serverPane.setManaged(!sqlite);
    }

    private void updateDefaultPort(DatabaseType oldType, DatabaseType newType) {
        if (newType == null || newType == DatabaseType.SQLITE) return;
        if (oldType != newType) portField.setText(newType == DatabaseType.SQL_SERVER ? "1433" : "3306");
    }

    @FXML
    private void handleBrowse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select SQLite Database");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("SQLite Database", "*.db", "*.sqlite", "*.sqlite3")
        );
        File file = chooser.showSaveDialog(sqlitePathField.getScene().getWindow());
        if (file != null) sqlitePathField.setText(file.getAbsolutePath());
    }

    @FXML
    private void handleTestConnection() { connect(false); }

    @FXML
    private void handleSave() {
        if (busy) return;
        if (!com.mycompany.lazar_app.security.SuperAdminAuthService.isConfigured()) {
            // Establish the existing local administrator before persisting first-run recovery settings.
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/super-admin-setup.fxml"),
                        com.mycompany.lazar_app.LanguageManager.getBundle());
                javafx.stage.Stage setup = new javafx.stage.Stage();
                setup.initOwner(statusLabel.getScene().getWindow());
                setup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                setup.setScene(new javafx.scene.Scene(loader.load()));
                com.mycompany.lazar_app.ui.WindowManager.showResponsiveAndWait(setup, 560, 480);
            } catch (Exception error) { showFailure(error); return; }
        }
        if (!com.mycompany.lazar_app.ui.dialog.AdminAccess.ensure()) return;
        connect(true);
    }

    private void connect(boolean save) {
        if (busy) return;
        final DatabaseConfig config;
        try { config = buildConfig(); }
        catch (Exception error) { showFailure(error); return; }
        setBusy(true);
        statusLabel.setText("جاري اختبار الاتصال… / Testing connection…");
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override protected Void call() throws Exception {
                com.mycompany.lazar_app.database.DatabaseHealthCheck.check(config);
                if (save) DatabaseConfigManager.save(config);
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            setBusy(false);
            statusLabel.setText(save
                    ? "تم الحفظ. أعد الاتصال للمتابعة. / Saved. Retry to continue."
                    : "نجح الاتصال؛ لم تُنفّذ ترحيلات. / Connection successful; no migrations executed.");
            if (save && restartAfterSave) {
                new com.mycompany.lazar_app.Lazar_APP().start((javafx.stage.Stage) statusLabel.getScene().getWindow());
            }
        });
        task.setOnFailed(event -> {
            setBusy(false);
            showFailure(task.getException());
        });
        Thread thread = new Thread(task, "gymflow-connection-test");
        thread.setDaemon(true); thread.start();
    }

    private void showFailure(Throwable error) {
        com.mycompany.lazar_app.database.DatabaseDiagnostics.failure(error);
        statusLabel.setText(com.mycompany.lazar_app.database.DatabaseExceptionHandler.message(error) + "\n"
                + com.mycompany.lazar_app.database.DatabaseExceptionHandler.diagnostic(error));
    }

    private DatabaseConfig buildConfig() {
        DatabaseConfig c = new DatabaseConfig();
        DatabaseType type = databaseTypeCombo.getValue();
        if (type == null) throw new IllegalArgumentException("Select a database engine.");
        c.setType(type);
        c.setSqlitePath(sqlitePathField.getText().trim());

        if (type != DatabaseType.SQLITE) {
            if (hostField.getText().isBlank()) throw new IllegalArgumentException("Host is required.");
            if (databaseNameField.getText().isBlank()) throw new IllegalArgumentException("Database name is required.");
            c.setHost(hostField.getText().trim());
            c.setPort(Integer.parseInt(portField.getText().trim()));
            c.setDatabaseName(databaseNameField.getText().trim());
            c.setUsername(usernameField.getText().trim());
            c.setPassword(passwordField.getText());
            c.setPlatformCredentialStorage(platformCredentialCheckBox.isSelected());
            c.setEncryptConnection(encryptCheckBox.isSelected());
            c.setTrustStore(trustStoreField.getText().trim());
            c.setTrustStorePassword(trustStorePasswordField.getText());
            c.setTrustStoreType(trustStoreTypeCombo.getValue());
            c.setRsaPublicKey(rsaPublicKeyField.getText().trim());
        }
        com.mycompany.lazar_app.database.DatabaseConnectionManager.validate(c);
        return c;
    }
}

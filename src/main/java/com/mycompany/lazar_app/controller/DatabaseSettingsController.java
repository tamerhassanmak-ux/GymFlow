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

    @FXML
    public void initialize() {
        databaseTypeCombo.getItems().setAll(DatabaseType.values());
        databaseTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateVisibleFields(newValue);
            updateDefaultPort(oldValue, newValue);
        });
        loadConfiguration();
    }

    private void loadConfiguration() {
        DatabaseConfig c = DatabaseConfigManager.load();
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
        if (oldType == DatabaseType.SQL_SERVER && newType == DatabaseType.MYSQL) {
            portField.setText("3306");
        } else if (oldType == DatabaseType.MYSQL && newType == DatabaseType.SQL_SERVER) {
            portField.setText("1433");
        } else if (portField.getText() == null || portField.getText().isBlank()) {
            portField.setText(newType == DatabaseType.SQL_SERVER ? "1433" : "3306");
        }
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
    private void handleTestConnection() {
        try {
            DatabaseConfig config = buildConfig();
            boolean ok = DatabaseManager.testConnection(config);
            statusLabel.setText(ok ? "✓ Connection successful." : "✕ Connection failed.");
            statusLabel.setStyle(ok ? "-fx-text-fill: #15803D;" : "-fx-text-fill: #DC2626;");
        } catch (Exception e) {
            statusLabel.setText("✕ " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #DC2626;");
        }
    }

    @FXML
    private void handleSave() {
        try {
            DatabaseConfig config = buildConfig();

            if (!DatabaseManager.testConnection(config)) {
                statusLabel.setText("✕ Configuration was not saved because the connection failed.");
                statusLabel.setStyle("-fx-text-fill: #DC2626;");
                return;
            }

            DatabaseConfigManager.save(config);
            statusLabel.setText("✓ Database configuration saved successfully.");
            statusLabel.setStyle("-fx-text-fill: #15803D;");
        } catch (Exception e) {
            statusLabel.setText("✕ " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #DC2626;");
        }
    }

    private DatabaseConfig buildConfig() {
        DatabaseConfig c = new DatabaseConfig();
        DatabaseType type = databaseTypeCombo.getValue() == null
                ? DatabaseType.SQLITE : databaseTypeCombo.getValue();
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
            c.setEncryptConnection(encryptCheckBox.isSelected());
        }
        return c;
    }
}

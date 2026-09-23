package com.mycompany.lazar_app.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseManager {

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        return getConnection(DatabaseConfigManager.load());
    }

    public static Connection getConnection(DatabaseConfig config) throws SQLException {
        return switch (config.getType()) {
            case SQLITE -> connectSQLite(config);
            case MYSQL -> DriverManager.getConnection(
                    "jdbc:mysql://" + config.getHost() + ":" + config.getPort()
                            + "/" + config.getDatabaseName()
                            + "?useSSL=" + config.isEncryptConnection()
                            + "&serverTimezone=UTC",
                    config.getUsername(), config.getPassword());
            case SQL_SERVER -> DriverManager.getConnection(
                    "jdbc:sqlserver://" + config.getHost() + ":" + config.getPort()
                            + ";databaseName=" + config.getDatabaseName()
                            + ";encrypt=" + config.isEncryptConnection()
                            + ";trustServerCertificate=" + (!config.isEncryptConnection()),
                    config.getUsername(), config.getPassword());
        };
    }

    private static Connection connectSQLite(DatabaseConfig config) throws SQLException {
        Path dbPath = Paths.get(config.getSqlitePath()).toAbsolutePath().normalize();
        try {
            Path parent = dbPath.getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (Exception e) {
            throw new SQLException("Could not create SQLite database directory.", e);
        }
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    public static boolean testConnection(DatabaseConfig config) {
        try (Connection ignored = getConnection(config)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}

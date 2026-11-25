package com.mycompany.lazar_app.database;

import com.mycompany.lazar_app.util.AppPaths;
import java.nio.file.*;
import java.sql.*;
import java.util.UUID;

/** Backup gate used by every MigrationRunner entry point, including staged restores. */
public final class DatabaseMigrationManager {
    private DatabaseMigrationManager() {}

    public static void verifySchema(Connection connection) throws SQLException {
        for (var table : SqliteBackupService.REQUIRED.entrySet()) {
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(5);
                try (ResultSet ignored = statement.executeQuery("SELECT " + String.join(",", table.getValue())
                        + " FROM " + table.getKey() + " WHERE 1=0")) { /* metadata-only compatibility check */ }
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet ignored = statement.executeQuery("SELECT membership_id,installment_no,amount,due_date FROM payment_installments WHERE 1=0")) { }
    }

    public static void beforeChanges(Connection connection, DatabaseConfig config) throws Exception {
        if (config.getType() != DatabaseType.SQLITE) {
            // A copied file or an unverified dump is not a validated server backup.
            throw new SQLException("Server schema upgrade requires an independently restored native backup and a reviewed maintenance procedure. Automatic server DDL is blocked.", "GF001");
        }
        Path directory = AppPaths.backupsDirectory();
        Files.createDirectories(directory);
        Path backup = directory.resolve("pre-migration-" + UUID.randomUUID() + ".db");
        try {
            Files.createFile(backup, java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
        } catch (UnsupportedOperationException unsupported) {
            Files.createFile(backup); // Windows inherits the user's profile ACL.
        }
        int result = connection.unwrap(org.sqlite.SQLiteConnection.class).getDatabase().backup("main", backup.toString(), null);
        if (result != 0) throw new SQLException("Pre-migration snapshot failed.");
        try (Connection check = DriverManager.getConnection("jdbc:sqlite:" + backup.toUri() + "?mode=ro");
             Statement statement = check.createStatement()) {
            try (ResultSet rows = statement.executeQuery("PRAGMA integrity_check")) {
                if (!rows.next() || !"ok".equals(rows.getString(1)) || rows.next())
                    throw new SQLException("Pre-migration snapshot integrity failed.");
            }
            try (ResultSet rows = statement.executeQuery("PRAGMA foreign_key_check")) {
                if (rows.next()) throw new SQLException("Pre-migration snapshot has foreign key violations.");
            }
        }
    }
}

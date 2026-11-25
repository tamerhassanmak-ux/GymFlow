package com.mycompany.lazar_app.database;

import com.mycompany.lazar_app.database.migration.MigrationRunner;

public final class DatabaseStartup {
    private DatabaseStartup() {}
    public static void prepare() throws Exception {
        long start = System.nanoTime();
        DatabaseDiagnostics.initialize();
        try {
            DatabaseConfig config = DatabaseConfigManager.load();
            DatabaseDiagnostics.connecting(config.getType());
            DatabaseHealthCheck.Result result = DatabaseHealthCheck.checkWithRetry(config);
            MigrationRunner.migrate(config);
            try (var connection = DatabaseManager.getConnection(config)) {
                DatabaseMigrationManager.verifySchema(connection);
            }
            DatabaseDiagnostics.ready(result, (System.nanoTime() - start) / 1_000_000);
        } catch (Exception failure) {
            DatabaseDiagnostics.startupFailed((System.nanoTime() - start) / 1_000_000);
            throw failure;
        }
    }
}

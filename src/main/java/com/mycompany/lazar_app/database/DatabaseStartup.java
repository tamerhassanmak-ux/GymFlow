package com.mycompany.lazar_app.database;

import com.mycompany.lazar_app.database.migration.MigrationRunner;

public final class DatabaseStartup {
    private DatabaseStartup() {}
    public static void prepare() throws Exception {
        DatabaseDiagnostics.initialize();
        prepare(DatabaseConfigManager.load());
    }

    /** Prepare an explicit candidate without changing the currently selected database. */
    public static void prepare(DatabaseConfig config) throws Exception {
        long start = System.nanoTime();
        DatabaseDiagnostics.initialize();
        try {
            DatabaseConnectionManager.validate(config);
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

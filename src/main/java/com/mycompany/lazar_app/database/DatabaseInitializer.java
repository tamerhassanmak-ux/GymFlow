package com.mycompany.lazar_app.database;

/** Compatibility entry point; connection setup and validation have a single owner. */
public final class DatabaseInitializer {
    private DatabaseInitializer() {}
    public static void initialize() {
        try { DatabaseHealthCheck.check(DatabaseConfigManager.load()); }
        catch (java.sql.SQLException failure) {
            throw new IllegalStateException("Database initialization failed; use startup recovery.", failure);
        }
    }
}

package com.mycompany.lazar_app.database;

import java.sql.*;

public final class DatabaseHealthCheck {
    public record Result(String engine, String serverVersion, String driverVersion) {}
    private DatabaseHealthCheck() {}
    @FunctionalInterface interface Probe { Result check() throws SQLException; }
    @FunctionalInterface interface Pause { void sleep(long millis) throws InterruptedException; }
    public static Result checkWithRetry(DatabaseConfig config) throws SQLException {
        return retry(() -> check(config), Thread::sleep);
    }
    static Result retry(Probe probe, Pause pause) throws SQLException {
        try { return probe.check(); }
        catch (SQLTransientConnectionException | SQLTimeoutException failure) {
            // Only a closed read-only probe is retried, once. Authentication/TLS/schema errors never retry.
            DatabaseExceptionHandler.Category category = DatabaseExceptionHandler.classify(failure);
            if (category != DatabaseExceptionHandler.Category.NETWORK && category != DatabaseExceptionHandler.Category.TIMEOUT)
                throw failure;
            try { pause.sleep(250); }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new SQLException("Readiness retry interrupted.", "HY008", interrupted);
            }
            return probe.check();
        }
    }
    /** Read-only preflight. No retries of writes, migrations or uncertain commits. */
    public static Result check(DatabaseConfig config) throws SQLException {
        DatabaseConnectionManager.validate(config);
        DatabaseConnectionManager.verifyDriver(config);
        try (Connection connection = DatabaseManager.getConnection(config)) {
            if (!connection.isValid(5)) throw new SQLTimeoutException("Connection validation failed.", "HYT00");
            DatabaseMetaData meta = connection.getMetaData();
            return new Result(meta.getDatabaseProductName(), meta.getDatabaseProductVersion(), meta.getDriverVersion());
        }
    }
}

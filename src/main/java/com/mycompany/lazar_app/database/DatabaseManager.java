package com.mycompany.lazar_app.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;
import java.io.IOException;

public final class DatabaseManager {

    private DatabaseManager() {}

    private static final StampedLock CONNECTIONS = new StampedLock();
    @FunctionalInterface
    public interface ExclusiveAction {
        void execute(Connection connection) throws SQLException, IOException;
    }

    public static void withExclusiveConnection(DatabaseConfig config, ExclusiveAction action)
            throws SQLException, IOException {
        long stamp;
        try {
            stamp = CONNECTIONS.tryWriteLock(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SQLException("Recovery interrupted before changing the database.", exception);
        }
        if (stamp == 0) throw new SQLException("Database is in use; close pending operations before restoring.");
        try (Connection connection = openConnection(config)) {
            action.execute(connection);
        } finally {
            CONNECTIONS.unlockWrite(stamp);
        }
    }

    @FunctionalInterface
    public interface TransactionAction<T> { T execute() throws Exception; }
    private static final ThreadLocal<TransactionContext> TRANSACTION = new ThreadLocal<>();
    private static final class TransactionContext {
        final Connection connection;
        final String databaseKey;
        boolean rollbackOnly;
        TransactionContext(Connection connection, DatabaseConfig config) {
            this.connection = connection; this.databaseKey = databaseKey(config);
        }
    }

    /** Repository-local transactions join this unit of work without committing it early. */
    public static <T> T inTransaction(TransactionAction<T> action) throws Exception {
        if (TRANSACTION.get() != null) {
            try { return action.execute(); }
            catch (Exception | Error exception) { TRANSACTION.get().rollbackOnly = true; throw exception; }
        }
        DatabaseConfig config = DatabaseConfigManager.load();
        try (Connection connection = getConnection(config)) {
            connection.setAutoCommit(false);
            TransactionContext context = new TransactionContext(connection, config);
            TRANSACTION.set(context);
            try {
                T value = action.execute();
                if (context.rollbackOnly) throw new SQLException("A nested operation marked the transaction for rollback.");
                connection.commit();
                return value;
            } catch (Exception | Error exception) {
                try { connection.rollback(); } catch (SQLException rollback) { exception.addSuppressed(rollback); }
                throw exception;
            } finally {
                TRANSACTION.remove();
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        return getConnection(DatabaseConfigManager.load());
    }

    public static Connection getConnection(DatabaseConfig config) throws SQLException {
        TransactionContext transaction = TRANSACTION.get();
        if (transaction != null) {
            if (!transaction.databaseKey.equals(databaseKey(config))) {
                throw new SQLException("A transaction cannot span different configured databases.");
            }
            return (Connection) Proxy.newProxyInstance(DatabaseManager.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "close", "commit", "setAutoCommit": return null;
                            case "rollback": transaction.rollbackOnly = true; return null;
                            default:
                                try { return method.invoke(transaction.connection, args); }
                                catch (InvocationTargetException exception) { throw exception.getCause(); }
                        }
                    });
        }
        long stamp = CONNECTIONS.readLock();
        try {
            Connection connection = openConnection(config);
            AtomicBoolean closed = new AtomicBoolean();
            return (Connection) Proxy.newProxyInstance(DatabaseManager.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        if (method.getName().equals("close")) {
                            if (closed.compareAndSet(false, true)) {
                                try { connection.close(); } finally { CONNECTIONS.unlockRead(stamp); }
                            }
                            return null;
                        }
                        try { return method.invoke(connection, args); }
                        catch (InvocationTargetException exception) { throw exception.getCause(); }
                    });
        } catch (SQLException | RuntimeException exception) {
            CONNECTIONS.unlockRead(stamp);
            throw exception;
        }
    }

    private static String databaseKey(DatabaseConfig config) {
        if (config == null || config.getType() == null) throw new IllegalArgumentException("Database configuration is required.");
        return config.getType() == DatabaseType.SQLITE
                ? "SQLITE:" + Path.of(config.getSqlitePath()).toAbsolutePath().normalize()
                : config.getType() + ":" + config.getHost() + ":" + config.getPort() + ":" + config.getDatabaseName();
    }

    private static Connection openConnection(DatabaseConfig config) throws SQLException {
        if (config == null || config.getType() == null) throw new IllegalArgumentException("Database configuration is required.");
        return switch (config.getType()) {
            case SQLITE -> connectSQLite(config);
            case MYSQL -> DriverManager.getConnection(
                    "jdbc:mysql://" + config.getHost() + ":" + config.getPort()
                            + "/" + config.getDatabaseName()
                            + "?sslMode=" + (config.isEncryptConnection() ? "VERIFY_IDENTITY" : "DISABLED")
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
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (var statement = connection.createStatement()) {
            // These settings are connection scoped, so initialization alone
            // cannot protect connections opened later by repositories.
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    public static boolean testConnection(DatabaseConfig config) {
        try (Connection connection = getConnection(config)) {
            return connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
}

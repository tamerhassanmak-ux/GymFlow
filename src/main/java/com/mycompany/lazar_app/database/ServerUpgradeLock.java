package com.mycompany.lazar_app.database;

import java.sql.*;

/** Serializes startup upgrades across cooperating application processes. */
public final class ServerUpgradeLock implements AutoCloseable {
    private final Connection connection;
    private final String name;
    private ServerUpgradeLock(Connection connection, String name) { this.connection = connection; this.name = name; }
    public static ServerUpgradeLock acquire(Connection connection, DatabaseConfig config) throws SQLException {
        if (config.getType() != DatabaseType.MYSQL && config.getType() != DatabaseType.MARIADB)
            return new ServerUpgradeLock(connection, null);
        String name = "gymflow-upgrade-" + java.util.HexFormat.of().formatHex(
                digest(config.getDatabaseName())).substring(0, 40);
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, 30)")) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next() || rows.getInt(1) != 1 || rows.wasNull())
                    throw new SQLTimeoutException("Another application instance is upgrading this database.", "HYT00");
            }
        }
        return new ServerUpgradeLock(connection, name);
    }
    private static byte[] digest(String name) {
        try { return java.security.MessageDigest.getInstance("SHA-256").digest(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    @Override public void close() throws SQLException {
        if (name == null) return;
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, name); statement.execute();
        }
    }
}

package com.mycompany.lazar_app.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;

/** Driver-specific connection construction. Never retries statements or transactions. */
public final class DatabaseConnectionManager {
    private DatabaseConnectionManager() {}

    public static boolean isLiteralLoopback(String host) {
        // No DNS-based security decision: aliases, alternate IPv4 forms and multi-host URLs are rejected.
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                || "::1".equals(host) || "[::1]".equals(host);
    }

    public static void validate(DatabaseConfig c) {
        if (c == null || c.getType() == null) throw new IllegalArgumentException("Database engine is required.");
        if (c.getType() == DatabaseType.SQLITE) {
            if (c.getSqlitePath() == null || c.getSqlitePath().isBlank())
                throw new IllegalArgumentException("SQLite path is required.");
            Path.of(c.getSqlitePath());
            return;
        }
        String host = c.getHost();
        if (host == null || host.isBlank() || !(host.matches("[A-Za-z0-9._-]+")
                || host.matches("[0-9a-fA-F]*:[0-9a-fA-F:]+") || host.matches("\\[[0-9a-fA-F]*:[0-9a-fA-F:]+\\]")))
            throw new IllegalArgumentException("Use one hostname or IP address without JDBC options.");
        if (c.getPort() < 1 || c.getPort() > 65535) throw new IllegalArgumentException("Port must be 1–65535.");
        if (c.getDatabaseName() == null || !c.getDatabaseName().matches("[\\p{L}\\p{N}_ -]+"))
            throw new IllegalArgumentException("Database name contains unsupported characters.");
        if (!c.isEncryptConnection() && !isLiteralLoopback(host))
            throw new IllegalArgumentException("TLS may only be disabled explicitly for localhost, 127.0.0.1 or ::1.");
        if (c.getConnectTimeoutMillis() < 1000 || c.getConnectTimeoutMillis() > 60000
                || c.getSocketTimeoutMillis() < 1000 || c.getSocketTimeoutMillis() > 120000)
            throw new IllegalArgumentException("Connection/read timeouts are outside the permitted range.");
        if (!c.getTrustStore().isBlank() && !Files.isRegularFile(Path.of(c.getTrustStore())))
            throw new IllegalArgumentException("Truststore file does not exist.");
        if (!c.getTrustStoreType().equals("PKCS12") && !c.getTrustStoreType().equals("JKS"))
            throw new IllegalArgumentException("Truststore type must be PKCS12 or JKS.");
        if (!c.getRsaPublicKey().isBlank() && !Files.isRegularFile(Path.of(c.getRsaPublicKey())))
            throw new IllegalArgumentException("RSA server public key file does not exist.");
    }

    public static String jdbcUrl(DatabaseConfig c) {
        validate(c);
        String host = c.getHost();
        if (host.contains(":" ) && !host.startsWith("[")) host = "[" + host + "]";
        return switch (c.getType()) {
            case SQLITE -> "jdbc:sqlite:" + Path.of(c.getSqlitePath()).toAbsolutePath().normalize();
            case MYSQL -> "jdbc:mysql://" + host + ":" + c.getPort() + "/"
                    + java.net.URLEncoder.encode(c.getDatabaseName(), java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
            case MARIADB -> "jdbc:mariadb://" + host + ":" + c.getPort() + "/"
                    + java.net.URLEncoder.encode(c.getDatabaseName(), java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
            case SQL_SERVER -> "jdbc:sqlserver://" + host + ":" + c.getPort();
        };
    }

    public static Properties properties(DatabaseConfig c) {
        validate(c);
        Properties p = new Properties();
        p.setProperty("user", c.getUsername());
        p.setProperty("password", c.getPassword());
        switch (c.getType()) {
            case MYSQL -> {
                p.setProperty("sslMode", c.isEncryptConnection() ? "VERIFY_IDENTITY" : "DISABLED");
                p.setProperty("allowPublicKeyRetrieval", "false");
                p.setProperty("tlsVersions", "TLSv1.2,TLSv1.3");
                p.setProperty("connectTimeout", "" + c.getConnectTimeoutMillis());
                p.setProperty("socketTimeout", "" + c.getSocketTimeoutMillis());
                p.setProperty("serverTimezone", "UTC");
                if (!c.getTrustStore().isBlank()) {
                    p.setProperty("trustCertificateKeyStoreUrl", Path.of(c.getTrustStore()).toUri().toString());
                    p.setProperty("trustCertificateKeyStorePassword", c.getTrustStorePassword());
                    p.setProperty("trustCertificateKeyStoreType", c.getTrustStoreType());
                    p.setProperty("fallbackToSystemTrustStore", "false");
                }
                if (!c.getRsaPublicKey().isBlank()) p.setProperty("serverRSAPublicKeyFile", c.getRsaPublicKey());
            }
            case MARIADB -> {
                p.setProperty("sslMode", c.isEncryptConnection() ? "verify-full" : "disable");
                p.setProperty("allowPublicKeyRetrieval", "false");
                p.setProperty("enabledSslProtocolSuites", "TLSv1.2,TLSv1.3");
                p.setProperty("connectTimeout", "" + c.getConnectTimeoutMillis());
                p.setProperty("socketTimeout", "" + c.getSocketTimeoutMillis());
                if (!c.getTrustStore().isBlank()) {
                    p.setProperty("trustStore", c.getTrustStore());
                    p.setProperty("trustStorePassword", c.getTrustStorePassword());
                }
                if (!c.getRsaPublicKey().isBlank()) p.setProperty("serverRsaPublicKeyFile", c.getRsaPublicKey());
            }
            case SQL_SERVER -> {
                p.setProperty("databaseName", c.getDatabaseName());
                p.setProperty("encrypt", Boolean.toString(c.isEncryptConnection()));
                p.setProperty("trustServerCertificate", "false");
                p.setProperty("sslProtocol", "TLS");
                p.setProperty("loginTimeout", "" + ((c.getConnectTimeoutMillis() + 999) / 1000));
                p.setProperty("socketTimeout", "" + c.getSocketTimeoutMillis());
                if (!c.getTrustStore().isBlank()) {
                    p.setProperty("trustStore", c.getTrustStore());
                    p.setProperty("trustStorePassword", c.getTrustStorePassword());
                    p.setProperty("trustStoreType", c.getTrustStoreType());
                }
            }
            default -> { }
        }
        return p;
    }

    public static void verifyDriver(DatabaseConfig c) throws SQLException {
        String driver = switch (c.getType()) {
            case SQLITE -> "org.sqlite.JDBC";
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case MARIADB -> "org.mariadb.jdbc.Driver";
            case SQL_SERVER -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        };
        try { Class.forName(driver); }
        catch (ClassNotFoundException | LinkageError e) { throw new SQLException("Required JDBC driver is unavailable.", "IM003", e); }
    }

    static Connection openServer(DatabaseConfig c) throws SQLException {
        verifyDriver(c);
        return DriverManager.getConnection(jdbcUrl(c), properties(c));
    }
}

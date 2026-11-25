package com.mycompany.lazar_app.database;

import com.mycompany.lazar_app.util.AppPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DatabaseConfigManager {

    private static final Path CONFIG_FILE =
            AppPaths.databaseConfigFile();

    private static String sessionPassword;
    private static String sessionTrustStorePassword;
    private static String sessionProfile;

    private DatabaseConfigManager() {
    }

    // =========================================================
    // LOAD DATABASE CONFIGURATION
    // =========================================================

    public static synchronized DatabaseConfig load() { return read(true); }

    /** Recovery may inspect existing fields without opening the vault or relaxing active connection validation. */
    public static synchronized DatabaseConfig loadForEditing() { return read(false); }

    private static DatabaseConfig read(boolean active) {

        DatabaseConfig config = new DatabaseConfig();

        /*
         * IMPORTANT:
         *
         * GymFlow no longer searches for or automatically imports
         * legacy Lazar_APP databases.
         *
         * Production data is stored in the per-user GymFlow
         * application data directory.
         *
         * Legacy database importing, if ever needed, must be done
         * explicitly through a dedicated import/restore operation.
         */

        if (!Files.exists(CONFIG_FILE)) {
            if (Files.exists(AppPaths.legacyDatabaseConfigFile()) || Files.exists(AppPaths.legacyRelativeDatabaseFile())
                    || Files.exists(AppPaths.databaseFile())) {
                throw new IllegalStateException("Existing database/configuration detected without active settings. Explicit administrator recovery is required.");
            }


            /*
             * DatabaseConfig already contains the correct
             * platform-specific default SQLite path.
             */
            save(config);

            return config;
        }

        Properties properties = new Properties();

        try (InputStream input =
                     Files.newInputStream(CONFIG_FILE)) {

            properties.load(input);

            // -------------------------------------------------
            // DATABASE TYPE
            // -------------------------------------------------

            config.setType(
                    parseType(
                            properties.getProperty("type", "")
                    )
            );

            // -------------------------------------------------
            // SQLITE
            // -------------------------------------------------

            String sqlitePath =
                    properties
                            .getProperty(
                                    "sqlitePath",
                                    ""
                            )
                            .trim();

            if (sqlitePath.isBlank()) {

                sqlitePath =
                        AppPaths
                                .databaseFile()
                                .toString();
            }

            config.setSqlitePath(sqlitePath);

            // -------------------------------------------------
            // SERVER DATABASE SETTINGS
            // -------------------------------------------------

            config.setHost(
                    properties.getProperty(
                            "host",
                            "localhost"
                    )
            );

            config.setPort(
                    parsePort(
                            properties.getProperty("port"),
                            defaultPort(config.getType())
                    )
            );

            config.setDatabaseName(
                    properties.getProperty(
                            "databaseName",
                            "gymflow"
                    )
            );

            config.setUsername(
                    properties.getProperty(
                            "username",
                            ""
                    )
            );

            if (!profileIdentity(config).equals(sessionProfile)) {
                sessionPassword = null;
                sessionTrustStorePassword = null;
            }
            String environmentPassword = System.getenv("GYMFLOW_DB_PASSWORD");
            config.setPassword(environmentPassword != null ? environmentPassword
                    : sessionPassword != null ? sessionPassword : properties.getProperty("password", ""));

            config.setEncryptConnection(
                    parseBoolean(
                            properties.getProperty(
                                    "encryptConnection",
                                    "true"
                            )
                    )
            );

            config.setTrustStore(properties.getProperty("trustStore", ""));
            config.setTrustStoreType(properties.getProperty("trustStoreType", "PKCS12"));
            config.setRsaPublicKey(properties.getProperty("rsaPublicKey", ""));
            String environmentTrustPassword = System.getenv("GYMFLOW_DB_TRUSTSTORE_PASSWORD");
            config.setTrustStorePassword(environmentTrustPassword != null ? environmentTrustPassword
                    : sessionTrustStorePassword != null ? sessionTrustStorePassword : "");
            config.setConnectTimeoutMillis(Integer.parseInt(properties.getProperty("connectTimeoutMillis", "5000")));
            config.setSocketTimeoutMillis(Integer.parseInt(properties.getProperty("socketTimeoutMillis", "15000")));
            config.setPlatformCredentialStorage(parseBoolean(properties.getProperty("platformCredentialStorage", "false")));
            if (active && config.isPlatformCredentialStorage() && ((environmentPassword == null && sessionPassword == null)
                    || (!config.getTrustStore().isBlank() && sessionTrustStorePassword == null
                    && System.getenv("GYMFLOW_DB_TRUSTSTORE_PASSWORD") == null))) {
                PlatformCredentialStore.Secrets secrets = PlatformCredentialStore.load(config);
                if (environmentPassword == null && sessionPassword == null) {
                    sessionPassword = secrets.password(); config.setPassword(sessionPassword);
                }
                if (sessionTrustStorePassword == null && System.getenv("GYMFLOW_DB_TRUSTSTORE_PASSWORD") == null)
                    { sessionTrustStorePassword = secrets.trustStorePassword(); config.setTrustStorePassword(sessionTrustStorePassword); }
            }
            sessionProfile = profileIdentity(config);
            if (active) DatabaseConnectionManager.validate(config);
            return config;

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Could not load GymFlow database configuration.",
                    exception
            );
        }
    }

    // =========================================================
    // SAVE DATABASE CONFIGURATION
    // =========================================================

    public static synchronized void save(
            DatabaseConfig config
    ) {

        if (Files.exists(CONFIG_FILE)) {
            com.mycompany.lazar_app.security.AuthorizationService.requireAdmin();
        }

        if (config == null) {

            throw new IllegalArgumentException(
                    "Database configuration cannot be null."
            );
        }

        try {

            Path parent =
                    CONFIG_FILE.getParent();

            if (parent != null) {

                Files.createDirectories(parent);
            }

            DatabaseConnectionManager.validate(config);
            Properties properties = new Properties();
            properties.setProperty("platformCredentialStorage", Boolean.toString(config.isPlatformCredentialStorage()));
            if (config.isPlatformCredentialStorage()) PlatformCredentialStore.save(config);
            properties.setProperty("trustStore", nullToEmpty(config.getTrustStore()));
            properties.setProperty("trustStoreType", nullToEmpty(config.getTrustStoreType()));
            properties.setProperty("rsaPublicKey", nullToEmpty(config.getRsaPublicKey()));
            properties.setProperty("connectTimeoutMillis", Integer.toString(config.getConnectTimeoutMillis()));
            properties.setProperty("socketTimeoutMillis", Integer.toString(config.getSocketTimeoutMillis()));

            // -------------------------------------------------
            // DATABASE TYPE
            // -------------------------------------------------

            DatabaseType databaseType =
                    config.getType() == null
                            ? DatabaseType.SQLITE
                            : config.getType();

            properties.setProperty(
                    "type",
                    databaseType.name()
            );

            // -------------------------------------------------
            // SQLITE PATH
            // -------------------------------------------------

            String sqlitePath =
                    config.getSqlitePath();

            if (sqlitePath == null
                    || sqlitePath.isBlank()) {

                sqlitePath =
                        AppPaths
                                .databaseFile()
                                .toString();

                config.setSqlitePath(sqlitePath);
            }

            properties.setProperty(
                    "sqlitePath",
                    sqlitePath
            );

            // -------------------------------------------------
            // SERVER DATABASE SETTINGS
            // -------------------------------------------------

            properties.setProperty(
                    "host",
                    nullToEmpty(
                            config.getHost()
                    )
            );

            properties.setProperty(
                    "port",
                    String.valueOf(
                            config.getPort()
                    )
            );

            properties.setProperty(
                    "databaseName",
                    nullToEmpty(
                            config.getDatabaseName()
                    )
            );

            properties.setProperty(
                    "username",
                    nullToEmpty(
                            config.getUsername()
                    )
            );

            // Passwords use the optional platform store or remain session/environment-only.


            properties.setProperty(
                    "encryptConnection",
                    String.valueOf(
                            config.isEncryptConnection()
                    )
            );

            // -------------------------------------------------
            // WRITE FILE
            // -------------------------------------------------

            com.mycompany.lazar_app.util.AtomicProperties.write(
                    CONFIG_FILE, properties, "GymFlow Database Configuration");
            sessionPassword = nullToEmpty(config.getPassword());
            sessionTrustStorePassword = nullToEmpty(config.getTrustStorePassword());
            sessionProfile = profileIdentity(config);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Could not save GymFlow database configuration.",
                    exception
            );
        }
    }

    // =========================================================
    // CONFIG FILE
    // =========================================================

    public static Path getConfigFile() {

        return CONFIG_FILE;
    }

    // =========================================================
    // PARSE DATABASE TYPE
    // =========================================================

    private static DatabaseType parseType(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException("Database engine is missing.");
        }

        try {

            return DatabaseType.valueOf(
                    value
                            .trim()
                            .toUpperCase()
            );

        } catch (IllegalArgumentException exception) {

            throw new IllegalArgumentException("Unknown database engine; configuration was not changed.", exception);
        }
    }

    // =========================================================
    // PARSE PORT
    // =========================================================

    private static int parsePort(
            String value,
            int fallback
    ) {

        if (value == null
                || value.isBlank()) {

            return fallback;
        }

        try {

            int port =
                    Integer.parseInt(
                            value.trim()
                    );

            if (port < 1
                    || port > 65535) {

                throw new IllegalArgumentException("Port must be between 1 and 65535.");
            }

            return port;

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException("Invalid database port.", exception);
        }
    }

    // =========================================================
    // DEFAULT PORT
    // =========================================================

    private static int defaultPort(
            DatabaseType type
    ) {

        if (type == DatabaseType.SQL_SERVER) {

            return 1433;
        }

        if (type == DatabaseType.MYSQL || type == DatabaseType.MARIADB) {

            return 3306;
        }

        /*
         * SQLite does not use a network port.
         * Keep zero as the internal default.
         */
        return 0;
    }

    // =========================================================
    // NULL SAFE STRING
    // =========================================================

    private static String profileIdentity(DatabaseConfig config) {
        return config.getType() + "\n" + config.getHost() + "\n" + config.getPort() + "\n"
                + config.getDatabaseName() + "\n" + config.getUsername();
    }

    private static boolean parseBoolean(String value) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value))
            throw new IllegalArgumentException("Invalid boolean database setting.");
        return Boolean.parseBoolean(value);
    }

    private static String nullToEmpty(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }
}

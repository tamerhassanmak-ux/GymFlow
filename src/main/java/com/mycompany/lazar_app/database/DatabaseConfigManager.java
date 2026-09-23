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

    private DatabaseConfigManager() {
    }

    // =========================================================
    // LOAD DATABASE CONFIGURATION
    // =========================================================

    public static synchronized DatabaseConfig load() {

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
                            properties.getProperty(
                                    "type",
                                    "SQLITE"
                            )
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

            config.setPassword(
                    properties.getProperty(
                            "password",
                            ""
                    )
            );

            config.setEncryptConnection(
                    Boolean.parseBoolean(
                            properties.getProperty(
                                    "encryptConnection",
                                    "false"
                            )
                    )
            );

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

            Properties properties =
                    new Properties();

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

            /*
             * SECURITY NOTE:
             *
             * Password storage should later be moved to the
             * operating system credential/keyring service.
             *
             * Do not print this value to logs.
             */
            properties.setProperty(
                    "password",
                    nullToEmpty(
                            config.getPassword()
                    )
            );

            properties.setProperty(
                    "encryptConnection",
                    String.valueOf(
                            config.isEncryptConnection()
                    )
            );

            // -------------------------------------------------
            // WRITE FILE
            // -------------------------------------------------

            try (OutputStream output =
                         Files.newOutputStream(CONFIG_FILE)) {

                properties.store(
                        output,
                        "GymFlow Database Configuration"
                );
            }

        } catch (IOException exception) {

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

            return DatabaseType.SQLITE;
        }

        try {

            return DatabaseType.valueOf(
                    value
                            .trim()
                            .toUpperCase()
            );

        } catch (IllegalArgumentException exception) {

            return DatabaseType.SQLITE;
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

                return fallback;
            }

            return port;

        } catch (NumberFormatException exception) {

            return fallback;
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

        if (type == DatabaseType.MYSQL) {

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

    private static String nullToEmpty(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }
}
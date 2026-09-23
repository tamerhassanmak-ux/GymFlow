package com.mycompany.lazar_app.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }


    // =========================================================
    // INITIALIZE DATABASE CONNECTION
    // =========================================================

    /**
     * Initializes and validates the configured database.
     *
     * Responsibilities:
     *
     * 1. Load database configuration.
     * 2. Open a database connection.
     * 3. Configure connection-specific settings.
     * 4. Verify that the connection is working.
     *
     * IMPORTANT:
     *
     * This class does NOT create or modify application tables.
     *
     * Database schema creation and modification are handled
     * exclusively by MigrationRunner.
     */
    public static void initialize() {

        DatabaseConfig config =
                DatabaseConfigManager.load();

        try (Connection connection =
                     DatabaseManager.getConnection(config)) {

            configureConnection(
                    connection,
                    config
            );

            validateConnection(
                    connection
            );

            System.out.println(
                    "Database connection initialized successfully using: "
                            + config.getType()
            );

        } catch (SQLException exception) {

            System.err.println(
                    "Database initialization failed."
            );

            throw new IllegalStateException(
                    "Unable to initialize database: "
                            + exception.getMessage(),
                    exception
            );
        }
    }


    // =========================================================
    // CONNECTION CONFIGURATION
    // =========================================================

    private static void configureConnection(
            Connection connection,
            DatabaseConfig config
    ) throws SQLException {

        if (config.getType()
                == DatabaseType.SQLITE) {

            configureSQLite(
                    connection
            );
        }
    }


    // =========================================================
    // SQLITE CONFIGURATION
    // =========================================================

    private static void configureSQLite(
            Connection connection
    ) throws SQLException {

        try (Statement statement =
                     connection.createStatement()) {

            // Enable foreign key constraints.
            statement.execute(
                    "PRAGMA foreign_keys = ON"
            );

            // Wait up to 5 seconds if database is temporarily locked.
            statement.execute(
                    "PRAGMA busy_timeout = 5000"
            );
        }
    }


    // =========================================================
    // VALIDATE CONNECTION
    // =========================================================

    private static void validateConnection(
            Connection connection
    ) throws SQLException {

        if (connection == null) {

            throw new SQLException(
                    "Database connection is null."
            );
        }

        if (connection.isClosed()) {

            throw new SQLException(
                    "Database connection is closed."
            );
        }

        /*
         * JDBC validation.
         *
         * Timeout = 5 seconds.
         *
         * Some JDBC drivers may not fully support isValid(),
         * therefore failure of the method itself is not treated
         * as a schema-related operation.
         */

        try {

            if (!connection.isValid(5)) {

                throw new SQLException(
                        "Database connection validation failed."
                );
            }

        } catch (AbstractMethodError
                 | UnsupportedOperationException exception) {

            // Driver does not support isValid().
            // Successfully opening the connection is enough.
        }
    }
}
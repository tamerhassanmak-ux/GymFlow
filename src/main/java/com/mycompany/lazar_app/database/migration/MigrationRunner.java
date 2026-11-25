package com.mycompany.lazar_app.database.migration;

import com.mycompany.lazar_app.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class MigrationRunner {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private MigrationRunner() {
    }

    private static final List<Migration> MIGRATIONS =
            List.of(
                    new M001CreateMembersTable(),
                    new M002CreateMembershipPlansTable(),
                    new M003CreateMemberMembershipsTable(),
                    new M004CreateCheckInsTable(),
                    new M005CreatePaymentsTable(),
                    new M006EnsureSessionLimitColumns(),
                    new M007CreateExpensesTable(),
                    new M008CreateRevenuesTable(),
                    new M009EnsureRevenueTables(),
                    new M010EnforceMemberPhoneUniqueness(),
                    new M011ProtectFinancialOwnership(),
                    new M012CreatePaymentInstallments(),
                    new M013PreventDuplicateSubmissions(),
                    new M014IndexMembershipLookups()
            );

    public static void migrate() throws Exception {
        migrate(com.mycompany.lazar_app.database.DatabaseConfigManager.load());
    }

    /** Upgrade an explicitly supplied disposable/staged database without changing active settings. */
    public static void migrate(com.mycompany.lazar_app.database.DatabaseConfig config) throws Exception {
        try (Connection connection = DatabaseManager.getConnection(config)) {

            // Read the ledger before any DDL, reject future/unknown versions.
            boolean ledgerExists = MigrationSupport.tableExists(connection, "schema_migrations");
            var known = MIGRATIONS.stream().map(Migration::version).collect(java.util.stream.Collectors.toSet());
            if (known.size() != MIGRATIONS.size()) throw new IllegalStateException("Duplicate migration versions.");
            if (ledgerExists) {
                try (Statement statement = connection.createStatement();
                     ResultSet rows = statement.executeQuery("SELECT version FROM schema_migrations")) {
                    while (rows.next()) if (!known.contains(rows.getString(1)))
                        throw new java.sql.SQLException("Database has an unsupported migration version.", "GF002");
                }
            }
            boolean pending = !ledgerExists;
            if (ledgerExists) for (Migration migration : MIGRATIONS)
                if (!isApplied(connection, migration.version())) pending = true;
            if (!pending) return;
            com.mycompany.lazar_app.database.DatabaseMigrationManager.beforeChanges(connection, config);

            boolean oldAutoCommit =
                    connection.getAutoCommit();

            try {

                connection.setAutoCommit(false);

                enableSQLiteForeignKeys(connection);

                createMigrationsTable(connection);

                for (Migration migration : MIGRATIONS) {

                    if (isApplied(
                            connection,
                            migration.version())) {

                        com.mycompany.lazar_app.database.DatabaseDiagnostics.migration(migration.version(), "skip");
                        System.out.println(
                                "[SKIP] "
                                        + migration.version()
                                        + " "
                                        + migration.description()
                        );

                        continue;
                    }

                    com.mycompany.lazar_app.database.DatabaseDiagnostics.migration(migration.version(), "run");
                    System.out.println(
                            "[RUN ] "
                                    + migration.version()
                                    + " "
                                    + migration.description()
                    );

                    try {

                        migration.up(connection);

                        recordMigration(
                                connection,
                                migration
                        );

                        connection.commit();
                        com.mycompany.lazar_app.database.DatabaseDiagnostics.migration(migration.version(), "ok");

                        System.out.println(
                                "[ OK ] "
                                        + migration.version()
                                        + " "
                                        + migration.description()
                        );

                    } catch (Exception migrationException) {
                        com.mycompany.lazar_app.database.DatabaseDiagnostics.migration(migration.version(), "failed");

                        connection.rollback();

                        throw new Exception(
                                "Migration "
                                        + migration.version()
                                        + " failed: "
                                        + migration.description()
                                        + " -> "
                                        + migrationException.getMessage(),
                                migrationException
                        );
                    }
                }

                System.out.println(
                        "Database migrations completed successfully."
                );

            } catch (Exception e) {

                try {
                    connection.rollback();
                } catch (Exception ignored) {
                }

                throw new Exception(
                        "Database migration failed: "
                                + e.getMessage(),
                        e
                );

            } finally {

                try {
                    connection.setAutoCommit(
                            oldAutoCommit
                    );
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void createMigrationsTable(
            Connection connection) throws Exception {

        if (MigrationSupport.tableExists(
                connection,
                "schema_migrations")) {

            return;
        }

        String sql;

        if (MigrationSupport.isSQLite(connection)) {

            sql = """
                    CREATE TABLE schema_migrations (
                        version VARCHAR(50) PRIMARY KEY,
                        description VARCHAR(255) NOT NULL,
                        applied_at TEXT NOT NULL
                    )
                    """;

        } else if (MigrationSupport.isMySQL(connection)) {

            sql = """
                    CREATE TABLE schema_migrations (
                        version VARCHAR(50) PRIMARY KEY,
                        description VARCHAR(255) NOT NULL,
                        applied_at VARCHAR(30) NOT NULL
                    )
                    """;

        } else if (
                MigrationSupport.isSqlServer(connection)) {

            sql = """
                    CREATE TABLE schema_migrations (
                        version NVARCHAR(50) PRIMARY KEY,
                        description NVARCHAR(255) NOT NULL,
                        applied_at NVARCHAR(30) NOT NULL
                    )
                    """;

        } else {

            throw new IllegalStateException(
                    "Unsupported database: "
                            + connection
                            .getMetaData()
                            .getDatabaseProductName()
            );
        }

        MigrationSupport.execute(
                connection,
                sql
        );
    }

    private static boolean isApplied(
            Connection connection,
            String version) throws Exception {

        String sql = """
                SELECT COUNT(*)
                FROM schema_migrations
                WHERE version = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    version
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                return rs.next()
                        && rs.getInt(1) > 0;
            }
        }
    }

    private static void recordMigration(
            Connection connection,
            Migration migration) throws Exception {

        String sql = """
                INSERT INTO schema_migrations
                (
                    version,
                    description,
                    applied_at
                )
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    migration.version()
            );

            statement.setString(
                    2,
                    migration.description()
            );

            statement.setString(
                    3,
                    LocalDateTime.now()
                            .format(DATE_TIME)
            );

            statement.executeUpdate();
        }
    }

    private static void enableSQLiteForeignKeys(
            Connection connection) throws Exception {

        if (!MigrationSupport.isSQLite(connection)) {
            return;
        }

        try (Statement statement =
                     connection.createStatement()) {

            statement.execute(
                    "PRAGMA foreign_keys = ON"
            );
        }
    }

    public static void main(String[] args) {

        try {

            migrate();

        } catch (Exception e) {

            com.mycompany.lazar_app.database.DatabaseDiagnostics.failure(e);

            System.exit(1);
        }
    }
}

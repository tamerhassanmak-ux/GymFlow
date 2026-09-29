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
                    new M009EnsureRevenueTables()
            );

    public static void migrate() throws Exception {

        try (Connection connection =
                     DatabaseManager.getConnection()) {

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

                        System.out.println(
                                "[SKIP] "
                                        + migration.version()
                                        + " "
                                        + migration.description()
                        );

                        continue;
                    }

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

                        System.out.println(
                                "[ OK ] "
                                        + migration.version()
                                        + " "
                                        + migration.description()
                        );

                    } catch (Exception migrationException) {

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

            System.err.println(
                    "Migration error: "
                            + e.getMessage()
            );

            e.printStackTrace();

            System.exit(1);
        }
    }
}

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

    // =========================================================
    // DATE / TIME FORMAT
    // =========================================================

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    private MigrationRunner() {
    }


    // =========================================================
    // REGISTERED MIGRATIONS
    // =========================================================

    /*
     * IMPORTANT:
     *
     * Never change the version or order of migrations
     * that have already been applied to a database.
     *
     * Every new database change must be created as
     * a new Migration and added at the END of this list.
     */

    private static final List<Migration> MIGRATIONS =
            List.of(

                    // -----------------------------------------
                    // Members
                    // -----------------------------------------

                    new M001CreateMembersTable(),


                    // -----------------------------------------
                    // Membership Plans
                    // -----------------------------------------

                    new M002CreateMembershipPlansTable(),


                    // -----------------------------------------
                    // Member Memberships
                    // -----------------------------------------

                    new M003CreateMemberMembershipsTable(),


                    // -----------------------------------------
                    // Check-ins
                    // -----------------------------------------

                    new M004CreateCheckInsTable(),


                    // -----------------------------------------
                    // Payments
                    // -----------------------------------------

                    new M005CreatePaymentsTable(),


                    // -----------------------------------------
                    // Membership Session Limits
                    // -----------------------------------------

                    new M006EnsureSessionLimitColumns(),


                    // -----------------------------------------
                    // Expenses
                    // -----------------------------------------

                    new M007CreateExpensesTable(),


                    // -----------------------------------------
                    // Unique Member Phone
                    // -----------------------------------------

                    new M008MakeMemberPhoneUnique()
            );


    // =========================================================
    // RUN MIGRATIONS
    // =========================================================

    public static void migrate() throws Exception {

        try (
                Connection connection =
                        DatabaseManager.getConnection()
        ) {

            boolean oldAutoCommit =
                    connection.getAutoCommit();


            try {

                // =================================================
                // MANUAL TRANSACTION CONTROL
                // =================================================

                connection.setAutoCommit(
                        false
                );


                // =================================================
                // SQLITE FOREIGN KEYS
                // =================================================

                /*
                 * SQLite requires foreign-key support
                 * to be enabled for every connection.
                 */

                enableSQLiteForeignKeys(
                        connection
                );


                // =================================================
                // MIGRATION HISTORY TABLE
                // =================================================

                createMigrationsTable(
                        connection
                );


                // =================================================
                // EXECUTE MIGRATIONS IN ORDER
                // =================================================

                for (Migration migration : MIGRATIONS) {

                    // ---------------------------------------------
                    // SKIP ALREADY APPLIED MIGRATIONS
                    // ---------------------------------------------

                    if (isApplied(
                            connection,
                            migration.version()
                    )) {

                        System.out.println(
                                "[SKIP] "
                                        + migration.version()
                                        + " "
                                        + migration.description()
                        );

                        continue;
                    }


                    // ---------------------------------------------
                    // START MIGRATION
                    // ---------------------------------------------

                    System.out.println(
                            "[RUN ] "
                                    + migration.version()
                                    + " "
                                    + migration.description()
                    );


                    try {

                        // -----------------------------------------
                        // EXECUTE MIGRATION
                        // -----------------------------------------

                        migration.up(
                                connection
                        );


                        // -----------------------------------------
                        // RECORD MIGRATION
                        // -----------------------------------------

                        recordMigration(
                                connection,
                                migration
                        );


                        // -----------------------------------------
                        // COMMIT MIGRATION
                        // -----------------------------------------

                        connection.commit();


                        System.out.println(
                                "[ OK ] "
                                        + migration.version()
                                        + " "
                                        + migration.description()
                        );


                    } catch (Exception migrationException) {

                        // -----------------------------------------
                        // ROLLBACK FAILED MIGRATION
                        // -----------------------------------------

                        try {

                            connection.rollback();

                        } catch (Exception rollbackException) {

                            migrationException.addSuppressed(
                                    rollbackException
                            );
                        }


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


                // =================================================
                // SUCCESS
                // =================================================

                System.out.println(
                        "Database migrations completed successfully."
                );


            } catch (Exception exception) {

                // =================================================
                // SAFE ROLLBACK
                // =================================================

                try {

                    connection.rollback();

                } catch (Exception rollbackException) {

                    exception.addSuppressed(
                            rollbackException
                    );
                }


                throw new Exception(
                        "Database migration failed: "
                                + exception.getMessage(),
                        exception
                );


            } finally {

                // =================================================
                // RESTORE AUTO COMMIT
                // =================================================

                try {

                    connection.setAutoCommit(
                            oldAutoCommit
                    );

                } catch (Exception ignored) {

                    /*
                     * Connection is about to be closed.
                     * Nothing else should be done here.
                     */
                }
            }
        }
    }


    // =========================================================
    // CREATE MIGRATION HISTORY TABLE
    // =========================================================

    private static void createMigrationsTable(
            Connection connection
    ) throws Exception {

        // =====================================================
        // TABLE ALREADY EXISTS
        // =====================================================

        if (MigrationSupport.tableExists(
                connection,
                "schema_migrations"
        )) {

            return;
        }


        String sql;


        // =====================================================
        // SQLITE
        // =====================================================

        if (MigrationSupport.isSQLite(
                connection
        )) {

            sql = """
                    CREATE TABLE schema_migrations (
                        version VARCHAR(50) PRIMARY KEY,
                        description VARCHAR(255) NOT NULL,
                        applied_at TEXT NOT NULL
                    )
                    """;


        // =====================================================
        // MYSQL / MARIADB
        // =====================================================

        } else if (MigrationSupport.isMySQL(
                connection
        )) {

            sql = """
                    CREATE TABLE schema_migrations (
                        version VARCHAR(50) PRIMARY KEY,
                        description VARCHAR(255) NOT NULL,
                        applied_at VARCHAR(30) NOT NULL
                    )
                    """;


        // =====================================================
        // SQL SERVER
        // =====================================================

        } else if (MigrationSupport.isSqlServer(
                connection
        )) {

            sql = """
                    CREATE TABLE schema_migrations (
                        version NVARCHAR(50) PRIMARY KEY,
                        description NVARCHAR(255) NOT NULL,
                        applied_at NVARCHAR(30) NOT NULL
                    )
                    """;


        // =====================================================
        // UNSUPPORTED DATABASE
        // =====================================================

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


    // =========================================================
    // CHECK IF MIGRATION IS APPLIED
    // =========================================================

    private static boolean isApplied(
            Connection connection,
            String version
    ) throws Exception {

        String sql = """
                SELECT COUNT(*)
                FROM schema_migrations
                WHERE version = ?
                """;


        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        )
        ) {

            statement.setString(
                    1,
                    version
            );


            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                return resultSet.next()
                        && resultSet.getInt(1) > 0;
            }
        }
    }


    // =========================================================
    // RECORD MIGRATION
    // =========================================================

    private static void recordMigration(
            Connection connection,
            Migration migration
    ) throws Exception {

        String sql = """
                INSERT INTO schema_migrations
                (
                    version,
                    description,
                    applied_at
                )
                VALUES (?, ?, ?)
                """;


        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        )
        ) {

            // =================================================
            // VERSION
            // =================================================

            statement.setString(
                    1,
                    migration.version()
            );


            // =================================================
            // DESCRIPTION
            // =================================================

            statement.setString(
                    2,
                    migration.description()
            );


            // =================================================
            // APPLIED AT
            // =================================================

            statement.setString(
                    3,
                    LocalDateTime
                            .now()
                            .format(
                                    DATE_TIME
                            )
            );


            statement.executeUpdate();
        }
    }


    // =========================================================
    // SQLITE FOREIGN KEYS
    // =========================================================

    private static void enableSQLiteForeignKeys(
            Connection connection
    ) throws Exception {

        /*
         * This setting is required only for SQLite.
         *
         * MySQL / MariaDB and SQL Server handle
         * foreign-key enforcement differently.
         */

        if (!MigrationSupport.isSQLite(
                connection
        )) {

            return;
        }


        try (
                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute(
                    "PRAGMA foreign_keys = ON"
            );
        }
    }


    // =========================================================
    // MANUAL MIGRATION RUNNER
    // =========================================================

    public static void main(
            String[] args
    ) {

        try {

            // =================================================
            // RUN
            // =================================================

            migrate();


        } catch (Exception exception) {

            // =================================================
            // ERROR
            // =================================================

            System.err.println(
                    "Migration error: "
                            + exception.getMessage()
            );


            exception.printStackTrace();


            System.exit(
                    1
            );
        }
    }
}
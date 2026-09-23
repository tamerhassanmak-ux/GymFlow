package com.mycompany.lazar_app.database.migration;

import java.sql.Connection;

public final class M007CreateExpensesTable implements Migration {

    @Override
    public String version() {
        return "007";
    }

    @Override
    public String description() {
        return "Create expenses table";
    }

    @Override
    public void up(Connection connection) throws Exception {

        if (MigrationSupport.tableExists(connection, "expenses")) {
            return;
        }

        String sql;

        // =========================================================
        // SQLITE
        // =========================================================

        if (MigrationSupport.isSQLite(connection)) {

            sql = """
                    CREATE TABLE expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,

                        expense_code TEXT NOT NULL UNIQUE,

                        title TEXT NOT NULL,

                        category TEXT NOT NULL,

                        amount NUMERIC NOT NULL
                            CHECK (amount >= 0),

                        expense_date TEXT NOT NULL,

                        payment_method TEXT NOT NULL,

                        status TEXT NOT NULL DEFAULT 'PAID'
                            CHECK (
                                status IN (
                                    'PAID',
                                    'PENDING',
                                    'CANCELLED'
                                )
                            ),

                        reference_number TEXT,

                        beneficiary TEXT,

                        notes TEXT,

                        created_at TEXT NOT NULL
                            DEFAULT CURRENT_TIMESTAMP,

                        updated_at TEXT NOT NULL
                            DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

        // =========================================================
        // MYSQL
        // =========================================================

        } else if (MigrationSupport.isMySQL(connection)) {

            sql = """
                    CREATE TABLE expenses (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,

                        expense_code VARCHAR(50)
                            NOT NULL UNIQUE,

                        title VARCHAR(255)
                            NOT NULL,

                        category VARCHAR(100)
                            NOT NULL,

                        amount DECIMAL(15,2)
                            NOT NULL,

                        expense_date DATE
                            NOT NULL,

                        payment_method VARCHAR(50)
                            NOT NULL,

                        status VARCHAR(30)
                            NOT NULL DEFAULT 'PAID',

                        reference_number VARCHAR(100),

                        beneficiary VARCHAR(255),

                        notes TEXT,

                        created_at DATETIME
                            NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        updated_at DATETIME
                            NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,

                        CHECK (amount >= 0)
                    )
                    """;

        // =========================================================
        // SQL SERVER
        // =========================================================

        } else if (MigrationSupport.isSqlServer(connection)) {

            sql = """
                    CREATE TABLE expenses (
                        id BIGINT IDENTITY(1,1)
                            PRIMARY KEY,

                        expense_code NVARCHAR(50)
                            NOT NULL UNIQUE,

                        title NVARCHAR(255)
                            NOT NULL,

                        category NVARCHAR(100)
                            NOT NULL,

                        amount DECIMAL(15,2)
                            NOT NULL,

                        expense_date DATE
                            NOT NULL,

                        payment_method NVARCHAR(50)
                            NOT NULL,

                        status NVARCHAR(30)
                            NOT NULL DEFAULT 'PAID',

                        reference_number NVARCHAR(100),

                        beneficiary NVARCHAR(255),

                        notes NVARCHAR(MAX),

                        created_at DATETIME2
                            NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        updated_at DATETIME2
                            NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT chk_expenses_amount
                            CHECK (amount >= 0)
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

        MigrationSupport.execute(connection, sql);

        createIndexes(connection);
    }

    // =========================================================
    // INDEXES
    // =========================================================

    private void createIndexes(Connection connection)
            throws Exception {

        MigrationSupport.execute(
                connection,
                """
                CREATE INDEX idx_expenses_date
                ON expenses(expense_date)
                """
        );

        MigrationSupport.execute(
                connection,
                """
                CREATE INDEX idx_expenses_category
                ON expenses(category)
                """
        );

        MigrationSupport.execute(
                connection,
                """
                CREATE INDEX idx_expenses_status
                ON expenses(status)
                """
        );

        MigrationSupport.execute(
                connection,
                """
                CREATE INDEX idx_expenses_payment_method
                ON expenses(payment_method)
                """
        );
    }
}
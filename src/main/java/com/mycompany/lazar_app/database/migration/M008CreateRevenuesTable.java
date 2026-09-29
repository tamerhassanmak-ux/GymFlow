package com.mycompany.lazar_app.database.migration;

import java.sql.Connection;

public final class M008CreateRevenuesTable implements Migration {

    @Override
    public String version() {
        return "008";
    }

    @Override
    public String description() {
        return "Create revenues and revenue types tables";
    }

    @Override
    public void up(Connection connection) throws Exception {

        createRevenueTypes(connection);
        createRevenues(connection);
        createIndexes(connection);
    }

    private void createRevenueTypes(Connection connection)
            throws Exception {

        if (MigrationSupport.tableExists(
                connection,
                "revenue_types"
        )) {
            return;
        }

        String sql;

        if (MigrationSupport.isSQLite(connection)) {

            sql = """
                    CREATE TABLE revenue_types (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

        } else if (MigrationSupport.isMySQL(connection)) {

            sql = """
                    CREATE TABLE revenue_types (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(150) NOT NULL UNIQUE,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

        } else if (MigrationSupport.isSqlServer(connection)) {

            sql = """
                    CREATE TABLE revenue_types (
                        id BIGINT IDENTITY(1,1) PRIMARY KEY,
                        name NVARCHAR(150) NOT NULL UNIQUE,
                        created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

        } else {
            throw new IllegalStateException(
                    "Unsupported database: "
                            + connection.getMetaData()
                                    .getDatabaseProductName()
            );
        }

        MigrationSupport.execute(connection, sql);
    }

    private void createRevenues(Connection connection)
            throws Exception {

        if (MigrationSupport.tableExists(
                connection,
                "revenues"
        )) {
            return;
        }

        String sql;

        if (MigrationSupport.isSQLite(connection)) {

            sql = """
                    CREATE TABLE revenues (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        revenue_code TEXT NOT NULL UNIQUE,
                        name TEXT NOT NULL,
                        revenue_type TEXT NOT NULL,
                        amount NUMERIC NOT NULL CHECK (amount > 0),
                        revenue_date TEXT NOT NULL,
                        attachment_path TEXT,
                        notes TEXT,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

        } else if (MigrationSupport.isMySQL(connection)) {

            sql = """
                    CREATE TABLE revenues (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        revenue_code VARCHAR(50) NOT NULL UNIQUE,
                        name VARCHAR(255) NOT NULL,
                        revenue_type VARCHAR(150) NOT NULL,
                        amount DECIMAL(15,2) NOT NULL,
                        revenue_date DATE NOT NULL,
                        attachment_path VARCHAR(1000),
                        notes TEXT,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,
                        CHECK (amount > 0)
                    )
                    """;

        } else if (MigrationSupport.isSqlServer(connection)) {

            sql = """
                    CREATE TABLE revenues (
                        id BIGINT IDENTITY(1,1) PRIMARY KEY,
                        revenue_code NVARCHAR(50) NOT NULL UNIQUE,
                        name NVARCHAR(255) NOT NULL,
                        revenue_type NVARCHAR(150) NOT NULL,
                        amount DECIMAL(15,2) NOT NULL,
                        revenue_date DATE NOT NULL,
                        attachment_path NVARCHAR(1000),
                        notes NVARCHAR(MAX),
                        created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT chk_revenues_amount CHECK (amount > 0)
                    )
                    """;

        } else {
            throw new IllegalStateException(
                    "Unsupported database: "
                            + connection.getMetaData()
                                    .getDatabaseProductName()
            );
        }

        MigrationSupport.execute(connection, sql);
    }

    private void createIndexes(Connection connection)
            throws Exception {

        if (!MigrationSupport.tableExists(connection, "revenues")) {
            return;
        }

        try {
            MigrationSupport.execute(
                    connection,
                    "CREATE INDEX idx_revenues_date ON revenues(revenue_date)"
            );
        } catch (Exception ignored) {
        }

        try {
            MigrationSupport.execute(
                    connection,
                    "CREATE INDEX idx_revenues_type ON revenues(revenue_type)"
            );
        } catch (Exception ignored) {
        }

        try {
            MigrationSupport.execute(
                    connection,
                    "CREATE INDEX idx_revenues_name ON revenues(name)"
            );
        } catch (Exception ignored) {
        }
    }
}

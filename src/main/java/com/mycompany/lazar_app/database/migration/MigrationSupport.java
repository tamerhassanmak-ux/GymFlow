package com.mycompany.lazar_app.database.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class MigrationSupport {

    private MigrationSupport() {
    }

    static String databaseProduct(Connection connection) throws SQLException {
        String product = connection.getMetaData().getDatabaseProductName();
        return product == null ? "" : product.toLowerCase();
    }

    static boolean isSQLite(Connection connection) throws SQLException {
        return databaseProduct(connection).contains("sqlite");
    }

    static boolean isMySQL(Connection connection) throws SQLException {
        String p = databaseProduct(connection);
        return p.contains("mysql") || p.contains("mariadb");
    }

    static boolean isSqlServer(Connection connection) throws SQLException {
        return databaseProduct(connection).contains("microsoft sql server");
    }

    static boolean tableExists(Connection connection, String tableName)
            throws SQLException {

        DatabaseMetaData meta = connection.getMetaData();

        String[] variants = {
            tableName,
            tableName.toUpperCase(),
            tableName.toLowerCase()
        };

        for (String variant : variants) {
            try (ResultSet rs = meta.getTables(
                    connection.getCatalog(),
                    null,
                    variant,
                    new String[]{"TABLE"})) {

                if (rs.next()) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean columnExists(
            Connection connection,
            String tableName,
            String columnName) throws SQLException {

        DatabaseMetaData meta = connection.getMetaData();

        String[] tableVariants = {
            tableName,
            tableName.toUpperCase(),
            tableName.toLowerCase()
        };

        String[] columnVariants = {
            columnName,
            columnName.toUpperCase(),
            columnName.toLowerCase()
        };

        for (String table : tableVariants) {
            for (String column : columnVariants) {
                try (ResultSet rs = meta.getColumns(
                        connection.getCatalog(),
                        null,
                        table,
                        column)) {

                    if (rs.next()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    static boolean indexExists(
            Connection connection,
            String tableName,
            String indexName) throws SQLException {

        DatabaseMetaData meta = connection.getMetaData();

        try (ResultSet rs = meta.getIndexInfo(
                connection.getCatalog(),
                null,
                tableName,
                false,
                false)) {

            while (rs.next()) {
                String current = rs.getString("INDEX_NAME");
                if (current != null
                        && current.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }

        return false;
    }

    static void execute(Connection connection, String sql)
            throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    static String idDefinition(Connection connection) throws SQLException {
        if (isSQLite(connection)) {
            return "INTEGER PRIMARY KEY AUTOINCREMENT";
        }

        if (isMySQL(connection)) {
            return "BIGINT PRIMARY KEY AUTO_INCREMENT";
        }

        if (isSqlServer(connection)) {
            return "BIGINT IDENTITY(1,1) PRIMARY KEY";
        }

        throw new SQLException(
                "Unsupported database: "
                        + connection.getMetaData().getDatabaseProductName()
        );
    }

    static String textType(Connection connection) throws SQLException {
        return isSqlServer(connection) ? "NVARCHAR(MAX)" : "TEXT";
    }

    static String shortText(Connection connection, int length)
            throws SQLException {
        return isSqlServer(connection)
                ? "NVARCHAR(" + length + ")"
                : "VARCHAR(" + length + ")";
    }

    static void createIndexIfMissing(
            Connection connection,
            String table,
            String index,
            String columns) throws SQLException {

        if (indexExists(connection, table, index)) {
            return;
        }

        execute(
                connection,
                "CREATE INDEX " + index
                        + " ON " + table
                        + "(" + columns + ")"
        );
    }
}

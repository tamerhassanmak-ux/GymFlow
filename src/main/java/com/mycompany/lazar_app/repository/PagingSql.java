package com.mycompany.lazar_app.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/** SQL fragments contain only application-owned identifiers; all values are bound. */
final class PagingSql {
    private PagingSql() {}

    static String limit(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("sql server")
                ? " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY" : " LIMIT ? OFFSET ?";
    }

    static boolean offsetFirst(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("sql server");
    }

    static String normalize(String value) {
        return (value == null ? "" : value).strip().toLowerCase(Locale.ROOT)
                .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
                .replace('ى', 'ي').replace('ة', 'ه').replaceAll("[\\u064B-\\u065F\\u0670]", "");
    }

    static String normalizedColumn(String column) {
        String sql = "LOWER(COALESCE(" + column + ", ''))";
        for (String[] pair : new String[][]{{"أ","ا"},{"إ","ا"},{"آ","ا"},{"ى","ي"},{"ة","ه"}}) {
            sql = "REPLACE(" + sql + ", '" + pair[0] + "', '" + pair[1] + "')";
        }
        for (char ch = '\u064B'; ch <= '\u065F'; ch++) sql = "REPLACE(" + sql + ", '" + ch + "', '')";
        return "REPLACE(" + sql + ", 'ٰ', '')";
    }

    static String pattern(String search) {
        return "%" + normalize(search).replace("!", "!!").replace("%", "!%")
                .replace("_", "!_").replace("[", "![") + "%";
    }
}

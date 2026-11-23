package com.mycompany.lazar_app.database.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Adds the missed constraint without reusing applied migration 008 or deleting records. */
public final class M010EnforceMemberPhoneUniqueness implements Migration {
    public String version() { return "010"; }
    public String description() { return "enforce_nonblank_member_phone_uniqueness"; }
    public void up(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("""
                SELECT COUNT(*) FROM members
                WHERE phone IS NOT NULL AND TRIM(phone) <> ''
                GROUP BY TRIM(phone) HAVING COUNT(*) > 1
                """)) {
            if (rows.next()) throw new SQLException(
                    "Duplicate member phones require administrator review before migration 010; no records were changed.");
        }
        if (MigrationSupport.indexExists(connection, "members", "idx_members_phone_nonblank_unique")) return;
        if (MigrationSupport.isSQLite(connection)) {
            MigrationSupport.execute(connection, "CREATE UNIQUE INDEX idx_members_phone_nonblank_unique ON members(NULLIF(TRIM(phone), ''))");
        } else if (MigrationSupport.isMySQL(connection)) {
            if (!MigrationSupport.columnExists(connection, "members", "phone_unique_key")) {
                MigrationSupport.execute(connection, "ALTER TABLE members ADD phone_unique_key VARCHAR(30) GENERATED ALWAYS AS (NULLIF(TRIM(phone), '')) STORED");
            }
            MigrationSupport.execute(connection, "CREATE UNIQUE INDEX idx_members_phone_nonblank_unique ON members(phone_unique_key)");
        } else if (MigrationSupport.isSqlServer(connection)) {
            if (!MigrationSupport.columnExists(connection, "members", "phone_unique_key")) {
                MigrationSupport.execute(connection, "ALTER TABLE members ADD phone_unique_key AS NULLIF(LTRIM(RTRIM(phone)), '') PERSISTED");
            }
            MigrationSupport.execute(connection, "CREATE UNIQUE INDEX idx_members_phone_nonblank_unique ON members(phone_unique_key) WHERE phone IS NOT NULL AND phone <> ''");
        } else throw new SQLException("Unsupported database for phone uniqueness migration.");
    }
}

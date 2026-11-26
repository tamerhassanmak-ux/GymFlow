package com.mycompany.lazar_app.database.migration;

import java.sql.*;

/** Prevents races between a payment write and subscription reassignment. */
public final class M011ProtectFinancialOwnership implements Migration {
    public String version() { return "011"; }
    public String description() { return "protect_financial_membership_ownership"; }
    public void up(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("""
                SELECT 1 FROM payments p JOIN member_memberships m ON m.id=p.membership_id WHERE p.member_id<>m.member_id
                UNION ALL SELECT 1 FROM check_ins c JOIN member_memberships m ON m.id=c.membership_id WHERE c.member_id<>m.member_id
                """)) {
            if (rows.next()) throw new SQLException("Existing membership ownership conflicts require administrator review; no records changed.", "GF007");
        }
        if (MigrationSupport.isSQLite(connection)) {
            for (String sql : sqliteTriggers().values()) MigrationSupport.execute(connection, sql);
        } else {
            if (!MigrationSupport.indexExists(connection, "member_memberships", "idx_membership_owner")) {
                MigrationSupport.execute(connection, "CREATE UNIQUE INDEX idx_membership_owner ON member_memberships(id, member_id)");
            }
            for (String table : new String[]{"payments", "check_ins"}) {
                String name = "fk_" + table + "_owner";
                boolean exists = false;
                try (ResultSet keys = connection.getMetaData().getImportedKeys(connection.getCatalog(), null, table)) {
                    while (keys.next()) if (name.equalsIgnoreCase(keys.getString("FK_NAME"))) exists = true;
                }
                // MySQL DDL may commit before a later step fails; retries must be safe.
                if (!exists) MigrationSupport.execute(connection, "ALTER TABLE " + table + " ADD CONSTRAINT " + name
                        + " FOREIGN KEY(membership_id, member_id) REFERENCES member_memberships(id, member_id)");
            }
        }
    }
    public static java.util.Map<String, String> sqliteTriggers() {
        var statements = new java.util.LinkedHashMap<String, String>();
        for (String table : new String[]{"payments", "check_ins"}) {
            for (String event : new String[]{"INSERT", "UPDATE"}) {
                String name = "guard_" + table + "_" + event.toLowerCase(java.util.Locale.ROOT);
                statements.put(name, "CREATE TRIGGER " + name + " BEFORE " + event + " ON " + table + " WHEN NEW.membership_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM member_memberships WHERE id=NEW.membership_id AND member_id=NEW.member_id) BEGIN SELECT RAISE(ABORT, 'Membership must belong to the selected member'); END");
            }
        }
        statements.put("guard_membership_owner", """
                CREATE TRIGGER guard_membership_owner BEFORE UPDATE OF member_id ON member_memberships
                WHEN NEW.member_id<>OLD.member_id AND
                (EXISTS(SELECT 1 FROM payments WHERE membership_id=OLD.id) OR EXISTS(SELECT 1 FROM check_ins WHERE membership_id=OLD.id))
                BEGIN SELECT RAISE(ABORT, 'Cannot reassign a membership with financial or attendance history'); END
                """);
        statements.put("guard_membership_history", """
                CREATE TRIGGER guard_membership_history BEFORE DELETE ON member_memberships
                WHEN EXISTS(SELECT 1 FROM payments WHERE membership_id=OLD.id) OR EXISTS(SELECT 1 FROM check_ins WHERE membership_id=OLD.id)
                BEGIN SELECT RAISE(ABORT, 'Cancel memberships with history instead of deleting them'); END
                """);
        return statements;
    }
}

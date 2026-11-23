package com.mycompany.lazar_app.database.migration;
import java.sql.*;

/** Move schedule creation out of read/write workflows: server DDL can implicitly commit. */
public final class M012CreatePaymentInstallments implements Migration {
    public String version() { return "012"; }
    public String description() { return "migrate_payment_installments"; }
    public void up(Connection connection) throws Exception {
        if (!MigrationSupport.tableExists(connection, "payment_installments")) {
            String id=MigrationSupport.idDefinition(connection);
            MigrationSupport.execute(connection, """
                    CREATE TABLE payment_installments (
                        id %s, membership_id BIGINT NOT NULL,
                        installment_no INTEGER NOT NULL, due_date VARCHAR(30) NOT NULL,
                        amount DECIMAL(18,2) NOT NULL CHECK(amount >= 0),
                        status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
                        created_at VARCHAR(30), updated_at VARCHAR(30),
                        FOREIGN KEY(membership_id) REFERENCES member_memberships(id) ON DELETE CASCADE
                    )
                    """.formatted(id));
        }
        try(Statement s=connection.createStatement();ResultSet r=s.executeQuery("SELECT 1 FROM payment_installments i LEFT JOIN member_memberships m ON m.id=i.membership_id WHERE m.id IS NULL OR i.amount<0")) {
            if(r.next()) throw new SQLException("Invalid existing installments require administrator review; no records changed.");
        }
        MigrationSupport.createIndexIfMissing(connection,"payment_installments","idx_payment_installments_membership","membership_id");
        if(MigrationSupport.isSQLite(connection)) {
            for(String sql:sqliteTriggers().values()) MigrationSupport.execute(connection,sql);
        }
    }
    public static java.util.Map<String,String> sqliteTriggers() {
        var statements=new java.util.LinkedHashMap<String,String>();
        for(String event:new String[]{"INSERT","UPDATE"}) {
            String name="guard_installments_"+event.toLowerCase(java.util.Locale.ROOT);
            statements.put(name,"CREATE TRIGGER "+name+" BEFORE "+event+" ON payment_installments WHEN NEW.amount<0 OR NOT EXISTS(SELECT 1 FROM member_memberships WHERE id=NEW.membership_id) BEGIN SELECT RAISE(ABORT, 'Invalid installment membership or amount'); END");
        }
        statements.put("cleanup_membership_installments","CREATE TRIGGER cleanup_membership_installments AFTER DELETE ON member_memberships BEGIN DELETE FROM payment_installments WHERE membership_id=OLD.id; END");
        return statements;
    }
}

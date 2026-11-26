package com.mycompany.lazar_app.database.migration;

import java.sql.*;

/** Database guards for racing check-ins and retries of one payment submission. */
public final class M013PreventDuplicateSubmissions implements Migration {
    public String version() { return "013"; }
    public String description() { return "prevent_duplicate_checkins_and_payment_submissions"; }
    public void up(Connection connection) throws Exception {
        try (Statement s=connection.createStatement(); ResultSet r=s.executeQuery("SELECT member_id FROM check_ins WHERE status='IN' AND check_out_time IS NULL GROUP BY member_id HAVING COUNT(*)>1")) {
            if(r.next()) throw new SQLException("Duplicate open attendance requires administrator review before migration 013; no records changed.", "GF008");
        }
        if(!MigrationSupport.indexExists(connection,"check_ins","idx_check_ins_one_open")) {
            if(MigrationSupport.isMySQL(connection)) {
                if(!MigrationSupport.columnExists(connection,"check_ins","open_member_key"))
                    MigrationSupport.execute(connection,"ALTER TABLE check_ins ADD open_member_key BIGINT GENERATED ALWAYS AS (CASE WHEN status='IN' AND check_out_time IS NULL THEN member_id ELSE NULL END) STORED");
                MigrationSupport.execute(connection,"CREATE UNIQUE INDEX idx_check_ins_one_open ON check_ins(open_member_key)");
            } else {
                MigrationSupport.execute(connection,"CREATE UNIQUE INDEX idx_check_ins_one_open ON check_ins(member_id) WHERE status='IN' AND check_out_time IS NULL");
            }
        }
        if(!MigrationSupport.columnExists(connection,"payments","submission_id"))
            MigrationSupport.execute(connection,"ALTER TABLE payments ADD submission_id VARCHAR(36)");
        if(!MigrationSupport.indexExists(connection,"payments","idx_payments_submission")) {
            MigrationSupport.execute(connection,"CREATE UNIQUE INDEX idx_payments_submission ON payments(submission_id)"
                    +(MigrationSupport.isSqlServer(connection)?" WHERE submission_id IS NOT NULL":""));
        }
    }
}

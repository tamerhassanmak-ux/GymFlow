package com.mycompany.lazar_app.database.migration;

import java.sql.Connection;

public class M005CreatePaymentsTable implements Migration {

    @Override
    public String version() {
        return "005";
    }

    @Override
    public String description() {
        return "create_payments";
    }

    @Override
    public void up(Connection connection) throws Exception {
        if (!MigrationSupport.tableExists(connection, "payments")) {
            String id = MigrationSupport.idDefinition(connection);
            String text = MigrationSupport.textType(connection);

            String sql = """
                    CREATE TABLE payments (
                        id %s,
                        member_id BIGINT NOT NULL,
                        membership_id BIGINT,
                        amount DECIMAL(12,2) NOT NULL,
                        payment_method VARCHAR(30) NOT NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'PAID',
                        payment_date VARCHAR(30) NOT NULL,
                        reference_number VARCHAR(150),
                        notes %s,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (member_id)
                            REFERENCES members(id)
                            ON DELETE RESTRICT,
                        FOREIGN KEY (membership_id)
                            REFERENCES member_memberships(id)
                            ON DELETE SET NULL
                    )
                    """.formatted(id, text);

            MigrationSupport.execute(connection, sql);
        }

        MigrationSupport.createIndexIfMissing(
                connection, "payments",
                "idx_payments_member", "member_id"
        );

        MigrationSupport.createIndexIfMissing(
                connection, "payments",
                "idx_payments_membership", "membership_id"
        );

        MigrationSupport.createIndexIfMissing(
                connection, "payments",
                "idx_payments_status", "status"
        );

        MigrationSupport.createIndexIfMissing(
                connection, "payments",
                "idx_payments_method", "payment_method"
        );

        MigrationSupport.createIndexIfMissing(
                connection, "payments",
                "idx_payments_date", "payment_date"
        );

        MigrationSupport.createIndexIfMissing(
                connection, "payments",
                "idx_payments_reference", "reference_number"
        );
    }
}

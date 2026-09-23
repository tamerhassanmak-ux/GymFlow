package com.mycompany.lazar_app.database.migration;

import java.sql.Connection;

public class M004CreateCheckInsTable implements Migration {

    @Override
    public String version() {
        return "004";
    }

    @Override
    public String description() {
        return "create_check_ins";
    }

    @Override
    public void up(Connection connection) throws Exception {
        if (!MigrationSupport.tableExists(connection, "check_ins")) {
            String id = MigrationSupport.idDefinition(connection);
            String text = MigrationSupport.textType(connection);

            String sql = """
                    CREATE TABLE check_ins (
                        id %s,
                        member_id BIGINT NOT NULL,
                        membership_id BIGINT,
                        check_in_time VARCHAR(30) NOT NULL,
                        check_out_time VARCHAR(30),
                        status VARCHAR(30) NOT NULL DEFAULT 'IN',
                        notes %s,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (member_id)
                            REFERENCES members(id)
                            ON DELETE CASCADE,
                        FOREIGN KEY (membership_id)
                            REFERENCES member_memberships(id)
                    )
                    """.formatted(id, text);

            MigrationSupport.execute(connection, sql);
        }

        MigrationSupport.createIndexIfMissing(
                connection,
                "check_ins",
                "idx_check_ins_member",
                "member_id"
        );

        MigrationSupport.createIndexIfMissing(
                connection,
                "check_ins",
                "idx_check_ins_status",
                "status"
        );

        MigrationSupport.createIndexIfMissing(
                connection,
                "check_ins",
                "idx_check_ins_check_in_time",
                "check_in_time"
        );
    }
}

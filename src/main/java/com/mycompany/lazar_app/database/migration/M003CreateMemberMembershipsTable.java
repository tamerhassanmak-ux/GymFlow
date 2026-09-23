package com.mycompany.lazar_app.database.migration;

import java.sql.Connection;

public class M003CreateMemberMembershipsTable implements Migration {

    @Override
    public String version() {
        return "003";
    }

    @Override
    public String description() {
        return "create_member_memberships";
    }

    @Override
    public void up(Connection connection) throws Exception {
        if (MigrationSupport.tableExists(connection, "member_memberships")) {
            return;
        }

        String id = MigrationSupport.idDefinition(connection);
        String text = MigrationSupport.textType(connection);

        String sql = """
                CREATE TABLE member_memberships (
                    id %s,
                    member_id BIGINT NOT NULL,
                    membership_plan_id BIGINT NOT NULL,
                    start_date VARCHAR(30) NOT NULL,
                    end_date VARCHAR(30) NOT NULL,
                    price DECIMAL(12,2) NOT NULL DEFAULT 0,
                    discount DECIMAL(12,2) NOT NULL DEFAULT 0,
                    final_price DECIMAL(12,2) NOT NULL DEFAULT 0,
                    status VARCHAR(30) NOT NULL DEFAULT 'Active',
                    notes %s,
                    allowed_sessions INTEGER NOT NULL DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (member_id)
                        REFERENCES members(id)
                        ON DELETE CASCADE,
                    FOREIGN KEY (membership_plan_id)
                        REFERENCES membership_plans(id)
                )
                """.formatted(id, text);

        MigrationSupport.execute(connection, sql);
    }
}

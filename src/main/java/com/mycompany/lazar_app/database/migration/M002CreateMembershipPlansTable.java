package com.mycompany.lazar_app.database.migration;

import java.sql.Connection;

public class M002CreateMembershipPlansTable implements Migration {

    @Override
    public String version() {
        return "002";
    }

    @Override
    public String description() {
        return "create_membership_plans";
    }

    @Override
    public void up(Connection connection) throws Exception {
        if (MigrationSupport.tableExists(connection, "membership_plans")) {
            return;
        }

        String id = MigrationSupport.idDefinition(connection);
        String text = MigrationSupport.textType(connection);

        String sql = """
                CREATE TABLE membership_plans (
                    id %s,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    name_ar VARCHAR(150) NOT NULL,
                    name_en VARCHAR(150) NOT NULL,
                    duration_months INTEGER NOT NULL,
                    price DECIMAL(12,2) NOT NULL DEFAULT 0,
                    status VARCHAR(30) NOT NULL DEFAULT 'Active',
                    description %s,
                    max_sessions INTEGER NOT NULL DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(id, text);

        MigrationSupport.execute(connection, sql);
    }
}

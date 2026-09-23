package com.mycompany.lazar_app.database.migration;

import java.sql.Connection;

public class M001CreateMembersTable implements Migration {

    @Override
    public String version() {
        return "001";
    }

    @Override
    public String description() {
        return "create_members";
    }

    @Override
    public void up(Connection connection) throws Exception {
        if (MigrationSupport.tableExists(connection, "members")) {
            return;
        }

        String id = MigrationSupport.idDefinition(connection);
        String text = MigrationSupport.textType(connection);

        String sql = """
                CREATE TABLE members (
                    id %s,
                    member_code VARCHAR(30) UNIQUE,
                    name VARCHAR(150) NOT NULL,
                    phone VARCHAR(30),
                    whatsapp VARCHAR(30),
                    email VARCHAR(150),
                    gender VARCHAR(20),
                    birth_date DATE,
                    membership VARCHAR(100),
                    start_date DATE,
                    end_date DATE,
                    status VARCHAR(30) NOT NULL DEFAULT 'Active',
                    notes %s,
                    photo_path %s,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(id, text, text);

        MigrationSupport.execute(connection, sql);
    }
}

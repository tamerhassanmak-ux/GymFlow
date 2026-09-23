package com.mycompany.lazar_app.database.migration;

import java.sql.Connection;

public class M006EnsureSessionLimitColumns implements Migration {

    @Override
    public String version() {
        return "006";
    }

    @Override
    public String description() {
        return "ensure_session_limit_columns";
    }

    @Override
    public void up(Connection connection) throws Exception {

        if (!MigrationSupport.columnExists(
                connection,
                "membership_plans",
                "max_sessions")) {

            MigrationSupport.execute(
                    connection,
                    """
                    ALTER TABLE membership_plans
                    ADD COLUMN max_sessions INTEGER NOT NULL DEFAULT 0
                    """
            );
        }

        if (!MigrationSupport.columnExists(
                connection,
                "member_memberships",
                "allowed_sessions")) {

            MigrationSupport.execute(
                    connection,
                    """
                    ALTER TABLE member_memberships
                    ADD COLUMN allowed_sessions INTEGER NOT NULL DEFAULT 0
                    """
            );
        }
    }
}

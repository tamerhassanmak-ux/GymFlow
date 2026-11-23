package com.mycompany.lazar_app.database.migration;
import java.sql.Connection;

/** Bound latest-membership and consumed-visit lookups as the history grows. */
public final class M014IndexMembershipLookups implements Migration {
    public String version() { return "014"; }
    public String description() { return "index_membership_and_visit_lookups"; }
    public void up(Connection connection) throws Exception {
        MigrationSupport.createIndexIfMissing(connection, "member_memberships", "idx_memberships_member_latest", "member_id, id");
        MigrationSupport.createIndexIfMissing(connection, "check_ins", "idx_check_ins_membership_time", "membership_id, check_in_time");
    }
}

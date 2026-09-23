package com.mycompany.lazar_app.database.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class M008MakeMemberPhoneUnique implements Migration {

    private static final String TABLE_NAME =
            "members";

    private static final String INDEX_NAME =
            "idx_members_phone_unique";


    // =========================================================
    // VERSION
    // =========================================================

    @Override
    public String version() {
        return "008";
    }


    // =========================================================
    // DESCRIPTION
    // =========================================================

    @Override
    public String description() {
        return "make_member_phone_unique";
    }


    // =========================================================
    // UP
    // =========================================================

    @Override
    public void up(
            Connection connection
    ) throws Exception {

        // =====================================================
        // CHECK MEMBERS TABLE
        // =====================================================

        if (!MigrationSupport.tableExists(
                connection,
                TABLE_NAME
        )) {

            throw new IllegalStateException(
                    "Table 'members' does not exist."
            );
        }


        // =====================================================
        // CHECK PHONE COLUMN
        // =====================================================

        if (!MigrationSupport.columnExists(
                connection,
                TABLE_NAME,
                "phone"
        )) {

            throw new IllegalStateException(
                    "Column 'phone' does not exist in members table."
            );
        }


        // =====================================================
        // CHECK IF INDEX ALREADY EXISTS
        // =====================================================

        if (MigrationSupport.indexExists(
                connection,
                TABLE_NAME,
                INDEX_NAME
        )) {

            return;
        }


        // =====================================================
        // CHECK EXISTING DUPLICATE PHONES
        // =====================================================

        String duplicatePhone =
                findDuplicatePhone(
                        connection
                );


        if (duplicatePhone != null) {

            throw new IllegalStateException(
                    "Cannot create unique member phone index. "
                            + "Duplicate phone found: "
                            + duplicatePhone
            );
        }


        // =====================================================
        // CREATE UNIQUE INDEX
        // =====================================================

        String sql =
                "CREATE UNIQUE INDEX "
                        + INDEX_NAME
                        + " ON "
                        + TABLE_NAME
                        + "(phone)";


        MigrationSupport.execute(
                connection,
                sql
        );
    }


    // =========================================================
    // FIND DUPLICATE PHONE
    // =========================================================

    private String findDuplicatePhone(
            Connection connection
    ) throws Exception {

        String sql = """
                SELECT phone
                FROM members
                WHERE phone IS NOT NULL
                  AND TRIM(phone) <> ''
                GROUP BY phone
                HAVING COUNT(*) > 1
                """;


        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        );

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            if (resultSet.next()) {

                return resultSet.getString(
                        "phone"
                );
            }
        }


        return null;
    }
}
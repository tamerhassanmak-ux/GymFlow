package com.mycompany.lazar_app.repository;

import com.mycompany.lazar_app.database.DatabaseManager;
import com.mycompany.lazar_app.model.Member;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberRepository {

    // =========================================================
    // FIND ALL
    // =========================================================

    public List<Member> findAll() throws SQLException {

        String sql = """
                SELECT
                    id,
                    member_code,
                    name,
                    phone,
                    whatsapp,
                    email,
                    gender,
                    birth_date,
                    membership,
                    start_date,
                    end_date,
                    status,
                    notes,
                    photo_path
                FROM members
                ORDER BY id DESC
                """;

        List<Member> members = new ArrayList<>();

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            while (resultSet.next()) {

                members.add(
                        mapMember(resultSet)
                );
            }
        }

        return members;
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    public Optional<Member> findById(
            int id
    ) throws SQLException {

        String sql = """
                SELECT
                    id,
                    member_code,
                    name,
                    phone,
                    whatsapp,
                    email,
                    gender,
                    birth_date,
                    membership,
                    start_date,
                    end_date,
                    status,
                    notes,
                    photo_path
                FROM members
                WHERE id = ?
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    id
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (resultSet.next()) {

                    return Optional.of(
                            mapMember(resultSet)
                    );
                }
            }
        }

        return Optional.empty();
    }


    // =========================================================
    // FIND BY PHONE
    // =========================================================

    public Optional<Member> findByPhone(
            String phone
    ) throws SQLException {

        String normalizedPhone = normalizePhone(phone);

        if (normalizedPhone.isBlank()) {
            return Optional.empty();
        }

        String sql = """
                SELECT
                    id,
                    member_code,
                    name,
                    phone,
                    whatsapp,
                    email,
                    gender,
                    birth_date,
                    membership,
                    start_date,
                    end_date,
                    status,
                    notes,
                    photo_path
                FROM members
                WHERE REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(phone, ' ', ''),
                        '-', ''),
                    '(', ''),
                ')', '') = ?
                LIMIT 1
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    normalizedPhone
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (resultSet.next()) {
                    return Optional.of(
                            mapMember(resultSet)
                    );
                }
            }
        }

        return Optional.empty();
    }


    // =========================================================
    // FIND ANOTHER MEMBER BY PHONE
    // =========================================================

    public Optional<Member> findAnotherByPhone(
            String phone,
            int excludedMemberId
    ) throws SQLException {

        String normalizedPhone = normalizePhone(phone);

        if (normalizedPhone.isBlank()) {
            return Optional.empty();
        }

        String sql = """
                SELECT
                    id,
                    member_code,
                    name,
                    phone,
                    whatsapp,
                    email,
                    gender,
                    birth_date,
                    membership,
                    start_date,
                    end_date,
                    status,
                    notes,
                    photo_path
                FROM members
                WHERE REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(phone, ' ', ''),
                        '-', ''),
                    '(', ''),
                ')', '') = ?
                AND id <> ?
                LIMIT 1
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, normalizedPhone);
            statement.setInt(2, excludedMemberId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (resultSet.next()) {
                    return Optional.of(
                            mapMember(resultSet)
                    );
                }
            }
        }

        return Optional.empty();
    }


    // =========================================================
    // INSERT
    // =========================================================

    public Member insert(
            Member member
    ) throws SQLException {

        if (member == null) {

            throw new IllegalArgumentException(
                    "Member cannot be null."
            );
        }

        Optional<Member> duplicate =
                findByPhone(member.getPhone());

        if (duplicate.isPresent()) {
            throw new SQLException(
                    "Duplicate member phone: " + normalizePhone(member.getPhone())
            );
        }

        String sql = """
                INSERT INTO members (
                    member_code,
                    name,
                    phone,
                    whatsapp,
                    email,
                    gender,
                    birth_date,
                    membership,
                    start_date,
                    end_date,
                    status,
                    notes,
                    photo_path
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection()
        ) {

            connection.setAutoCommit(false);

            try {

                int generatedId;


                // =================================================
                // INSERT MEMBER
                // =================================================

                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        sql,
                                        Statement.RETURN_GENERATED_KEYS
                                )
                ) {

                    // Member code is created AFTER database ID.
                    statement.setNull(
                            1,
                            java.sql.Types.VARCHAR
                    );

                    statement.setString(
                            2,
                            safe(member.getName())
                    );

                    statement.setString(
                            3,
                            safe(member.getPhone())
                    );

                    statement.setString(
                            4,
                            safe(member.getWhatsapp())
                    );

                    statement.setString(
                            5,
                            safe(member.getEmail())
                    );

                    statement.setString(
                            6,
                            safe(member.getGender())
                    );

                    setLocalDate(
                            statement,
                            7,
                            member.getBirthDate()
                    );

                    statement.setString(
                            8,
                            safe(member.getMembership())
                    );

                    setLocalDate(
                            statement,
                            9,
                            member.getStartDate()
                    );

                    setLocalDate(
                            statement,
                            10,
                            member.getEndDate()
                    );

                    statement.setString(
                            11,
                            normalizeStatus(
                                    member.getStatus()
                            )
                    );

                    statement.setString(
                            12,
                            safe(member.getNotes())
                    );

                    statement.setString(
                            13,
                            safe(member.getPhotoPath())
                    );


                    int affectedRows =
                            statement.executeUpdate();


                    if (affectedRows == 0) {

                        throw new SQLException(
                                "Creating member failed. No rows were inserted."
                        );
                    }


                    try (
                            ResultSet generatedKeys =
                                    statement.getGeneratedKeys()
                    ) {

                        if (!generatedKeys.next()) {

                            throw new SQLException(
                                    "Creating member failed. Database did not return an ID."
                            );
                        }

                        generatedId =
                                generatedKeys.getInt(1);
                    }
                }


                // =================================================
                // GENERATE MEMBER CODE
                // =================================================

                String memberCode =
                        generateMemberCode(
                                generatedId
                        );


                // =================================================
                // UPDATE MEMBER CODE
                // =================================================

                String updateCodeSql = """
                        UPDATE members
                        SET
                            member_code = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """;


                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        updateCodeSql
                                )
                ) {

                    statement.setString(
                            1,
                            memberCode
                    );

                    statement.setInt(
                            2,
                            generatedId
                    );

                    statement.executeUpdate();
                }


                connection.commit();


                // =================================================
                // UPDATE OBJECT
                // =================================================

                member.setId(
                        generatedId
                );

                member.setMemberCode(
                        memberCode
                );


                return member;


            } catch (SQLException exception) {

                connection.rollback();

                throw exception;

            } finally {

                try {

                    connection.setAutoCommit(
                            true
                    );

                } catch (SQLException ignored) {
                }
            }
        }
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public boolean update(
            Member member
    ) throws SQLException {

        if (member == null) {

            throw new IllegalArgumentException(
                    "Member cannot be null."
            );
        }


        if (member.getId() <= 0) {

            throw new IllegalArgumentException(
                    "Member ID is invalid."
            );
        }

        Optional<Member> duplicate =
                findAnotherByPhone(
                        member.getPhone(),
                        member.getId()
                );

        if (duplicate.isPresent()) {
            throw new SQLException(
                    "Duplicate member phone: " + normalizePhone(member.getPhone())
            );
        }


        String sql = """
                UPDATE members
                SET
                    member_code = ?,
                    name = ?,
                    phone = ?,
                    whatsapp = ?,
                    email = ?,
                    gender = ?,
                    birth_date = ?,
                    membership = ?,
                    start_date = ?,
                    end_date = ?,
                    status = ?,
                    notes = ?,
                    photo_path = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            String memberCode =
                    member.getMemberCode();


            if (memberCode == null
                    || memberCode.isBlank()
                    || "LZR-000000".equals(memberCode)) {

                memberCode =
                        generateMemberCode(
                                member.getId()
                        );

                member.setMemberCode(
                        memberCode
                );
            }


            statement.setString(
                    1,
                    memberCode
            );

            statement.setString(
                    2,
                    safe(member.getName())
            );

            statement.setString(
                    3,
                    safe(member.getPhone())
            );

            statement.setString(
                    4,
                    safe(member.getWhatsapp())
            );

            statement.setString(
                    5,
                    safe(member.getEmail())
            );

            statement.setString(
                    6,
                    safe(member.getGender())
            );

            setLocalDate(
                    statement,
                    7,
                    member.getBirthDate()
            );

            statement.setString(
                    8,
                    safe(member.getMembership())
            );

            setLocalDate(
                    statement,
                    9,
                    member.getStartDate()
            );

            setLocalDate(
                    statement,
                    10,
                    member.getEndDate()
            );

            statement.setString(
                    11,
                    normalizeStatus(
                            member.getStatus()
                    )
            );

            statement.setString(
                    12,
                    safe(member.getNotes())
            );

            statement.setString(
                    13,
                    safe(member.getPhotoPath())
            );

            statement.setInt(
                    14,
                    member.getId()
            );


            return statement.executeUpdate() > 0;
        }
    }


    // =========================================================
    // DELETE
    // =========================================================

    public boolean delete(
            int memberId
    ) throws SQLException {

        String sql = """
                DELETE FROM members
                WHERE id = ?
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    memberId
            );

            return statement.executeUpdate() > 0;
        }
    }


    // =========================================================
    // EXISTS
    // =========================================================

    public boolean exists(
            int memberId
    ) throws SQLException {

        String sql = """
                SELECT 1
                FROM members
                WHERE id = ?
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    memberId
            );


            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                return resultSet.next();
            }
        }
    }


    // =========================================================
    // COUNT
    // =========================================================

    public int count() throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM members
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            if (resultSet.next()) {

                return resultSet.getInt(1);
            }
        }

        return 0;
    }


    // =========================================================
    // RESULT SET -> MEMBER
    // =========================================================

    private Member mapMember(
            ResultSet resultSet
    ) throws SQLException {

        return new Member(

                resultSet.getInt(
                        "id"
                ),

                resultSet.getString(
                        "member_code"
                ),

                resultSet.getString(
                        "name"
                ),

                resultSet.getString(
                        "phone"
                ),

                resultSet.getString(
                        "whatsapp"
                ),

                resultSet.getString(
                        "email"
                ),

                resultSet.getString(
                        "gender"
                ),

                getLocalDate(
                        resultSet,
                        "birth_date"
                ),

                resultSet.getString(
                        "membership"
                ),

                getLocalDate(
                        resultSet,
                        "start_date"
                ),

                getLocalDate(
                        resultSet,
                        "end_date"
                ),

                resultSet.getString(
                        "status"
                ),

                resultSet.getString(
                        "notes"
                ),

                resultSet.getString(
                        "photo_path"
                )
        );
    }


    // =========================================================
    // LOCAL DATE -> SQL DATE
    // =========================================================

    private void setLocalDate(
            PreparedStatement statement,
            int index,
            LocalDate date
    ) throws SQLException {

        if (date == null) {

            statement.setNull(
                    index,
                    java.sql.Types.DATE
            );

        } else {

            statement.setDate(
                    index,
                    Date.valueOf(date)
            );
        }
    }


    // =========================================================
    // SQL DATE -> LOCAL DATE
    // =========================================================

    private LocalDate getLocalDate(
            ResultSet resultSet,
            String column
    ) throws SQLException {

        Date date =
                resultSet.getDate(
                        column
                );

        return date == null
                ? null
                : date.toLocalDate();
    }


    // =========================================================
    // GENERATE MEMBER CODE
    // =========================================================

    private String generateMemberCode(
            int id
    ) {

        return String.format(
                "LZR-%06d",
                id
        );
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }


    // =========================================================
    // NORMALIZE PHONE
    // =========================================================

    private String normalizePhone(
            String phone
    ) {

        if (phone == null) {
            return "";
        }

        return phone
                .trim()
                .replaceAll("[\\s\\-()]", "");
    }


    // =========================================================
    // STATUS
    // =========================================================

    private String normalizeStatus(
            String status
    ) {

        if (status == null
                || status.isBlank()) {

            return "Active";
        }

        return status.trim();
    }
}

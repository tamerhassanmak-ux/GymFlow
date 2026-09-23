package com.mycompany.lazar_app.repository;

import com.mycompany.lazar_app.database.DatabaseManager;
import com.mycompany.lazar_app.model.CheckIn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class CheckInRepository {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );


    private static final String BASE_SELECT = """

            SELECT
                ci.id,
                ci.member_id,
                ci.membership_id,
                ci.check_in_time,
                ci.check_out_time,
                ci.status,
                ci.notes,
                ci.created_at,
                ci.updated_at,

                m.member_code,
                m.name AS member_name,
                m.phone,

                mp.name_ar AS membership_name_ar,
                mp.name_en AS membership_name_en

            FROM check_ins ci

            INNER JOIN members m
                ON m.id = ci.member_id

            LEFT JOIN member_memberships mm
                ON mm.id = ci.membership_id

            LEFT JOIN membership_plans mp
                ON mp.id = mm.membership_plan_id

            """;


    // =========================================================
    // FIND ALL
    // =========================================================

    public List<CheckIn> findAll() throws SQLException {

        String sql =
                BASE_SELECT
                        + """
                        ORDER BY ci.id DESC
                        """;

        List<CheckIn> records =
                new ArrayList<>();


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            while (resultSet.next()) {

                records.add(
                        mapCheckIn(resultSet)
                );
            }
        }

        return records;
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    public Optional<CheckIn> findById(
            int id
    ) throws SQLException {

        String sql =
                BASE_SELECT
                        + """
                        WHERE ci.id = ?
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
                            mapCheckIn(resultSet)
                    );
                }
            }
        }

        return Optional.empty();
    }


    // =========================================================
    // FIND TODAY
    // =========================================================

    public List<CheckIn> findToday()
            throws SQLException {

        LocalDate today =
                LocalDate.now();

        List<CheckIn> result =
                new ArrayList<>();


        for (CheckIn checkIn : findAll()) {

            if (checkIn.getCheckInTime() != null
                    && checkIn.getCheckInTime()
                            .toLocalDate()
                            .equals(today)) {

                result.add(checkIn);
            }
        }

        return result;
    }


    // =========================================================
    // FIND OPEN CHECK-IN
    // =========================================================

    public Optional<CheckIn> findOpenByMemberId(
            int memberId
    ) throws SQLException {

        String sql =
                BASE_SELECT
                        + """
                        WHERE ci.member_id = ?
                          AND ci.status = 'IN'
                          AND ci.check_out_time IS NULL
                        ORDER BY ci.id DESC
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

                if (resultSet.next()) {

                    return Optional.of(
                            mapCheckIn(resultSet)
                    );
                }
            }
        }

        return Optional.empty();
    }


    // =========================================================
    // INSERT
    // =========================================================

    public CheckIn insert(
            CheckIn checkIn
    ) throws SQLException {

        validate(checkIn);


        if (findOpenByMemberId(
                checkIn.getMemberId()
        ).isPresent()) {

            throw new SQLException(
                    "Member is already checked in."
            );
        }


        String sql = """

                INSERT INTO check_ins (
                    member_id,
                    membership_id,
                    check_in_time,
                    check_out_time,
                    status,
                    notes
                )

                VALUES (?, ?, ?, ?, ?, ?)

                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setInt(
                    1,
                    checkIn.getMemberId()
            );


            if (checkIn.getMembershipId() == null) {

                statement.setNull(
                        2,
                        Types.INTEGER
                );

            } else {

                statement.setInt(
                        2,
                        checkIn.getMembershipId()
                );
            }


            setLocalDateTime(
                    statement,
                    3,
                    checkIn.getCheckInTime()
            );


            setLocalDateTime(
                    statement,
                    4,
                    checkIn.getCheckOutTime()
            );


            statement.setString(
                    5,
                    normalizeStatus(
                            checkIn.getStatus()
                    )
            );


            statement.setString(
                    6,
                    safe(checkIn.getNotes())
            );


            int affected =
                    statement.executeUpdate();


            if (affected == 0) {

                throw new SQLException(
                        "Check-in was not created."
                );
            }


            try (
                    ResultSet keys =
                            statement.getGeneratedKeys()
            ) {

                if (keys.next()) {

                    checkIn.setId(
                            keys.getInt(1)
                    );
                }
            }
        }

        return checkIn;
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public boolean update(
            CheckIn checkIn
    ) throws SQLException {

        validate(checkIn);


        String sql = """

                UPDATE check_ins

                SET
                    member_id = ?,
                    membership_id = ?,
                    check_in_time = ?,
                    check_out_time = ?,
                    status = ?,
                    notes = ?,
                    updated_at = CURRENT_TIMESTAMP

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
                    checkIn.getMemberId()
            );


            if (checkIn.getMembershipId() == null) {

                statement.setNull(
                        2,
                        Types.INTEGER
                );

            } else {

                statement.setInt(
                        2,
                        checkIn.getMembershipId()
                );
            }


            setLocalDateTime(
                    statement,
                    3,
                    checkIn.getCheckInTime()
            );


            setLocalDateTime(
                    statement,
                    4,
                    checkIn.getCheckOutTime()
            );


            statement.setString(
                    5,
                    normalizeStatus(
                            checkIn.getStatus()
                    )
            );


            statement.setString(
                    6,
                    safe(checkIn.getNotes())
            );


            statement.setInt(
                    7,
                    checkIn.getId()
            );


            return statement.executeUpdate() > 0;
        }
    }


    // =========================================================
    // CHECK OUT
    // =========================================================

    public boolean checkOut(
            int checkInId
    ) throws SQLException {

        String sql = """

                UPDATE check_ins

                SET
                    check_out_time = ?,
                    status = 'OUT',
                    updated_at = CURRENT_TIMESTAMP

                WHERE id = ?
                  AND status = 'IN'
                  AND check_out_time IS NULL

                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            setLocalDateTime(
                    statement,
                    1,
                    LocalDateTime.now()
            );

            statement.setInt(
                    2,
                    checkInId
            );


            return statement.executeUpdate() > 0;
        }
    }


    // =========================================================
    // DELETE
    // =========================================================

    public boolean delete(
            int id
    ) throws SQLException {

        String sql = """

                DELETE FROM check_ins
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

            return statement.executeUpdate() > 0;
        }
    }


    // =========================================================
    // COUNTERS
    // =========================================================

    public int countToday()
            throws SQLException {

        return findToday().size();
    }


    public int countCurrentlyInside()
            throws SQLException {

        int count = 0;

        for (CheckIn record : findToday()) {

            if (record.isInside()) {
                count++;
            }
        }

        return count;
    }


    public int countCheckedOutToday()
            throws SQLException {

        int count = 0;

        for (CheckIn record : findToday()) {

            if (record.isCheckedOut()) {
                count++;
            }
        }

        return count;
    }


    // =========================================================
    // COUNT SESSIONS BY MEMBERSHIP
    // =========================================================

    /**
     * Counts consumed training sessions for one membership.
     *
     * Business rule:
     * one calendar day = one consumed training session.
     *
     * If the member checks in more than once on the same day,
     * all check-ins remain stored for attendance/audit purposes,
     * but that day consumes only one membership session.
     *
     * This implementation intentionally performs the distinct-date
     * calculation in Java so it remains portable across SQLite,
     * MySQL and SQL Server without relying on database-specific
     * DATE/CAST functions.
     */
    public int countByMembershipId(
            int membershipId
    ) throws SQLException {

        if (membershipId <= 0) {
            throw new IllegalArgumentException(
                    "Membership ID is invalid."
            );
        }

        String sql = """

                SELECT check_in_time
                FROM check_ins
                WHERE membership_id = ?
                ORDER BY id ASC

                """;

        java.util.Set<LocalDate> trainingDays =
                new java.util.HashSet<>();

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    membershipId
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                while (resultSet.next()) {

                    LocalDateTime checkInTime =
                            getLocalDateTime(
                                    resultSet,
                                    "check_in_time"
                            );

                    if (checkInTime != null) {
                        trainingDays.add(
                                checkInTime.toLocalDate()
                        );
                    }
                }
            }
        }

        return trainingDays.size();
    }


    // =========================================================
    // CHECK SESSION ALREADY COUNTED TODAY
    // =========================================================

    /**
     * Returns true when this membership already has at least one
     * check-in on the supplied calendar date.
     *
     * Useful for the UI to tell the user that a repeated check-in
     * today is being recorded without consuming another session.
     */
    public boolean hasCheckInForMembershipOnDate(
            int membershipId,
            LocalDate date
    ) throws SQLException {

        if (membershipId <= 0) {
            throw new IllegalArgumentException(
                    "Membership ID is invalid."
            );
        }

        if (date == null) {
            throw new IllegalArgumentException(
                    "Date is required."
            );
        }

        String sql = """

                SELECT check_in_time
                FROM check_ins
                WHERE membership_id = ?
                ORDER BY id DESC

                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    membershipId
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                while (resultSet.next()) {

                    LocalDateTime checkInTime =
                            getLocalDateTime(
                                    resultSet,
                                    "check_in_time"
                            );

                    if (checkInTime != null
                            && checkInTime.toLocalDate()
                                    .equals(date)) {

                        return true;
                    }
                }
            }
        }

        return false;
    }


    // =========================================================
    // MAP
    // =========================================================

    private CheckIn mapCheckIn(
            ResultSet resultSet
    ) throws SQLException {

        CheckIn checkIn =
                new CheckIn();


        checkIn.setId(
                resultSet.getInt("id")
        );

        checkIn.setMemberId(
                resultSet.getInt("member_id")
        );


        Object membershipId =
                resultSet.getObject(
                        "membership_id"
                );

        if (membershipId != null) {

            checkIn.setMembershipId(
                    ((Number) membershipId)
                            .intValue()
            );
        }


        checkIn.setCheckInTime(
                getLocalDateTime(
                        resultSet,
                        "check_in_time"
                )
        );


        checkIn.setCheckOutTime(
                getLocalDateTime(
                        resultSet,
                        "check_out_time"
                )
        );


        checkIn.setStatus(
                resultSet.getString(
                        "status"
                )
        );


        checkIn.setNotes(
                resultSet.getString(
                        "notes"
                )
        );


        checkIn.setCreatedAt(
                getLocalDateTime(
                        resultSet,
                        "created_at"
                )
        );


        checkIn.setUpdatedAt(
                getLocalDateTime(
                        resultSet,
                        "updated_at"
                )
        );


        checkIn.setMemberCode(
                resultSet.getString(
                        "member_code"
                )
        );


        checkIn.setMemberName(
                resultSet.getString(
                        "member_name"
                )
        );


        checkIn.setPhone(
                resultSet.getString(
                        "phone"
                )
        );


        checkIn.setMembershipNameAr(
                resultSet.getString(
                        "membership_name_ar"
                )
        );


        checkIn.setMembershipNameEn(
                resultSet.getString(
                        "membership_name_en"
                )
        );


        return checkIn;
    }


    // =========================================================
    // DATE TIME
    // =========================================================

    private void setLocalDateTime(
            PreparedStatement statement,
            int index,
            LocalDateTime value
    ) throws SQLException {

        if (value == null) {

            statement.setNull(
                    index,
                    Types.VARCHAR
            );

        } else {

            statement.setString(
                    index,
                    value.format(
                            DATE_TIME_FORMAT
                    )
            );
        }
    }


    private LocalDateTime getLocalDateTime(
            ResultSet resultSet,
            String column
    ) throws SQLException {

        String value =
                resultSet.getString(
                        column
                );


        if (value == null
                || value.isBlank()) {

            return null;
        }


        value = value.trim();


        try {

            return LocalDateTime.parse(
                    value,
                    DATE_TIME_FORMAT
            );

        } catch (Exception ignored) {
        }


        try {

            return LocalDateTime.parse(
                    value
            );

        } catch (Exception ignored) {
        }


        return null;
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void validate(
            CheckIn checkIn
    ) {

        if (checkIn == null) {

            throw new IllegalArgumentException(
                    "Check-in cannot be null."
            );
        }


        if (checkIn.getMemberId() <= 0) {

            throw new IllegalArgumentException(
                    "Member ID is required."
            );
        }


        if (checkIn.getCheckInTime() == null) {

            throw new IllegalArgumentException(
                    "Check-in time is required."
            );
        }
    }


    private String normalizeStatus(
            String status
    ) {

        if (status == null
                || status.isBlank()) {

            return "IN";
        }

        return status.trim()
                .toUpperCase();
    }


    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
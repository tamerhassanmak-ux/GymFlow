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


    public enum AttendanceField { ID, CODE, NAME, PHONE, PLAN_AR, PLAN_EN, CHECK_IN, CHECK_OUT, STATUS }
    public record AttendanceOrder(AttendanceField field, boolean ascending) {}
    public record AttendanceFilter(LocalDate from, LocalDate to, String search,
                                   boolean extendedSearch, List<AttendanceOrder> order) {
        public AttendanceFilter { order = List.copyOf(order); }
    }

    public com.mycompany.lazar_app.ui.paging.PageResult<CheckIn> findPage(
            AttendanceFilter filter, int page, int size) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            return findPage(connection, filter, page, size);
        }
    }

    // Connection overload enables isolated repository checks with temporary fixtures.
    com.mycompany.lazar_app.ui.paging.PageResult<CheckIn> findPage(
            Connection connection, AttendanceFilter filter, int page, int size) throws SQLException {
        if (size < 1 || size > 100 || page < 0) throw new IllegalArgumentException("Invalid page bounds");
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new IllegalArgumentException("Invalid attendance date range");
        }
        List<String> values = new ArrayList<>();
        String where = pageWhere(filter, values);
        java.util.Map<String, Long> stats = new java.util.HashMap<>(pageStatistics(connection, where, values));
        long total = stats.get("total");
        int safePage = (int) Math.min(page, Math.max(0, (total - 1) / size));
        if (filter.extendedSearch()) {
            // Daily KPI cards are independent of the current search and page.
            List<String> dailyValues = new ArrayList<>();
            String dailyWhere = pageWhere(new AttendanceFilter(filter.from(), filter.to(), "", false, List.of()), dailyValues);
            java.util.Map<String, Long> daily = pageStatistics(connection, dailyWhere, dailyValues);
            stats.put("today", daily.get("total"));
            stats.put("todayInside", daily.get("inside"));
            stats.put("todayOut", daily.get("out"));
            try (PreparedStatement active = connection.prepareStatement("""
                    SELECT COUNT(*) FROM member_memberships mm
                    WHERE mm.id = (SELECT MAX(latest.id) FROM member_memberships latest WHERE latest.member_id = mm.member_id)
                      AND UPPER(COALESCE(mm.status, '')) = 'ACTIVE'
                      AND mm.start_date <= ? AND mm.end_date >= ?
                    """)) {
                active.setString(1, LocalDate.now().toString());
                active.setString(2, LocalDate.now().toString());
                try (ResultSet result = active.executeQuery()) {
                    stats.put("activeMembers", result.next() ? result.getLong(1) : 0);
                }
            }
        }
        List<CheckIn> rows = new ArrayList<>();
        if (total > 0) {
            String sql = BASE_SELECT + where + pageOrder(filter.order()) + PagingSql.limit(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int position = 1;
                for (String value : values) statement.setString(position++, value);
                long offset = (long) safePage * size;
                if (PagingSql.offsetFirst(connection)) {
                    statement.setLong(position++, offset);
                    statement.setInt(position, size);
                } else {
                    statement.setInt(position++, size);
                    statement.setLong(position, offset);
                }
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) rows.add(mapCheckIn(result));
                }
            }
        }
        return new com.mycompany.lazar_app.ui.paging.PageResult<>(rows, total, safePage, stats);
    }

    private java.util.Map<String, Long> pageStatistics(Connection connection, String where,
                                                      List<String> values) throws SQLException {
        String from = BASE_SELECT.substring(BASE_SELECT.indexOf("FROM check_ins"));
        String sql = "SELECT COUNT(*) AS total, COALESCE(SUM(CASE WHEN UPPER(COALESCE(ci.status,'')) = 'IN' "
                + "AND ci.check_out_time IS NULL THEN 1 ELSE 0 END),0) AS inside_count " + from + where;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) statement.setString(i + 1, values.get(i));
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                long total = result.getLong("total");
                long inside = result.getLong("inside_count");
                return java.util.Map.of("total", total, "inside", inside, "out", total - inside);
            }
        }
    }

    private String pageWhere(AttendanceFilter filter, List<String> values) {
        StringBuilder where = new StringBuilder(" WHERE ci.check_in_time IS NOT NULL ");
        if (filter.from() != null) {
            where.append(" AND ci.check_in_time >= ? ");
            values.add(filter.from().atStartOfDay().format(DATE_TIME_FORMAT));
        }
        if (filter.to() != null && !filter.to().equals(LocalDate.MAX)) {
            where.append(" AND ci.check_in_time < ? ");
            values.add(filter.to().plusDays(1).atStartOfDay().format(DATE_TIME_FORMAT));
        }
        if (!PagingSql.normalize(filter.search()).isBlank()) {
            List<String> columns = new ArrayList<>(List.of("m.name", "m.member_code", "m.phone"));
            if (filter.extendedSearch()) columns.addAll(List.of("mp.name_ar", "mp.name_en", "ci.status"));
            where.append(" AND (");
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) where.append(" OR ");
                where.append(PagingSql.normalizedColumn(columns.get(i))).append(" LIKE ? ESCAPE '!' ");
                values.add(PagingSql.pattern(filter.search()));
            }
            where.append(") ");
        }
        return where.toString();
    }

    private String pageOrder(List<AttendanceOrder> orders) {
        StringBuilder sql = new StringBuilder(" ORDER BY ");
        for (AttendanceOrder order : orders) {
            String field = switch (order.field()) {
                case ID -> "ci.id";
                case CODE -> "m.member_code";
                case NAME -> "m.name";
                case PHONE -> "m.phone";
                case PLAN_AR -> "COALESCE(NULLIF(mp.name_ar,''),mp.name_en,'')";
                case PLAN_EN -> "COALESCE(mp.name_en,'')";
                case CHECK_IN -> "ci.check_in_time";
                case CHECK_OUT -> "ci.check_out_time";
                case STATUS -> "CASE WHEN UPPER(COALESCE(ci.status,'')) = 'IN' AND ci.check_out_time IS NULL THEN 0 ELSE 1 END";
            };
            sql.append(field).append(order.ascending() ? " ASC, " : " DESC, ");
        }
        // Unique tie-breaker keeps adjacent pages disjoint even with equal dates/names.
        return sql.append("ci.check_in_time DESC, ci.id DESC ").toString();
    }

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

    public List<CheckIn> findToday() throws SQLException {
        LocalDate today = LocalDate.now();
        List<CheckIn> records = new ArrayList<>();

        // Other database engines retain their existing legacy date handling.
        if (com.mycompany.lazar_app.database.DatabaseConfigManager.load()
                .getType() != com.mycompany.lazar_app.database.DatabaseType.MYSQL) {
            for (CheckIn item : findAll()) {
                if (item.getCheckInTime() != null
                        && today.equals(item.getCheckInTime().toLocalDate())) {
                    records.add(item);
                }
            }
            return records;
        }

        // Bind DATETIME boundaries as calendar strings, not JDBC Timestamps,
        // to avoid client default-time-zone conversion while binding.
        // With a MySQL TIMESTAMP column, session time_zone still matters.
        String sql = BASE_SELECT + """
                WHERE ci.check_in_time >= ?
                  AND ci.check_in_time < ?
                ORDER BY ci.check_in_time DESC, ci.id DESC
                """;

        String startOfDay = today.atStartOfDay().format(DATE_TIME_FORMAT);
        String startOfNextDay = today.plusDays(1).atStartOfDay()
                .format(DATE_TIME_FORMAT);

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, startOfDay);
            statement.setString(2, startOfNextDay);

            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    CheckIn record = mapCheckIn(results);

                    // Defensive check: the attendance screen must never show
                    // a different local calendar date.
                    if (record.getCheckInTime() != null
                            && today.equals(record.getCheckInTime().toLocalDate())) {
                        records.add(record);
                    } else {
                        System.err.println(
                                "[GymFlow] Attendance date mismatch: id="
                                        + record.getId()
                                        + ", checkInTime=" + record.getCheckInTime()
                                        + ", appDate=" + today);
                    }
                }
            }
        }
        return records;
    }

    /**
     * Close unfinished attendance from earlier calendar days.
     * Uses Java date parsing so SQLite, MySQL and SQL Server work alike.
     * Closed at 23:59:59 on the ORIGINAL attendance date, not today.
     * Returns the number of records actually closed.
     */
    public int autoCheckOutPreviousDays() throws SQLException {
        LocalDate today = LocalDate.now();
        String selectSql = "SELECT id, check_in_time FROM check_ins "
                + "WHERE status = 'IN' AND check_out_time IS NULL";
        String updateSql = "UPDATE check_ins SET check_out_time = ?, "
                + "status = 'OUT', updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND status = 'IN' AND check_out_time IS NULL";

        try (Connection connection = DatabaseManager.getConnection()) {
            // Read first and close the result set before writing, for DB portability.
            List<Integer> ids = new ArrayList<>();
            List<LocalDateTime> checkoutTimes = new ArrayList<>();
            try (PreparedStatement query = connection.prepareStatement(selectSql);
                 ResultSet results = query.executeQuery()) {
                while (results.next()) {
                    LocalDateTime checkedIn = getLocalDateTime(results, "check_in_time");
                    if (checkedIn != null && checkedIn.toLocalDate().isBefore(today)) {
                        ids.add(results.getInt("id"));
                        checkoutTimes.add(checkedIn.toLocalDate().atTime(23, 59, 59));
                    }
                }
            }
            if (ids.isEmpty()) {
                return 0;
            }
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                for (int i = 0; i < ids.size(); i++) {
                    setLocalDateTime(update, 1, checkoutTimes.get(i));
                    update.setInt(2, ids.get(i));
                    update.addBatch();
                }
                int count = 0;
                for (int result : update.executeBatch()) {
                    if (result == Statement.SUCCESS_NO_INFO) {
                        count++;
                    } else if (result > 0) {
                        count += result;
                    }
                }
                connection.commit();
                return count;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    // =========================================================
    // FIND OPEN CHECK-IN
    // =========================================================

    public Optional<CheckIn> findOpenByMemberId(
            int memberId
    ) throws SQLException {

        autoCheckOutPreviousDays();

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


    private LocalDateTime getLocalDateTime(ResultSet resultSet, String column)
            throws SQLException {
        String value = resultSet.getString(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        // Some JDBC drivers return microsecond precision, e.g.
        // 2026-11-01 10:20:30.123456. ISO parsing accepts that precision.
        try {
            return LocalDateTime.parse(normalized.replace(' ', 'T'));
        } catch (java.time.format.DateTimeParseException ex) {
            // Do not silently change an invalid date to NULL: the calling
            // controller can log the real data conversion problem.
            throw new SQLException("Invalid datetime in " + column + ": "
                    + normalized, ex);
        }
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
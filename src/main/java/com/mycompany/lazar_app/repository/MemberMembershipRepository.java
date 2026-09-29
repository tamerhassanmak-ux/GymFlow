package com.mycompany.lazar_app.repository;

import com.mycompany.lazar_app.database.DatabaseManager;
import com.mycompany.lazar_app.model.MemberMembership;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class MemberMembershipRepository {


    // =========================================================
    // DATE FORMAT
    // =========================================================

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE;


    // =========================================================
    // BASE SELECT
    // =========================================================

    private static final String BASE_SELECT = """
            SELECT
                mm.id,
                mm.member_id,
                mm.membership_plan_id,
                mm.start_date,
                mm.end_date,
                mm.allowed_sessions,
                mm.price,
                mm.discount,
                mm.final_price,
                mm.status,
                mm.notes,
                mm.created_at,
                mm.updated_at,

                m.member_code,
                m.name AS member_name,

                mp.code AS plan_code,
                mp.name_ar AS plan_name_ar,
                mp.name_en AS plan_name_en

            FROM member_memberships mm

            INNER JOIN members m
                ON m.id = mm.member_id

            INNER JOIN membership_plans mp
                ON mp.id = mm.membership_plan_id
            """;


    // =========================================================
    // FIND ALL
    // =========================================================

    public List<MemberMembership> findAll()
            throws SQLException {

        String sql =
                BASE_SELECT
                + """
                  
                  ORDER BY mm.id DESC
                  """;


        List<MemberMembership> memberships =
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

                memberships.add(
                        mapMembership(
                                resultSet
                        )
                );
            }
        }


        return memberships;
    }


    // =========================================================
    // FIND LATEST MEMBERSHIP PER MEMBER
    // =========================================================

    public List<MemberMembership> findLatestPerMember()
            throws SQLException {

        /*
         * Keeps the complete membership history in the database,
         * but returns only the latest membership for each member.
         *
         * This prevents duplicate member names in the main
         * memberships table.
         */

        String sql =
                BASE_SELECT
                + """
                  
                  WHERE mm.id = (
                      SELECT MAX(mm2.id)
                      FROM member_memberships mm2
                      WHERE mm2.member_id = mm.member_id
                  )

                  ORDER BY mm.id DESC
                  """;


        List<MemberMembership> memberships =
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

                memberships.add(
                        mapMembership(
                                resultSet
                        )
                );
            }
        }


        return memberships;
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    public Optional<MemberMembership> findById(
            int id
    ) throws SQLException {

        String sql =
                BASE_SELECT
                + """
                  
                  WHERE mm.id = ?
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
                            mapMembership(
                                    resultSet
                            )
                    );
                }
            }
        }


        return Optional.empty();
    }


    // =========================================================
    // FIND BY MEMBER
    // =========================================================

    public List<MemberMembership> findByMemberId(
            int memberId
    ) throws SQLException {

        String sql =
                BASE_SELECT
                + """
                  
                  WHERE mm.member_id = ?
                  ORDER BY mm.start_date DESC, mm.id DESC
                  """;


        List<MemberMembership> memberships =
                new ArrayList<>();


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

                while (resultSet.next()) {

                    memberships.add(
                            mapMembership(
                                    resultSet
                            )
                    );
                }
            }
        }


        return memberships;
    }


    // =========================================================
    // FIND LATEST MEMBERSHIP BY MEMBER
    // =========================================================

    public Optional<MemberMembership> findLatestByMemberId(
            int memberId
    ) throws SQLException {

        if (memberId <= 0) {
            return Optional.empty();
        }

        List<MemberMembership> memberships =
                findByMemberId(
                        memberId
                );

        return memberships.isEmpty()
                ? Optional.empty()
                : Optional.of(
                        memberships.get(0)
                );
    }


    // =========================================================
    // FIND CURRENT OR LATEST MEMBERSHIP
    // =========================================================

    public Optional<MemberMembership> findCurrentOrLatestByMemberId(
            int memberId
    ) throws SQLException {

        Optional<MemberMembership> current =
                findCurrentMembershipByMemberId(
                        memberId
                );

        return current.isPresent()
                ? current
                : findLatestByMemberId(
                        memberId
                );
    }


    // =========================================================
    // FIND CURRENT MEMBERSHIP BY MEMBER
    // =========================================================

    public Optional<MemberMembership> findCurrentMembershipByMemberId(
            int memberId
    ) throws SQLException {

        /*
         * We intentionally read the member's memberships and
         * compare LocalDate values in Java.
         *
         * This keeps compatibility with legacy SQLite records
         * that may contain Unix timestamps.
         */

        List<MemberMembership> memberships =
                findByMemberId(
                        memberId
                );


        LocalDate today =
                LocalDate.now();


        for (MemberMembership membership : memberships) {

            if (!"Active".equalsIgnoreCase(
                    safe(
                            membership.getStatus()
                    )
            )) {

                continue;
            }


            LocalDate startDate =
                    membership.getStartDate();

            LocalDate endDate =
                    membership.getEndDate();


            if (startDate == null
                    || endDate == null) {

                continue;
            }


            boolean alreadyStarted =
                    !startDate.isAfter(
                            today
                    );


            boolean notExpired =
                    !endDate.isBefore(
                            today
                    );


            if (alreadyStarted
                    && notExpired) {

                return Optional.of(
                        membership
                );
            }
        }


        return Optional.empty();
    }


    // =========================================================
    // CHECK OVERLAPPING MEMBERSHIP
    // =========================================================

    public Optional<MemberMembership> findOverlappingMembership(
            int memberId,
            LocalDate newStartDate,
            LocalDate newEndDate
    ) throws SQLException {

        if (memberId <= 0
                || newStartDate == null
                || newEndDate == null) {

            return Optional.empty();
        }


        List<MemberMembership> memberships =
                findByMemberId(
                        memberId
                );


        for (MemberMembership membership : memberships) {

            if ("Cancelled".equalsIgnoreCase(
                    safe(
                            membership.getStatus()
                    )
            )) {

                continue;
            }


            LocalDate existingStart =
                    membership.getStartDate();

            LocalDate existingEnd =
                    membership.getEndDate();


            if (existingStart == null
                    || existingEnd == null) {

                continue;
            }


            /*
             * Two periods overlap when:
             *
             * newStart <= existingEnd
             * AND
             * newEnd >= existingStart
             */

            boolean overlaps =
                    !newStartDate.isAfter(
                            existingEnd
                    )
                    &&
                    !newEndDate.isBefore(
                            existingStart
                    );


            if (overlaps) {

                return Optional.of(
                        membership
                );
            }
        }


        return Optional.empty();
    }


    // =========================================================
    // FIND ACTIVE
    // =========================================================

    public List<MemberMembership> findActive()
            throws SQLException {

        List<MemberMembership> all =
                findAll();


        List<MemberMembership> active =
                new ArrayList<>();


        LocalDate today =
                LocalDate.now();


        for (MemberMembership membership : all) {

            if (!"Active".equalsIgnoreCase(
                    safe(
                            membership.getStatus()
                    )
            )) {

                continue;
            }


            LocalDate startDate =
                    membership.getStartDate();

            LocalDate endDate =
                    membership.getEndDate();


            if (startDate == null
                    || endDate == null) {

                continue;
            }


            if (!startDate.isAfter(today)
                    && !endDate.isBefore(today)) {

                active.add(
                        membership
                );
            }
        }


        return active;
    }


    // =========================================================
    // FIND EXPIRING SOON
    // =========================================================

    public List<MemberMembership> findExpiringSoon(
            int days
    ) throws SQLException {

        if (days < 0) {

            throw new IllegalArgumentException(
                    "Days cannot be negative."
            );
        }


        LocalDate today =
                LocalDate.now();

        LocalDate limit =
                today.plusDays(
                        days
                );


        List<MemberMembership> result =
                new ArrayList<>();


        for (MemberMembership membership : findAll()) {

            if (!"Active".equalsIgnoreCase(
                    safe(
                            membership.getStatus()
                    )
            )) {

                continue;
            }


            LocalDate endDate =
                    membership.getEndDate();


            if (endDate == null) {

                continue;
            }


            if (!endDate.isBefore(today)
                    && !endDate.isAfter(limit)) {

                result.add(
                        membership
                );
            }
        }


        result.sort(
                (first, second) ->
                        first.getEndDate()
                                .compareTo(
                                        second.getEndDate()
                                )
        );


        return result;
    }


    // =========================================================
    // INSERT
    // =========================================================

    public MemberMembership insert(
            MemberMembership membership
    ) throws SQLException {

        validateMembershipCore(
                membership
        );


        // =====================================================
        // REPOSITORY-LEVEL OVERLAP PROTECTION
        // =====================================================

        Optional<MemberMembership> overlapping =
                findOverlappingMembership(
                        membership.getMemberId(),
                        membership.getStartDate(),
                        membership.getEndDate()
                );


        if (overlapping.isPresent()) {

            MemberMembership existing =
                    overlapping.get();


            throw new SQLException(
                    "Member already has a membership during this period. "
                    + "Existing membership ID: "
                    + existing.getId()
            );
        }


        String sql = """
                INSERT INTO member_memberships (
                    member_id,
                    membership_plan_id,
                    start_date,
                    end_date,
                    allowed_sessions,
                    price,
                    discount,
                    final_price,
                    status,
                    notes
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection()
        ) {

            connection.setAutoCommit(
                    false
            );


            try {

                /*
                 * IMPORTANT DOMAIN RULE
                 * ---------------------
                 * The controller does not own plan price/session values.
                 * We always read the plan from the database and snapshot
                 * those commercial terms into the new membership.
                 */
                PlanSnapshot planSnapshot =
                        loadPlanSnapshot(
                                connection,
                                membership.getMembershipPlanId()
                        );


                membership.setAllowedSessions(
                        planSnapshot.maxSessions()
                );


                membership.setPlanPriceSnapshot(
                        planSnapshot.price()
                );


                validateCommercialTerms(
                        membership
                );


                membership.calculateFinalPrice();


                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        sql,
                                        Statement.RETURN_GENERATED_KEYS
                                )
                ) {

                    bindMembershipValues(
                            statement,
                            membership,
                            false
                    );


                    int affectedRows =
                            statement.executeUpdate();


                    if (affectedRows == 0) {

                        throw new SQLException(
                                "Creating membership failed. No rows were inserted."
                        );
                    }


                    try (
                            ResultSet generatedKeys =
                                    statement.getGeneratedKeys()
                    ) {

                        if (!generatedKeys.next()) {

                            throw new SQLException(
                                    "Creating membership failed. Database did not return an ID."
                            );
                        }


                        membership.setId(
                                generatedKeys.getInt(1)
                        );
                    }
                }


                connection.commit();

                return membership;


            } catch (Exception exception) {

                connection.rollback();


                if (exception instanceof SQLException sqlException) {

                    throw sqlException;
                }


                throw new SQLException(
                        "Unable to create membership.",
                        exception
                );

            } finally {

                connection.setAutoCommit(
                        true
                );
            }
        }
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public boolean update(
            MemberMembership membership
    ) throws SQLException {

        validateMembershipCore(
                membership
        );


        if (membership.getId() <= 0) {

            throw new IllegalArgumentException(
                    "Membership ID is invalid."
            );
        }


        /*
         * When editing an existing membership we check
         * other memberships for overlapping periods,
         * excluding the record currently being edited.
         */

        List<MemberMembership> memberMemberships =
                findByMemberId(
                        membership.getMemberId()
                );


        for (MemberMembership existing : memberMemberships) {

            if (existing.getId()
                    == membership.getId()) {

                continue;
            }


            if ("Cancelled".equalsIgnoreCase(
                    safe(
                            existing.getStatus()
                    )
            )) {

                continue;
            }


            LocalDate existingStart =
                    existing.getStartDate();

            LocalDate existingEnd =
                    existing.getEndDate();


            if (existingStart == null
                    || existingEnd == null) {

                continue;
            }


            boolean overlaps =
                    !membership
                            .getStartDate()
                            .isAfter(existingEnd)
                    &&
                    !membership
                            .getEndDate()
                            .isBefore(existingStart);


            if (overlaps) {

                throw new SQLException(
                        "Membership period overlaps another membership. "
                        + "Existing membership ID: "
                        + existing.getId()
                );
            }
        }


        String sql = """
                UPDATE member_memberships
                SET
                    member_id = ?,
                    membership_plan_id = ?,
                    start_date = ?,
                    end_date = ?,
                    allowed_sessions = ?,
                    price = ?,
                    discount = ?,
                    final_price = ?,
                    status = ?,
                    notes = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection()
        ) {

            connection.setAutoCommit(
                    false
            );


            try {

                MembershipSnapshot existingSnapshot =
                        loadExistingMembershipSnapshot(
                                connection,
                                membership.getId()
                        );


                /*
                 * Preserve the original commercial snapshot while editing
                 * the same plan. If the operator explicitly changes the
                 * plan, snapshot the new plan's current terms.
                 */
                if (existingSnapshot.membershipPlanId()
                        == membership.getMembershipPlanId()) {

                    membership.setAllowedSessions(
                            existingSnapshot.allowedSessions()
                    );

                    membership.setPlanPriceSnapshot(
                            existingSnapshot.price()
                    );

                } else {

                    PlanSnapshot planSnapshot =
                            loadPlanSnapshot(
                                    connection,
                                    membership.getMembershipPlanId()
                            );

                    membership.setAllowedSessions(
                            planSnapshot.maxSessions()
                    );

                    membership.setPlanPriceSnapshot(
                            planSnapshot.price()
                    );
                }


                validateCommercialTerms(
                        membership
                );


                membership.calculateFinalPrice();


                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        sql
                                )
                ) {

                    bindMembershipValues(
                            statement,
                            membership,
                            true
                    );


                    boolean updated =
                            statement.executeUpdate()
                                    > 0;


                    if (!updated) {

                        connection.rollback();

                        return false;
                    }
                }


                connection.commit();

                return true;


            } catch (Exception exception) {

                connection.rollback();


                if (exception instanceof SQLException sqlException) {

                    throw sqlException;
                }


                throw new SQLException(
                        "Unable to update membership.",
                        exception
                );

            } finally {

                connection.setAutoCommit(
                        true
                );
            }
        }
    }


    // =========================================================
    // CANCEL
    // =========================================================

    public boolean cancel(
            int membershipId
    ) throws SQLException {

        String sql = """
                UPDATE member_memberships
                SET
                    status = 'Cancelled',
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
                    membershipId
            );


            return statement.executeUpdate()
                    > 0;
        }
    }


    // =========================================================
    // DELETE
    // =========================================================

    public boolean delete(
            int membershipId
    ) throws SQLException {

        if (membershipId <= 0) {

            throw new IllegalArgumentException(
                    "Membership ID is invalid."
            );
        }


        /*
         * Historical subscriptions that already have financial or
         * attendance activity must never be physically deleted.
         * Use cancel(...) instead so audit history remains intact.
         */
        if (hasDependentActivity(
                membershipId
        )) {

            throw new SQLException(
                    "Membership has payments or check-ins and cannot be deleted. "
                    + "Cancel the membership instead."
            );
        }


        String sql = """
                DELETE FROM member_memberships
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
                    membershipId
            );


            return statement.executeUpdate()
                    > 0;
        }
    }


    // =========================================================
    // DEPENDENT ACTIVITY
    // =========================================================

    private boolean hasDependentActivity(
            int membershipId
    ) throws SQLException {

        String sql = """
                SELECT
                    (
                        SELECT COUNT(*)
                        FROM payments
                        WHERE membership_id = ?
                    )
                    +
                    (
                        SELECT COUNT(*)
                        FROM check_ins
                        WHERE membership_id = ?
                    ) AS dependency_count
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        )
        ) {

            statement.setInt(
                    1,
                    membershipId
            );

            statement.setInt(
                    2,
                    membershipId
            );


            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                return resultSet.next()
                        && resultSet.getInt(
                                "dependency_count"
                        ) > 0;
            }
        }
    }


    // =========================================================
    // COUNT ALL
    // =========================================================

    public int count()
            throws SQLException {

        return findLatestPerMember()
                .size();
    }


    // =========================================================
    // COUNT ACTIVE
    // =========================================================

    public int countActive()
            throws SQLException {

        int count =
                0;


        LocalDate today =
                LocalDate.now();


        for (
                MemberMembership membership
                : findLatestPerMember()
        ) {

            if (!"Active".equalsIgnoreCase(
                    safe(
                            membership.getStatus()
                    )
            )) {

                continue;
            }


            LocalDate start =
                    membership.getStartDate();

            LocalDate end =
                    membership.getEndDate();


            if (start != null
                    && end != null
                    && !start.isAfter(today)
                    && !end.isBefore(today)) {

                count++;
            }
        }


        return count;
    }


    // =========================================================
    // COUNT EXPIRED
    // =========================================================

    public int countExpired()
            throws SQLException {

        int count =
                0;


        LocalDate today =
                LocalDate.now();


        for (
                MemberMembership membership
                : findLatestPerMember()
        ) {

            LocalDate endDate =
                    membership.getEndDate();


            if (endDate != null
                    && endDate.isBefore(today)) {

                count++;
            }
        }


        return count;
    }


    // =========================================================
    // COUNT EXPIRING SOON
    // =========================================================

    public int countExpiringSoon(
            int days
    ) throws SQLException {

        if (days < 0) {

            throw new IllegalArgumentException(
                    "Days cannot be negative."
            );
        }


        LocalDate today =
                LocalDate.now();

        LocalDate limit =
                today.plusDays(
                        days
                );


        int count =
                0;


        for (
                MemberMembership membership
                : findLatestPerMember()
        ) {

            if (!"Active".equalsIgnoreCase(
                    safe(
                            membership.getStatus()
                    )
            )) {

                continue;
            }


            LocalDate endDate =
                    membership.getEndDate();


            if (endDate != null
                    && !endDate.isBefore(today)
                    && !endDate.isAfter(limit)) {

                count++;
            }
        }


        return count;
    }


    // =========================================================
    // TOTAL REVENUE
    // =========================================================

    public double getTotalRevenue()
            throws SQLException {

        String sql = """
                SELECT COALESCE(
                    SUM(final_price),
                    0
                )
                FROM member_memberships
                WHERE status <> 'Cancelled'
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

                return resultSet.getDouble(
                        1
                );
            }
        }


        return 0;
    }


    // =========================================================
    // MONTHLY REVENUE
    // =========================================================

    public double getMonthlyRevenue()
            throws SQLException {

        LocalDate firstDay =
                LocalDate.now()
                        .withDayOfMonth(1);


        LocalDate firstDayNextMonth =
                firstDay.plusMonths(1);


        /*
         * We calculate in Java for compatibility with both:
         *
         * 1. Old SQLite timestamp records
         * 2. New ISO yyyy-MM-dd records
         */

        double total =
                0;


        for (MemberMembership membership : findAll()) {

            if ("Cancelled".equalsIgnoreCase(
                    safe(
                            membership.getStatus()
                    )
            )) {

                continue;
            }


            LocalDate startDate =
                    membership.getStartDate();


            if (startDate == null) {

                continue;
            }


            if (!startDate.isBefore(firstDay)
                    && startDate.isBefore(firstDayNextMonth)) {

                total +=
                        membership.getFinalPrice();
            }
        }


        return total;
    }


    // =========================================================
    // MAP RESULT SET
    // =========================================================

    private MemberMembership mapMembership(
            ResultSet resultSet
    ) throws SQLException {

        MemberMembership membership =
                new MemberMembership();


        membership.setId(
                resultSet.getInt(
                        "id"
                )
        );


        membership.setMemberId(
                resultSet.getInt(
                        "member_id"
                )
        );


        membership.setMembershipPlanId(
                resultSet.getInt(
                        "membership_plan_id"
                )
        );


        membership.setStartDate(
                getLocalDate(
                        resultSet,
                        "start_date"
                )
        );


        membership.setEndDate(
                getLocalDate(
                        resultSet,
                        "end_date"
                )
        );


        membership.setAllowedSessions(
                resultSet.getInt(
                        "allowed_sessions"
                )
        );


        membership.setPlanPriceSnapshot(
                resultSet.getDouble(
                        "price"
                )
        );


        membership.setDiscount(
                resultSet.getDouble(
                        "discount"
                )
        );


        membership.setFinalPrice(
                resultSet.getDouble(
                        "final_price"
                )
        );


        membership.setStatus(
                resultSet.getString(
                        "status"
                )
        );


        membership.setNotes(
                resultSet.getString(
                        "notes"
                )
        );


        membership.setCreatedAt(
                getLocalDateTime(
                        resultSet,
                        "created_at"
                )
        );


        membership.setUpdatedAt(
                getLocalDateTime(
                        resultSet,
                        "updated_at"
                )
        );


        membership.setMemberCode(
                resultSet.getString(
                        "member_code"
                )
        );


        membership.setMemberName(
                resultSet.getString(
                        "member_name"
                )
        );


        membership.setPlanCode(
                resultSet.getString(
                        "plan_code"
                )
        );


        membership.setPlanNameAr(
                resultSet.getString(
                        "plan_name_ar"
                )
        );


        membership.setPlanNameEn(
                resultSet.getString(
                        "plan_name_en"
                )
        );


        return membership;
    }


    // =========================================================
    // SAVE LOCAL DATE
    // =========================================================

    private void setLocalDate(
            PreparedStatement statement,
            int index,
            LocalDate date
    ) throws SQLException {

        /*
         * IMPORTANT:
         *
         * Store LocalDate as ISO text:
         *
         * 2026-09-21
         *
         * instead of SQLite JDBC milliseconds:
         *
         * 1789938000000
         */

        if (date == null) {

            statement.setNull(
                    index,
                    Types.VARCHAR
            );

            return;
        }


        statement.setString(
                index,
                date.format(
                        DATE_FORMAT
                )
        );
    }


    // =========================================================
    // READ LOCAL DATE
    // =========================================================

    private LocalDate getLocalDate(
            ResultSet resultSet,
            String columnName
    ) throws SQLException {

        Object value =
                resultSet.getObject(
                        columnName
                );


        if (value == null) {

            return null;
        }


        // =====================================================
        // SQL DATE
        // =====================================================

        if (value instanceof java.sql.Date sqlDate) {

            return sqlDate.toLocalDate();
        }


        // =====================================================
        // SQL TIMESTAMP
        // =====================================================

        if (value instanceof java.sql.Timestamp timestamp) {

            return timestamp
                    .toLocalDateTime()
                    .toLocalDate();
        }


        // =====================================================
        // NUMBER
        // =====================================================

        if (value instanceof Number number) {

            return timestampToLocalDate(
                    number.longValue()
            );
        }


        // =====================================================
        // STRING
        // =====================================================

        String text =
                value.toString()
                        .trim();


        if (text.isBlank()) {

            return null;
        }


        // =====================================================
        // NUMERIC STRING
        // =====================================================

        if (text.matches(
                "-?\\d+"
        )) {

            try {

                long timestamp =
                        Long.parseLong(
                                text
                        );


                return timestampToLocalDate(
                        timestamp
                );


            } catch (NumberFormatException ignored) {

                // Continue parsing.
            }
        }


        // =====================================================
        // ISO LOCAL DATE
        // =====================================================

        try {

            return LocalDate.parse(
                    text,
                    DATE_FORMAT
            );


        } catch (Exception ignored) {

            // Continue.
        }


        // =====================================================
        // ISO DATE TIME
        // =====================================================

        try {

            return LocalDateTime
                    .parse(
                            text.replace(
                                    " ",
                                    "T"
                            )
                    )
                    .toLocalDate();


        } catch (Exception ignored) {

            // Continue.
        }


        throw new SQLException(
                "Unsupported date format in column '"
                + columnName
                + "': "
                + text
        );
    }


    // =========================================================
    // TIMESTAMP -> LOCAL DATE
    // =========================================================

    private LocalDate timestampToLocalDate(
            long timestamp
    ) {

        /*
         * Values above 10 billion are treated as milliseconds.
         */

        Instant instant;


        if (Math.abs(timestamp)
                > 10_000_000_000L) {

            instant =
                    Instant.ofEpochMilli(
                            timestamp
                    );

        } else {

            instant =
                    Instant.ofEpochSecond(
                            timestamp
                    );
        }


        return instant
                .atZone(
                        ZoneId.systemDefault()
                )
                .toLocalDate();
    }


    // =========================================================
    // READ LOCAL DATE TIME
    // =========================================================

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


        String normalized =
                value.trim()
                        .replace(
                                " ",
                                "T"
                        );


        try {

            return LocalDateTime.parse(
                    normalized
            );


        } catch (Exception exception) {

            throw new SQLException(
                    "Unsupported datetime format in column '"
                    + column
                    + "': "
                    + value,
                    exception
            );
        }
    }


    // =========================================================
    // VALIDATE MEMBERSHIP CORE
    // =========================================================

    private void validateMembershipCore(
            MemberMembership membership
    ) {

        if (membership == null) {

            throw new IllegalArgumentException(
                    "Membership cannot be null."
            );
        }


        if (membership.getMemberId()
                <= 0) {

            throw new IllegalArgumentException(
                    "Member ID is invalid."
            );
        }


        if (membership.getMembershipPlanId()
                <= 0) {

            throw new IllegalArgumentException(
                    "Membership plan ID is invalid."
            );
        }


        if (membership.getStartDate()
                == null) {

            throw new IllegalArgumentException(
                    "Membership start date is required."
            );
        }


        if (membership.getEndDate()
                == null) {

            throw new IllegalArgumentException(
                    "Membership end date is required."
            );
        }


        if (membership
                .getEndDate()
                .isBefore(
                        membership.getStartDate()
                )) {

            throw new IllegalArgumentException(
                    "Membership end date cannot be before start date."
            );
        }


        if (membership.getDiscount()
                < 0) {

            throw new IllegalArgumentException(
                    "Membership discount cannot be negative."
            );
        }
    }


    // =========================================================
    // VALIDATE COMMERCIAL SNAPSHOT
    // =========================================================

    private void validateCommercialTerms(
            MemberMembership membership
    ) {

        if (membership.getAllowedSessions()
                <= 0) {

            throw new IllegalArgumentException(
                    "Membership plan must allow at least one session."
            );
        }


        if (membership.getPlanPriceSnapshot()
                < 0) {

            throw new IllegalArgumentException(
                    "Membership plan price cannot be negative."
            );
        }


        if (membership.getDiscount()
                > membership.getPlanPriceSnapshot()) {

            throw new IllegalArgumentException(
                    "Membership discount cannot exceed the plan price."
            );
        }
    }


    // =========================================================
    // LOAD PLAN SNAPSHOT
    // =========================================================

    private PlanSnapshot loadPlanSnapshot(
            Connection connection,
            int membershipPlanId
    ) throws SQLException {

        String sql = """
                SELECT
                    price,
                    max_sessions
                FROM membership_plans
                WHERE id = ?
                """;


        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        )
        ) {

            statement.setInt(
                    1,
                    membershipPlanId
            );


            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {

                    throw new SQLException(
                            "Membership plan not found: "
                            + membershipPlanId
                    );
                }


                return new PlanSnapshot(
                        resultSet.getDouble(
                                "price"
                        ),
                        resultSet.getInt(
                                "max_sessions"
                        )
                );
            }
        }
    }


    // =========================================================
    // LOAD EXISTING MEMBERSHIP SNAPSHOT
    // =========================================================

    private MembershipSnapshot loadExistingMembershipSnapshot(
            Connection connection,
            int membershipId
    ) throws SQLException {

        String sql = """
                SELECT
                    membership_plan_id,
                    price,
                    allowed_sessions
                FROM member_memberships
                WHERE id = ?
                """;


        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        )
        ) {

            statement.setInt(
                    1,
                    membershipId
            );


            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {

                    throw new SQLException(
                            "Membership not found: "
                            + membershipId
                    );
                }


                return new MembershipSnapshot(
                        resultSet.getInt(
                                "membership_plan_id"
                        ),
                        resultSet.getDouble(
                                "price"
                        ),
                        resultSet.getInt(
                                "allowed_sessions"
                        )
                );
            }
        }
    }


    // =========================================================
    // BIND INSERT / UPDATE VALUES
    // =========================================================

    private void bindMembershipValues(
            PreparedStatement statement,
            MemberMembership membership,
            boolean includeId
    ) throws SQLException {

        statement.setInt(
                1,
                membership.getMemberId()
        );


        statement.setInt(
                2,
                membership.getMembershipPlanId()
        );


        setLocalDate(
                statement,
                3,
                membership.getStartDate()
        );


        setLocalDate(
                statement,
                4,
                membership.getEndDate()
        );


        statement.setInt(
                5,
                membership.getAllowedSessions()
        );


        statement.setDouble(
                6,
                membership.getPlanPriceSnapshot()
        );


        statement.setDouble(
                7,
                membership.getDiscount()
        );


        statement.setDouble(
                8,
                membership.getFinalPrice()
        );


        statement.setString(
                9,
                normalizeStatus(
                        membership.getStatus()
                )
        );


        statement.setString(
                10,
                safe(
                        membership.getNotes()
                )
        );


        if (includeId) {

            statement.setInt(
                    11,
                    membership.getId()
            );
        }
    }


    // =========================================================
    // SNAPSHOT RECORDS
    // =========================================================

    private record PlanSnapshot(
            double price,
            int maxSessions
    ) {
    }


    private record MembershipSnapshot(
            int membershipPlanId,
            double price,
            int allowedSessions
    ) {
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
    // NORMALIZE STATUS
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
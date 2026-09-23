package com.mycompany.lazar_app.repository;

import com.mycompany.lazar_app.database.DatabaseManager;
import com.mycompany.lazar_app.model.Payment;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PaymentRepository {

    private static final DateTimeFormatter DB_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    // =========================================================
    // MEMBER OPTION
    // =========================================================

    public static final class MemberOption {

        private final long id;
        private final String code;
        private final String name;

        public MemberOption(
                long id,
                String code,
                String name
        ) {
            this.id = id;
            this.code = code;
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {

            String safeCode =
                    code == null
                            ? ""
                            : code.trim();

            String safeName =
                    name == null
                            ? ""
                            : name.trim();

            return safeCode.isBlank()
                    ? safeName
                    : safeCode + " - " + safeName;
        }
    }


    // =========================================================
    // MEMBERSHIP OPTION
    // =========================================================

    public static final class MembershipOption {

        private final long id;
        private final long memberId;

        private final String planNameAr;
        private final String planNameEn;

        private final String startDate;
        private final String endDate;

        private final String status;


        public MembershipOption(
                long id,
                long memberId,
                String planNameAr,
                String planNameEn,
                String startDate,
                String endDate,
                String status
        ) {

            this.id = id;
            this.memberId = memberId;

            this.planNameAr = planNameAr;
            this.planNameEn = planNameEn;

            this.startDate = startDate;
            this.endDate = endDate;

            this.status = status;
        }


        public long getId() {
            return id;
        }

        public long getMemberId() {
            return memberId;
        }

        public String getPlanNameAr() {
            return planNameAr;
        }

        public String getPlanNameEn() {
            return planNameEn;
        }

        public String getStartDate() {
            return startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public String getStatus() {
            return status;
        }


        public String getDisplayName(
                boolean arabic
        ) {

            String name =
                    arabic
                            ? planNameAr
                            : planNameEn;


            if (name == null
                    || name.isBlank()) {

                name =
                        arabic
                                ? planNameEn
                                : planNameAr;
            }


            if (name == null
                    || name.isBlank()) {

                name = "#" + id;
            }


            return name
                    + "  ("
                    + startDate
                    + " → "
                    + endDate
                    + ")";
        }


        @Override
        public String toString() {

            return getDisplayName(false);
        }
    }


    // =========================================================
    // FIND ALL
    // =========================================================

    public List<Payment> findAll()
            throws SQLException {

        String sql = """
                SELECT
                    id,
                    member_id,
                    membership_id,
                    amount,
                    payment_method,
                    status,
                    payment_date,
                    reference_number,
                    notes
                FROM payments
                ORDER BY payment_date DESC, id DESC
                """;


        List<Payment> result =
                new ArrayList<>();


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet rs =
                        statement.executeQuery()
        ) {

            while (rs.next()) {

                result.add(
                        mapPayment(rs)
                );
            }
        }


        return result;
    }


    // =========================================================
    // SMART SEARCH
    // =========================================================

    public List<Payment> search(
            String keyword,
            String paymentMethod,
            String status
    ) throws SQLException {

        StringBuilder sql =
                new StringBuilder("""
                        SELECT DISTINCT
                               p.id,
                               p.member_id,
                               p.membership_id,
                               p.amount,
                               p.payment_method,
                               p.status,
                               p.payment_date,
                               p.reference_number,
                               p.notes

                        FROM payments p

                        LEFT JOIN members m
                               ON m.id = p.member_id

                        LEFT JOIN member_memberships mm
                               ON mm.id = p.membership_id

                        LEFT JOIN membership_plans mp
                               ON mp.id = mm.membership_plan_id

                        WHERE 1 = 1
                        """);


        List<Object> params =
                new ArrayList<>();


        // =====================================================
        // SMART KEYWORD
        // =====================================================

        if (keyword != null
                && !keyword.isBlank()) {

            /*
             * Multi-word search:
             *
             * Ahmed 500
             *
             * Each word must match at least one
             * searchable field.
             */

            String[] words =
                    keyword
                            .trim()
                            .toLowerCase()
                            .split("\\s+");


            for (String word : words) {

                if (word == null
                        || word.isBlank()) {

                    continue;
                }


                sql.append("""
                        AND (
                               LOWER(COALESCE(m.name, ''))
                                   LIKE ?

                            OR LOWER(COALESCE(m.member_code, ''))
                                   LIKE ?

                            OR LOWER(COALESCE(m.phone, ''))
                                   LIKE ?

                            OR LOWER(COALESCE(m.whatsapp, ''))
                                   LIKE ?

                            OR LOWER(COALESCE(m.email, ''))
                                   LIKE ?

                            OR LOWER(COALESCE(p.reference_number, ''))
                                   LIKE ?

                            OR LOWER(COALESCE(p.notes, ''))
                                   LIKE ?

                            OR LOWER(COALESCE(mp.name_ar, ''))
                                   LIKE ?

                            OR LOWER(COALESCE(mp.name_en, ''))
                                   LIKE ?

                            OR LOWER(COALESCE(p.payment_method, ''))
                                   LIKE ?

                            OR LOWER(COALESCE(p.status, ''))
                                   LIKE ?

                            OR CAST(p.id AS VARCHAR(50))
                                   LIKE ?

                            OR CAST(p.member_id AS VARCHAR(50))
                                   LIKE ?

                            OR CAST(p.amount AS VARCHAR(50))
                                   LIKE ?

                            OR CAST(p.payment_date AS VARCHAR(50))
                                   LIKE ?
                        )
                        """);


                String like =
                        "%"
                        + word.trim()
                        + "%";


                // 15 searchable fields

                for (int i = 0; i < 15; i++) {

                    params.add(like);
                }
            }
        }


        // =====================================================
        // PAYMENT METHOD FILTER
        // =====================================================

        if (paymentMethod != null
                && !paymentMethod.isBlank()) {

            sql.append("""
                    AND UPPER(p.payment_method) = ?
                    """);


            params.add(
                    paymentMethod
                            .trim()
                            .toUpperCase()
            );
        }


        // =====================================================
        // STATUS FILTER
        // =====================================================

        if (status != null
                && !status.isBlank()) {

            sql.append("""
                    AND UPPER(p.status) = ?
                    """);


            params.add(
                    status
                            .trim()
                            .toUpperCase()
            );
        }


        // =====================================================
        // ORDER
        // =====================================================

        sql.append("""
                ORDER BY
                    p.payment_date DESC,
                    p.id DESC
                """);


        List<Payment> result =
                new ArrayList<>();


        // =====================================================
        // EXECUTE
        // =====================================================

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            for (int i = 0;
                 i < params.size();
                 i++) {

                statement.setObject(
                        i + 1,
                        params.get(i)
                );
            }


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    result.add(
                            mapPayment(rs)
                    );
                }
            }
        }


        return result;
    }


    // =========================================================
    // SAVE
    // =========================================================

    public Payment save(
            Payment payment
    ) throws SQLException {

        validate(payment);


        String sql = """
                INSERT INTO payments
                (
                    member_id,
                    membership_id,
                    amount,
                    payment_method,
                    status,
                    payment_date,
                    reference_number,
                    notes,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;


        String now =
                formatDateTime(
                        LocalDateTime.now()
                );


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setLong(
                    1,
                    payment.getMemberId()
            );


            if (payment.getMembershipId() == null) {

                statement.setNull(
                        2,
                        Types.BIGINT
                );

            } else {

                statement.setLong(
                        2,
                        payment.getMembershipId()
                );
            }


            statement.setBigDecimal(
                    3,
                    payment.getAmount()
            );


            statement.setString(
                    4,
                    payment.getPaymentMethod()
            );


            statement.setString(
                    5,
                    payment.getStatus()
            );


            statement.setString(
                    6,
                    formatDateTime(
                            payment.getPaymentDate()
                    )
            );


            setNullableString(
                    statement,
                    7,
                    payment.getReferenceNumber()
            );


            setNullableString(
                    statement,
                    8,
                    payment.getNotes()
            );


            statement.setString(
                    9,
                    now
            );


            statement.setString(
                    10,
                    now
            );


            statement.executeUpdate();


            try (
                    ResultSet keys =
                            statement.getGeneratedKeys()
            ) {

                if (keys.next()) {

                    payment.setId(
                            keys.getLong(1)
                    );
                }
            }
        }


        return payment;
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public void update(
            Payment payment
    ) throws SQLException {

        if (payment.getId() <= 0) {

            throw new IllegalArgumentException(
                    "Payment ID is required for update."
            );
        }


        validate(payment);


        String sql = """
                UPDATE payments

                SET member_id = ?,
                    membership_id = ?,
                    amount = ?,
                    payment_method = ?,
                    status = ?,
                    payment_date = ?,
                    reference_number = ?,
                    notes = ?,
                    updated_at = ?

                WHERE id = ?
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    payment.getMemberId()
            );


            if (payment.getMembershipId() == null) {

                statement.setNull(
                        2,
                        Types.BIGINT
                );

            } else {

                statement.setLong(
                        2,
                        payment.getMembershipId()
                );
            }


            statement.setBigDecimal(
                    3,
                    payment.getAmount()
            );


            statement.setString(
                    4,
                    payment.getPaymentMethod()
            );


            statement.setString(
                    5,
                    payment.getStatus()
            );


            statement.setString(
                    6,
                    formatDateTime(
                            payment.getPaymentDate()
                    )
            );


            setNullableString(
                    statement,
                    7,
                    payment.getReferenceNumber()
            );


            setNullableString(
                    statement,
                    8,
                    payment.getNotes()
            );


            statement.setString(
                    9,
                    formatDateTime(
                            LocalDateTime.now()
                    )
            );


            statement.setLong(
                    10,
                    payment.getId()
            );


            if (statement.executeUpdate() == 0) {

                throw new SQLException(
                        "Payment not found: "
                        + payment.getId()
                );
            }
        }
    }


    // =========================================================
    // DELETE - SERVICE COMPATIBILITY
    // =========================================================

    public void delete(
            long id
    ) throws SQLException {

        deleteById(id);
    }


    // =========================================================
    // FIND BY MEMBER
    // =========================================================

    public List<Payment> findByMemberId(
            long memberId
    ) throws SQLException {

        String sql = """
                SELECT
                    id,
                    member_id,
                    membership_id,
                    amount,
                    payment_method,
                    status,
                    payment_date,
                    reference_number,
                    notes

                FROM payments

                WHERE member_id = ?

                ORDER BY payment_date DESC, id DESC
                """;


        List<Payment> result =
                new ArrayList<>();


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    memberId
            );


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    result.add(
                            mapPayment(rs)
                    );
                }
            }
        }


        return result;
    }


    // =========================================================
    // TOTAL PAYMENTS
    // =========================================================

    public BigDecimal getTotalPayments()
            throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM payments
                """;


        return queryTotal(sql);
    }


    // =========================================================
    // TOTAL BETWEEN DATES
    // =========================================================

    public BigDecimal getTotalPaymentsBetween(
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {

        if (startDate == null
                || endDate == null) {

            throw new IllegalArgumentException(
                    "Start date and end date are required."
            );
        }


        if (endDate.isBefore(startDate)) {

            throw new IllegalArgumentException(
                    "End date cannot be before start date."
            );
        }


        String sql = """
                SELECT COALESCE(SUM(amount), 0)

                FROM payments

                WHERE payment_date >= ?
                  AND payment_date < ?
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    formatDateTime(
                            startDate.atStartOfDay()
                    )
            );


            statement.setString(
                    2,
                    formatDateTime(
                            endDate
                                    .plusDays(1)
                                    .atStartOfDay()
                    )
            );


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                return readBigDecimal(rs);
            }
        }
    }


    // =========================================================
    // TOTAL BY STATUS
    // =========================================================

    public BigDecimal getTotalByStatus(
            String status
    ) throws SQLException {

        if (status == null
                || status.isBlank()) {

            return BigDecimal.ZERO;
        }


        String sql = """
                SELECT COALESCE(SUM(amount), 0)

                FROM payments

                WHERE UPPER(status) = ?
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    status
                            .trim()
                            .toUpperCase()
            );


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                return readBigDecimal(rs);
            }
        }
    }


    // =========================================================
    // COUNT ALL
    // =========================================================

    public long countAll()
            throws SQLException {

        String sql =
                "SELECT COUNT(*) FROM payments";


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet rs =
                        statement.executeQuery()
        ) {

            return rs.next()
                    ? rs.getLong(1)
                    : 0L;
        }
    }


    // =========================================================
    // DELETE BY ID
    // =========================================================

    public void deleteById(
            long id
    ) throws SQLException {

        String sql =
                "DELETE FROM payments WHERE id = ?";


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    id
            );


            statement.executeUpdate();
        }
    }


    // =========================================================
    // MEMBER OPTIONS
    // =========================================================

    public List<MemberOption> findMemberOptions()
            throws SQLException {

        String sql = """
                SELECT
                    id,
                    member_code,
                    name

                FROM members

                ORDER BY name, id
                """;


        List<MemberOption> result =
                new ArrayList<>();


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet rs =
                        statement.executeQuery()
        ) {

            while (rs.next()) {

                result.add(
                        new MemberOption(
                                rs.getLong("id"),
                                rs.getString("member_code"),
                                rs.getString("name")
                        )
                );
            }
        }


        return result;
    }


    // =========================================================
    // MEMBERSHIP OPTIONS
    // =========================================================

    public List<MembershipOption>
            findMembershipOptionsByMemberId(
                    long memberId
            ) throws SQLException {

        String sql = """
                SELECT
                    mm.id,
                    mm.member_id,
                    mp.name_ar,
                    mp.name_en,
                    mm.start_date,
                    mm.end_date,
                    mm.status

                FROM member_memberships mm

                JOIN membership_plans mp
                  ON mp.id = mm.membership_plan_id

                WHERE mm.member_id = ?

                ORDER BY mm.id DESC
                """;


        List<MembershipOption> result =
                new ArrayList<>();


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    memberId
            );


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    result.add(
                            new MembershipOption(
                                    rs.getLong("id"),
                                    rs.getLong("member_id"),
                                    rs.getString("name_ar"),
                                    rs.getString("name_en"),
                                    safeDateText(
                                            rs.getObject(
                                                    "start_date"
                                            )
                                    ),
                                    safeDateText(
                                            rs.getObject(
                                                    "end_date"
                                            )
                                    ),
                                    rs.getString("status")
                            )
                    );
                }
            }
        }


        return result;
    }


    // =========================================================
    // MEMBER DISPLAY NAME
    // =========================================================

    public String findMemberDisplayName(
            long memberId
    ) throws SQLException {

        String sql = """
                SELECT
                    member_code,
                    name

                FROM members

                WHERE id = ?
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    memberId
            );


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                if (rs.next()) {

                    String code =
                            rs.getString(
                                    "member_code"
                            );


                    String name =
                            rs.getString(
                                    "name"
                            );


                    return code == null
                            || code.isBlank()
                            ? name
                            : code
                                + " - "
                                + name;
                }
            }
        }


        return "#" + memberId;
    }


    // =========================================================
    // MEMBERSHIP DISPLAY NAME
    // =========================================================

    public String findMembershipDisplayName(
            Long membershipId,
            boolean arabic
    ) throws SQLException {

        if (membershipId == null) {

            return "-";
        }


        String sql = """
                SELECT
                    mp.name_ar,
                    mp.name_en

                FROM member_memberships mm

                JOIN membership_plans mp
                  ON mp.id = mm.membership_plan_id

                WHERE mm.id = ?
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    membershipId
            );


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                if (rs.next()) {

                    String ar =
                            rs.getString(
                                    "name_ar"
                            );


                    String en =
                            rs.getString(
                                    "name_en"
                            );


                    String value =
                            arabic
                                    ? ar
                                    : en;


                    if (value == null
                            || value.isBlank()) {

                        value =
                                arabic
                                        ? en
                                        : ar;
                    }


                    return value == null
                            || value.isBlank()
                            ? "#" + membershipId
                            : value;
                }
            }
        }


        return "#" + membershipId;
    }


    // =========================================================
    // QUERY TOTAL
    // =========================================================

    private BigDecimal queryTotal(
            String sql
    ) throws SQLException {

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet rs =
                        statement.executeQuery()
        ) {

            return readBigDecimal(rs);
        }
    }


    // =========================================================
    // READ BIG DECIMAL
    // =========================================================

    private BigDecimal readBigDecimal(
            ResultSet rs
    ) throws SQLException {

        if (!rs.next()) {

            return BigDecimal.ZERO;
        }


        BigDecimal value =
                rs.getBigDecimal(1);


        return value == null
                ? BigDecimal.ZERO
                : value;
    }


    // =========================================================
    // MAP PAYMENT
    // =========================================================

    private Payment mapPayment(
            ResultSet rs
    ) throws SQLException {

        Payment payment =
                new Payment();


        payment.setId(
                rs.getLong("id")
        );


        payment.setMemberId(
                rs.getLong("member_id")
        );


        Object membership =
                rs.getObject(
                        "membership_id"
                );


        payment.setMembershipId(
                membership == null
                        ? null
                        : ((Number) membership)
                                .longValue()
        );


        payment.setAmount(
                rs.getBigDecimal(
                        "amount"
                )
        );


        payment.setPaymentMethod(
                rs.getString(
                        "payment_method"
                )
        );


        payment.setStatus(
                rs.getString(
                        "status"
                )
        );


        payment.setPaymentDate(
                parseDateTime(
                        rs.getObject(
                                "payment_date"
                        )
                )
        );


        payment.setReferenceNumber(
                rs.getString(
                        "reference_number"
                )
        );


        payment.setNotes(
                rs.getString(
                        "notes"
                )
        );


        return payment;
    }


    // =========================================================
    // VALIDATE
    // =========================================================

    private void validate(
            Payment payment
    ) {

        if (payment == null) {

            throw new IllegalArgumentException(
                    "Payment is required."
            );
        }


        if (payment.getMemberId() <= 0) {

            throw new IllegalArgumentException(
                    "Member is required."
            );
        }


        if (payment.getAmount() == null
                || payment
                        .getAmount()
                        .compareTo(
                                BigDecimal.ZERO
                        ) <= 0) {

            throw new IllegalArgumentException(
                    "Amount must be greater than zero."
            );
        }


        if (payment.getPaymentMethod() == null
                || payment
                        .getPaymentMethod()
                        .isBlank()) {

            throw new IllegalArgumentException(
                    "Payment method is required."
            );
        }


        if (payment.getStatus() == null
                || payment
                        .getStatus()
                        .isBlank()) {

            throw new IllegalArgumentException(
                    "Payment status is required."
            );
        }


        if (payment.getPaymentDate() == null) {

            throw new IllegalArgumentException(
                    "Payment date is required."
            );
        }
    }


    // =========================================================
    // NULLABLE STRING
    // =========================================================

    private void setNullableString(
            PreparedStatement statement,
            int index,
            String value
    ) throws SQLException {

        if (value == null
                || value.isBlank()) {

            statement.setNull(
                    index,
                    Types.VARCHAR
            );

        } else {

            statement.setString(
                    index,
                    value.trim()
            );
        }
    }


    // =========================================================
    // FORMAT DATE TIME
    // =========================================================

    private String formatDateTime(
            LocalDateTime value
    ) {

        return value == null
                ? null
                : value.format(
                        DB_DATE_TIME
                );
    }


    // =========================================================
    // PARSE DATE TIME
    // =========================================================

    private LocalDateTime parseDateTime(
            Object value
    ) {

        if (value == null) {

            return null;
        }


        if (value instanceof Timestamp timestamp) {

            return timestamp
                    .toLocalDateTime();
        }


        if (value instanceof java.sql.Date date) {

            return date
                    .toLocalDate()
                    .atStartOfDay();
        }


        if (value instanceof Number number) {

            long epoch =
                    number.longValue();


            return java.time.Instant
                    .ofEpochMilli(epoch)
                    .atZone(
                            java.time.ZoneId
                                    .systemDefault()
                    )
                    .toLocalDateTime();
        }


        String text =
                value
                        .toString()
                        .trim();


        if (text.matches("-?\\d+")) {

            try {

                long epoch =
                        Long.parseLong(text);


                return java.time.Instant
                        .ofEpochMilli(epoch)
                        .atZone(
                                java.time.ZoneId
                                        .systemDefault()
                        )
                        .toLocalDateTime();

            } catch (Exception ignored) {
            }
        }


        try {

            return LocalDateTime.parse(
                    text
            );

        } catch (Exception ignored) {
        }


        try {

            return LocalDateTime.parse(
                    text,
                    DB_DATE_TIME
            );

        } catch (Exception ignored) {
        }


        try {

            return LocalDate
                    .parse(text)
                    .atStartOfDay();

        } catch (Exception ignored) {
        }


        return null;
    }


    // =========================================================
    // SAFE DATE TEXT
    // =========================================================

    private String safeDateText(
            Object value
    ) {

        if (value == null) {

            return "-";
        }


        String text =
                value.toString();


        if (text.matches("-?\\d+")) {

            try {

                long epoch =
                        Long.parseLong(text);


                return java.time.Instant
                        .ofEpochMilli(epoch)
                        .atZone(
                                java.time.ZoneId
                                        .systemDefault()
                        )
                        .toLocalDate()
                        .toString();

            } catch (Exception ignored) {
            }
        }


        return text.length() >= 10
                ? text.substring(0, 10)
                : text;
    }
}
package com.mycompany.lazar_app.repository;

import com.mycompany.lazar_app.database.DatabaseManager;
import com.mycompany.lazar_app.model.Payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PaymentRepository {

    private static final DateTimeFormatter DB_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public enum PaymentField { ID, MEMBER_CODE, MEMBER, PLAN, SUBSCRIPTION, AMOUNT, PAID, REMAINING, METHOD, DATE, STATUS }
    public record PaymentOrder(PaymentField field, boolean ascending) {}
    public record PaymentFilter(String search, String method, String status,
                                boolean arabic, List<PaymentOrder> order) {
        public PaymentFilter { order = List.copyOf(order); }
    }

    /** Payment projection with all display data already loaded; no per-cell IO. */
    public static final class DisplayPayment extends Payment {
        private final String memberName;
        private final String memberCode;
        private final String planName;
        private MembershipFinancialSummary finance;
        DisplayPayment(Payment payment, String memberName, String memberCode, String planName) {
            super(payment.getId(), payment.getMemberId(), payment.getMembershipId(), payment.getAmount(),
                    payment.getPaymentMethod(), payment.getStatus(), payment.getPaymentDate(),
                    payment.getReferenceNumber(), payment.getNotes());
            this.memberName = memberName;
            this.memberCode = memberCode;
            this.planName = planName;
        }
        public String memberName() { return memberName; }
        public String memberCode() { return memberCode; }
        public String planName() { return planName; }
        public MembershipFinancialSummary finance() { return finance; }
    }

    private static final String PAGE_FROM = """
            FROM payments p
            LEFT JOIN members m ON m.id = p.member_id
            LEFT JOIN member_memberships mm ON mm.id = p.membership_id
            LEFT JOIN membership_plans mp ON mp.id = mm.membership_plan_id
            """;
    private static final String PAGE_PAID = """
            (SELECT COALESCE(SUM(ledger.amount),0) FROM payments ledger
             WHERE ledger.membership_id = p.membership_id AND UPPER(COALESCE(ledger.status,'')) = 'PAID')
            """;
    private static final String PAGE_BALANCE = "CASE WHEN COALESCE(mm.final_price,0) > " + PAGE_PAID
            + " THEN COALESCE(mm.final_price,0) - " + PAGE_PAID + " ELSE 0 END";

    public com.mycompany.lazar_app.ui.paging.PageResult<Payment> findPage(
            PaymentFilter filter, int page, int size) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            return findPage(connection, filter, page, size);
        }
    }

    com.mycompany.lazar_app.ui.paging.PageResult<Payment> findPage(
            Connection connection, PaymentFilter filter, int page, int size) throws SQLException {
        if (size < 1 || size > 100 || page < 0) throw new IllegalArgumentException("Invalid page bounds");
        List<String> params = new ArrayList<>();
        String where = paymentPageWhere(filter, params);
        java.util.Map<String, BigDecimal> amounts = new java.util.HashMap<>();
        String date = "p.payment_date";
        if (connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite")) {
            // Legacy SQLite stores milliseconds; match Java's local-date interpretation.
            date = "CASE WHEN typeof(p.payment_date) IN ('integer','real') "
                    + "OR (p.payment_date <> '' AND p.payment_date NOT GLOB '*[^0-9]*') "
                    + "THEN datetime(CAST(p.payment_date AS REAL)/1000,'unixepoch','localtime') "
                    + "ELSE REPLACE(p.payment_date,'T',' ') END";
        }
        String totalsSql = "SELECT COUNT(*) AS total_count, COALESCE(SUM(p.amount),0) AS total_amount, "
                + "COALESCE(SUM(CASE WHEN " + date + " >= ? AND " + date + " < ? THEN p.amount ELSE 0 END),0) AS month_amount, "
                + "COALESCE(SUM(CASE WHEN UPPER(COALESCE(p.status,''))='PAID' THEN p.amount ELSE 0 END),0) AS paid_amount "
                + PAGE_FROM + where;
        long total;
        try (PreparedStatement statement = connection.prepareStatement(totalsSql)) {
            LocalDate month = LocalDate.now().withDayOfMonth(1);
            statement.setString(1, month.atStartOfDay().format(DB_DATE_TIME));
            statement.setString(2, month.plusMonths(1).atStartOfDay().format(DB_DATE_TIME));
            bindPageStrings(statement, 3, params);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                total = rs.getLong("total_count");
                amounts.put("total", rs.getBigDecimal("total_amount"));
                amounts.put("month", rs.getBigDecimal("month_amount"));
                amounts.put("paid", rs.getBigDecimal("paid_amount"));
            }
        }
        // Match the existing KPI semantics: each matching subscription once,
        // but deduct ALL its paid receipts, including receipts outside the filter/page.
        String outstandingSql = """
                SELECT COALESCE(SUM(CASE WHEN balance.membership_amount > balance.paid_amount
                       THEN balance.membership_amount - balance.paid_amount ELSE 0 END),0)
                FROM (
                    SELECT sub.id, COALESCE(sub.final_price,0) AS membership_amount,
                           COALESCE(SUM(CASE WHEN UPPER(COALESCE(receipt.status,''))='PAID'
                                            THEN receipt.amount ELSE 0 END),0) AS paid_amount
                    FROM member_memberships sub
                    LEFT JOIN payments receipt ON receipt.membership_id = sub.id
                    WHERE sub.id IN (SELECT p.membership_id
                """ + PAGE_FROM + where + " GROUP BY p.membership_id) GROUP BY sub.id, sub.final_price) balance";
        if (total == 0) amounts.put("pending", BigDecimal.ZERO);
        else try (PreparedStatement statement = connection.prepareStatement(outstandingSql)) {
            bindPageStrings(statement, 1, params);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                amounts.put("pending", rs.getBigDecimal(1));
            }
        }
        int safePage = (int) Math.min(page, Math.max(0, (total - 1) / size));
        List<Payment> rows = new ArrayList<>();
        if (total > 0) {
            String sql = """
                    SELECT p.id,p.member_id,p.membership_id,p.amount,p.payment_method,p.status,
                           p.payment_date,p.reference_number,p.notes,
                           m.member_code AS display_code,m.name AS display_member,
                           mp.name_ar AS display_plan_ar,mp.name_en AS display_plan_en
                    """ + PAGE_FROM + where + paymentPageOrder(filter) + PagingSql.limit(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int at = bindPageStrings(statement, 1, params);
                long offset = (long) safePage * size;
                if (PagingSql.offsetFirst(connection)) {
                    statement.setLong(at++, offset); statement.setInt(at, size);
                } else {
                    statement.setInt(at++, size); statement.setLong(at, offset);
                }
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Payment payment = mapPayment(rs);
                        String code = rs.getString("display_code");
                        String name = rs.getString("display_member");
                        String member = name == null || name.isBlank() ? "#" + payment.getMemberId() : name;
                        String memberCode = code == null || code.isBlank() ? "—" : code;
                        String primary = rs.getString(filter.arabic() ? "display_plan_ar" : "display_plan_en");
                        String fallback = rs.getString(filter.arabic() ? "display_plan_en" : "display_plan_ar");
                        String plan = primary == null || primary.isBlank() ? fallback : primary;
                        if (plan == null || plan.isBlank()) plan = payment.getMembershipId() == null ? "-" : "#" + payment.getMembershipId();
                        rows.add(new DisplayPayment(payment, member, memberCode, plan));
                    }
                }
            }
            hydratePageFinances(connection, rows);
        }
        return new com.mycompany.lazar_app.ui.paging.PageResult<>(rows, total, safePage,
                java.util.Map.of("total", total), amounts);
    }

    private static int bindPageStrings(PreparedStatement statement, int at, List<String> values) throws SQLException {
        for (String value : values) statement.setString(at++, value);
        return at;
    }

    private void hydratePageFinances(Connection connection, List<Payment> rows) throws SQLException {
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        for (Payment row : rows) if (row.getMembershipId() != null) ids.add(row.getMembershipId());
        if (ids.isEmpty()) return;
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        String sql = """
                SELECT sub.id,COALESCE(sub.final_price,0) AS membership_amount,
                       COALESCE(SUM(CASE WHEN UPPER(COALESCE(receipt.status,''))='PAID'
                                        THEN receipt.amount ELSE 0 END),0) AS paid_amount
                FROM member_memberships sub LEFT JOIN payments receipt ON receipt.membership_id = sub.id
                WHERE sub.id IN (
                """ + placeholders + ") GROUP BY sub.id, sub.final_price";
        java.util.Map<Long, MembershipFinancialSummary> summaries = new java.util.HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int at = 1;
            for (Long id : ids) statement.setLong(at++, id);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    BigDecimal amount = rs.getBigDecimal("membership_amount");
                    BigDecimal paid = rs.getBigDecimal("paid_amount");
                    long id = rs.getLong("id");
                    summaries.put(id, new MembershipFinancialSummary(id, amount, paid, calculateBalance(amount, paid)));
                }
            }
        }
        for (Payment row : rows) ((DisplayPayment) row).finance = summaries.get(row.getMembershipId());
    }

    private String paymentPageWhere(PaymentFilter filter, List<String> values) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1 ");
        String search = PagingSql.normalize(filter.search());
        if (!search.isBlank()) {
            List<String> textColumns = List.of("m.name","m.member_code","m.phone","m.whatsapp","m.email",
                    "p.reference_number","p.notes","mp.name_ar","mp.name_en","p.payment_method","p.status");
            List<String> numericColumns = List.of("p.id","p.member_id","p.amount","p.payment_date");
            for (String word : search.split("\\s+")) {
                sql.append(" AND (");
                boolean first = true;
                for (String column : textColumns) {
                    if (!first) sql.append(" OR ");
                    first = false;
                    sql.append(PagingSql.normalizedColumn(column)).append(" LIKE ? ESCAPE '!' ");
                    values.add(PagingSql.pattern(word));
                }
                for (String column : numericColumns) {
                    sql.append(" OR CAST(").append(column).append(" AS VARCHAR(50)) LIKE ? ESCAPE '!' ");
                    values.add(PagingSql.pattern(word));
                }
                sql.append(") ");
            }
        }
        if (filter.method() != null && !filter.method().isBlank()) {
            sql.append(" AND UPPER(p.payment_method)=? ");
            values.add(filter.method().strip().toUpperCase(java.util.Locale.ROOT));
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            String status = filter.status().strip().toUpperCase(java.util.Locale.ROOT);
            if ("REMAINING".equals(status)) {
                sql.append(" AND UPPER(p.status) <> 'CANCELLED' AND ")
                        .append(PAGE_BALANCE).append(" > 0 ");
            } else {
                sql.append(" AND UPPER(p.status)=? ");
                values.add(status);
            }
        }
        return sql.toString();
    }

    private String paymentPageOrder(PaymentFilter filter) {
        StringBuilder sql = new StringBuilder(" ORDER BY ");
        for (PaymentOrder order : filter.order()) {
            String column = switch (order.field()) {
                case ID -> "p.id";
                case MEMBER_CODE -> "m.member_code";
                case MEMBER -> "m.name";
                case PLAN -> filter.arabic() ? "COALESCE(NULLIF(mp.name_ar,''),mp.name_en,'')"
                        : "COALESCE(NULLIF(mp.name_en,''),mp.name_ar,'')";
                case SUBSCRIPTION -> "COALESCE(mm.final_price,0)";
                case AMOUNT -> "p.amount";
                case PAID -> PAGE_PAID;
                case REMAINING -> PAGE_BALANCE;
                case METHOD -> "p.payment_method";
                case DATE -> "p.payment_date";
                case STATUS -> "p.status";
            };
            sql.append(column).append(order.ascending() ? " ASC, " : " DESC, ");
        }
        return sql.append("p.payment_date DESC,p.id DESC ").toString();
    }

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

        /*
         * Financial values belong to the concrete member membership,
         * not to the current catalogue plan price.
         */
        private final BigDecimal membershipAmount;
        private final BigDecimal paidAmount;
        private final BigDecimal balanceDue;


        public MembershipOption(
                long id,
                long memberId,
                String planNameAr,
                String planNameEn,
                String startDate,
                String endDate,
                String status,
                BigDecimal membershipAmount,
                BigDecimal paidAmount,
                BigDecimal balanceDue
        ) {

            this.id = id;
            this.memberId = memberId;

            this.planNameAr = planNameAr;
            this.planNameEn = planNameEn;

            this.startDate = startDate;
            this.endDate = endDate;

            this.status = status;

            this.membershipAmount =
                    zeroIfNull(
                            membershipAmount
                    );

            this.paidAmount =
                    zeroIfNull(
                            paidAmount
                    );

            this.balanceDue =
                    zeroIfNull(
                            balanceDue
                    ).max(
                            BigDecimal.ZERO
                    );
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


        public BigDecimal getMembershipAmount() {
            return membershipAmount;
        }


        public BigDecimal getPaidAmount() {
            return paidAmount;
        }


        public BigDecimal getBalanceDue() {
            return balanceDue;
        }


        public boolean isFullyPaid() {
            return balanceDue.compareTo(
                    BigDecimal.ZERO
            ) <= 0;
        }


        private static BigDecimal zeroIfNull(
                BigDecimal value
        ) {
            return value == null
                    ? BigDecimal.ZERO
                    : value;
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
    // MEMBERSHIP FINANCIAL SUMMARY
    // =========================================================

    public static final class MembershipFinancialSummary {

        private final long membershipId;
        private final BigDecimal membershipAmount;
        private final BigDecimal paidAmount;
        private final BigDecimal balanceDue;

        public MembershipFinancialSummary(
                long membershipId,
                BigDecimal membershipAmount,
                BigDecimal paidAmount,
                BigDecimal balanceDue
        ) {
            this.membershipId = membershipId;
            this.membershipAmount = zero(membershipAmount);
            this.paidAmount = zero(paidAmount);
            this.balanceDue = zero(balanceDue).max(BigDecimal.ZERO);
        }

        public long getMembershipId() {
            return membershipId;
        }

        public BigDecimal getMembershipAmount() {
            return membershipAmount;
        }

        public BigDecimal getPaidAmount() {
            return paidAmount;
        }

        public BigDecimal getBalanceDue() {
            return balanceDue;
        }

        private static BigDecimal zero(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }


    // =========================================================
    // PAYMENT INSTALLMENT
    // =========================================================

    public static final class PaymentInstallment {

        private final long id;
        private final long membershipId;
        private final int installmentNo;
        private final LocalDate dueDate;
        private final BigDecimal amount;
        private final String status;

        public PaymentInstallment(
                long id,
                long membershipId,
                int installmentNo,
                LocalDate dueDate,
                BigDecimal amount,
                String status
        ) {
            this.id = id;
            this.membershipId = membershipId;
            this.installmentNo = installmentNo;
            this.dueDate = dueDate;
            this.amount = amount == null ? BigDecimal.ZERO : amount;
            this.status = status;
        }

        public long getId() {
            return id;
        }

        public long getMembershipId() {
            return membershipId;
        }

        public int getInstallmentNo() {
            return installmentNo;
        }

        public LocalDate getDueDate() {
            return dueDate;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getStatus() {
            return status;
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
                    updated_at,
                    submission_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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

            validateMembershipOwner(connection, payment);

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


            statement.setString(11, payment.getSubmissionId());
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
        com.mycompany.lazar_app.security.AuthorizationService.requireAdmin();

        validate(payment);

        if (payment.getId() <= 0) {

            throw new IllegalArgumentException(
                    "Payment ID is required for update."
            );
        }


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

            validateMembershipOwner(connection, payment);

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
        com.mycompany.lazar_app.security.AuthorizationService.requireAdmin();

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
                    mm.status,
                    mm.final_price AS membership_amount,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN UPPER(COALESCE(p.status, '')) = 'PAID'
                                THEN p.amount
                                ELSE 0
                            END
                        ),
                        0
                    ) AS paid_amount

                FROM member_memberships mm

                JOIN membership_plans mp
                  ON mp.id = mm.membership_plan_id

                LEFT JOIN payments p
                  ON p.membership_id = mm.id

                WHERE mm.member_id = ?

                GROUP BY
                    mm.id,
                    mm.member_id,
                    mp.name_ar,
                    mp.name_en,
                    mm.start_date,
                    mm.end_date,
                    mm.status,
                    mm.final_price

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
                                    rs.getString("status"),
                                    rs.getBigDecimal(
                                            "membership_amount"
                                    ),
                                    rs.getBigDecimal(
                                            "paid_amount"
                                    ),
                                    calculateBalance(
                                            rs.getBigDecimal(
                                                    "membership_amount"
                                            ),
                                            rs.getBigDecimal(
                                                    "paid_amount"
                                            )
                                    )
                            )
                    );
                }
            }
        }


        return result;
    }


    // =========================================================
    // CALCULATE MEMBERSHIP BALANCE
    // =========================================================

    private BigDecimal calculateBalance(
            BigDecimal membershipAmount,
            BigDecimal paidAmount
    ) {

        BigDecimal total =
                membershipAmount == null
                        ? BigDecimal.ZERO
                        : membershipAmount;

        BigDecimal paid =
                paidAmount == null
                        ? BigDecimal.ZERO
                        : paidAmount;

        return total
                .subtract(
                        paid
                )
                .max(
                        BigDecimal.ZERO
                );
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
    // MEMBERSHIP FINANCIAL SUMMARY
    // =========================================================

    public MembershipFinancialSummary findMembershipFinancialSummary(
            long membershipId
    ) throws SQLException {

        String sql = """
                SELECT
                    mm.id,
                    mm.final_price AS membership_amount,
                    COALESCE(
                        SUM(
                            CASE
                                WHEN UPPER(COALESCE(p.status, '')) = 'PAID'
                                THEN p.amount
                                ELSE 0
                            END
                        ),
                        0
                    ) AS paid_amount
                FROM member_memberships mm
                LEFT JOIN payments p
                       ON p.membership_id = mm.id
                WHERE mm.id = ?
                GROUP BY mm.id, mm.final_price
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, membershipId);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return new MembershipFinancialSummary(
                            membershipId,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO
                    );
                }

                BigDecimal membershipAmount = rs.getBigDecimal("membership_amount");
                BigDecimal paidAmount = rs.getBigDecimal("paid_amount");

                return new MembershipFinancialSummary(
                        membershipId,
                        membershipAmount,
                        paidAmount,
                        calculateBalance(membershipAmount, paidAmount)
                );
            }
        }
    }


    // =========================================================
    // PAYMENTS BY MEMBERSHIP
    // =========================================================

    public List<Payment> findPaymentsByMembershipId(
            long membershipId
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
                WHERE membership_id = ?
                ORDER BY payment_date ASC, id ASC
                """;

        List<Payment> result = new ArrayList<>();

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, membershipId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(mapPayment(rs));
                }
            }
        }

        return result;
    }


    // =========================================================
    // PAYMENT SCHEDULE
    // =========================================================

    public List<PaymentInstallment> findPaymentScheduleByMembershipId(
            long membershipId
    ) throws SQLException {

        List<PaymentInstallment> result = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection()) {
            ensurePaymentInstallmentsTable(connection);

            String sql = """
                    SELECT
                        id,
                        membership_id,
                        installment_no,
                        due_date,
                        amount,
                        status
                    FROM payment_installments
                    WHERE membership_id = ?
                    ORDER BY installment_no ASC, due_date ASC, id ASC
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, membershipId);

                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        result.add(
                                new PaymentInstallment(
                                        rs.getLong("id"),
                                        rs.getLong("membership_id"),
                                        rs.getInt("installment_no"),
                                        parseDate(rs.getObject("due_date")),
                                        rs.getBigDecimal("amount"),
                                        rs.getString("status")
                                )
                        );
                    }
                }
            }
        }

        return result;
    }


    public void replaceOutstandingSchedule(
            long membershipId,
            BigDecimal remainingBalance,
            int installments,
            LocalDate firstDueDate
    ) throws SQLException {

        if (membershipId <= 0) {
            throw new IllegalArgumentException("Membership ID is required.");
        }

        BigDecimal remaining = remainingBalance == null
                ? BigDecimal.ZERO
                : remainingBalance.max(BigDecimal.ZERO);

        try (Connection connection = DatabaseManager.getConnection()) {
            ensurePaymentInstallmentsTable(connection);
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM payment_installments WHERE membership_id = ?"
                )) {
                    delete.setLong(1, membershipId);
                    delete.executeUpdate();
                }

                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    if (installments <= 0) {
                        throw new IllegalArgumentException(
                                "Installment count must be greater than zero."
                        );
                    }

                    if (firstDueDate == null) {
                        throw new IllegalArgumentException(
                                "First installment due date is required."
                        );
                    }

                    String insertSql = """
                            INSERT INTO payment_installments
                            (
                                membership_id,
                                installment_no,
                                due_date,
                                amount,
                                status,
                                created_at,
                                updated_at
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """;

                    BigDecimal base = remaining.divide(
                            BigDecimal.valueOf(installments),
                            2,
                            RoundingMode.DOWN
                    );

                    String now = formatDateTime(LocalDateTime.now());

                    try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                        BigDecimal allocated = BigDecimal.ZERO;

                        for (int i = 1; i <= installments; i++) {
                            BigDecimal amount = i == installments
                                    ? remaining.subtract(allocated)
                                    : base;

                            allocated = allocated.add(amount);

                            insert.setLong(1, membershipId);
                            insert.setInt(2, i);
                            insert.setString(3, firstDueDate.plusMonths(i - 1L).toString());
                            insert.setBigDecimal(4, amount);
                            insert.setString(5, "SCHEDULED");
                            insert.setString(6, now);
                            insert.setString(7, now);
                            insert.addBatch();
                        }

                        insert.executeBatch();
                    }
                }

                connection.commit();

            } catch (Exception exception) {
                connection.rollback();

                if (exception instanceof SQLException sqlException) {
                    throw sqlException;
                }

                throw new SQLException(
                        "Unable to update payment schedule.",
                        exception
                );

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }


    // =========================================================
    // SCHEDULE TABLE BOOTSTRAP
    // =========================================================

    private void ensurePaymentInstallmentsTable(
            Connection connection
    ) throws SQLException {
        if (!tableExists(connection, "payment_installments")) {
            throw new SQLException("Payment schedule schema is missing; run database migrations.");
        }
    }


    private boolean tableExists(
            Connection connection,
            String tableName
    ) throws SQLException {

        DatabaseMetaData metaData = connection.getMetaData();

        try (ResultSet tables = metaData.getTables(
                null,
                null,
                "%",
                new String[]{"TABLE"}
        )) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME");

                if (name != null && name.equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
        }

        return false;
    }


    private LocalDate parseDate(
            Object value
    ) {

        if (value == null) {
            return null;
        }

        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }

        if (value instanceof Number number) {
            return java.time.Instant
                    .ofEpochMilli(number.longValue())
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }

        String text = value.toString().trim();

        if (text.matches("-?\\d+")) {
            try {
                return java.time.Instant
                        .ofEpochMilli(Long.parseLong(text))
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            } catch (Exception ignored) {
            }
        }

        if (text.length() >= 10) {
            text = text.substring(0, 10);
        }

        try {
            return LocalDate.parse(text);
        } catch (Exception ignored) {
            return null;
        }
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

    private void validateMembershipOwner(Connection connection, Payment payment)
            throws SQLException {
        if (payment.getMembershipId() == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT member_id FROM member_memberships WHERE id = ?")) {
            statement.setLong(1, payment.getMembershipId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getLong(1) != payment.getMemberId()) {
                    throw new SQLException("Payment membership must belong to the selected member.");
                }
            }
        }
    }

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


        try {
            payment.getAmount().setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Payment amount cannot contain fractions smaller than one currency cent.", exception);
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
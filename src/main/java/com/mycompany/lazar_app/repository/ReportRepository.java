package com.mycompany.lazar_app.repository;

import com.mycompany.lazar_app.database.DatabaseManager;
import com.mycompany.lazar_app.model.report.FinancialActivity;
import com.mycompany.lazar_app.model.report.MonthlyFinancialSummary;
import com.mycompany.lazar_app.model.report.ReportSummary;
import com.mycompany.lazar_app.model.report.ReportType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportRepository {

    private static final DateTimeFormatter DB_DATE_TIME =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );


    // =========================================================
    // SUMMARY
    // =========================================================

    public ReportSummary getSummary(
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        validateDates(from, to);

        ReportSummary summary =
                new ReportSummary();

        summary.setTotalRevenue(
                getRevenue(
                        from,
                        to,
                        null
                )
        );

        summary.setMembershipRevenue(
                getRevenue(
                        from,
                        to,
                        Boolean.TRUE
                )
        );

        summary.setOtherRevenue(
                getRevenue(
                        from,
                        to,
                        Boolean.FALSE
                )
        );

        summary.setTotalExpenses(
                getExpenses(
                        from,
                        to
                )
        );

        summary.setTransactions(
                countTransactions(
                        from,
                        to
                )
        );

        summary.setActiveMembers(
                countActiveMembers()
        );

        summary.setNewMembers(
                countNewMembers(
                        from,
                        to
                )
        );

        return summary;
    }


    // =========================================================
    // SUMMARY BY REPORT TYPE
    // =========================================================

    public ReportSummary getSummary(
            LocalDate from,
            LocalDate to,
            ReportType reportType
    ) throws SQLException {

        validateDates(from, to);

        ReportType effectiveType =
                reportType == null
                        ? ReportType.FINANCIAL_SUMMARY
                        : reportType;

        if (effectiveType == ReportType.FINANCIAL_SUMMARY) {
            return getSummary(from, to);
        }

        ReportSummary summary =
                new ReportSummary();

        BigDecimal membershipRevenue =
                getRevenue(from, to, Boolean.TRUE);

        BigDecimal otherRevenue =
                getRevenue(from, to, Boolean.FALSE);

        BigDecimal totalRevenue =
                membershipRevenue.add(otherRevenue);

        BigDecimal totalExpenses =
                getExpenses(from, to);

        switch (effectiveType) {

            case REVENUE -> {
                summary.setTotalRevenue(totalRevenue);
                summary.setMembershipRevenue(membershipRevenue);
                summary.setOtherRevenue(otherRevenue);
                summary.setTotalExpenses(BigDecimal.ZERO);
                summary.setTransactions(
                        countRevenueTransactions(from, to, null)
                );
            }

            case EXPENSES -> {
                summary.setTotalRevenue(BigDecimal.ZERO);
                summary.setMembershipRevenue(BigDecimal.ZERO);
                summary.setOtherRevenue(BigDecimal.ZERO);
                summary.setTotalExpenses(totalExpenses);
                summary.setTransactions(
                        countExpenseTransactions(from, to)
                );
            }

            case MEMBERSHIPS -> {
                summary.setTotalRevenue(membershipRevenue);
                summary.setMembershipRevenue(membershipRevenue);
                summary.setOtherRevenue(BigDecimal.ZERO);
                summary.setTotalExpenses(BigDecimal.ZERO);
                summary.setTransactions(
                        countRevenueTransactions(
                                from,
                                to,
                                Boolean.TRUE
                        )
                );
            }

            default -> {
                return getSummary(from, to);
            }
        }

        summary.setActiveMembers(
                countActiveMembers()
        );

        summary.setNewMembers(
                countNewMembers(from, to)
        );

        return summary;
    }


    // =========================================================
    // REVENUE
    // =========================================================

    private BigDecimal getRevenue(
            LocalDate from,
            LocalDate to,
            Boolean membershipOnly
    ) throws SQLException {

        BigDecimal paymentRevenue =
                getPaymentRevenue(
                        from,
                        to,
                        membershipOnly
                );

        /*
         * Manual revenues belong to Other Revenue.
         * Therefore they are included in TOTAL revenue and OTHER revenue,
         * but never in Membership Revenue.
         */
        if (Boolean.TRUE.equals(membershipOnly)) {
            return paymentRevenue;
        }

        return paymentRevenue.add(
                getManualRevenue(from, to)
        );
    }


    private BigDecimal getPaymentRevenue(
            LocalDate from,
            LocalDate to,
            Boolean membershipOnly
    ) throws SQLException {

        StringBuilder sql =
                new StringBuilder("""
                        SELECT COALESCE(SUM(amount), 0)
                        FROM payments
                        WHERE UPPER(status) = 'PAID'
                          AND payment_date >= ?
                          AND payment_date < ?
                        """);

        if (Boolean.TRUE.equals(membershipOnly)) {
            sql.append(" AND membership_id IS NOT NULL ");
        } else if (Boolean.FALSE.equals(membershipOnly)) {
            sql.append(" AND membership_id IS NULL ");
        }

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql.toString()
                )
        ) {
            setDateRange(statement, from, to);

            try (ResultSet rs = statement.executeQuery()) {
                return readDecimal(rs);
            }
        }
    }


    private BigDecimal getManualRevenue(
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM revenues
                WHERE revenue_date >= ?
                  AND revenue_date <= ?
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());

            try (ResultSet rs = statement.executeQuery()) {
                return readDecimal(rs);
            }
        }
    }


    // =========================================================
    // EXPENSES
    // =========================================================

    private BigDecimal getExpenses(
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM expenses
                WHERE UPPER(status) = 'PAID'
                  AND expense_date >= ?
                  AND expense_date <= ?
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    from.toString()
            );

            statement.setString(
                    2,
                    to.toString()
            );

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                return readDecimal(rs);
            }
        }
    }


    // =========================================================
    // TRANSACTIONS
    // =========================================================

    private long countTransactions(
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql = """
                SELECT
                    (
                        SELECT COUNT(*)
                        FROM payments
                        WHERE UPPER(status) = 'PAID'
                          AND payment_date >= ?
                          AND payment_date < ?
                    )
                    +
                    (
                        SELECT COUNT(*)
                        FROM expenses
                        WHERE UPPER(status) = 'PAID'
                          AND expense_date >= ?
                          AND expense_date <= ?
                    )
                    +
                    (
                        SELECT COUNT(*)
                        FROM revenues
                        WHERE revenue_date >= ?
                          AND revenue_date <= ?
                    )
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
                            from.atStartOfDay()
                    )
            );

            statement.setString(
                    2,
                    formatDateTime(
                            to.plusDays(1)
                                    .atStartOfDay()
                    )
            );

            statement.setString(
                    3,
                    from.toString()
            );

            statement.setString(
                    4,
                    to.toString()
            );

            statement.setString(
                    5,
                    from.toString()
            );

            statement.setString(
                    6,
                    to.toString()
            );

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                return rs.next()
                        ? rs.getLong(1)
                        : 0L;
            }
        }
    }


    // =========================================================
    // FILTERED TRANSACTION COUNTS
    // =========================================================

    private long countRevenueTransactions(
            LocalDate from,
            LocalDate to,
            Boolean membershipOnly
    ) throws SQLException {

        StringBuilder sql =
                new StringBuilder("""
                        SELECT COUNT(*)
                        FROM payments
                        WHERE UPPER(status) = 'PAID'
                          AND payment_date >= ?
                          AND payment_date < ?
                        """);

        if (Boolean.TRUE.equals(membershipOnly)) {
            sql.append(" AND membership_id IS NOT NULL ");
        } else if (Boolean.FALSE.equals(membershipOnly)) {
            sql.append(" AND membership_id IS NULL ");
        }

        long paymentCount;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql.toString())
        ) {
            setDateRange(statement, from, to);

            try (ResultSet rs = statement.executeQuery()) {
                paymentCount = rs.next() ? rs.getLong(1) : 0L;
            }
        }

        /*
         * Manually entered revenues are part of Revenue and Financial Summary,
         * but never part of Memberships.
         */
        if (Boolean.TRUE.equals(membershipOnly)) {
            return paymentCount;
        }

        String manualSql = """
                SELECT COUNT(*)
                FROM revenues
                WHERE revenue_date >= ?
                  AND revenue_date <= ?
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(manualSql)
        ) {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());

            try (ResultSet rs = statement.executeQuery()) {
                return paymentCount
                        + (rs.next() ? rs.getLong(1) : 0L);
            }
        }
    }


    private long countExpenseTransactions(
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM expenses
                WHERE UPPER(status) = 'PAID'
                  AND expense_date >= ?
                  AND expense_date <= ?
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }


    // =========================================================
    // ACTIVE MEMBERS
    // =========================================================

    private long countActiveMembers()
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM members
                WHERE UPPER(status) = 'ACTIVE'
                """;

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
    // NEW MEMBERS
    // =========================================================

    private long countNewMembers(
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM members
                WHERE start_date >= ?
                  AND start_date <= ?
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    from.toString()
            );

            statement.setString(
                    2,
                    to.toString()
            );

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                return rs.next()
                        ? rs.getLong(1)
                        : 0L;
            }
        }
    }


    // =========================================================
    // FINANCIAL ACTIVITY
    // =========================================================

    public List<FinancialActivity> getFinancialActivity(
            LocalDate from,
            LocalDate to,
            int limit
    ) throws SQLException {

        validateDates(from, to);

        List<FinancialActivity> result =
                new ArrayList<>();

        loadPayments(
                result,
                from,
                to
        );

        loadManualRevenues(
                result,
                from,
                to
        );

        loadExpenses(
                result,
                from,
                to
        );

        result.sort(
                (a, b) -> {

                    if (a.getDate() == null
                            && b.getDate() == null) {

                        return 0;
                    }

                    if (a.getDate() == null) {
                        return 1;
                    }

                    if (b.getDate() == null) {
                        return -1;
                    }

                    return b.getDate()
                            .compareTo(
                                    a.getDate()
                            );
                }
        );

        if (limit > 0
                && result.size() > limit) {

            return new ArrayList<>(
                    result.subList(
                            0,
                            limit
                    )
            );
        }

        return result;
    }


    // =========================================================
    // FINANCIAL ACTIVITY BY REPORT TYPE
    // =========================================================

    public List<FinancialActivity> getFinancialActivity(
            LocalDate from,
            LocalDate to,
            ReportType reportType,
            int limit
    ) throws SQLException {

        validateDates(from, to);

        ReportType effectiveType =
                reportType == null
                        ? ReportType.FINANCIAL_SUMMARY
                        : reportType;

        List<FinancialActivity> result =
                new ArrayList<>();

        switch (effectiveType) {

            case REVENUE -> {
                loadPayments(result, from, to);
                loadManualRevenues(result, from, to);
            }

            case EXPENSES ->
                    loadExpenses(result, from, to);

            case MEMBERSHIPS ->
                    loadMembershipPayments(result, from, to);

            case FINANCIAL_SUMMARY -> {
                loadPayments(result, from, to);
                loadManualRevenues(result, from, to);
                loadExpenses(result, from, to);
            }
        }

        sortAndLimit(result, limit);

        if (limit > 0 && result.size() > limit) {
            return new ArrayList<>(
                    result.subList(0, limit)
            );
        }

        return result;
    }


    private void sortAndLimit(
            List<FinancialActivity> result,
            int ignoredLimit
    ) {

        result.sort(
                (a, b) -> {
                    if (a.getDate() == null && b.getDate() == null) {
                        return 0;
                    }
                    if (a.getDate() == null) {
                        return 1;
                    }
                    if (b.getDate() == null) {
                        return -1;
                    }
                    return b.getDate().compareTo(a.getDate());
                }
        );
    }


    private void loadMembershipPayments(
            List<FinancialActivity> result,
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql = """
                SELECT
                    p.payment_date,
                    p.amount,
                    p.payment_method,
                    p.status,
                    p.member_id,
                    p.membership_id,
                    m.member_code,
                    m.name
                FROM payments p
                LEFT JOIN members m
                    ON m.id = p.member_id
                WHERE p.payment_date >= ?
                  AND p.payment_date < ?
                  AND p.membership_id IS NOT NULL
                ORDER BY p.payment_date DESC
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            setDateRange(statement, from, to);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {

                    String memberCode = safe(
                            rs.getString("member_code")
                    );

                    String memberName = safe(
                            rs.getString("name")
                    );

                    String description = memberCode.isBlank()
                            ? memberName
                            : memberCode + " - " + memberName;

                    result.add(
                            new FinancialActivity(
                                    parseDateTime(
                                            rs.getObject("payment_date")
                                    ),
                                    description,
                                    "REVENUE",
                                    rs.getBigDecimal("amount"),
                                    rs.getString("payment_method"),
                                    rs.getString("status"),
                                    true
                            )
                    );
                }
            }
        }
    }


    // =========================================================
    // LOAD PAYMENTS
    // =========================================================

    private void loadPayments(
            List<FinancialActivity> result,
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql = """
                SELECT
                    p.payment_date,
                    p.amount,
                    p.payment_method,
                    p.status,
                    p.reference_number,
                    p.member_id,
                    p.membership_id,
                    m.member_code,
                    m.name

                FROM payments p

                LEFT JOIN members m
                    ON m.id = p.member_id

                WHERE p.payment_date >= ?
                  AND p.payment_date < ?

                ORDER BY p.payment_date DESC
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            setDateRange(
                    statement,
                    from,
                    to
            );

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    String memberCode =
                            safe(
                                    rs.getString(
                                            "member_code"
                                    )
                            );

                    String memberName =
                            safe(
                                    rs.getString(
                                            "name"
                                    )
                            );

                    String description =
                            memberCode.isBlank()
                                    ? memberName
                                    : memberCode
                                        + " - "
                                        + memberName;

                    result.add(
                            new FinancialActivity(
                                    parseDateTime(
                                            rs.getObject(
                                                    "payment_date"
                                            )
                                    ),
                                    description,
                                    "REVENUE",
                                    rs.getBigDecimal(
                                            "amount"
                                    ),
                                    rs.getString(
                                            "payment_method"
                                    ),
                                    rs.getString(
                                            "status"
                                    ),
                                    rs.getObject(
                                            "membership_id"
                                    ) != null
                            )
                    );
                }
            }
        }
    }


    // =========================================================
    // LOAD MANUAL REVENUES
    // =========================================================

    private void loadManualRevenues(
            List<FinancialActivity> result,
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql = """
                SELECT
                    revenue_date,
                    name,
                    revenue_type,
                    amount
                FROM revenues
                WHERE revenue_date >= ?
                  AND revenue_date <= ?
                ORDER BY revenue_date DESC, id DESC
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = LocalDate.parse(
                            rs.getString("revenue_date")
                    );

                    String name = safe(rs.getString("name"));
                    String type = safe(rs.getString("revenue_type"));
                    String description = type.isBlank()
                            ? name
                            : name + " - " + type;

                    result.add(
                            new FinancialActivity(
                                    date.atStartOfDay(),
                                    description,
                                    "REVENUE",
                                    rs.getBigDecimal("amount"),
                                    "OTHER",
                                    "PAID",
                                    false
                            )
                    );
                }
            }
        }
    }


    // =========================================================
    // LOAD EXPENSES
    // =========================================================

    private void loadExpenses(
            List<FinancialActivity> result,
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql = """
                SELECT
                    expense_date,
                    title,
                    amount,
                    payment_method,
                    status

                FROM expenses

                WHERE expense_date >= ?
                  AND expense_date <= ?

                ORDER BY expense_date DESC
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    from.toString()
            );

            statement.setString(
                    2,
                    to.toString()
            );

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    LocalDate date =
                            LocalDate.parse(
                                    rs.getString(
                                            "expense_date"
                                    )
                            );

                    result.add(
                            new FinancialActivity(
                                    date.atStartOfDay(),
                                    safe(
                                            rs.getString(
                                                    "title"
                                            )
                                    ),
                                    "EXPENSE",
                                    rs.getBigDecimal(
                                            "amount"
                                    ),
                                    rs.getString(
                                            "payment_method"
                                    ),
                                    rs.getString(
                                            "status"
                                    ),
                                    false
                            )
                    );
                }
            }
        }
    }


    // =========================================================
    // MONTHLY FINANCIAL DATA
    // =========================================================

    public List<MonthlyFinancialSummary>
            getMonthlyFinancialSummary(
                    LocalDate from,
                    LocalDate to
            ) throws SQLException {

        validateDates(from, to);

        Map<YearMonth, BigDecimal> revenues =
                new LinkedHashMap<>();

        Map<YearMonth, BigDecimal> expenses =
                new LinkedHashMap<>();

        YearMonth current =
                YearMonth.from(from);

        YearMonth last =
                YearMonth.from(to);

        while (!current.isAfter(last)) {

            revenues.put(
                    current,
                    BigDecimal.ZERO
            );

            expenses.put(
                    current,
                    BigDecimal.ZERO
            );

            current =
                    current.plusMonths(1);
        }

        loadMonthlyRevenue(
                revenues,
                from,
                to
        );

        loadMonthlyManualRevenue(
                revenues,
                from,
                to
        );

        loadMonthlyExpenses(
                expenses,
                from,
                to
        );

        List<MonthlyFinancialSummary> result =
                new ArrayList<>();

        for (YearMonth month
                : revenues.keySet()) {

            result.add(
                    new MonthlyFinancialSummary(
                            month,
                            revenues.getOrDefault(
                                    month,
                                    BigDecimal.ZERO
                            ),
                            expenses.getOrDefault(
                                    month,
                                    BigDecimal.ZERO
                            )
                    )
            );
        }

        return result;
    }


    // =========================================================
    // MONTHLY FINANCIAL DATA BY REPORT TYPE
    // =========================================================

    public List<MonthlyFinancialSummary>
            getMonthlyFinancialSummary(
                    LocalDate from,
                    LocalDate to,
                    ReportType reportType
            ) throws SQLException {

        validateDates(from, to);

        ReportType effectiveType =
                reportType == null
                        ? ReportType.FINANCIAL_SUMMARY
                        : reportType;

        if (effectiveType == ReportType.FINANCIAL_SUMMARY) {
            return getMonthlyFinancialSummary(from, to);
        }

        Map<YearMonth, BigDecimal> revenues =
                createMonthlyMap(from, to);

        Map<YearMonth, BigDecimal> expenses =
                createMonthlyMap(from, to);

        switch (effectiveType) {

            case REVENUE -> {
                loadMonthlyRevenue(revenues, from, to);
                loadMonthlyManualRevenue(revenues, from, to);
            }

            case EXPENSES ->
                    loadMonthlyExpenses(expenses, from, to);

            case MEMBERSHIPS ->
                    loadMonthlyMembershipRevenue(
                            revenues,
                            from,
                            to
                    );

            default -> {
                return getMonthlyFinancialSummary(from, to);
            }
        }

        List<MonthlyFinancialSummary> result =
                new ArrayList<>();

        for (YearMonth month : revenues.keySet()) {
            result.add(
                    new MonthlyFinancialSummary(
                            month,
                            revenues.getOrDefault(
                                    month,
                                    BigDecimal.ZERO
                            ),
                            expenses.getOrDefault(
                                    month,
                                    BigDecimal.ZERO
                            )
                    )
            );
        }

        return result;
    }


    private Map<YearMonth, BigDecimal> createMonthlyMap(
            LocalDate from,
            LocalDate to
    ) {

        Map<YearMonth, BigDecimal> result =
                new LinkedHashMap<>();

        YearMonth current = YearMonth.from(from);
        YearMonth last = YearMonth.from(to);

        while (!current.isAfter(last)) {
            result.put(current, BigDecimal.ZERO);
            current = current.plusMonths(1);
        }

        return result;
    }


    private void loadMonthlyMembershipRevenue(
            Map<YearMonth, BigDecimal> target,
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql = """
                SELECT payment_date, amount
                FROM payments
                WHERE UPPER(status) = 'PAID'
                  AND membership_id IS NOT NULL
                  AND payment_date >= ?
                  AND payment_date < ?
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            setDateRange(statement, from, to);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {

                    LocalDateTime date =
                            parseDateTime(
                                    rs.getObject("payment_date")
                            );

                    if (date == null) {
                        continue;
                    }

                    target.merge(
                            YearMonth.from(date),
                            safeDecimal(
                                    rs.getBigDecimal("amount")
                            ),
                            BigDecimal::add
                    );
                }
            }
        }
    }


    // =========================================================
    // MONTHLY REVENUE
    // =========================================================

    private void loadMonthlyRevenue(
            Map<YearMonth, BigDecimal> target,
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql = """
                SELECT
                    payment_date,
                    amount
                FROM payments
                WHERE UPPER(status) = 'PAID'
                  AND payment_date >= ?
                  AND payment_date < ?
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            setDateRange(
                    statement,
                    from,
                    to
            );

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    LocalDateTime date =
                            parseDateTime(
                                    rs.getObject(
                                            "payment_date"
                                    )
                            );

                    if (date == null) {
                        continue;
                    }

                    YearMonth month =
                            YearMonth.from(date);

                    target.merge(
                            month,
                            safeDecimal(
                                    rs.getBigDecimal(
                                            "amount"
                                    )
                            ),
                            BigDecimal::add
                    );
                }
            }
        }
    }


    // =========================================================
    // MONTHLY MANUAL REVENUE
    // =========================================================

    private void loadMonthlyManualRevenue(
            Map<YearMonth, BigDecimal> target,
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql = """
                SELECT revenue_date, amount
                FROM revenues
                WHERE revenue_date >= ?
                  AND revenue_date <= ?
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = LocalDate.parse(
                            rs.getString("revenue_date")
                    );

                    target.merge(
                            YearMonth.from(date),
                            safeDecimal(rs.getBigDecimal("amount")),
                            BigDecimal::add
                    );
                }
            }
        }
    }


    // =========================================================
    // MONTHLY EXPENSES
    // =========================================================

    private void loadMonthlyExpenses(
            Map<YearMonth, BigDecimal> target,
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql = """
                SELECT
                    expense_date,
                    amount
                FROM expenses
                WHERE UPPER(status) = 'PAID'
                  AND expense_date >= ?
                  AND expense_date <= ?
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    from.toString()
            );

            statement.setString(
                    2,
                    to.toString()
            );

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    LocalDate date =
                            LocalDate.parse(
                                    rs.getString(
                                            "expense_date"
                                    )
                            );

                    YearMonth month =
                            YearMonth.from(date);

                    target.merge(
                            month,
                            safeDecimal(
                                    rs.getBigDecimal(
                                            "amount"
                                    )
                            ),
                            BigDecimal::add
                    );
                }
            }
        }
    }


    // =========================================================
    // DATE RANGE
    // =========================================================

    private void setDateRange(
            PreparedStatement statement,
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        statement.setString(
                1,
                formatDateTime(
                        from.atStartOfDay()
                )
        );

        statement.setString(
                2,
                formatDateTime(
                        to.plusDays(1)
                                .atStartOfDay()
                )
        );
    }


    // =========================================================
    // VALIDATE
    // =========================================================

    private void validateDates(
            LocalDate from,
            LocalDate to
    ) {

        if (from == null || to == null) {

            throw new IllegalArgumentException(
                    "Report date range is required."
            );
        }

        if (to.isBefore(from)) {

            throw new IllegalArgumentException(
                    "End date cannot be before start date."
            );
        }
    }


    // =========================================================
    // DECIMAL
    // =========================================================

    private BigDecimal readDecimal(
            ResultSet rs
    ) throws SQLException {

        if (!rs.next()) {
            return BigDecimal.ZERO;
        }

        return safeDecimal(
                rs.getBigDecimal(1)
        );
    }

    private BigDecimal safeDecimal(
            BigDecimal value
    ) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }


    // =========================================================
    // DATE
    // =========================================================

    private String formatDateTime(
            LocalDateTime value
    ) {

        return value.format(
                DB_DATE_TIME
        );
    }

    private LocalDateTime parseDateTime(
            Object value
    ) {

        if (value == null) {
            return null;
        }

        if (value instanceof java.sql.Timestamp timestamp) {

            return timestamp
                    .toLocalDateTime();
        }

        String text =
                value.toString().trim();

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

            return LocalDate.parse(
                    text
            ).atStartOfDay();

        } catch (Exception ignored) {
        }

        return null;
    }


    // =========================================================
    // STRING
    // =========================================================

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
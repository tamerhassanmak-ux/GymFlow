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
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    // =========================================================
    // SUMMARY
    // =========================================================

    public ReportSummary getSummary(
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        return getSummary(
                from,
                to,
                ReportType.FINANCIAL_SUMMARY
        );
    }


    public ReportSummary getSummary(
            LocalDate from,
            LocalDate to,
            ReportType reportType
    ) throws SQLException {

        validateDates(from, to);

        ReportType type =
                reportType == null
                        ? ReportType.FINANCIAL_SUMMARY
                        : reportType;

        ReportSummary summary =
                new ReportSummary();


        // Always useful membership statistics.

        summary.setActiveMembers(
                countActiveMembers()
        );

        summary.setNewMembers(
                countNewMembers(from, to)
        );


        switch (type) {

            case FINANCIAL_SUMMARY -> {

                summary.setTotalRevenue(
                        getRevenue(from, to, null)
                );

                summary.setMembershipRevenue(
                        getRevenue(from, to, Boolean.TRUE)
                );

                summary.setOtherRevenue(
                        getRevenue(from, to, Boolean.FALSE)
                );

                summary.setTotalExpenses(
                        getExpenses(from, to)
                );

                summary.setTransactions(
                        countTransactions(from, to)
                );
            }


            case REVENUE -> {

                BigDecimal revenue =
                        getRevenue(from, to, null);

                summary.setTotalRevenue(revenue);

                summary.setMembershipRevenue(
                        getRevenue(from, to, Boolean.TRUE)
                );

                summary.setOtherRevenue(
                        getRevenue(from, to, Boolean.FALSE)
                );

                summary.setTotalExpenses(
                        BigDecimal.ZERO
                );

                summary.setTransactions(
                        countPayments(
                                from,
                                to,
                                null
                        )
                );
            }


            case EXPENSES -> {

                summary.setTotalRevenue(
                        BigDecimal.ZERO
                );

                summary.setMembershipRevenue(
                        BigDecimal.ZERO
                );

                summary.setOtherRevenue(
                        BigDecimal.ZERO
                );

                summary.setTotalExpenses(
                        getExpenses(from, to)
                );

                summary.setTransactions(
                        countExpenses(from, to)
                );
            }


            case MEMBERSHIPS -> {

                BigDecimal membershipRevenue =
                        getRevenue(
                                from,
                                to,
                                Boolean.TRUE
                        );

                summary.setTotalRevenue(
                        membershipRevenue
                );

                summary.setMembershipRevenue(
                        membershipRevenue
                );

                summary.setOtherRevenue(
                        BigDecimal.ZERO
                );

                summary.setTotalExpenses(
                        BigDecimal.ZERO
                );

                summary.setTransactions(
                        countPayments(
                                from,
                                to,
                                Boolean.TRUE
                        )
                );
            }
        }


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

        StringBuilder sql =
                new StringBuilder("""
                        SELECT COALESCE(SUM(amount), 0)
                        FROM payments
                        WHERE UPPER(status) = 'PAID'
                          AND payment_date >= ?
                          AND payment_date < ?
                        """);


        appendMembershipFilter(
                sql,
                membershipOnly
        );


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
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

            setExpenseDateRange(
                    statement,
                    from,
                    to
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
    // TRANSACTION COUNTS
    // =========================================================

    private long countTransactions(
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        return countPayments(
                from,
                to,
                null
        ) + countExpenses(
                from,
                to
        );
    }


    private long countPayments(
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


        appendMembershipFilter(
                sql,
                membershipOnly
        );


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
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

                return rs.next()
                        ? rs.getLong(1)
                        : 0L;
            }
        }
    }


    private long countExpenses(
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
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            setExpenseDateRange(
                    statement,
                    from,
                    to
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

        return getFinancialActivity(
                from,
                to,
                ReportType.FINANCIAL_SUMMARY,
                limit
        );
    }


    public List<FinancialActivity> getFinancialActivity(
            LocalDate from,
            LocalDate to,
            ReportType reportType,
            int limit
    ) throws SQLException {

        validateDates(from, to);

        ReportType type =
                reportType == null
                        ? ReportType.FINANCIAL_SUMMARY
                        : reportType;


        List<FinancialActivity> result =
                new ArrayList<>();


        switch (type) {

            case FINANCIAL_SUMMARY -> {

                loadPayments(
                        result,
                        from,
                        to,
                        null
                );

                loadExpenses(
                        result,
                        from,
                        to
                );
            }


            case REVENUE ->

                loadPayments(
                        result,
                        from,
                        to,
                        null
                );


            case EXPENSES ->

                loadExpenses(
                        result,
                        from,
                        to
                );


            case MEMBERSHIPS ->

                loadPayments(
                        result,
                        from,
                        to,
                        Boolean.TRUE
                );
        }


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
    // LOAD PAYMENTS
    // =========================================================

    private void loadPayments(
            List<FinancialActivity> result,
            LocalDate from,
            LocalDate to,
            Boolean membershipOnly
    ) throws SQLException {

        StringBuilder sql =
                new StringBuilder("""
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

                        WHERE UPPER(p.status) = 'PAID'
                          AND p.payment_date >= ?
                          AND p.payment_date < ?
                        """);


        if (Boolean.TRUE.equals(membershipOnly)) {

            sql.append("""
                    AND p.membership_id IS NOT NULL
                    """);

        } else if (Boolean.FALSE.equals(membershipOnly)) {

            sql.append("""
                    AND p.membership_id IS NULL
                    """);
        }


        sql.append("""
                ORDER BY p.payment_date DESC
                """);


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
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

                    String reference =
                            safe(
                                    rs.getString(
                                            "reference_number"
                                    )
                            );


                    String description =
                            buildPaymentDescription(
                                    memberCode,
                                    memberName,
                                    reference
                            );


                    Object membershipId =
                            rs.getObject(
                                    "membership_id"
                            );


                    result.add(
                            new FinancialActivity(
                                    parseDateTime(
                                            rs.getObject(
                                                    "payment_date"
                                            )
                                    ),
                                    description,
                                    "REVENUE",
                                    safeDecimal(
                                            rs.getBigDecimal(
                                                    "amount"
                                            )
                                    ),
                                    rs.getString(
                                            "payment_method"
                                    ),
                                    rs.getString(
                                            "status"
                                    ),
                                    membershipId != null
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

                WHERE UPPER(status) = 'PAID'
                  AND expense_date >= ?
                  AND expense_date <= ?

                ORDER BY expense_date DESC
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            setExpenseDateRange(
                    statement,
                    from,
                    to
            );


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    LocalDate date =
                            parseDate(
                                    rs.getObject(
                                            "expense_date"
                                    )
                            );


                    if (date == null) {
                        continue;
                    }


                    result.add(
                            new FinancialActivity(
                                    date.atStartOfDay(),
                                    safe(
                                            rs.getString(
                                                    "title"
                                            )
                                    ),
                                    "EXPENSE",
                                    safeDecimal(
                                            rs.getBigDecimal(
                                                    "amount"
                                            )
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

        return getMonthlyFinancialSummary(
                from,
                to,
                ReportType.FINANCIAL_SUMMARY
        );
    }


    public List<MonthlyFinancialSummary>
            getMonthlyFinancialSummary(
                    LocalDate from,
                    LocalDate to,
                    ReportType reportType
            ) throws SQLException {

        validateDates(from, to);


        ReportType type =
                reportType == null
                        ? ReportType.FINANCIAL_SUMMARY
                        : reportType;


        Map<YearMonth, BigDecimal> revenues =
                createMonthMap(
                        from,
                        to
                );

        Map<YearMonth, BigDecimal> expenses =
                createMonthMap(
                        from,
                        to
                );


        switch (type) {

            case FINANCIAL_SUMMARY -> {

                loadMonthlyRevenue(
                        revenues,
                        from,
                        to,
                        null
                );

                loadMonthlyExpenses(
                        expenses,
                        from,
                        to
                );
            }


            case REVENUE ->

                loadMonthlyRevenue(
                        revenues,
                        from,
                        to,
                        null
                );


            case EXPENSES ->

                loadMonthlyExpenses(
                        expenses,
                        from,
                        to
                );


            case MEMBERSHIPS ->

                loadMonthlyRevenue(
                        revenues,
                        from,
                        to,
                        Boolean.TRUE
                );
        }


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
    // CREATE MONTH MAP
    // =========================================================

    private Map<YearMonth, BigDecimal> createMonthMap(
            LocalDate from,
            LocalDate to
    ) {

        Map<YearMonth, BigDecimal> result =
                new LinkedHashMap<>();


        YearMonth current =
                YearMonth.from(from);

        YearMonth last =
                YearMonth.from(to);


        while (!current.isAfter(last)) {

            result.put(
                    current,
                    BigDecimal.ZERO
            );

            current =
                    current.plusMonths(1);
        }


        return result;
    }


    // =========================================================
    // MONTHLY REVENUE
    // =========================================================

    private void loadMonthlyRevenue(
            Map<YearMonth, BigDecimal> target,
            LocalDate from,
            LocalDate to,
            Boolean membershipOnly
    ) throws SQLException {

        StringBuilder sql =
                new StringBuilder("""
                        SELECT
                            payment_date,
                            amount
                        FROM payments
                        WHERE UPPER(status) = 'PAID'
                          AND payment_date >= ?
                          AND payment_date < ?
                        """);


        appendMembershipFilter(
                sql,
                membershipOnly
        );


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
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

            setExpenseDateRange(
                    statement,
                    from,
                    to
            );


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    LocalDate date =
                            parseDate(
                                    rs.getObject(
                                            "expense_date"
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
    // MEMBERSHIP SQL FILTER
    // =========================================================

    private void appendMembershipFilter(
            StringBuilder sql,
            Boolean membershipOnly
    ) {

        if (Boolean.TRUE.equals(
                membershipOnly
        )) {

            sql.append("""
                    AND membership_id IS NOT NULL
                    """);

        } else if (Boolean.FALSE.equals(
                membershipOnly
        )) {

            sql.append("""
                    AND membership_id IS NULL
                    """);
        }
    }


    // =========================================================
    // PAYMENT DESCRIPTION
    // =========================================================

    private String buildPaymentDescription(
            String memberCode,
            String memberName,
            String reference
    ) {

        StringBuilder result =
                new StringBuilder();


        if (!memberCode.isBlank()) {

            result.append(
                    memberCode
            );
        }


        if (!memberName.isBlank()) {

            if (!result.isEmpty()) {

                result.append(
                        " - "
                );
            }

            result.append(
                    memberName
            );
        }


        if (result.isEmpty()
                && !reference.isBlank()) {

            result.append(
                    reference
            );
        }


        if (result.isEmpty()) {

            result.append(
                    "Payment"
            );
        }


        return result.toString();
    }


    // =========================================================
    // PAYMENT DATE RANGE
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
    // EXPENSE DATE RANGE
    // =========================================================

    private void setExpenseDateRange(
            PreparedStatement statement,
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        statement.setString(
                1,
                from.toString()
        );

        statement.setString(
                2,
                to.toString()
        );
    }


    // =========================================================
    // VALIDATE
    // =========================================================

    private void validateDates(
            LocalDate from,
            LocalDate to
    ) {

        if (from == null
                || to == null) {

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


        if (value instanceof LocalDateTime dateTime) {

            return dateTime;
        }


        if (value instanceof java.sql.Date sqlDate) {

            return sqlDate
                    .toLocalDate()
                    .atStartOfDay();
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


    private LocalDate parseDate(
            Object value
    ) {

        if (value == null) {
            return null;
        }


        if (value instanceof LocalDate date) {

            return date;
        }


        if (value instanceof java.sql.Date sqlDate) {

            return sqlDate.toLocalDate();
        }


        if (value instanceof java.sql.Timestamp timestamp) {

            return timestamp
                    .toLocalDateTime()
                    .toLocalDate();
        }


        String text =
                value.toString().trim();


        try {

            return LocalDate.parse(
                    text
            );

        } catch (Exception ignored) {
        }


        LocalDateTime dateTime =
                parseDateTime(value);


        return dateTime == null
                ? null
                : dateTime.toLocalDate();
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
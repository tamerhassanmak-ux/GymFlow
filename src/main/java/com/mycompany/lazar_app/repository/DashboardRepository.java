package com.mycompany.lazar_app.repository;

import com.mycompany.lazar_app.database.DatabaseManager;
import com.mycompany.lazar_app.model.dashboard.DashboardExpiration;
import com.mycompany.lazar_app.model.dashboard.DashboardMember;
import com.mycompany.lazar_app.model.dashboard.DashboardMembershipData;
import com.mycompany.lazar_app.model.dashboard.DashboardMonthlyData;
import com.mycompany.lazar_app.model.dashboard.DashboardPayment;
import com.mycompany.lazar_app.model.dashboard.DashboardSummary;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class DashboardRepository {


    private static final DateTimeFormatter DATABASE_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    // =========================================================
    // DASHBOARD SUMMARY
    // =========================================================

    public DashboardSummary getSummary() {

        DashboardSummary summary =
                new DashboardSummary();


        LocalDate today =
                LocalDate.now();


        YearMonth currentMonth =
                YearMonth.from(today);


        YearMonth previousMonth =
                currentMonth.minusMonths(1);


        try (
                Connection connection =
                        DatabaseManager.getConnection()
        ) {

            summary.setActiveMembers(
                    getActiveMembers(
                            connection
                    )
            );


            summary.setTodayCheckIns(
                    getTodayCheckIns(
                            connection,
                            today
                    )
            );


            summary.setExpiringMemberships(
                    getExpiringMembershipsCount(
                            connection,
                            today,
                            today.plusDays(7)
                    )
            );


            summary.setMonthlyRevenue(
                    getRevenueForPeriod(
                            connection,
                            currentMonth.atDay(1),
                            currentMonth.atEndOfMonth()
                    )
            );


            summary.setPreviousMonthRevenue(
                    getRevenueForPeriod(
                            connection,
                            previousMonth.atDay(1),
                            previousMonth.atEndOfMonth()
                    )
            );


            summary.setNewMembersThisMonth(
                    getNewMembersForPeriod(
                            connection,
                            currentMonth.atDay(1),
                            currentMonth.atEndOfMonth()
                    )
            );


            summary.setNewMembersPreviousMonth(
                    getNewMembersForPeriod(
                            connection,
                            previousMonth.atDay(1),
                            previousMonth.atEndOfMonth()
                    )
            );


        } catch (SQLException exception) {

            throw new RuntimeException(
                    "Failed to load dashboard summary.",
                    exception
            );
        }


        return summary;
    }


    // =========================================================
    // ACTIVE MEMBERS
    // =========================================================

    private long getActiveMembers(
            Connection connection
    ) throws SQLException {

        String sql =
                """
                SELECT COUNT(*)
                FROM members
                WHERE UPPER(status) = 'ACTIVE'
                """;


        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet result =
                        statement.executeQuery()
        ) {

            if (result.next()) {

                return result.getLong(1);
            }
        }


        return 0;
    }


    // =========================================================
    // TODAY CHECK-INS
    // =========================================================

    private long getTodayCheckIns(
            Connection connection,
            LocalDate date
    ) throws SQLException {

        /*
         * check_in_time is VARCHAR/TEXT and is normally stored as:
         * yyyy-MM-dd HH:mm:ss
         *
         * The dashboard previously relied on a text range comparison.
         * That becomes fragile when one writer uses a space and another
         * uses the ISO 'T' separator.  For the daily KPI we only need the
         * calendar date, so matching the yyyy-MM-dd prefix is both simpler
         * and tolerant of either time separator.
         *
         * Examples matched for the same day:
         * 2026-09-26 17:30:00
         * 2026-09-26T17:30:00
         * 2026-09-26 17:30
         */

        String sql =
                """
                SELECT COUNT(*)
                FROM check_ins
                WHERE check_in_time IS NOT NULL
                  AND check_in_time LIKE ?
                """;


        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    date.toString() + "%"
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    return result.getLong(1);
                }
            }
        }


        return 0;
    }


    // =========================================================
    // EXPIRING MEMBERSHIPS COUNT
    // =========================================================

    private long getExpiringMembershipsCount(
            Connection connection,
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql =
                """
                SELECT COUNT(*)
                FROM member_memberships
                WHERE UPPER(status) = 'ACTIVE'
                  AND end_date >= ?
                  AND end_date <= ?
                """;


        try (
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
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    return result.getLong(1);
                }
            }
        }


        return 0;
    }


    // =========================================================
    // REVENUE FOR PERIOD
    // =========================================================

    private BigDecimal getRevenueForPeriod(
            Connection connection,
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql =
                """
                SELECT COALESCE(SUM(amount), 0)
                FROM payments
                WHERE UPPER(status) = 'PAID'
                  AND payment_date >= ?
                  AND payment_date < ?
                """;


        LocalDateTime start =
                from.atStartOfDay();


        LocalDateTime end =
                to.plusDays(1)
                        .atStartOfDay();


        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    start.format(DATABASE_DATE_TIME_FORMATTER)
            );

            statement.setString(
                    2,
                    end.format(DATABASE_DATE_TIME_FORMATTER)
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    BigDecimal value =
                            result.getBigDecimal(1);


                    return value == null
                            ? BigDecimal.ZERO
                            : value;
                }
            }
        }


        return BigDecimal.ZERO;
    }


    // =========================================================
    // NEW MEMBERS FOR PERIOD
    // =========================================================

    private long getNewMembersForPeriod(
            Connection connection,
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        String sql =
                """
                SELECT COUNT(*)
                FROM members
                WHERE start_date >= ?
                  AND start_date <= ?
                """;


        try (
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
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    return result.getLong(1);
                }
            }
        }


        return 0;
    }


    // =========================================================
    // MONTHLY FINANCIAL DATA
    // =========================================================

    public List<DashboardMonthlyData> getMonthlyData(
            int numberOfMonths
    ) {

        int months =
                Math.max(
                        1,
                        numberOfMonths
                );


        YearMonth currentMonth =
                YearMonth.now();


        YearMonth firstMonth =
                currentMonth.minusMonths(
                        months - 1L
                );


        Map<YearMonth, DashboardMonthlyData> monthlyData =
                new LinkedHashMap<>();


        for (int i = 0; i < months; i++) {

            YearMonth month =
                    firstMonth.plusMonths(i);


            monthlyData.put(
                    month,
                    new DashboardMonthlyData(
                            month
                    )
            );
        }


        LocalDate from =
                firstMonth.atDay(1);


        LocalDate to =
                currentMonth.atEndOfMonth();


        try (
                Connection connection =
                        DatabaseManager.getConnection()
        ) {

            loadRevenueByMonth(
                    connection,
                    from,
                    to,
                    monthlyData
            );


            loadExpensesByMonth(
                    connection,
                    from,
                    to,
                    monthlyData
            );


            loadNewMembersByMonth(
                    connection,
                    from,
                    to,
                    monthlyData
            );


        } catch (SQLException exception) {

            throw new RuntimeException(
                    "Failed to load monthly dashboard data.",
                    exception
            );
        }


        return new ArrayList<>(
                monthlyData.values()
        );
    }


    // =========================================================
    // LOAD REVENUE BY MONTH
    // =========================================================

    private void loadRevenueByMonth(
            Connection connection,
            LocalDate from,
            LocalDate to,
            Map<YearMonth, DashboardMonthlyData> data
    ) throws SQLException {

        String sql =
                """
                SELECT payment_date, amount
                FROM payments
                WHERE UPPER(status) = 'PAID'
                  AND payment_date >= ?
                  AND payment_date < ?
                ORDER BY payment_date
                """;


        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    from.atStartOfDay()
                            .format(DATABASE_DATE_TIME_FORMATTER)
            );

            statement.setString(
                    2,
                    to.plusDays(1)
                            .atStartOfDay()
                            .format(DATABASE_DATE_TIME_FORMATTER)
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    LocalDateTime date =
                            parseDateTime(
                                    result.getString(
                                            "payment_date"
                                    )
                            );


                    if (date == null) {
                        continue;
                    }


                    YearMonth month =
                            YearMonth.from(date);


                    DashboardMonthlyData monthData =
                            data.get(month);


                    if (monthData == null) {
                        continue;
                    }


                    BigDecimal amount =
                            result.getBigDecimal(
                                    "amount"
                            );


                    if (amount == null) {
                        amount = BigDecimal.ZERO;
                    }


                    monthData.setRevenue(
                            monthData
                                    .getRevenue()
                                    .add(amount)
                    );
                }
            }
        }
    }


    // =========================================================
    // LOAD EXPENSES BY MONTH
    // =========================================================

    private void loadExpensesByMonth(
            Connection connection,
            LocalDate from,
            LocalDate to,
            Map<YearMonth, DashboardMonthlyData> data
    ) throws SQLException {

        String sql =
                """
                SELECT expense_date, amount
                FROM expenses
                WHERE UPPER(status) = 'PAID'
                  AND expense_date >= ?
                  AND expense_date <= ?
                ORDER BY expense_date
                """;


        try (
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
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    LocalDate date =
                            parseDate(
                                    result.getString(
                                            "expense_date"
                                    )
                            );


                    if (date == null) {
                        continue;
                    }


                    YearMonth month =
                            YearMonth.from(date);


                    DashboardMonthlyData monthData =
                            data.get(month);


                    if (monthData == null) {
                        continue;
                    }


                    BigDecimal amount =
                            result.getBigDecimal(
                                    "amount"
                            );


                    if (amount == null) {
                        amount = BigDecimal.ZERO;
                    }


                    monthData.setExpenses(
                            monthData
                                    .getExpenses()
                                    .add(amount)
                    );
                }
            }
        }
    }


    // =========================================================
    // LOAD NEW MEMBERS BY MONTH
    // =========================================================

    private void loadNewMembersByMonth(
            Connection connection,
            LocalDate from,
            LocalDate to,
            Map<YearMonth, DashboardMonthlyData> data
    ) throws SQLException {

        String sql =
                """
                SELECT start_date
                FROM members
                WHERE start_date >= ?
                  AND start_date <= ?
                ORDER BY start_date
                """;


        try (
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
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    LocalDate date =
                            parseDate(
                                    result.getString(
                                            "start_date"
                                    )
                            );


                    if (date == null) {
                        continue;
                    }


                    YearMonth month =
                            YearMonth.from(date);


                    DashboardMonthlyData monthData =
                            data.get(month);


                    if (monthData == null) {
                        continue;
                    }


                    monthData.setNewMembers(
                            monthData.getNewMembers()
                            + 1
                    );
                }
            }
        }
    }


    // =========================================================
    // MEMBERSHIP DISTRIBUTION
    // =========================================================

    public List<DashboardMembershipData>
            getMembershipDistribution() {

        List<DashboardMembershipData> data =
                new ArrayList<>();


        String sql =
                """
                SELECT
                    mp.id,
                    mp.name_ar,
                    mp.name_en,
                    COUNT(mm.id) AS members_count

                FROM membership_plans mp

                LEFT JOIN member_memberships mm
                    ON mm.membership_plan_id = mp.id
                    AND UPPER(mm.status) = 'ACTIVE'

                WHERE UPPER(mp.status) = 'ACTIVE'

                GROUP BY
                    mp.id,
                    mp.name_ar,
                    mp.name_en

                ORDER BY members_count DESC,
                         mp.id ASC
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet result =
                        statement.executeQuery()
        ) {

            while (result.next()) {

                data.add(
                        new DashboardMembershipData(
                                result.getLong("id"),
                                result.getString("name_ar"),
                                result.getString("name_en"),
                                result.getLong("members_count")
                        )
                );
            }


        } catch (SQLException exception) {

            throw new RuntimeException(
                    "Failed to load membership distribution.",
                    exception
            );
        }


        return data;
    }


    // =========================================================
    // RECENT MEMBERS
    // =========================================================

    public List<DashboardMember> getRecentMembers(
            int limit
    ) {

        List<DashboardMember> members =
                new ArrayList<>();


        int safeLimit =
                Math.max(
                        1,
                        limit
                );


        String sql =
                """
                SELECT
                    m.id,
                    m.member_code,
                    m.name,
                    m.membership,
                    m.status,
                    m.start_date

                FROM members m

                ORDER BY
                    m.created_at DESC,
                    m.id DESC

                LIMIT ?
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    safeLimit
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    members.add(
                            new DashboardMember(
                                    result.getLong("id"),
                                    result.getString(
                                            "member_code"
                                    ),
                                    result.getString(
                                            "name"
                                    ),
                                    result.getString(
                                            "membership"
                                    ),
                                    result.getString(
                                            "status"
                                    ),
                                    parseDate(
                                            result.getString(
                                                    "start_date"
                                            )
                                    )
                            )
                    );
                }
            }


        } catch (SQLException exception) {

            throw new RuntimeException(
                    "Failed to load recent members.",
                    exception
            );
        }


        return members;
    }


    // =========================================================
    // UPCOMING EXPIRATIONS
    // =========================================================

    public List<DashboardExpiration>
            getUpcomingExpirations(
                    int days,
                    int limit
            ) {

        List<DashboardExpiration> expirations =
                new ArrayList<>();


        LocalDate today =
                LocalDate.now();


        LocalDate end =
                today.plusDays(
                        Math.max(
                                1,
                                days
                        )
                );


        int safeLimit =
                Math.max(
                        1,
                        limit
                );


        String sql =
                """
                SELECT
                    mm.id AS membership_id,
                    m.id AS member_id,
                    m.member_code,
                    m.name AS member_name,
                    mp.name_ar,
                    mp.name_en,
                    mm.end_date

                FROM member_memberships mm

                INNER JOIN members m
                    ON m.id = mm.member_id

                INNER JOIN membership_plans mp
                    ON mp.id = mm.membership_plan_id

                WHERE UPPER(mm.status) = 'ACTIVE'
                  AND mm.end_date >= ?
                  AND mm.end_date <= ?

                ORDER BY
                    mm.end_date ASC,
                    mm.id ASC

                LIMIT ?
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    today.toString()
            );

            statement.setString(
                    2,
                    end.toString()
            );

            statement.setInt(
                    3,
                    safeLimit
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    expirations.add(
                            new DashboardExpiration(
                                    result.getLong(
                                            "membership_id"
                                    ),
                                    result.getLong(
                                            "member_id"
                                    ),
                                    result.getString(
                                            "member_code"
                                    ),
                                    result.getString(
                                            "member_name"
                                    ),
                                    result.getString(
                                            "name_ar"
                                    ),
                                    result.getString(
                                            "name_en"
                                    ),
                                    parseDate(
                                            result.getString(
                                                    "end_date"
                                            )
                                    )
                            )
                    );
                }
            }


        } catch (SQLException exception) {

            throw new RuntimeException(
                    "Failed to load upcoming expirations.",
                    exception
            );
        }


        return expirations;
    }


    // =========================================================
    // RECENT PAYMENTS
    // =========================================================

    public List<DashboardPayment> getRecentPayments(
            int limit
    ) {

        List<DashboardPayment> payments =
                new ArrayList<>();


        int safeLimit =
                Math.max(
                        1,
                        limit
                );


        String sql =
                """
                SELECT
                    p.id,
                    p.member_id,
                    m.member_code,
                    m.name AS member_name,
                    p.amount,
                    p.payment_method,
                    p.status,
                    p.payment_date

                FROM payments p

                INNER JOIN members m
                    ON m.id = p.member_id

                ORDER BY
                    p.payment_date DESC,
                    p.id DESC

                LIMIT ?
                """;


        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    safeLimit
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    payments.add(
                            new DashboardPayment(
                                    result.getLong(
                                            "id"
                                    ),
                                    result.getLong(
                                            "member_id"
                                    ),
                                    result.getString(
                                            "member_code"
                                    ),
                                    result.getString(
                                            "member_name"
                                    ),
                                    result.getBigDecimal(
                                            "amount"
                                    ),
                                    result.getString(
                                            "payment_method"
                                    ),
                                    result.getString(
                                            "status"
                                    ),
                                    parseDateTime(
                                            result.getString(
                                                    "payment_date"
                                            )
                                    )
                            )
                    );
                }
            }


        } catch (SQLException exception) {

            throw new RuntimeException(
                    "Failed to load recent payments.",
                    exception
            );
        }


        return payments;
    }


    // =========================================================
    // DATE PARSER
    // =========================================================

    private LocalDate parseDate(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }


        String text =
                value.trim();


        try {

            return LocalDate.parse(
                    text
            );

        } catch (DateTimeParseException ignored) {
        }


        try {

            return LocalDateTime.parse(
                    text
            ).toLocalDate();

        } catch (DateTimeParseException ignored) {
        }


        /*
         * SQLite CURRENT_TIMESTAMP usually returns:
         *
         * yyyy-MM-dd HH:mm:ss
         */

        try {

            return LocalDateTime.parse(
                    text,
                    DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd HH:mm:ss"
                    )
            ).toLocalDate();

        } catch (DateTimeParseException ignored) {
        }


        return null;
    }


    // =========================================================
    // DATE TIME PARSER
    // =========================================================

    private LocalDateTime parseDateTime(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }


        String text =
                value.trim();


        try {

            return LocalDateTime.parse(
                    text
            );

        } catch (DateTimeParseException ignored) {
        }


        try {

            return LocalDateTime.parse(
                    text,
                    DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd HH:mm:ss"
                    )
            );

        } catch (DateTimeParseException ignored) {
        }


        try {

            return LocalDate
                    .parse(text)
                    .atStartOfDay();

        } catch (DateTimeParseException ignored) {
        }


        return null;
    }
}
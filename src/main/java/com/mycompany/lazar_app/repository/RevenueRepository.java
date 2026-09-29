package com.mycompany.lazar_app.repository;

import com.mycompany.lazar_app.database.DatabaseManager;
import com.mycompany.lazar_app.model.Revenue;

import java.math.BigDecimal;
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

public class RevenueRepository {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<Revenue> findAll() throws SQLException {

        String sql = """
                SELECT
                    id,
                    revenue_code,
                    name,
                    revenue_type,
                    amount,
                    revenue_date,
                    attachment_path,
                    notes,
                    created_at,
                    updated_at
                FROM revenues
                ORDER BY revenue_date DESC, id DESC
                """;

        List<Revenue> result = new ArrayList<>();

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()
        ) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }

        return result;
    }

    public List<Revenue> search(
            String keyword,
            String type
    ) throws SQLException {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    revenue_code,
                    name,
                    revenue_type,
                    amount,
                    revenue_date,
                    attachment_path,
                    notes,
                    created_at,
                    updated_at
                FROM revenues
                WHERE 1 = 1
                """);

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {

            String[] words = keyword.trim().toLowerCase().split("\\s+");

            for (String word : words) {

                if (word.isBlank()) {
                    continue;
                }

                sql.append("""
                        AND (
                               LOWER(COALESCE(revenue_code, '')) LIKE ?
                            OR LOWER(COALESCE(name, '')) LIKE ?
                            OR LOWER(COALESCE(revenue_type, '')) LIKE ?
                            OR LOWER(COALESCE(notes, '')) LIKE ?
                            OR CAST(amount AS VARCHAR(60)) LIKE ?
                            OR CAST(revenue_date AS VARCHAR(60)) LIKE ?
                        )
                        """);

                String like = "%" + word + "%";
                for (int i = 0; i < 6; i++) {
                    params.add(like);
                }
            }
        }

        if (type != null
                && !type.isBlank()
                && !"ALL".equalsIgnoreCase(type)) {

            sql.append(" AND LOWER(revenue_type) = LOWER(?) ");
            params.add(type.trim());
        }

        sql.append(" ORDER BY revenue_date DESC, id DESC ");

        List<Revenue> result = new ArrayList<>();

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql.toString()
                )
        ) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }

        return result;
    }

    public Revenue save(Revenue revenue) throws SQLException {

        validate(revenue);

        String sql = """
                INSERT INTO revenues
                (
                    revenue_code,
                    name,
                    revenue_type,
                    amount,
                    revenue_date,
                    attachment_path,
                    notes,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection()) {

            boolean oldAutoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);

                saveRevenueTypeIfMissing(
                        connection,
                        revenue.getRevenueType()
                );

                if (revenue.getRevenueCode() == null
                        || revenue.getRevenueCode().isBlank()) {
                    revenue.setRevenueCode(
                            nextRevenueCode(connection)
                    );
                }

                String now = LocalDateTime.now().format(DATE_TIME);

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     sql,
                                     Statement.RETURN_GENERATED_KEYS
                             )) {

                    statement.setString(1, revenue.getRevenueCode());
                    statement.setString(2, revenue.getName().trim());
                    statement.setString(3, revenue.getRevenueType().trim());
                    statement.setBigDecimal(4, revenue.getAmount());
                    statement.setString(5, revenue.getRevenueDate().toString());
                    setNullableString(statement, 6, revenue.getAttachmentPath());
                    setNullableString(statement, 7, revenue.getNotes());
                    statement.setString(8, now);
                    statement.setString(9, now);

                    statement.executeUpdate();

                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (keys.next()) {
                            revenue.setId(keys.getLong(1));
                        }
                    }
                }

                connection.commit();
                return revenue;

            } catch (Exception exception) {
                connection.rollback();

                if (exception instanceof SQLException sqlException) {
                    throw sqlException;
                }

                throw new SQLException(
                        "Unable to save revenue: " + exception.getMessage(),
                        exception
                );

            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public void update(Revenue revenue) throws SQLException {

        if (revenue == null || revenue.getId() <= 0) {
            throw new IllegalArgumentException(
                    "Revenue ID is required for update."
            );
        }

        validate(revenue);

        String sql = """
                UPDATE revenues
                SET name = ?,
                    revenue_type = ?,
                    amount = ?,
                    revenue_date = ?,
                    attachment_path = ?,
                    notes = ?,
                    updated_at = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection()) {

            boolean oldAutoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);

                saveRevenueTypeIfMissing(
                        connection,
                        revenue.getRevenueType()
                );

                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setString(1, revenue.getName().trim());
                    statement.setString(2, revenue.getRevenueType().trim());
                    statement.setBigDecimal(3, revenue.getAmount());
                    statement.setString(4, revenue.getRevenueDate().toString());
                    setNullableString(statement, 5, revenue.getAttachmentPath());
                    setNullableString(statement, 6, revenue.getNotes());
                    statement.setString(
                            7,
                            LocalDateTime.now().format(DATE_TIME)
                    );
                    statement.setLong(8, revenue.getId());

                    if (statement.executeUpdate() == 0) {
                        throw new SQLException(
                                "Revenue not found: " + revenue.getId()
                        );
                    }
                }

                connection.commit();

            } catch (Exception exception) {
                connection.rollback();

                if (exception instanceof SQLException sqlException) {
                    throw sqlException;
                }

                throw new SQLException(
                        "Unable to update revenue: "
                                + exception.getMessage(),
                        exception
                );

            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public void deleteById(long id) throws SQLException {

        String sql = "DELETE FROM revenues WHERE id = ?";

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public List<String> findRevenueTypes() throws SQLException {

        String sql = """
                SELECT name
                FROM revenue_types
                ORDER BY LOWER(name), name
                """;

        List<String> result = new ArrayList<>();

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()
        ) {
            while (rs.next()) {
                String value = rs.getString(1);
                if (value != null && !value.isBlank()) {
                    result.add(value.trim());
                }
            }
        }

        return result;
    }

    public BigDecimal getTotalRevenue() throws SQLException {
        return queryAmount("SELECT COALESCE(SUM(amount), 0) FROM revenues");
    }

    public BigDecimal getCurrentMonthRevenue() throws SQLException {

        LocalDate today = LocalDate.now();
        LocalDate first = today.withDayOfMonth(1);
        LocalDate next = first.plusMonths(1);

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM revenues
                WHERE revenue_date >= ?
                  AND revenue_date < ?
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, first.toString());
            statement.setString(2, next.toString());

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getBigDecimal(1) != null
                        ? rs.getBigDecimal(1)
                        : BigDecimal.ZERO;
            }
        }
    }

    public long countAll() throws SQLException {
        return queryLong("SELECT COUNT(*) FROM revenues");
    }

    public long countRevenueTypes() throws SQLException {
        return queryLong("SELECT COUNT(*) FROM revenue_types");
    }

    private void saveRevenueTypeIfMissing(
            Connection connection,
            String type
    ) throws SQLException {

        String normalized = type == null ? "" : type.trim();

        if (normalized.isBlank()) {
            return;
        }

        String checkSql = """
                SELECT COUNT(*)
                FROM revenue_types
                WHERE LOWER(name) = LOWER(?)
                """;

        try (PreparedStatement check =
                     connection.prepareStatement(checkSql)) {

            check.setString(1, normalized);

            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
        }

        String insertSql = "INSERT INTO revenue_types(name) VALUES (?)";

        try (PreparedStatement insert =
                     connection.prepareStatement(insertSql)) {
            insert.setString(1, normalized);
            insert.executeUpdate();
        }
    }

    private String nextRevenueCode(Connection connection)
            throws SQLException {

        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM revenues";

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()
        ) {
            long next = rs.next() ? rs.getLong(1) : 1L;
            return String.format("REV-%06d", next);
        }
    }

    private BigDecimal queryAmount(String sql) throws SQLException {

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()
        ) {
            if (!rs.next()) {
                return BigDecimal.ZERO;
            }

            BigDecimal value = rs.getBigDecimal(1);
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    private long queryLong(String sql) throws SQLException {

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()
        ) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private Revenue map(ResultSet rs) throws SQLException {

        Revenue revenue = new Revenue();
        revenue.setId(rs.getLong("id"));
        revenue.setRevenueCode(rs.getString("revenue_code"));
        revenue.setName(rs.getString("name"));
        revenue.setRevenueType(rs.getString("revenue_type"));
        revenue.setAmount(rs.getBigDecimal("amount"));

        String date = rs.getString("revenue_date");
        if (date != null && !date.isBlank()) {
            revenue.setRevenueDate(LocalDate.parse(date.trim()));
        }

        revenue.setAttachmentPath(rs.getString("attachment_path"));
        revenue.setNotes(rs.getString("notes"));
        revenue.setCreatedAt(parseDateTime(rs.getObject("created_at")));
        revenue.setUpdatedAt(parseDateTime(rs.getObject("updated_at")));

        return revenue;
    }

    private void validate(Revenue revenue) {

        if (revenue == null) {
            throw new IllegalArgumentException("Revenue is required.");
        }

        if (revenue.getName() == null
                || revenue.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Revenue name is required."
            );
        }

        if (revenue.getRevenueType() == null
                || revenue.getRevenueType().isBlank()) {
            throw new IllegalArgumentException(
                    "Revenue type is required."
            );
        }

        if (revenue.getAmount() == null
                || revenue.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Revenue amount must be greater than zero."
            );
        }

        if (revenue.getRevenueDate() == null) {
            throw new IllegalArgumentException(
                    "Revenue date is required."
            );
        }
    }

    private void setNullableString(
            PreparedStatement statement,
            int index,
            String value
    ) throws SQLException {

        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    private LocalDateTime parseDateTime(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        String text = value.toString().trim();

        try {
            return LocalDateTime.parse(text);
        } catch (Exception ignored) {
        }

        try {
            return LocalDateTime.parse(text, DATE_TIME);
        } catch (Exception ignored) {
        }

        return null;
    }
}

package com.mycompany.lazar_app.repository;

import com.mycompany.lazar_app.database.DatabaseManager;
import com.mycompany.lazar_app.model.Expense;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExpenseRepository {

    // =========================================================
    // SAVE
    // =========================================================

    public Expense save(Expense expense) {

        String sql = """
                INSERT INTO expenses
                (
                    expense_code,
                    title,
                    category,
                    amount,
                    expense_date,
                    payment_method,
                    status,
                    reference_number,
                    beneficiary,
                    notes,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        LocalDateTime now = LocalDateTime.now();

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            /*
             * Generate the business code before insert.
             */
            if (expense.getExpenseCode() == null
                    || expense.getExpenseCode().isBlank()) {

                expense.setExpenseCode(
                        generateExpenseCode(connection)
                );
            }

            statement.setString(
                    1,
                    expense.getExpenseCode()
            );

            statement.setString(
                    2,
                    expense.getTitle()
            );

            statement.setString(
                    3,
                    expense.getCategory()
            );

            statement.setBigDecimal(
                    4,
                    expense.getAmount()
            );

            statement.setString(
                    5,
                    expense.getExpenseDate().toString()
            );

            statement.setString(
                    6,
                    expense.getPaymentMethod()
            );

            statement.setString(
                    7,
                    normalizeStatus(
                            expense.getStatus()
                    )
            );

            statement.setString(
                    8,
                    emptyToNull(
                            expense.getReferenceNumber()
                    )
            );

            statement.setString(
                    9,
                    emptyToNull(
                            expense.getBeneficiary()
                    )
            );

            statement.setString(
                    10,
                    emptyToNull(
                            expense.getNotes()
                    )
            );

            statement.setString(
                    11,
                    now.toString()
            );

            statement.setString(
                    12,
                    now.toString()
            );

            int affected =
                    statement.executeUpdate();

            if (affected == 0) {

                throw new RuntimeException(
                        "Failed to create expense."
                );
            }

            try (ResultSet keys =
                         statement.getGeneratedKeys()) {

                if (keys.next()) {

                    expense.setId(
                            keys.getLong(1)
                    );
                }
            }

            expense.setCreatedAt(now);
            expense.setUpdatedAt(now);

            return expense;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to save expense: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public void update(Expense expense) {

        String sql = """
                UPDATE expenses
                SET
                    title = ?,
                    category = ?,
                    amount = ?,
                    expense_date = ?,
                    payment_method = ?,
                    status = ?,
                    reference_number = ?,
                    beneficiary = ?,
                    notes = ?,
                    updated_at = ?
                WHERE id = ?
                """;

        LocalDateTime now =
                LocalDateTime.now();

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    expense.getTitle()
            );

            statement.setString(
                    2,
                    expense.getCategory()
            );

            statement.setBigDecimal(
                    3,
                    expense.getAmount()
            );

            statement.setString(
                    4,
                    expense.getExpenseDate().toString()
            );

            statement.setString(
                    5,
                    expense.getPaymentMethod()
            );

            statement.setString(
                    6,
                    normalizeStatus(
                            expense.getStatus()
                    )
            );

            statement.setString(
                    7,
                    emptyToNull(
                            expense.getReferenceNumber()
                    )
            );

            statement.setString(
                    8,
                    emptyToNull(
                            expense.getBeneficiary()
                    )
            );

            statement.setString(
                    9,
                    emptyToNull(
                            expense.getNotes()
                    )
            );

            statement.setString(
                    10,
                    now.toString()
            );

            statement.setLong(
                    11,
                    expense.getId()
            );

            int affected =
                    statement.executeUpdate();

            if (affected == 0) {

                throw new RuntimeException(
                        "Expense not found."
                );
            }

            expense.setUpdatedAt(now);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to update expense: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // DELETE
    // =========================================================

    public void delete(long id) {

        String sql = """
                DELETE FROM expenses
                WHERE id = ?
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            statement.executeUpdate();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to delete expense: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // FIND ALL
    // =========================================================

    public List<Expense> findAll() {

        String sql = """
                SELECT *
                FROM expenses
                ORDER BY expense_date DESC, id DESC
                """;

        List<Expense> expenses =
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

                expenses.add(
                        mapExpense(rs)
                );
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to load expenses: "
                            + e.getMessage(),
                    e
            );
        }

        return expenses;
    }

    // =========================================================
    // FIND BY ID
    // =========================================================

    public Optional<Expense> findById(long id) {

        String sql = """
                SELECT *
                FROM expenses
                WHERE id = ?
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    return Optional.of(
                            mapExpense(rs)
                    );
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to find expense: "
                            + e.getMessage(),
                    e
            );
        }

        return Optional.empty();
    }

    // =========================================================
    // SEARCH
    // =========================================================

    public List<Expense> search(
            String keyword,
            String category,
            String status) {

        StringBuilder sql =
                new StringBuilder(
                        """
                        SELECT *
                        FROM expenses
                        WHERE 1 = 1
                        """
                );

        List<Object> parameters =
                new ArrayList<>();

        // -----------------------------------------------------
        // SMART KEYWORD SEARCH
        // -----------------------------------------------------

        if (keyword != null
                && !keyword.isBlank()) {

            sql.append(
                    """
                     AND (
                            LOWER(expense_code) LIKE ?
                         OR LOWER(title) LIKE ?
                         OR LOWER(category) LIKE ?
                         OR LOWER(beneficiary) LIKE ?
                         OR LOWER(reference_number) LIKE ?
                         OR LOWER(payment_method) LIKE ?
                         OR LOWER(status) LIKE ?
                         OR LOWER(notes) LIKE ?
                         OR CAST(amount AS VARCHAR) LIKE ?
                         OR CAST(expense_date AS VARCHAR) LIKE ?
                     )
                    """
            );

            String value =
                    "%"
                            + keyword
                            .trim()
                            .toLowerCase()
                            + "%";

            for (int i = 0; i < 10; i++) {
                parameters.add(value);
            }
        }

        // -----------------------------------------------------
        // CATEGORY FILTER
        // -----------------------------------------------------

        if (category != null
                && !category.isBlank()
                && !category.equalsIgnoreCase("ALL")) {

            sql.append(
                    " AND category = ? "
            );

            parameters.add(category);
        }

        // -----------------------------------------------------
        // STATUS FILTER
        // -----------------------------------------------------

        if (status != null
                && !status.isBlank()
                && !status.equalsIgnoreCase("ALL")) {

            sql.append(
                    " AND status = ? "
            );

            parameters.add(
                    status.toUpperCase()
            );
        }

        sql.append(
                " ORDER BY expense_date DESC, id DESC"
        );

        List<Expense> expenses =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            for (int i = 0;
                 i < parameters.size();
                 i++) {

                statement.setObject(
                        i + 1,
                        parameters.get(i)
                );
            }

            try (ResultSet rs =
                         statement.executeQuery()) {

                while (rs.next()) {

                    expenses.add(
                            mapExpense(rs)
                    );
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to search expenses: "
                            + e.getMessage(),
                    e
            );
        }

        return expenses;
    }

    // =========================================================
    // TOTAL PAID EXPENSES
    // =========================================================

    public BigDecimal getTotalExpenses() {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM expenses
                WHERE status = 'PAID'
                """;

        return queryAmount(sql);
    }

    // =========================================================
    // TOTAL BETWEEN DATES
    // =========================================================

    public BigDecimal getTotalExpensesBetween(
            LocalDate start,
            LocalDate end) {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM expenses
                WHERE status = 'PAID'
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
                    start.toString()
            );

            statement.setString(
                    2,
                    end.toString()
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    BigDecimal amount =
                            rs.getBigDecimal(1);

                    return amount == null
                            ? BigDecimal.ZERO
                            : amount;
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to calculate monthly expenses: "
                            + e.getMessage(),
                    e
            );
        }

        return BigDecimal.ZERO;
    }

    // =========================================================
    // PENDING AMOUNT
    // =========================================================

    public BigDecimal getPendingAmount() {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM expenses
                WHERE status = 'PENDING'
                """;

        return queryAmount(sql);
    }

    // =========================================================
    // COUNT ALL
    // =========================================================

    public long countAll() {

        String sql = """
                SELECT COUNT(*)
                FROM expenses
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet rs =
                        statement.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to count expenses: "
                            + e.getMessage(),
                    e
            );
        }

        return 0;
    }

    // =========================================================
    // GENERATE EXPENSE CODE
    // =========================================================
private String generateExpenseCode(
        Connection connection)
        throws Exception {

    String sql = """
            SELECT expense_code
            FROM expenses
            WHERE expense_code LIKE 'EXP-%'
            ORDER BY id DESC
            """;

    long maxNumber = 0;

    try (
            PreparedStatement statement =
                    connection.prepareStatement(sql);

            ResultSet rs =
                    statement.executeQuery()
    ) {

        while (rs.next()) {

            String code =
                    rs.getString(
                            "expense_code"
                    );

            if (code == null
                    || !code.startsWith(
                            "EXP-"
                    )) {

                continue;
            }

            try {

                long number =
                        Long.parseLong(
                                code.substring(4)
                        );

                if (number > maxNumber) {
                    maxNumber = number;
                }

            } catch (NumberFormatException ignored) {
            }
        }
    }

    return String.format(
            "EXP-%06d",
            maxNumber + 1
    );
}

    // =========================================================
    // MAP DATABASE -> MODEL
    // =========================================================

    private Expense mapExpense(
            ResultSet rs) throws Exception {

        Expense expense =
                new Expense();

        expense.setId(
                rs.getLong("id")
        );

        expense.setExpenseCode(
                rs.getString("expense_code")
        );

        expense.setTitle(
                rs.getString("title")
        );

        expense.setCategory(
                rs.getString("category")
        );

        expense.setAmount(
                rs.getBigDecimal("amount")
        );

        String expenseDate =
                rs.getString("expense_date");

        if (expenseDate != null
                && !expenseDate.isBlank()) {

            expense.setExpenseDate(
                    LocalDate.parse(
                            expenseDate
                                    .substring(0, 10)
                    )
            );
        }

        expense.setPaymentMethod(
                rs.getString(
                        "payment_method"
                )
        );

        expense.setStatus(
                rs.getString("status")
        );

        expense.setReferenceNumber(
                rs.getString(
                        "reference_number"
                )
        );

        expense.setBeneficiary(
                rs.getString("beneficiary")
        );

        expense.setNotes(
                rs.getString("notes")
        );

        expense.setCreatedAt(
                parseDateTime(
                        rs.getString(
                                "created_at"
                        )
                )
        );

        expense.setUpdatedAt(
                parseDateTime(
                        rs.getString(
                                "updated_at"
                        )
                )
        );

        return expense;
    }

    // =========================================================
    // QUERY SINGLE AMOUNT
    // =========================================================

    private BigDecimal queryAmount(
            String sql) {

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet rs =
                        statement.executeQuery()
        ) {

            if (rs.next()) {

                BigDecimal amount =
                        rs.getBigDecimal(1);

                return amount == null
                        ? BigDecimal.ZERO
                        : amount;
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to calculate expense amount: "
                            + e.getMessage(),
                    e
            );
        }

        return BigDecimal.ZERO;
    }

    // =========================================================
    // NORMALIZE STATUS
    // =========================================================

    private String normalizeStatus(
            String status) {

        if (status == null
                || status.isBlank()) {

            return "PAID";
        }

        return status
                .trim()
                .toUpperCase();
    }

    // =========================================================
    // EMPTY STRING -> NULL
    // =========================================================

    private String emptyToNull(
            String value) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }

    // =========================================================
    // PARSE DATETIME
    // =========================================================

    private LocalDateTime parseDateTime(
            String value) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        try {

            return LocalDateTime.parse(value);

        } catch (Exception ignored) {

            try {

                return Timestamp
                        .valueOf(value)
                        .toLocalDateTime();

            } catch (Exception ignoredAgain) {

                return null;
            }
        }
    }
}
package com.mycompany.lazar_app.repository;

import com.mycompany.lazar_app.database.DatabaseManager;
import com.mycompany.lazar_app.model.MembershipPlan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MembershipPlanRepository {

    // =========================================================
    // FIND ALL
    // =========================================================

    public List<MembershipPlan> findAll() throws SQLException {

        String sql = """
                SELECT
                    id,
                    code,
                    name_ar,
                    name_en,
                    duration_months,
                    max_sessions,
                    price,
                    status,
                    description,
                    created_at,
                    updated_at
                FROM membership_plans
                ORDER BY duration_months ASC, id ASC
                """;

        List<MembershipPlan> plans = new ArrayList<>();

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            while (resultSet.next()) {

                plans.add(
                        mapMembershipPlan(resultSet)
                );
            }
        }

        return plans;
    }


    // =========================================================
    // FIND ACTIVE
    // =========================================================

    public List<MembershipPlan> findActive() throws SQLException {

        String sql = """
                SELECT
                    id,
                    code,
                    name_ar,
                    name_en,
                    duration_months,
                    max_sessions,
                    price,
                    status,
                    description,
                    created_at,
                    updated_at
                FROM membership_plans
                WHERE status = 'Active'
                ORDER BY duration_months ASC, id ASC
                """;

        List<MembershipPlan> plans = new ArrayList<>();

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            while (resultSet.next()) {

                plans.add(
                        mapMembershipPlan(resultSet)
                );
            }
        }

        return plans;
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    public Optional<MembershipPlan> findById(
            int id
    ) throws SQLException {

        String sql = """
                SELECT
                    id,
                    code,
                    name_ar,
                    name_en,
                    duration_months,
                    max_sessions,
                    price,
                    status,
                    description,
                    created_at,
                    updated_at
                FROM membership_plans
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

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (resultSet.next()) {

                    return Optional.of(
                            mapMembershipPlan(resultSet)
                    );
                }
            }
        }

        return Optional.empty();
    }


    // =========================================================
    // FIND BY CODE
    // =========================================================

    public Optional<MembershipPlan> findByCode(
            String code
    ) throws SQLException {

        String sql = """
                SELECT
                    id,
                    code,
                    name_ar,
                    name_en,
                    duration_months,
                    max_sessions,
                    price,
                    status,
                    description,
                    created_at,
                    updated_at
                FROM membership_plans
                WHERE code = ?
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    safe(code)
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (resultSet.next()) {

                    return Optional.of(
                            mapMembershipPlan(resultSet)
                    );
                }
            }
        }

        return Optional.empty();
    }


    // =========================================================
    // INSERT
    // =========================================================

    public MembershipPlan insert(
            MembershipPlan plan
    ) throws SQLException {

        validatePlan(plan);

        String sql = """
                INSERT INTO membership_plans (
                    code,
                    name_ar,
                    name_en,
                    duration_months,
                    max_sessions,
                    price,
                    status,
                    description
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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

            statement.setString(
                    1,
                    safe(plan.getCode())
            );

            statement.setString(
                    2,
                    safe(plan.getNameAr())
            );

            statement.setString(
                    3,
                    safe(plan.getNameEn())
            );

            statement.setInt(
                    4,
                    plan.getDurationMonths()
            );

            statement.setInt(
                    5,
                    plan.getMaxSessions()
            );

            statement.setDouble(
                    6,
                    plan.getPrice()
            );

            statement.setString(
                    7,
                    normalizeStatus(plan.getStatus())
            );

            statement.setString(
                    8,
                    safe(plan.getDescription())
            );

            int affectedRows =
                    statement.executeUpdate();

            if (affectedRows == 0) {

                throw new SQLException(
                        "Creating membership plan failed. No rows were inserted."
                );
            }

            try (
                    ResultSet generatedKeys =
                            statement.getGeneratedKeys()
            ) {

                if (!generatedKeys.next()) {

                    throw new SQLException(
                            "Creating membership plan failed. Database did not return an ID."
                    );
                }

                plan.setId(
                        generatedKeys.getInt(1)
                );
            }

            return plan;
        }
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public boolean update(
            MembershipPlan plan
    ) throws SQLException {

        validatePlan(plan);

        if (plan.getId() <= 0) {

            throw new IllegalArgumentException(
                    "Membership plan ID is invalid."
            );
        }

        String sql = """
                UPDATE membership_plans
                SET
                    code = ?,
                    name_ar = ?,
                    name_en = ?,
                    duration_months = ?,
                    max_sessions = ?,
                    price = ?,
                    status = ?,
                    description = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    safe(plan.getCode())
            );

            statement.setString(
                    2,
                    safe(plan.getNameAr())
            );

            statement.setString(
                    3,
                    safe(plan.getNameEn())
            );

            statement.setInt(
                    4,
                    plan.getDurationMonths()
            );

            statement.setInt(
                    5,
                    plan.getMaxSessions()
            );

            statement.setDouble(
                    6,
                    plan.getPrice()
            );

            statement.setString(
                    7,
                    normalizeStatus(plan.getStatus())
            );

            statement.setString(
                    8,
                    safe(plan.getDescription())
            );

            statement.setInt(
                    9,
                    plan.getId()
            );

            return statement.executeUpdate() > 0;
        }
    }


    // =========================================================
    // DELETE
    // =========================================================

    public boolean delete(
            int planId
    ) throws SQLException {

        if (planId <= 0) {

            throw new IllegalArgumentException(
                    "Membership plan ID is invalid."
            );
        }

        String sql = """
                DELETE FROM membership_plans
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
                    planId
            );

            return statement.executeUpdate() > 0;
        }
    }


    // =========================================================
    // EXISTS
    // =========================================================

    public boolean exists(
            int planId
    ) throws SQLException {

        String sql = """
                SELECT 1
                FROM membership_plans
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
                    planId
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                return resultSet.next();
            }
        }
    }


    // =========================================================
    // EXISTS BY CODE
    // =========================================================

    public boolean existsByCode(
            String code
    ) throws SQLException {

        String sql = """
                SELECT 1
                FROM membership_plans
                WHERE code = ?
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    safe(code)
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                return resultSet.next();
            }
        }
    }


    // =========================================================
    // COUNT
    // =========================================================

    public int count() throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM membership_plans
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

                return resultSet.getInt(1);
            }
        }

        return 0;
    }


    // =========================================================
    // RESULT SET -> MEMBERSHIP PLAN
    // =========================================================

    private MembershipPlan mapMembershipPlan(
            ResultSet resultSet
    ) throws SQLException {

        return new MembershipPlan(
                resultSet.getInt(
                        "id"
                ),

                resultSet.getString(
                        "code"
                ),

                resultSet.getString(
                        "name_ar"
                ),

                resultSet.getString(
                        "name_en"
                ),

                resultSet.getInt(
                        "duration_months"
                ),

                resultSet.getInt(
                        "max_sessions"
                ),

                resultSet.getDouble(
                        "price"
                ),

                resultSet.getString(
                        "status"
                ),

                resultSet.getString(
                        "description"
                ),

                getLocalDateTime(
                        resultSet,
                        "created_at"
                ),

                getLocalDateTime(
                        resultSet,
                        "updated_at"
                )
        );
    }


    // =========================================================
    // SQL DATETIME -> LOCAL DATETIME
    // =========================================================

    private LocalDateTime getLocalDateTime(
            ResultSet resultSet,
            String column
    ) throws SQLException {

        String value =
                resultSet.getString(column);

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return LocalDateTime.parse(
                value.trim().replace(
                        " ",
                        "T"
                )
        );
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void validatePlan(
            MembershipPlan plan
    ) {

        if (plan == null) {

            throw new IllegalArgumentException(
                    "Membership plan cannot be null."
            );
        }

        if (plan.getCode() == null
                || plan.getCode().isBlank()) {

            throw new IllegalArgumentException(
                    "Membership plan code is required."
            );
        }

        if (plan.getNameAr() == null
                || plan.getNameAr().isBlank()) {

            throw new IllegalArgumentException(
                    "Arabic membership plan name is required."
            );
        }

        if (plan.getNameEn() == null
                || plan.getNameEn().isBlank()) {

            throw new IllegalArgumentException(
                    "English membership plan name is required."
            );
        }

        if (plan.getDurationMonths() <= 0) {

            throw new IllegalArgumentException(
                    "Membership duration must be greater than zero."
            );
        }

        if (plan.getMaxSessions() <= 0) {

            throw new IllegalArgumentException(
                    "Membership sessions must be greater than zero."
            );
        }

        if (plan.getPrice() < 0) {

            throw new IllegalArgumentException(
                    "Membership price cannot be negative."
            );
        }
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
    // STATUS
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
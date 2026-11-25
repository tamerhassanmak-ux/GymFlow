package com.mycompany.lazar_app.database;

import org.sqlite.SQLiteConnection;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Consistent online SQLite snapshots and validated, SQLite-transactional recovery. */
public final class SqliteBackupService {
    static final Map<String, List<String>> REQUIRED = Map.of(
            "members", List.of("id", "member_code", "name", "phone", "whatsapp", "email", "gender", "birth_date", "membership", "start_date", "end_date", "status", "notes", "photo_path", "created_at", "updated_at"),
            "membership_plans", List.of("id", "code", "name_ar", "name_en", "duration_months", "price", "status", "description", "max_sessions", "created_at", "updated_at"),
            "member_memberships", List.of("id", "member_id", "membership_plan_id", "start_date", "end_date", "price", "discount", "final_price", "status", "notes", "allowed_sessions", "created_at", "updated_at"),
            "payments", List.of("id", "member_id", "membership_id", "amount", "payment_method", "status", "payment_date", "reference_number", "notes", "created_at", "updated_at"),
            "check_ins", List.of("id", "member_id", "membership_id", "check_in_time", "check_out_time", "status", "notes", "created_at", "updated_at"),
            "expenses", List.of("id", "expense_code", "title", "category", "amount", "expense_date", "payment_method", "status", "reference_number", "beneficiary", "notes", "created_at", "updated_at"),
            "revenues", List.of("id", "revenue_code", "name", "revenue_type", "amount", "revenue_date", "attachment_path", "notes", "created_at", "updated_at"),
            "revenue_types", List.of("id", "name", "created_at"),
            "schema_migrations", List.of("version", "description", "applied_at"));

    public void backup(DatabaseConfig config, Path destination) throws SQLException, IOException {
        com.mycompany.lazar_app.security.AuthorizationService.requireAdmin();
        requireSQLite(config);
        Path target = destination.toAbsolutePath().normalize();
        Path live = Path.of(config.getSqlitePath()).toAbsolutePath().normalize();
        if (target.equals(live) || (Files.exists(target) && Files.isSameFile(live, target))) {
            throw new IOException("Backup destination must differ from the active database.");
        }
        if (!Files.isRegularFile(live)) throw new IOException("Active database does not exist.");
        Files.createDirectories(target.getParent());
        Path stage = Files.createTempFile(target.getParent(), ".gymflow-backup-", ".db");
        try {
            try (Connection connection = DatabaseManager.getConnection(config)) {
                snapshot(connection, stage);
            }
            validate(stage);
            // Never fall back to a partial replacement if atomic rename is unavailable.
            Files.move(stage, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(stage);
        }
    }

    /** Returns a verified pre-restore recovery snapshot. No raw file overwrites of the live DB. */
    public Path restore(DatabaseConfig config, Path source) throws SQLException, IOException {
        com.mycompany.lazar_app.security.AuthorizationService.requireAdmin();
        requireSQLite(config);
        Path input = source.toAbsolutePath().normalize();
        Path live = Path.of(config.getSqlitePath()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(input) || input.equals(live) || Files.isSameFile(input, live)) {
            throw new IOException("Select a different, existing GymFlow backup.");
        }
        Path stage = Files.createTempFile(live.getParent(), ".gymflow-restore-", ".db");
        try {
            // Snapshot through a read-only connection to include committed source WAL and
            // prevent a later source-file edit from bypassing validation.
            try (Connection connection = readOnly(input)) { snapshot(connection, stage); }
            validate(stage);
            DatabaseConfig stagedConfig = new DatabaseConfig();
            stagedConfig.setSqlitePath(stage.toString());
            try { com.mycompany.lazar_app.database.migration.MigrationRunner.migrate(stagedConfig); }
            catch (Exception exception) { throw new SQLException("Backup upgrade failed before changing the active database.", exception); }
            validate(stage);
            Path safety = live.resolveSibling("pre-restore-" + UUID.randomUUID() + ".db");
            DatabaseManager.withExclusiveConnection(config, connection -> {
                long originalVersion = dataVersion(connection);
                snapshot(connection, safety);
                validate(safety);
                restoreRows(connection, stage, originalVersion);
            });
            return safety;
        } finally {
            Files.deleteIfExists(stage);
        }
    }

    private static final List<String> RESTORE_ORDER = List.of(
            "members", "membership_plans", "member_memberships", "check_ins", "payments",
            "expenses", "revenue_types", "revenues", "payment_installments", "schema_migrations");

    private static long dataVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("PRAGMA data_version")) {
            if (!rows.next()) throw new SQLException("Could not verify database version before recovery.");
            return rows.getLong(1);
        }
    }

    /** Copy validated application rows under a real exclusive SQLite transaction.
     * Native restore can report success without restoring when another process holds a write lock.
     * No constraints are disabled and no live database files are overwritten.
     */
    private static void restoreRows(Connection connection, Path stage, long originalVersion) throws SQLException {
        try (PreparedStatement attach = connection.prepareStatement("ATTACH DATABASE ? AS recovery")) {
            attach.setString(1, stage.toString()); attach.execute();
        }
        connection.unwrap(SQLiteConnection.class).getConnectionConfig()
                .setTransactionMode(org.sqlite.SQLiteConfig.TransactionMode.EXCLUSIVE);
        connection.setAutoCommit(false); // BEGIN EXCLUSIVE: refuses external writers.
        try {
                if (dataVersion(connection) != originalVersion) throw new SQLException(
                        "Another process changed the database during recovery preparation; retry after stopping it.");
                try (Statement statement = connection.createStatement()) {
                    try (ResultSet rows = statement.executeQuery("SELECT name FROM main.sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
                        while (rows.next()) if (!RESTORE_ORDER.contains(rows.getString(1)))
                            throw new SQLException("Active database contains unsupported application tables.");
                    }
                    for (int i = RESTORE_ORDER.size() - 1; i >= 0; i--) {
                        statement.executeUpdate("DELETE FROM main." + RESTORE_ORDER.get(i));
                    }
                    for (String table : RESTORE_ORDER) {
                        List<String> columns = new ArrayList<>();
                        try (ResultSet rows = statement.executeQuery("PRAGMA main.table_info(" + table + ")")) {
                            while (rows.next()) columns.add(rows.getString("name"));
                        }
                        Set<String> sourceColumns = new HashSet<>();
                        try (ResultSet rows = statement.executeQuery("PRAGMA recovery.table_info(" + table + ")")) {
                            while (rows.next()) sourceColumns.add(rows.getString("name"));
                        }
                        if (columns.isEmpty() || !sourceColumns.equals(new HashSet<>(columns)))
                            throw new SQLException("Backup table schema differs from active schema: " + table);
                        String names = columns.stream().map(name -> "\"" + name.replace("\"", "\"\"") + "\"")
                                .collect(java.util.stream.Collectors.joining(","));
                        statement.executeUpdate("INSERT INTO main." + table + "(" + names + ") SELECT " + names + " FROM recovery." + table);
                    }
                }
                connection.commit();
        } catch (SQLException | RuntimeException exception) {
            try { connection.rollback(); } catch (SQLException rollback) { exception.addSuppressed(rollback); }
            throw exception;
        }
        // The dedicated connection closes with the staging attachment still read-only in use.
    }

    private static void requireSQLite(DatabaseConfig config) {
        if (config == null || config.getType() != DatabaseType.SQLITE) {
            throw new IllegalArgumentException("SQLite configuration is required.");
        }
    }
    private static Connection readOnly(Path path) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + path.toUri() + "?mode=ro");
    }
    private static void snapshot(Connection connection, Path target) throws SQLException {
        int result = connection.unwrap(SQLiteConnection.class).getDatabase()
                .backup("main", target.toString(), null);
        if (result != 0) throw new SQLException("SQLite snapshot failed with code " + result);
    }
    private static String normalizedSql(String sql) {
        return sql.strip().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }
    public static void validate(Path path) throws SQLException {
        try (Connection connection = readOnly(path); Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery("PRAGMA integrity_check")) {
                if (!rows.next() || !"ok".equals(rows.getString(1)) || rows.next()) {
                    throw new SQLException("Backup failed SQLite integrity verification.");
                }
            }
            try (ResultSet rows = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
                while (rows.next()) if (!RESTORE_ORDER.contains(rows.getString(1)))
                    throw new SQLException("Backup contains unsupported application tables.");
            }
            for (var table : REQUIRED.entrySet()) {
                Set<String> columns = new HashSet<>();
                try (ResultSet rows = statement.executeQuery("PRAGMA table_info(" + table.getKey() + ")")) {
                    while (rows.next()) columns.add(rows.getString("name"));
                }
                if (!columns.containsAll(table.getValue())) {
                    throw new SQLException("Backup is missing required GymFlow schema: " + table.getKey());
                }
            }
            Map<String, String> allowedTriggers = new HashMap<>(
                    com.mycompany.lazar_app.database.migration.M011ProtectFinancialOwnership.sqliteTriggers());
            allowedTriggers.putAll(com.mycompany.lazar_app.database.migration.M012CreatePaymentInstallments.sqliteTriggers());
            Set<String> actualTriggers = new HashSet<>();
            try (ResultSet rows = statement.executeQuery("SELECT name, sql, type FROM sqlite_master WHERE type IN ('trigger','view')")) {
                while (rows.next()) {
                    actualTriggers.add(rows.getString("name"));
                    String expected = allowedTriggers.get(rows.getString("name"));
                    if (!"trigger".equals(rows.getString("type")) || expected == null
                            || !normalizedSql(expected).equals(normalizedSql(rows.getString("sql")))) {
                        throw new SQLException("Backup contains unrecognized executable schema objects.");
                    }
                }
            }
            Set<String> versions = new HashSet<>();
            try (ResultSet rows = statement.executeQuery("SELECT version FROM schema_migrations")) {
                while (rows.next()) versions.add(rows.getString(1));
            }
            for (int i = 1; i <= 9; i++) {
                if (!versions.contains(String.format(java.util.Locale.ROOT, "%03d", i))) {
                    throw new SQLException("Backup does not contain the required migration history.");
                }
            }
            if (versions.contains("011") && !actualTriggers.containsAll(
                    com.mycompany.lazar_app.database.migration.M011ProtectFinancialOwnership.sqliteTriggers().keySet())) {
                throw new SQLException("Backup is missing financial ownership constraints.");
            }
            if (versions.contains("012")) {
                if (!actualTriggers.containsAll(com.mycompany.lazar_app.database.migration.M012CreatePaymentInstallments.sqliteTriggers().keySet())) {
                    throw new SQLException("Backup is missing installment constraints.");
                }
                try (ResultSet rows = statement.executeQuery("SELECT id,membership_id,installment_no,due_date,amount,status,created_at,updated_at FROM payment_installments WHERE 1=0")) {
                    if (rows.next()) throw new SQLException("Unexpected installment validation result.");
                }
                try (ResultSet rows = statement.executeQuery("SELECT 1 FROM payment_installments i LEFT JOIN member_memberships m ON i.membership_id=m.id WHERE m.id IS NULL OR i.amount<0")) {
                    if (rows.next()) throw new SQLException("Backup contains invalid installments.");
                }
            }
            if (versions.contains("010")) {
                boolean uniquePhone = false;
                try (ResultSet rows = statement.executeQuery("PRAGMA index_list(members)")) {
                    while (rows.next()) if ("idx_members_phone_nonblank_unique".equals(rows.getString("name")) && rows.getInt("unique")==1) uniquePhone=true;
                }
                if (!uniquePhone) throw new SQLException("Backup is missing phone uniqueness constraint.");
            }
            if (versions.contains("013")) {
                try (ResultSet rows = statement.executeQuery("SELECT submission_id FROM payments WHERE 1=0")) {
                    if (rows.next()) throw new SQLException("Unexpected submission schema result.");
                }
                for (String table : List.of("payments", "check_ins")) {
                    String expected = table.equals("payments") ? "idx_payments_submission" : "idx_check_ins_one_open";
                    boolean found = false;
                    try (ResultSet rows = statement.executeQuery("PRAGMA index_list(" + table + ")")) {
                        while (rows.next()) if (expected.equals(rows.getString("name")) && rows.getInt("unique") == 1) found = true;
                    }
                    if (!found) throw new SQLException("Backup is missing duplicate-submission constraints.");
                }
            }
            for (String table : List.of("member_memberships", "payments", "check_ins")) {
                Set<String> references = new HashSet<>();
                try (ResultSet rows = statement.executeQuery("PRAGMA foreign_key_list(" + table + ")")) {
                    while (rows.next()) references.add(rows.getString("from") + ":" + rows.getString("table") + ":" + rows.getString("to"));
                }
                Set<String> expected = table.equals("member_memberships")
                        ? Set.of("member_id:members:id", "membership_plan_id:membership_plans:id")
                        : Set.of("member_id:members:id", "membership_id:member_memberships:id");
                if (!references.containsAll(expected)) throw new SQLException("Backup is missing referential constraints: " + table);
            }
            try (ResultSet rows = statement.executeQuery("PRAGMA foreign_key_check")) {
                if (rows.next()) throw new SQLException("Backup contains broken foreign keys.");
            }
            try (ResultSet rows = statement.executeQuery("SELECT version FROM schema_migrations")) {
                while (rows.next()) {
                    String version = rows.getString(1);
                    if (!version.matches("00[1-9]|01[0-4]")) {
                        throw new SQLException("Backup has an unsupported migration version.");
                    }
                }
            }
            try (ResultSet rows = statement.executeQuery("""
                    SELECT 1 FROM payments p JOIN member_memberships m ON m.id=p.membership_id
                    WHERE p.member_id<>m.member_id
                    UNION ALL SELECT 1 FROM payments WHERE amount<=0
                    UNION ALL SELECT 1 FROM expenses WHERE amount<0
                    UNION ALL SELECT 1 FROM revenues WHERE amount<0
                    """)) {
                if (rows.next()) throw new SQLException("Backup contains invalid financial relationships or amounts.");
            }
        }
    }
}

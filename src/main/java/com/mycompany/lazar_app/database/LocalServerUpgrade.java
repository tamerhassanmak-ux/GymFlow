package com.mycompany.lazar_app.database;

import com.mycompany.lazar_app.util.AppPaths;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Validates a native local MySQL/MariaDB backup and rehearses upgrades before production DDL. */
public final class LocalServerUpgrade {
    @FunctionalInterface public interface Rehearsal { void run(Connection staged) throws Exception; }
    private LocalServerUpgrade() {}

    public static void prepare(Connection source, DatabaseConfig config, Rehearsal rehearsal) throws Exception {
        // Never weaken TLS or silently replace the selected database to get past a startup error.
        if (config.isEncryptConnection() || !DatabaseConnectionManager.isLiteralLoopback(config.getHost()))
            throw new SQLException("Automatic native upgrade currently requires a local, explicitly unencrypted profile.", "GF003");
        Path dumpTool = executable(config.getType() == DatabaseType.MARIADB ? "mariadb-dump" : "mysqldump",
                "mariadb-dump", "mysqldump");
        Path client = executable("mariadb", "mysql");
        ensureSupportedObjects(source);
        Path directory = AppPaths.backupsDirectory();
        Files.createDirectories(directory);
        Path dump = privateFile(directory, "pre-migration-", ".sql");
        Path errors = privateFile(directory, ".upgrade-", ".err");
        String stage = "gymflow_verify_" + UUID.randomUUID().toString().replace("-", "");
        boolean created = false;
        try {
            List<String> dumpArgs = arguments(dumpTool, config);
            dumpArgs.addAll(List.of("--lock-all-tables", "--skip-add-locks", "--hex-blob", "--default-character-set=utf8mb4"));
            // Oracle's client defaults to dumping tablespaces/GTIDs, requiring unrelated privileges.
            if (!isMariaTool(dumpTool)) dumpArgs.addAll(List.of("--no-tablespaces", "--set-gtid-purged=OFF"));
            dumpArgs.add("--"); dumpArgs.add(config.getDatabaseName());
            run(dumpArgs, config, null, dump, errors);
            if (Files.size(dump) == 0) throw new IOException("Native backup is empty.");
            try (Statement statement = source.createStatement()) {
                statement.execute("CREATE DATABASE `" + stage + "`"); created = true;
            }
            List<String> restoreArgs = arguments(client, config);
            restoreArgs.addAll(List.of("--binary-mode", "--default-character-set=utf8mb4", "--database=" + stage));
            run(restoreArgs, config, dump, null, errors);
            // Open the restored database through the same JDBC driver and credentials.
            String url = DatabaseConnectionManager.jdbcUrl(config);
            url = url.substring(0, url.lastIndexOf('/') + 1) + stage;
            try (Connection restored = DriverManager.getConnection(url, DatabaseConnectionManager.properties(config))) {
                if (!fingerprints(source).equals(fingerprints(restored)))
                    throw new SQLException("Restored backup does not match the source; original schema was not changed.", "GF004");
                rehearsal.run(restored);
                DatabaseMigrationManager.verifySchema(restored);
            }
            // Keep the verified dump for manual disaster recovery, never restore over live data automatically.
            Files.writeString(dump.resolveSibling(dump.getFileName() + ".verified"),
                    "Native restore, row fingerprints and migration rehearsal passed.\n", StandardOpenOption.CREATE_NEW);
        } catch (Exception failure) {
            throw new SQLException("Automatic upgrade preparation failed; original schema has not been upgraded.", "GF004", failure);
        } finally {
            try {
                if (created) try (Statement statement = source.createStatement()) { statement.execute("DROP DATABASE `" + stage + "`"); }
            } finally { Files.deleteIfExists(errors); }
        }
    }

    private static void ensureSupportedObjects(Connection source) throws SQLException {
        // Native routines/events may have side effects or database-qualified references on a restored clone.
        for (String sql : List.of(
                "SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE()",
                "SELECT 1 FROM information_schema.EVENTS WHERE EVENT_SCHEMA=DATABASE()",
                "SELECT 1 FROM information_schema.VIEWS WHERE TABLE_SCHEMA=DATABASE()",
                "SELECT 1 FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE()")) {
            try (Statement statement = source.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
                if (rows.next()) throw new SQLException("Custom server objects require a reviewed upgrade.", "GF003");
            }
        }
    }

    /** Multiset digest handles tables without primary keys and preserves duplicate-row counts. */
    static Map<String, String> fingerprints(Connection connection) throws Exception {
        List<String> tables = new ArrayList<>();
        try (ResultSet rows = connection.getMetaData().getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rows.next()) tables.add(rows.getString("TABLE_NAME"));
        }
        Map<String, String> result = new TreeMap<>();
        for (String table : tables) {
            byte[] sum = new byte[32]; long count = 0;
            try (Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                statement.setFetchSize(connection.getMetaData().getDriverName().toLowerCase(Locale.ROOT).contains("mariadb") ? 1000 : Integer.MIN_VALUE);
                try (ResultSet rows = statement.executeQuery("SELECT * FROM `" + table.replace("`", "``") + "`")) {
                    int columns = rows.getMetaData().getColumnCount();
                    while (rows.next()) {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        for (int i = 1; i <= columns; i++) {
                            byte[] value = rows.getBytes(i);
                            digest.update(java.nio.ByteBuffer.allocate(4).putInt(value == null ? -1 : value.length).array());
                            if (value != null) digest.update(value);
                        }
                        byte[] hash = digest.digest(); int carry = 0;
                        for (int i = 31; i >= 0; i--) {
                            int n = (sum[i] & 255) + (hash[i] & 255) + carry;
                            sum[i] = (byte)n; carry = n >>> 8;
                        }
                        count++;
                    }
                }
            }
            result.put(table, count + ":" + HexFormat.of().formatHex(sum));
        }
        return result;
    }

    private static List<String> arguments(Path tool, DatabaseConfig config) throws Exception {
        List<String> args = new ArrayList<>(List.of(tool.toString(), "--no-defaults", "--protocol=TCP",
                "--host=" + config.getHost().replace("[", "").replace("]", ""),
                "--port=" + config.getPort(), "--user=" + config.getUsername()));
        args.add(isMariaTool(tool) ? "--skip-ssl" : "--ssl-mode=DISABLED");
        return args;
    }
    private static boolean isMariaTool(Path tool) throws Exception {
        Process process = new ProcessBuilder(tool.toString(), "--no-defaults", "--version").redirectErrorStream(true).start();
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) throw new IOException("Native client version check timed out.");
            String version = new String(process.getInputStream().readNBytes(8192), java.nio.charset.StandardCharsets.UTF_8);
            if (process.exitValue() != 0) throw new IOException("Native client version check failed.");
            return version.toLowerCase(Locale.ROOT).contains("mariadb");
        } finally { if (process.isAlive()) process.destroyForcibly(); }
    }
    private static void run(List<String> args, DatabaseConfig config, Path input, Path output, Path errors) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.environment().remove("MYSQL_PWD");
        builder.environment().put("MYSQL_PWD", config.getPassword());
        builder.redirectError(errors.toFile());
        builder.redirectOutput(output == null ? ProcessBuilder.Redirect.DISCARD : ProcessBuilder.Redirect.to(output.toFile()));
        if (input != null) builder.redirectInput(input.toFile());
        Process process = builder.start();
        try {
            if (input == null) process.getOutputStream().close();
            if (!process.waitFor(120, TimeUnit.SECONDS)) throw new SQLTimeoutException("Native backup/restore timed out.", "HYT00");
            // Do not expose native stderr, which may contain identities or data.
            if (process.exitValue() != 0) throw new IOException("Native backup/restore command failed.");
        } finally { if (process.isAlive()) { process.destroyForcibly(); process.waitFor(); } }
    }
    private static Path privateFile(Path directory, String prefix, String suffix) throws IOException {
        try { return Files.createTempFile(directory, prefix, suffix,
                java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"))); }
        catch (UnsupportedOperationException windows) { return Files.createTempFile(directory, prefix, suffix); }
    }
    private static Path executable(String... names) throws SQLException {
        List<String> directories = new ArrayList<>(Arrays.asList(System.getenv().getOrDefault("PATH", "").split(java.io.File.pathSeparator)));
        directories.add("/opt/lampp/bin");
        for (String name : names) for (String directory : directories) {
            for (String suffix : List.of("", ".exe")) {
                Path candidate = Path.of(directory, name + suffix);
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) return candidate.toAbsolutePath();
            }
        }
        throw new SQLException("Install native MySQL/MariaDB backup and restore clients on PATH.", "GF005");
    }
}

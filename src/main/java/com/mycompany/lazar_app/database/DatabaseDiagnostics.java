package com.mycompany.lazar_app.database;

import com.mycompany.lazar_app.util.AppPaths;
import java.io.IOException;
import java.nio.file.*;
import java.util.logging.*;

/** Allowlisted structured events. Never persist arbitrary exception messages or SQL. */
public final class DatabaseDiagnostics {
    private static final Logger LOG = Logger.getLogger("gymflow.startup");
    private static boolean initialized;
    private DatabaseDiagnostics() {}
    public static synchronized void initialize() throws IOException {
        if (initialized) return;
        Path directory = AppPaths.dataDirectory().resolve("logs");
        Files.createDirectories(directory);
        FileHandler handler = new FileHandler(directory.resolve("startup-%g.log").toString(), 1024 * 1024, 5, true);
        handler.setEncoding("UTF-8");
        handler.setFormatter(new java.util.logging.Formatter() {
            public String format(LogRecord r) { return java.time.Instant.ofEpochMilli(r.getMillis()) + " " + r.getMessage() + System.lineSeparator(); }
        });
        LOG.setUseParentHandlers(false);
        LOG.addHandler(handler);
        initialized = true;
        LOG.info("event=runtime app=1.0.7 os=" + safe(System.getProperty("os.name"))
                + " java=" + safe(System.getProperty("java.version")));
    }
    private static String safe(String text) { return text == null ? "unknown" : text.replaceAll("[^a-zA-Z0-9 ._/-]", "_"); }
    public static void connecting(DatabaseType type) {
        LOG.info("event=connecting engine=" + type.name());
    }
    public static void startupFailed(long millis) {
        LOG.warning("event=readiness_failed duration_ms=" + millis);
    }
    public static void ready(DatabaseHealthCheck.Result result, long millis) {
        LOG.info("event=ready engine=" + safe(result.engine()) + " server=" + safe(result.serverVersion())
                + " driver=" + safe(result.driverVersion()) + " duration_ms=" + millis);
    }
    public static void migration(String version, String status) {
        if (!version.matches("[0-9]{3}") || !java.util.Set.of("run","skip","ok","failed").contains(status))
            throw new IllegalArgumentException("Invalid migration event.");
        LOG.info("event=migration version=" + version + " status=" + status);
    }
    public static void failure(Throwable error) {
        LOG.warning("event=startup_failure " + DatabaseExceptionHandler.diagnostic(error));
        // Retain stack locations only; Throwable.toString() can expose credentials/member data.
        for (Throwable t = error; t != null; t = t.getCause()) {
            LOG.warning("exception=" + t.getClass().getName());
            for (StackTraceElement frame : t.getStackTrace()) LOG.warning("at=" + frame);
        }
    }
    public static void export(Path destination) throws IOException {
        // Fresh allowlisted support summary, excluding configuration and arbitrary application logs.
        Files.writeString(destination, "GymFlow 1.0.7\nJava=" + safe(System.getProperty("java.version"))
                + "\nOS=" + safe(System.getProperty("os.name")) + "\n");
    }
}

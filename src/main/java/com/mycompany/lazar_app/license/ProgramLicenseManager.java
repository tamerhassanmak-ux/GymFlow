package com.mycompany.lazar_app.license;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Properties;
import java.util.Set;

/**
 * Persists and validates the local GymFlow program license.
 *
 * The license is deliberately kept separate from ordinary application settings,
 * so saving normal Settings cannot accidentally alter the software license.
 *
 * A signature detects casual/manual editing of the license file. This is local
 * desktop protection, not a replacement for server-side licensing if stronger
 * commercial DRM is required later.
 */
public final class ProgramLicenseManager {

    private static final Path SECURITY_DIR =
            Paths.get(System.getProperty("user.home"), ".lazar-gym");

    private static final Path LICENSE_FILE =
            SECURITY_DIR.resolve("program-license.properties");

    private static final String SIGNING_PEPPER =
            "GymFlow::ProgramLicense::2026::MAKSOFT";

    private ProgramLicenseManager() {
    }

    public static synchronized ProgramLicense load() {

        ProgramLicense license = new ProgramLicense();

        if (!Files.exists(LICENSE_FILE)) {
            license.setConfigured(false);
            return license;
        }

        Properties properties = new Properties();

        try (InputStream in = Files.newInputStream(LICENSE_FILE)) {
            properties.load(in);
        } catch (Exception exception) {
            license.setConfigured(true);
            license.setIntegrityValid(false);
            return license;
        }

        String storedSignature = properties.getProperty("signature", "").trim();
        String expectedSignature = calculateSignature(properties);

        license.setConfigured(
                Boolean.parseBoolean(
                        properties.getProperty("configured", "false")
                )
        );

        if (storedSignature.isBlank()
                || !MessageDigest.isEqual(
                        storedSignature.getBytes(StandardCharsets.UTF_8),
                        expectedSignature.getBytes(StandardCharsets.UTF_8))) {

            license.setIntegrityValid(false);
            return license;
        }

        try {
            license.setMode(
                    ProgramLicense.Mode.valueOf(
                            properties.getProperty("mode", "PERIOD")
                                    .trim()
                                    .toUpperCase()
                    )
            );

            license.setStartDate(
                    parseDate(properties.getProperty("startDate", ""))
            );

            license.setEndDate(
                    parseDate(properties.getProperty("endDate", ""))
            );

            license.setLastSeenDate(
                    parseDate(properties.getProperty("lastSeenDate", ""))
            );

            String updatedAt =
                    properties.getProperty("updatedAt", "").trim();

            if (!updatedAt.isBlank()) {
                license.setUpdatedAt(LocalDateTime.parse(updatedAt));
            }

            license.setIntegrityValid(true);
            return license;

        } catch (Exception exception) {
            license.setIntegrityValid(false);
            return license;
        }
    }

    public static synchronized ProgramLicense activatePeriod(
            LocalDate startDate,
            LocalDate endDate
    ) {

        validatePeriod(startDate, endDate);

        ProgramLicense license = new ProgramLicense();
        license.setConfigured(true);
        license.setIntegrityValid(true);
        license.setMode(ProgramLicense.Mode.PERIOD);
        license.setStartDate(startDate);
        license.setEndDate(endDate);
        license.setLastSeenDate(LocalDate.now());
        license.setUpdatedAt(LocalDateTime.now());

        saveInternal(license);

        return license;
    }

    public static synchronized ProgramLicense activateLifetime() {

        ProgramLicense license = new ProgramLicense();
        license.setConfigured(true);
        license.setIntegrityValid(true);
        license.setMode(ProgramLicense.Mode.LIFETIME);
        license.setStartDate(null);
        license.setEndDate(null);
        license.setLastSeenDate(LocalDate.now());
        license.setUpdatedAt(LocalDateTime.now());

        saveInternal(license);

        return license;
    }

    public static synchronized ProgramLicenseStatus evaluate() {

        ProgramLicense license = load();

        if (!license.isConfigured()) {
            return ProgramLicenseStatus.NOT_CONFIGURED;
        }

        if (!license.isIntegrityValid()) {
            return ProgramLicenseStatus.INVALID;
        }

        if (license.isLifetime()) {
            return ProgramLicenseStatus.LIFETIME;
        }

        LocalDate startDate = license.getStartDate();
        LocalDate endDate = license.getEndDate();

        if (startDate == null
                || endDate == null
                || endDate.isBefore(startDate)) {

            return ProgramLicenseStatus.INVALID;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastSeenDate = license.getLastSeenDate();

        /*
         * If the machine date goes backwards after the application has already
         * been used on a later date, require Super Admin re-activation.
         */
        if (lastSeenDate != null && today.isBefore(lastSeenDate)) {
            return ProgramLicenseStatus.CLOCK_ROLLBACK;
        }

        if (today.isBefore(startDate)) {
            return ProgramLicenseStatus.NOT_STARTED;
        }

        if (today.isAfter(endDate)) {
            return ProgramLicenseStatus.EXPIRED;
        }

        return ProgramLicenseStatus.ACTIVE;
    }

    public static synchronized boolean isProgramAllowed() {
        return evaluate().isAllowed();
    }

    public static synchronized void recordSuccessfulOpen() {

        ProgramLicenseStatus status = evaluate();

        if (!status.isAllowed()) {
            return;
        }

        ProgramLicense license = load();
        LocalDate today = LocalDate.now();

        if (license.getLastSeenDate() == null
                || today.isAfter(license.getLastSeenDate())) {

            license.setLastSeenDate(today);
            license.setUpdatedAt(LocalDateTime.now());
            saveInternal(license);
        }
    }

    public static long daysRemaining() {

        ProgramLicense license = load();
        ProgramLicenseStatus status = evaluate();

        if (status == ProgramLicenseStatus.LIFETIME) {
            return Long.MAX_VALUE;
        }

        if (license.getEndDate() == null) {
            return 0;
        }

        return Math.max(
                0,
                ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        license.getEndDate()
                )
        );
    }

    public static Path getLicenseFile() {
        return LICENSE_FILE;
    }

    private static void validatePeriod(
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Program license start and end dates are required."
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "Program license end date cannot be before the start date."
            );
        }
    }

    private static void saveInternal(
            ProgramLicense license
    ) {

        try {
            Files.createDirectories(SECURITY_DIR);

            Properties properties = new Properties();

            properties.setProperty(
                    "configured",
                    String.valueOf(license.isConfigured())
            );

            properties.setProperty(
                    "mode",
                    license.getMode().name()
            );

            properties.setProperty(
                    "startDate",
                    formatDate(license.getStartDate())
            );

            properties.setProperty(
                    "endDate",
                    formatDate(license.getEndDate())
            );

            properties.setProperty(
                    "lastSeenDate",
                    formatDate(license.getLastSeenDate())
            );

            properties.setProperty(
                    "updatedAt",
                    license.getUpdatedAt() == null
                            ? LocalDateTime.now().toString()
                            : license.getUpdatedAt().toString()
            );

            properties.setProperty(
                    "signature",
                    calculateSignature(properties)
            );

            Path temporary =
                    LICENSE_FILE.resolveSibling(
                            LICENSE_FILE.getFileName() + ".tmp"
                    );

            try (OutputStream out = Files.newOutputStream(temporary)) {
                properties.store(out, "GymFlow Program License");
            }

            try {
                Files.move(
                        temporary,
                        LICENSE_FILE,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(
                        temporary,
                        LICENSE_FILE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            protectFilePermissions();

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not save GymFlow program license.",
                    exception
            );
        }
    }

    private static String calculateSignature(
            Properties properties
    ) {

        String payload =
                properties.getProperty("configured", "false") + "|"
                + properties.getProperty("mode", "PERIOD") + "|"
                + properties.getProperty("startDate", "") + "|"
                + properties.getProperty("endDate", "") + "|"
                + properties.getProperty("lastSeenDate", "") + "|"
                + properties.getProperty("updatedAt", "") + "|"
                + SIGNING_PEPPER;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            payload.getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat.of().formatHex(hash);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not calculate program license signature.",
                    exception
            );
        }
    }

    private static LocalDate parseDate(
            String value
    ) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Invalid program license date: " + value,
                    exception
            );
        }
    }

    private static String formatDate(
            LocalDate date
    ) {
        return date == null ? "" : date.toString();
    }

    private static void protectFilePermissions() {

        try {
            Files.setPosixFilePermissions(
                    LICENSE_FILE,
                    Set.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE
                    )
            );
        } catch (UnsupportedOperationException ignored) {
            // Windows / non-POSIX file system.
        } catch (Exception ignored) {
            // Permission hardening should not prevent the license from saving.
        }
    }
}

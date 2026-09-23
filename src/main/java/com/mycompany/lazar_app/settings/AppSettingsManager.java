package com.mycompany.lazar_app.settings;

import com.mycompany.lazar_app.util.AppPaths;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class AppSettingsManager {

    private static final Path CONFIG_FILE =
            AppPaths.applicationSettingsFile();

    private static AppSettings cached;

    private AppSettingsManager() {}

    public static synchronized AppSettings load() {
        if (cached != null) {
            return cached;
        }

        migrateLegacySettingsIfNeeded();

        AppSettings s = new AppSettings();

        if (!Files.exists(CONFIG_FILE)) {
            cached = s;
            return s;
        }

        Properties p = new Properties();

        try (InputStream in =
                     Files.newInputStream(CONFIG_FILE)) {

            p.load(in);

            s.setGymName(
                    p.getProperty(
                            "gym.name",
                            "GymFlow"
                    )
            );

            s.setPhone(
                    p.getProperty(
                            "gym.phone",
                            ""
                    )
            );

            s.setEmail(
                    p.getProperty(
                            "gym.email",
                            ""
                    )
            );

            s.setAddress(
                    p.getProperty(
                            "gym.address",
                            ""
                    )
            );

            s.setLanguage(
                    p.getProperty(
                            "regional.language",
                            "English"
                    )
            );

            s.setCurrency(
                    p.getProperty(
                            "regional.currency",
                            "EGP"
                    )
            );

            s.setDateFormat(
                    p.getProperty(
                            "regional.dateFormat",
                            "DD/MM/YYYY"
                    )
            );

            s.setTimeZone(
                    p.getProperty(
                            "regional.timeZone",
                            "Africa/Cairo"
                    )
            );

            s.setDefaultMembership(
                    p.getProperty(
                            "membership.default",
                            "Monthly"
                    )
            );

            s.setGracePeriodDays(
                    integer(
                            p,
                            "membership.gracePeriodDays",
                            0
                    )
            );

            s.setAutoExpireMemberships(
                    bool(
                            p,
                            "membership.autoExpire",
                            true
                    )
            );

            s.setRenewBeforeExpiry(
                    bool(
                            p,
                            "membership.renewBeforeExpiry",
                            true
                    )
            );

            s.setExpiryAlerts(
                    bool(
                            p,
                            "notification.expiryAlerts",
                            true
                    )
            );

            s.setPaymentReminders(
                    bool(
                            p,
                            "notification.paymentReminders",
                            true
                    )
            );

            s.setNewMemberNotification(
                    bool(
                            p,
                            "notification.newMember",
                            true
                    )
            );

            s.setExpenseNotifications(
                    bool(
                            p,
                            "notification.expenses",
                            true
                    )
            );

            s.setExpiryWarningDays(
                    integer(
                            p,
                            "notification.expiryWarningDays",
                            3
                    )
            );

            s.setDarkMode(
                    bool(
                            p,
                            "appearance.darkMode",
                            false
                    )
            );

            s.setLastBackup(
                    p.getProperty(
                            "backup.lastBackup",
                            ""
                    )
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not load application settings.",
                    e
            );
        }

        cached = s;

        return s;
    }

    public static synchronized void save(
            AppSettings s
    ) {

        try {
            Path parent =
                    CONFIG_FILE.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Properties p =
                    new Properties();

            p.setProperty(
                    "gym.name",
                    s.getGymName()
            );

            p.setProperty(
                    "gym.phone",
                    s.getPhone()
            );

            p.setProperty(
                    "gym.email",
                    s.getEmail()
            );

            p.setProperty(
                    "gym.address",
                    s.getAddress()
            );

            p.setProperty(
                    "regional.language",
                    s.getLanguage()
            );

            p.setProperty(
                    "regional.currency",
                    s.getCurrency()
            );

            p.setProperty(
                    "regional.dateFormat",
                    s.getDateFormat()
            );

            p.setProperty(
                    "regional.timeZone",
                    s.getTimeZone()
            );

            p.setProperty(
                    "membership.default",
                    s.getDefaultMembership()
            );

            p.setProperty(
                    "membership.gracePeriodDays",
                    String.valueOf(
                            s.getGracePeriodDays()
                    )
            );

            p.setProperty(
                    "membership.autoExpire",
                    String.valueOf(
                            s.isAutoExpireMemberships()
                    )
            );

            p.setProperty(
                    "membership.renewBeforeExpiry",
                    String.valueOf(
                            s.isRenewBeforeExpiry()
                    )
            );

            p.setProperty(
                    "notification.expiryAlerts",
                    String.valueOf(
                            s.isExpiryAlerts()
                    )
            );

            p.setProperty(
                    "notification.paymentReminders",
                    String.valueOf(
                            s.isPaymentReminders()
                    )
            );

            p.setProperty(
                    "notification.newMember",
                    String.valueOf(
                            s.isNewMemberNotification()
                    )
            );

            p.setProperty(
                    "notification.expenses",
                    String.valueOf(
                            s.isExpenseNotifications()
                    )
            );

            p.setProperty(
                    "notification.expiryWarningDays",
                    String.valueOf(
                            s.getExpiryWarningDays()
                    )
            );

            p.setProperty(
                    "appearance.darkMode",
                    String.valueOf(
                            s.isDarkMode()
                    )
            );

            p.setProperty(
                    "backup.lastBackup",
                    s.getLastBackup()
            );

            try (OutputStream out =
                         Files.newOutputStream(CONFIG_FILE)) {

                p.store(
                        out,
                        "GymFlow Application Settings"
                );
            }

            cached = s;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not save application settings.",
                    e
            );
        }
    }

    public static String getCurrencyCode() {
        return load().getCurrency();
    }

    public static String getCurrencyLabel(
            boolean arabic
    ) {

        return switch (load().getCurrency()) {
            case "SAR" -> arabic ? "ر.س" : "SAR";
            case "USD" -> "$";
            default -> arabic ? "ج.م" : "EGP";
        };
    }

    public static Path getSettingsFile() {
        return CONFIG_FILE;
    }

    private static void migrateLegacySettingsIfNeeded() {

        Path legacy =
                AppPaths.legacyApplicationSettingsFile();

        if (Files.exists(CONFIG_FILE)
                || !Files.isRegularFile(legacy)) {

            return;
        }

        try {
            Path parent =
                    CONFIG_FILE.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            /*
             * Copy rather than move so the old file remains as
             * a rollback/safety copy.
             */
            Files.copy(
                    legacy,
                    CONFIG_FILE,
                    StandardCopyOption.COPY_ATTRIBUTES
            );

            if (!Files.exists(CONFIG_FILE)
                    || Files.size(CONFIG_FILE)
                    != Files.size(legacy)) {

                Files.deleteIfExists(CONFIG_FILE);

                throw new IllegalStateException(
                        "Legacy application settings copy verification failed."
                );
            }

            System.out.println(
                    "GymFlow application settings migrated to "
                            + CONFIG_FILE
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not migrate legacy application settings.",
                    e
            );
        }
    }

    private static boolean bool(
            Properties p,
            String key,
            boolean fallback
    ) {

        return Boolean.parseBoolean(
                p.getProperty(
                        key,
                        String.valueOf(fallback)
                )
        );
    }

    private static int integer(
            Properties p,
            String key,
            int fallback
    ) {

        try {
            return Integer.parseInt(
                    p.getProperty(
                            key,
                            String.valueOf(fallback)
                    )
            );

        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

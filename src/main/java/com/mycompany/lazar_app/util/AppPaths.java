package com.mycompany.lazar_app.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public final class AppPaths {

    private static final String APP_NAME = "GymFlow";

    private AppPaths() {}

    public static Path dataDirectory() {
        String os = osName();

        if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (notBlank(localAppData)) {
                return Paths.get(localAppData, APP_NAME);
            }
            return home().resolve("AppData").resolve("Local").resolve(APP_NAME);
        }

        if (os.contains("mac")) {
            return home().resolve("Library")
                    .resolve("Application Support")
                    .resolve(APP_NAME);
        }

        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        if (notBlank(xdgDataHome)) {
            return Paths.get(xdgDataHome, APP_NAME);
        }

        return home().resolve(".local").resolve("share").resolve(APP_NAME);
    }

    public static Path configDirectory() {
        String os = osName();

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (notBlank(appData)) {
                return Paths.get(appData, APP_NAME);
            }
            return home().resolve("AppData").resolve("Roaming").resolve(APP_NAME);
        }

        if (os.contains("mac")) {
            return home().resolve("Library")
                    .resolve("Application Support")
                    .resolve(APP_NAME);
        }

        String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
        if (notBlank(xdgConfigHome)) {
            return Paths.get(xdgConfigHome, APP_NAME);
        }

        return home().resolve(".config").resolve(APP_NAME);
    }

    public static Path databaseFile() {
        return dataDirectory().resolve("data").resolve("gymflow.db");
    }

    public static Path backupsDirectory() {
        return dataDirectory().resolve("backups");
    }

    public static Path exportsDirectory() {
        return dataDirectory().resolve("exports");
    }

    public static Path databaseConfigFile() {
        return configDirectory().resolve("database.properties");
    }

    public static Path applicationSettingsFile() {
        return configDirectory().resolve("application.properties");
    }

    public static Path legacyConfigDirectory() {
        return home().resolve(".lazar-gym");
    }

    public static Path legacyDatabaseConfigFile() {
        return legacyConfigDirectory().resolve("database.properties");
    }

    public static Path legacyApplicationSettingsFile() {
        return legacyConfigDirectory().resolve("application.properties");
    }

    public static Path legacyRelativeDatabaseFile() {
        return home().resolve("data").resolve("lazar_gym.db");
    }

    private static Path home() {
        return Paths.get(System.getProperty("user.home"));
    }

    private static String osName() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}

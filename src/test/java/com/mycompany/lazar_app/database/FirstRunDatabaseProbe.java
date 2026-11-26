package com.mycompany.lazar_app.database;

/** Runs in a brand-new isolated user profile, without an administrator account. */
public class FirstRunDatabaseProbe {
    public static void main(String[] args) throws Exception {
        if (java.nio.file.Files.exists(DatabaseConfigManager.getConfigFile())) throw new AssertionError("Profile is not fresh");
        DatabaseStartup.prepare();
        DatabaseConfig config = DatabaseConfigManager.load();
        if (config.getType() != DatabaseType.SQLITE || !config.getDatabaseName().equals("gymflow")
                || !java.nio.file.Path.of(config.getSqlitePath()).getFileName().toString().equals("gymflow.db"))
            throw new AssertionError("Incorrect first-run defaults");
        DatabaseStartup.prepare();
        System.out.println("FIRST_RUN_SQLITE_GYMFLOW_READY");
    }
}

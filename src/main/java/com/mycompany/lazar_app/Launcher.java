package com.mycompany.lazar_app;

/** Classpath entry point allows JavaFX and JDBC service providers to ship together in jpackage. */
public final class Launcher {
    private Launcher() {}
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--self-test")) {
            Class.forName("javax.script.Bindings"); // FXMLLoader links java.scripting even without script expressions.
            for (var type : com.mycompany.lazar_app.database.DatabaseType.values()) {
                var config = new com.mycompany.lazar_app.database.DatabaseConfig(); config.setType(type);
                com.mycompany.lazar_app.database.DatabaseConnectionManager.verifyDriver(config);
            }
            for (String resource : new String[]{"/fonts/NotoNaskhArabic-Regular.ttf", "/i18n/messages_ar.properties", "/i18n/messages_en.properties", "/fxml/database-settings.fxml"}) {
                if (Launcher.class.getResource(resource) == null) throw new IllegalStateException("Missing packaged resource: " + resource);
            }
            java.nio.file.Path file = java.nio.file.Files.createTempFile("gymflow-runtime-", ".db");
            try {
                var config = new com.mycompany.lazar_app.database.DatabaseConfig(); config.setSqlitePath(file.toString());
                com.mycompany.lazar_app.database.DatabaseHealthCheck.check(config);
            } finally { java.nio.file.Files.deleteIfExists(file); }
            System.out.println("GymFlow 1.0.8 runtime self-test passed (drivers, resources, SQLite)");
            return;
        }
        Lazar_APP.main(args);
    }
}

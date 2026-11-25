package com.mycompany.lazar_app;

import com.mycompany.lazar_app.database.*;
import com.mycompany.lazar_app.database.migration.MigrationRunner;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseStabilityTest {
    @TempDir Path directory;
    private DatabaseConfig server(DatabaseType type) {
        DatabaseConfig c = new DatabaseConfig(); c.setType(type); c.setHost("db.example.org");
        c.setDatabaseName("gymflow"); c.setUsername("operator"); c.setPassword("secret-not-for-logs"); return c;
    }
    @Test void remoteTlsIsVerifiedByEveryDriver() {
        assertEquals("VERIFY_IDENTITY", DatabaseConnectionManager.properties(server(DatabaseType.MYSQL)).getProperty("sslMode"));
        assertEquals("verify-full", DatabaseConnectionManager.properties(server(DatabaseType.MARIADB)).getProperty("sslMode"));
        Properties sql = DatabaseConnectionManager.properties(server(DatabaseType.SQL_SERVER));
        assertEquals("true", sql.getProperty("encrypt")); assertEquals("false", sql.getProperty("trustServerCertificate"));
    }
    @Test void plaintextRestrictedToExplicitLoopback() {
        for (DatabaseType type : List.of(DatabaseType.MYSQL,DatabaseType.MARIADB,DatabaseType.SQL_SERVER)) {
            DatabaseConfig c = server(type); c.setEncryptConnection(false);
            assertThrows(IllegalArgumentException.class, () -> DatabaseConnectionManager.validate(c));
            for (String host : List.of("localhost","127.0.0.1","::1","[::1]")) {
                c.setHost(host); assertDoesNotThrow(() -> DatabaseConnectionManager.validate(c));
            }
            for (String host : List.of("127.1","127.0.0.2","localhost.example.com","2130706433")) {
                c.setHost(host); assertThrows(IllegalArgumentException.class, () -> DatabaseConnectionManager.validate(c));
            }
        }
    }
    @Test void maliciousJdbcOptionsAreRejected() {
        DatabaseConfig c = server(DatabaseType.MYSQL);
        for (String host : List.of("db?sslMode=DISABLED","db;encrypt=false","db,evil","db/path","db@evil")) {
            c.setHost(host); assertThrows(IllegalArgumentException.class, () -> DatabaseConnectionManager.jdbcUrl(c));
        }
        c.setHost("localhost"); c.setDatabaseName("gym?sslMode=DISABLED");
        assertThrows(IllegalArgumentException.class, () -> DatabaseConnectionManager.jdbcUrl(c));
    }
    @Test void ipv6AndDedicatedProtocolsAndCredentials() {
        DatabaseConfig c = server(DatabaseType.MARIADB); c.setHost("::1");
        assertEquals("jdbc:mariadb://[::1]:3306/gymflow", DatabaseConnectionManager.jdbcUrl(c));
        assertFalse(DatabaseConnectionManager.jdbcUrl(c).contains(c.getPassword()));
        assertEquals("false",DatabaseConnectionManager.properties(c).getProperty("allowPublicKeyRetrieval"));
        c.setType(DatabaseType.MYSQL); assertTrue(DatabaseConnectionManager.jdbcUrl(c).startsWith("jdbc:mysql:"));
    }
    @Test void timeoutsAreBoundedAndNotGlobal() {
        DatabaseConfig c = server(DatabaseType.MYSQL);
        assertEquals("5000",DatabaseConnectionManager.properties(c).getProperty("connectTimeout"));
        c.setConnectTimeoutMillis(0); assertThrows(IllegalArgumentException.class, () -> DatabaseConnectionManager.validate(c));
        c.setConnectTimeoutMillis(5000); c.setSocketTimeoutMillis(Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> DatabaseConnectionManager.validate(c));
    }
    @Test void diagnosticsNeverIncludeVendorSecrets() {
        SQLException failure = new SQLException("password=secret-not-for-logs member=private", "28000",1045);
        assertEquals(DatabaseExceptionHandler.Category.AUTHENTICATION,DatabaseExceptionHandler.classify(failure));
        assertEquals("AUTHENTICATION SQLState=28000 code=1045",DatabaseExceptionHandler.diagnostic(failure));
        assertFalse(DatabaseExceptionHandler.message(failure).contains("private"));
        assertEquals(DatabaseExceptionHandler.Category.TLS, DatabaseExceptionHandler.classify(new SQLException("",new javax.net.ssl.SSLHandshakeException("secret"))));
        assertEquals(DatabaseExceptionHandler.Category.TIMEOUT,DatabaseExceptionHandler.classify(new SQLTimeoutException()));
        assertEquals(DatabaseExceptionHandler.Category.NETWORK,DatabaseExceptionHandler.classify(new SQLException("", "08001")));
        assertEquals(DatabaseExceptionHandler.Category.DRIVER,DatabaseExceptionHandler.classify(new SQLException("", "IM003")));
    }
    @Test void allBundledDriversAvailable() {
        for (DatabaseType type : DatabaseType.values()) assertDoesNotThrow(() -> DatabaseConnectionManager.verifyDriver(server(type)));
    }
    @Test void customTruststoreIsPerConnection() throws Exception {
        Path store = directory.resolve("CA with spaces.p12"); Files.write(store,new byte[0]);
        DatabaseConfig c = server(DatabaseType.MYSQL); c.setTrustStore(store.toString());
        c.setTrustStorePassword("trust-secret");
        assertEquals(store.toUri().toString(),DatabaseConnectionManager.properties(c).getProperty("trustCertificateKeyStoreUrl"));
        assertEquals("false",DatabaseConnectionManager.properties(c).getProperty("fallbackToSystemTrustStore"));
        assertNull(System.getProperty("javax.net.ssl.trustStore"));
        c.setTrustStore(directory.resolve("missing").toString());
        assertThrows(IllegalArgumentException.class, () -> DatabaseConnectionManager.validate(c));
    }
    @Test void migrationRejectsFutureLedgerWithoutDataChanges() throws Exception {
        DatabaseConfig c = new DatabaseConfig(); c.setSqlitePath(directory.resolve("future.db").toString());
        try (Connection con = DatabaseManager.getConnection(c); Statement s = con.createStatement()) {
            s.execute("CREATE TABLE schema_migrations(version TEXT)"); s.execute("INSERT INTO schema_migrations VALUES('999')");
            s.execute("CREATE TABLE customer_data(value TEXT)"); s.execute("INSERT INTO customer_data VALUES('preserve')");
        }
        assertThrows(SQLException.class, () -> MigrationRunner.migrate(c));
        try(Connection con = DatabaseManager.getConnection(c);Statement s=con.createStatement();ResultSet rows=s.executeQuery("SELECT value FROM customer_data")) {
            assertTrue(rows.next()); assertEquals("preserve",rows.getString(1));
        }
    }
    @Test void backupRestoresPreMigrationSchema() throws Exception {
        DatabaseConfig c = new DatabaseConfig(); c.setSqlitePath(directory.resolve("old.db").toString());
        Path backupDir = com.mycompany.lazar_app.util.AppPaths.backupsDirectory();
        Set<Path> before = new HashSet<>(); if(Files.exists(backupDir)) try(var paths=Files.list(backupDir)){paths.forEach(before::add);}
        try(Connection con=DatabaseManager.getConnection(c);Statement s=con.createStatement()) {
            s.execute("CREATE TABLE customer_data(value TEXT)"); s.execute("INSERT INTO customer_data VALUES('preserve')");
        }
        MigrationRunner.migrate(c);
        Path snapshot;
        try(var paths=Files.list(backupDir)){snapshot=paths.filter(p -> !before.contains(p)).findFirst().orElseThrow();}
        try(Connection con=DriverManager.getConnection("jdbc:sqlite:"+snapshot);Statement s=con.createStatement();ResultSet rows=s.executeQuery("SELECT value FROM customer_data")) {
            assertTrue(rows.next()); assertEquals("preserve",rows.getString(1));
        }
        MigrationRunner.migrate(c);
        assertTrue(DatabaseHealthCheck.check(c).engine().toLowerCase().contains("sqlite"));
    }
    @Test void corruptedConfigurationNeverFallsBackToSqlite() throws Exception {
        Path file=DatabaseConfigManager.getConfigFile();
        byte[] original=Files.exists(file)?Files.readAllBytes(file):null;
        Files.createDirectories(file.getParent());
        try {
            for(String data:List.of("type=UNKNOWN\n", "host=localhost\n", "type=MYSQL\nport=bad\n", "type=MYSQL\nport=70000\n", "type=MYSQL\nencryptConnection=garbage\n")) {
                Files.writeString(file,data);
                assertThrows(IllegalStateException.class,DatabaseConfigManager::load);
                assertEquals(data,Files.readString(file));
            }
        } finally {if(original==null)Files.deleteIfExists(file);else Files.write(file,original);}
    }
    @Test void plaintextRemoteLegacySettingsRemainEditableButCannotConnect() throws Exception {
        Path file=DatabaseConfigManager.getConfigFile();
        byte[] original=Files.exists(file)?Files.readAllBytes(file):null;
        Files.createDirectories(file.getParent());
        try {
            Files.writeString(file,"type=MYSQL\nhost=remote.example.org\nport=3306\ndatabaseName=gymflow\nencryptConnection=false\n");
            assertThrows(IllegalStateException.class,DatabaseConfigManager::load);
            DatabaseConfig draft=DatabaseConfigManager.loadForEditing();
            assertEquals("remote.example.org",draft.getHost());assertEquals(DatabaseType.MYSQL,draft.getType());
            assertThrows(IllegalArgumentException.class,()->DatabaseConnectionManager.validate(draft));
        } finally {if(original==null)Files.deleteIfExists(file);else Files.write(file,original);}
    }
    @Test void currentLedgerWithMissingTablesIsNotReady() throws Exception {
        DatabaseConfig c=new DatabaseConfig();c.setSqlitePath(directory.resolve("schema.db").toString());
        MigrationRunner.migrate(c);
        try(Connection con=DatabaseManager.getConnection(c);Statement statement=con.createStatement()) {
            DatabaseMigrationManager.verifySchema(con);
            statement.execute("DROP TABLE revenues");
            assertThrows(SQLException.class,()->DatabaseMigrationManager.verifySchema(con));
        }
    }

    @Test void missingDriverIsClassifiedWithoutPoisoningBundledDrivers() throws Exception {
        java.net.URL classes=DatabaseConfig.class.getProtectionDomain().getCodeSource().getLocation();
        try(var isolated=new java.net.URLClassLoader(new java.net.URL[]{classes},ClassLoader.getPlatformClassLoader())) {
            Class<?> configClass=Class.forName("com.mycompany.lazar_app.database.DatabaseConfig",true,isolated);
            Object config=configClass.getConstructor().newInstance();
            Class<?> manager=Class.forName("com.mycompany.lazar_app.database.DatabaseConnectionManager",true,isolated);
            var invocation=assertThrows(java.lang.reflect.InvocationTargetException.class,
                    ()->manager.getMethod("verifyDriver",configClass).invoke(null,config));
            SQLException error=assertInstanceOf(SQLException.class,invocation.getCause());
            assertEquals("IM003",error.getSQLState());
            assertEquals(DatabaseExceptionHandler.Category.DRIVER,DatabaseExceptionHandler.classify(error));
        }
        assertDoesNotThrow(()->DatabaseConnectionManager.verifyDriver(new DatabaseConfig()));
    }

    @Test void configurationRoundTripPreservesSecuritySettingsWithoutWritingPasswords() throws Exception {
        var auth=com.mycompany.lazar_app.security.AuthorizationService.currentRole();
        if(!com.mycompany.lazar_app.security.SuperAdminAuthService.isConfigured())
            com.mycompany.lazar_app.security.SuperAdminAuthService.setPassword("Release-QA-only-123");
        assertTrue(com.mycompany.lazar_app.security.AuthorizationService.authenticate("Release-QA-only-123"));
        Path file=DatabaseConfigManager.getConfigFile();byte[] original=Files.exists(file)?Files.readAllBytes(file):null;
        try {
            Path store=directory.resolve("custom CA مخزن.p12");Files.write(store,new byte[0]);
            DatabaseConfig c=server(DatabaseType.MARIADB);c.setTrustStore(store.toString());
            c.setTrustStorePassword("session-trust-secret");c.setConnectTimeoutMillis(7000);c.setSocketTimeoutMillis(8000);
            DatabaseConfigManager.save(c);DatabaseConfig loaded=DatabaseConfigManager.load();
            assertEquals(DatabaseType.MARIADB,loaded.getType());assertTrue(loaded.isEncryptConnection());
            assertEquals(c.getHost(),loaded.getHost());assertEquals(store.toString(),loaded.getTrustStore());
            assertEquals(7000,loaded.getConnectTimeoutMillis());assertEquals(8000,loaded.getSocketTimeoutMillis());
            assertEquals(c.getPassword(),loaded.getPassword());assertEquals(c.getTrustStorePassword(),loaded.getTrustStorePassword());
            String stored=Files.readString(file);assertFalse(stored.contains(c.getPassword()));assertFalse(stored.contains(c.getTrustStorePassword()));
        } finally {
            if(original==null)Files.deleteIfExists(file);else Files.write(file,original);
            if(auth!=com.mycompany.lazar_app.security.AuthorizationService.Role.SUPER_ADMIN)
                com.mycompany.lazar_app.security.AuthorizationService.logout();
        }
    }

}

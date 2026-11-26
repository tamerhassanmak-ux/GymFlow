package com.mycompany.lazar_app.database;

import com.mycompany.lazar_app.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseActivationTest {
    @TempDir Path directory;
    private byte[] original;
    private static final String PASSWORD = "Release-QA-only-123";
    @BeforeEach void setup() throws Exception {
        Path file=DatabaseConfigManager.getConfigFile(); original=Files.exists(file)?Files.readAllBytes(file):null;
        if(!SuperAdminAuthService.isConfigured()) SuperAdminAuthService.setPassword(PASSWORD);
        assertTrue(AuthorizationService.authenticate(PASSWORD));
    }
    @AfterEach void restore() throws Exception {
        Path file=DatabaseConfigManager.getConfigFile();
        if(original==null) Files.deleteIfExists(file); else Files.write(file,original);
        assertTrue(AuthorizationService.authenticate(PASSWORD));
    }
    private DatabaseConfig sqlite(String name) {
        DatabaseConfig config=new DatabaseConfig();config.setSqlitePath(directory.resolve(name).toString());return config;
    }
    @Test void firstInstallationStartsWithoutServerOrAdministrator() throws Exception {
        Path output=directory.resolve("first-run.log");
        ProcessBuilder builder=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),
                "-Duser.home="+directory.resolve("fresh-home"),"-cp",System.getProperty("java.class.path"),FirstRunDatabaseProbe.class.getName());
        builder.environment().put("XDG_DATA_HOME",directory.resolve("fresh-data").toString());
        builder.environment().put("XDG_CONFIG_HOME",directory.resolve("fresh-config").toString());
        builder.environment().put("LOCALAPPDATA",directory.resolve("fresh-data").toString());
        builder.environment().put("APPDATA",directory.resolve("fresh-config").toString());
        Process child=builder.redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(child.waitFor(30,TimeUnit.SECONDS));
            assertEquals(0,child.exitValue(),()->{try{return Files.readString(output);}catch(Exception e){return e.toString();}});
            assertTrue(Files.readString(output).contains("FIRST_RUN_SQLITE_GYMFLOW_READY"));
        } finally {if(child.isAlive())child.destroyForcibly();}
    }
    @Test void successfulSwitchPreparesCandidateAndPreservesOriginalData() throws Exception {
        DatabaseConfig old=sqlite("old.db"),next=sqlite("next.db");
        DatabaseConfigManager.activate(old);
        try(Connection c=DatabaseManager.getConnection();Statement s=c.createStatement()) {s.execute("INSERT INTO members(member_code,name) VALUES('KEEP','Original')");}
        DatabaseConfigManager.activate(next);
        assertEquals(next.getSqlitePath(),DatabaseConfigManager.load().getSqlitePath());
        try(Connection c=DatabaseManager.getConnection()) {DatabaseMigrationManager.verifySchema(c);}
        DatabaseConfigManager.activate(old);
        try(Connection c=DatabaseManager.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT name FROM members")) {assertTrue(r.next());assertEquals("Original",r.getString(1));}
    }
    @Test void failedCandidateDoesNotReplaceActiveConfiguration() throws Exception {
        DatabaseConfigManager.activate(sqlite("working.db"));
        byte[] before=Files.readAllBytes(DatabaseConfigManager.getConfigFile());
        DatabaseConfig broken=sqlite("broken.db");
        try(Connection c=DatabaseManager.getConnection(broken);Statement s=c.createStatement()) {s.execute("CREATE TABLE schema_migrations(version TEXT)");s.execute("INSERT INTO schema_migrations VALUES('999')");}
        assertThrows(SQLException.class,()->DatabaseConfigManager.activate(broken));
        assertArrayEquals(before,Files.readAllBytes(DatabaseConfigManager.getConfigFile()));
        try(Connection c=DatabaseManager.getConnection()) {DatabaseMigrationManager.verifySchema(c);}
    }
    @Test void operatorCannotSwitchOrCreateCandidate() throws Exception {
        DatabaseConfig candidate=sqlite("unauthorized.db");AuthorizationService.logout();
        assertThrows(SecurityException.class,()->DatabaseConfigManager.activate(candidate));
        assertFalse(Files.exists(Path.of(candidate.getSqlitePath())));
    }
}

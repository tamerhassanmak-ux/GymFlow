package com.mycompany.lazar_app.database;

import com.mycompany.lazar_app.database.migration.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in integration check: only random disposable schemas; never migrates the configured database. */
@EnabledIfSystemProperty(named="gymflow.localUpgradeTest", matches="true")
class LocalServerUpgradeTest {
    private DatabaseConfig config;
    private String fixture;
    @BeforeEach void create() throws Exception {
        Properties properties = new Properties();
        try(var input=Files.newInputStream(Path.of(System.getProperty("gymflow.testConfig")))) { properties.load(input); }
        config = new DatabaseConfig(); config.setType(DatabaseType.valueOf(properties.getProperty("type")));
        config.setHost(properties.getProperty("host")); config.setPort(Integer.parseInt(properties.getProperty("port")));
        config.setUsername(properties.getProperty("username", "")); config.setPassword(properties.getProperty("password", ""));
        config.setEncryptConnection(false); config.setDatabaseName(properties.getProperty("databaseName"));
        assertTrue(DatabaseConnectionManager.isLiteralLoopback(config.getHost()));
        fixture="gymflow_test_"+UUID.randomUUID().toString().replace("-", "");
        try(Connection c=DatabaseManager.getConnection(config);Statement s=c.createStatement()) {s.execute("CREATE DATABASE `"+fixture+"`");}
        config.setDatabaseName(fixture);
    }
    @AfterEach void cleanup() throws Exception {
        if(fixture!=null) try(Connection c=DatabaseManager.getConnection(config);Statement s=c.createStatement()) {s.execute("DROP DATABASE `"+fixture+"`");}
    }
    @Test void nativeBackupRestoreRehearsalUpgradeAndRestart() throws Exception {
        try(Connection c=DatabaseManager.getConnection(config);Statement s=c.createStatement()) {
            new M001CreateMembersTable().up(c);
            s.execute("INSERT INTO members(member_code,name,phone) VALUES('TEST','عضو تجريبي','01000000001')");
        }
        MigrationRunner.migrate(config);
        try(Connection c=DatabaseManager.getConnection(config)) {DatabaseMigrationManager.verifySchema(c);}
        MigrationRunner.migrate(config);
        try(Connection c=DatabaseManager.getConnection(config);Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT name FROM members")) {
            assertTrue(r.next());assertEquals("عضو تجريبي",r.getString(1));assertFalse(r.next());
        }
    }
    @Test void failedRehearsalLeavesOriginalLedgerAndRowsUntouched() throws Exception {
        try(Connection c=DatabaseManager.getConnection(config);Statement s=c.createStatement()) {
            new M001CreateMembersTable().up(c);
            s.execute("INSERT INTO members(member_code,name,phone) VALUES('ONE','One','123'),('TWO','Two','123')");
        }
        SQLException failure = assertThrows(SQLException.class,()->MigrationRunner.migrate(config));
        Throwable root = failure; while(root.getCause()!=null) root=root.getCause();
        assertTrue(root.getMessage().contains("Duplicate member phones"), "Must reach the data-conflict rehearsal, not just fail backup");
        try(Connection c=DatabaseManager.getConnection(config);Statement s=c.createStatement()) {
            try(ResultSet r=c.getMetaData().getTables(c.getCatalog(),null,"schema_migrations",new String[]{"TABLE"})) {assertFalse(r.next());}
            try(ResultSet r=s.executeQuery("SELECT COUNT(*) FROM members")) {assertTrue(r.next());assertEquals(2,r.getInt(1));}
        }
    }
    @Test void concurrentUpgradeIsSerializedAndLockIsReleased() throws Exception {
        try(Connection c=DatabaseManager.getConnection(config);Connection other=DatabaseManager.getConnection(config)) {
            try(var lock=ServerUpgradeLock.acquire(c,config);Statement s=other.createStatement();
                ResultSet r=s.executeQuery("SELECT IS_USED_LOCK((SELECT CONCAT('gymflow-upgrade-',LEFT(SHA2(DATABASE(),256),40))))")) {
                assertTrue(r.next());assertNotNull(r.getObject(1));
            }
            try(var lock=ServerUpgradeLock.acquire(other,config)) {assertNotNull(lock);}
        }
    }
}

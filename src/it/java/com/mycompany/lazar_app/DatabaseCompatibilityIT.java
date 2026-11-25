package com.mycompany.lazar_app;

import com.mycompany.lazar_app.database.*;
import com.mycompany.lazar_app.database.migration.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import java.time.Duration;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/** Mandatory profile: Docker absence is a failure, never an automatic skip. Disposable fixtures only. */
class DatabaseCompatibilityIT {
    @Test void schemaCompatibilityAndNegativeConnections() throws Exception {
        String engine = System.getProperty("gymflow.it.engine", "MYSQL");
        String image = System.getProperty("gymflow.it.image", "mysql:8.0.40");
        boolean mssql = engine.equals("SQL_SERVER");
        int port = mssql ? 1433 : 3306;
        String password = "Disposable-fixture-938!";
        try (GenericContainer<?> db = new GenericContainer<>(DockerImageName.parse(image))) {
            db.withExposedPorts(port).withStartupTimeout(Duration.ofMinutes(4));
            if (mssql) db.withEnv("ACCEPT_EULA", "Y").withEnv("MSSQL_SA_PASSWORD", password)
                    .waitingFor(Wait.forLogMessage(".*SQL Server is now ready for client connections.*",1));
            else if (engine.equals("MARIADB")) db.withEnv("MARIADB_ROOT_PASSWORD",password)
                    .withEnv("MARIADB_DATABASE","gymflow").withEnv("MARIADB_ROOT_HOST","%")
                    .waitingFor(Wait.forLogMessage(".*ready for connections.*",2));
            else db.withEnv("MYSQL_ROOT_PASSWORD",password).withEnv("MYSQL_DATABASE","gymflow")
                    .withEnv("MYSQL_ROOT_HOST","%")
                    .waitingFor(Wait.forLogMessage(".*ready for connections.*",2));
            db.start();
            DatabaseConfig c = new DatabaseConfig(); c.setType(DatabaseType.valueOf(engine));
            c.setHost(db.getHost()); c.setPort(db.getMappedPort(port));
            // This fixture intentionally uses plaintext on verified loopback only. TLS tests are separate release requirements.
            assertTrue(DatabaseConnectionManager.isLiteralLoopback(c.getHost()), "Use a local Docker daemon for plaintext fixtures");
            c.setEncryptConnection(false); c.setUsername(mssql ? "sa" : "root"); c.setPassword(password);
            c.setDatabaseName(mssql ? "master" : "gymflow");
            if (mssql) try(Connection con=DatabaseManager.getConnection(c); Statement s=con.createStatement()){s.execute("CREATE DATABASE gymflow");}
            c.setDatabaseName("gymflow");
            DatabaseHealthCheck.Result versions = DatabaseHealthCheck.check(c);
            System.out.println("TESTED image="+image+" engine="+versions.engine()+" server="+versions.serverVersion()+" driver="+versions.driverVersion());
            // Production refuses unbacked server DDL. Provision reviewed synthetic fixtures explicitly here.
            assertThrows(SQLException.class, () -> MigrationRunner.migrate(c));
            try (Connection con = DatabaseManager.getConnection(c); Statement statement = con.createStatement()) {
                statement.execute("CREATE TABLE schema_migrations(version VARCHAR(50) PRIMARY KEY, description VARCHAR(255) NOT NULL, applied_at VARCHAR(30) NOT NULL)");
                Migration[] migrations = {new M001CreateMembersTable(),new M002CreateMembershipPlansTable(),new M003CreateMemberMembershipsTable(),
                    new M004CreateCheckInsTable(),new M005CreatePaymentsTable(),new M006EnsureSessionLimitColumns(),new M007CreateExpensesTable(),
                    new M008CreateRevenuesTable(),new M009EnsureRevenueTables(),new M010EnforceMemberPhoneUniqueness(),new M011ProtectFinancialOwnership(),
                    new M012CreatePaymentInstallments(),new M013PreventDuplicateSubmissions(),new M014IndexMembershipLookups()};
                for(Migration migration:migrations) {
                    migration.up(con);
                    try(PreparedStatement record=con.prepareStatement("INSERT INTO schema_migrations VALUES(?,?,?)")) {
                        record.setString(1,migration.version());record.setString(2,migration.description());record.setString(3,"2026-11-25"); record.executeUpdate();
                    }
                }
                statement.execute("INSERT INTO members(member_code,name,phone) VALUES('SYNTHETIC','Synthetic customer','01000000000')");
            }
            MigrationRunner.migrate(c); // Current ledger causes no DDL and is repeat-safe.
            MigrationRunner.migrate(c);
            try(Connection con=DatabaseManager.getConnection(c);Statement s=con.createStatement();ResultSet rows=s.executeQuery("SELECT COUNT(*) FROM members")) {
                assertTrue(rows.next()); assertEquals(1,rows.getInt(1));
            }
            c.setPassword("incorrect"); assertThrows(SQLException.class, () -> DatabaseHealthCheck.check(c));
            c.setPassword(password);c.setDatabaseName("does_not_exist");assertThrows(SQLException.class, () -> DatabaseHealthCheck.check(c));
        }
    }
}

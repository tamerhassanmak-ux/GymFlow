package com.mycompany.lazar_app;

import com.mycompany.lazar_app.database.*;
import com.mycompany.lazar_app.database.migration.*;
import com.mycompany.lazar_app.model.*;
import com.mycompany.lazar_app.repository.*;
import com.mycompany.lazar_app.security.*;
import com.mycompany.lazar_app.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseRegressionTest {
    private static final String PASSWORD = "Release-QA-only-123";
    @TempDir Path directory;
    DatabaseConfig config;
    PaymentRepository payments;
    SqliteBackupService backups;

    @BeforeAll static void isolatedAuthentication() {
        assertTrue(System.getProperty("user.home").replace('\\','/').contains("target/qa-home"),
                "Maven test isolation must be configured before running tests");
        if (!SuperAdminAuthService.isConfigured()) SuperAdminAuthService.setPassword(PASSWORD);
        assertTrue(AuthorizationService.authenticate(PASSWORD));
    }
    @BeforeEach void initialize() throws Exception {
        assertTrue(AuthorizationService.authenticate(PASSWORD));
        config = new DatabaseConfig();
        config.setSqlitePath(directory.resolve("active.db").toString());
        DatabaseConfigManager.save(config);
        MigrationRunner.migrate();
        sql("INSERT INTO members(id,member_code,name,phone) VALUES(1,'A','أحمد','01000000001'),(2,'B','English','01000000002')");
        sql("INSERT INTO membership_plans(id,code,name_ar,name_en,duration_months,price,max_sessions) VALUES(1,'P','شهري','Monthly',1,100,12)");
        sql("INSERT INTO member_memberships(id,member_id,membership_plan_id,start_date,end_date,price,final_price,allowed_sessions) VALUES(1,1,1,'2026-11-01','2026-11-30',100,100,12)");
        payments = new PaymentRepository(); backups = new SqliteBackupService();
    }
    void sql(String text) throws SQLException {
        try (Connection c=DatabaseManager.getConnection(config); Statement s=c.createStatement()) {s.execute(text);}
    }
    long scalar(String text) throws SQLException {
        try (Connection c=DatabaseManager.getConnection(config); Statement s=c.createStatement(); ResultSet r=s.executeQuery(text)) {assertTrue(r.next());return r.getLong(1);}
    }
    Payment payment(long owner, Long membership) {
        return new Payment(0,owner,membership,new BigDecimal("25.50"),"CASH","PAID",LocalDateTime.now(),"QA",null);
    }
    @Test void RI01_migrationsAreIdempotent() throws Exception {
        MigrationRunner.migrate(); assertEquals(14, scalar("SELECT count(*) FROM schema_migrations"));
    }
    @Test void RI02_everyConnectionEnforcesForeignKeys() throws Exception {
        assertEquals(1,scalar("PRAGMA foreign_keys"));
        assertThrows(SQLException.class,()->payments.save(payment(999L,null)));
        assertEquals(0, payments.countAll());
    }
    @Test void RI03_createRejectsWrongSubscriptionOwner() {
        assertThrows(SQLException.class,()->payments.save(payment(2,1L)));
    }
    @Test void RI04_updateRejectsWrongOwnerWithoutChangingPayment() throws Exception {
        Payment p=payments.save(payment(1,1L));p.setMemberId(2);
        assertThrows(SQLException.class,()->payments.update(p));
        assertEquals(1,payments.findByMemberId(1).size());
    }
    @Test void RI05_nullUpdatesRejectClearly() {
        assertThrows(IllegalArgumentException.class,()->payments.update(null));
        assertThrows(IllegalArgumentException.class,()->new ExpenseRepository().update(null));
        assertThrows(IllegalArgumentException.class,()->new RevenueRepository().update(null));
        assertThrows(IllegalArgumentException.class,()->new MembershipPlanRepository().update(null));
    }
    @Test void RI06_databaseBlocksDirectCrossOwnerWriteAndReassignment() throws Exception {
        assertThrows(SQLException.class,()->sql("INSERT INTO payments(member_id,membership_id,amount,payment_method,payment_date) VALUES(2,1,10,'CASH','2026-11-01')"));
        payments.save(payment(1,1L));
        assertThrows(SQLException.class,()->sql("UPDATE member_memberships SET member_id=2 WHERE id=1"));
        assertThrows(SQLException.class,()->sql("DELETE FROM member_memberships WHERE id=1"));
        assertEquals(1,scalar("SELECT member_id FROM member_memberships WHERE id=1"));
    }
    @Test void RI07_duplicateNonblankPhonesRejectedAndBlanksAllowed() throws Exception {
        assertThrows(SQLException.class,()->sql("INSERT INTO members(name,phone) VALUES('duplicate',' 01000000001 ')"));
        sql("INSERT INTO members(name,phone) VALUES('blank1',''),('blank2',' '),('null1',NULL),('null2',NULL)");
        assertEquals(6,scalar("SELECT count(*) FROM members"));
    }
    @Test void RI08_legacyDuplicateMigrationFailsWithoutDeletingData() throws Exception {
        sql("DROP INDEX idx_members_phone_nonblank_unique");
        sql("DELETE FROM schema_migrations WHERE version='010'");
        sql("INSERT INTO members(name,phone) VALUES('duplicate','01000000001')");
        assertThrows(Exception.class,MigrationRunner::migrate);
        assertEquals(3,scalar("SELECT count(*) FROM members"));
        assertEquals(0,scalar("SELECT count(*) FROM schema_migrations WHERE version='010'"));
    }
    @Test void RI09_backupIncludesCommittedWalAndUncommittedRowsAreExcluded() throws Exception {
        sql("PRAGMA journal_mode=WAL");
        try(Connection writer=DatabaseManager.getConnection(config);Statement s=writer.createStatement()) {
            sql("INSERT INTO members(name) VALUES('committed WAL')");
            writer.setAutoCommit(false);s.executeUpdate("INSERT INTO members(name) VALUES('uncommitted')");
            Path copy=directory.resolve("snapshot.db");backups.backup(config,copy);
            try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+copy);Statement query=c.createStatement();ResultSet r=query.executeQuery("SELECT count(*) FROM members")){assertTrue(r.next());assertEquals(3,r.getLong(1));}
            writer.rollback();
        }
    }
    @Test void RI10_corruptRestorePreservesLiveFinancialData() throws Exception {
        payments.save(payment(1,1L));
        Path bad=directory.resolve("corrupt.db");Files.writeString(bad,"not sqlite");
        byte[] original=Files.readAllBytes(Path.of(config.getSqlitePath()));
        assertThrows(SQLException.class,()->backups.restore(config,bad));
        assertArrayEquals(original,Files.readAllBytes(Path.of(config.getSqlitePath())));
        assertEquals(new BigDecimal("25.5"),payments.getTotalPayments());
    }
    @Test void RI11_wrongApplicationDatabaseRejected() throws Exception {
        Path wrong=directory.resolve("other.db");
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+wrong);Statement s=c.createStatement()){s.execute("CREATE TABLE arbitrary(value TEXT)");}
        assertThrows(SQLException.class,()->backups.restore(config,wrong));assertEquals(2,scalar("SELECT count(*) FROM members"));
    }
    @Test void RI12_restoreCreatesRecoverableSafetySnapshot() throws Exception {
        Path copy=directory.resolve("restore.db");backups.backup(config,copy);
        payments.save(payment(1,1L));
        Path safety=backups.restore(config,copy);
        assertEquals(0,payments.countAll());assertTrue(Files.isRegularFile(safety));
        backups.restore(config,safety);assertEquals(1,payments.countAll());SqliteBackupService.validate(safety);
    }
    @Test void RI13_restoreRefusesActiveManagedConnection() throws Exception {
        Path copy=directory.resolve("restore.db");backups.backup(config,copy);
        try(Connection held=DatabaseManager.getConnection(config)) {
            assertFalse(held.isClosed());assertThrows(SQLException.class,()->backups.restore(config,copy));
        }
        assertEquals(2,scalar("SELECT count(*) FROM members"));
    }
    @Test void RI14_backupCannotOverwriteActiveDatabase() {
        assertThrows(java.io.IOException.class,()->backups.backup(config,Path.of(config.getSqlitePath())));
    }
    @Test void RI15_futureSchemaBackupRejected() throws Exception {
        Path copy=directory.resolve("future.db");backups.backup(config,copy);
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+copy);Statement s=c.createStatement()){s.execute("INSERT INTO schema_migrations VALUES('999','future','now')");}
        assertThrows(SQLException.class,()->backups.restore(config,copy));
    }
    @Test void RI16_installmentRollbackAndMonthEndRounding() throws Exception {
        payments.replaceOutstandingSchedule(1,new BigDecimal("100"),3,LocalDate.of(2026,1,31));
        assertThrows(SQLException.class,()->payments.replaceOutstandingSchedule(1,new BigDecimal("100"),0,LocalDate.now()));
        var rows=payments.findPaymentScheduleByMembershipId(1);assertEquals(3,rows.size());
        assertEquals(LocalDate.of(2026,2,28),rows.get(1).getDueDate());
        assertEquals(0,rows.stream().map(PaymentRepository.PaymentInstallment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(new BigDecimal("100")));
    }
    @Test void RI17_operatorCannotPerformFinancialAdministrativeWrites() throws Exception {
        AuthorizationService.logout();
        assertEquals(AuthorizationService.Role.OPERATOR,AuthorizationService.currentRole());
        assertThrows(SecurityException.class,()->payments.save(payment(1,1L)));
        assertThrows(SecurityException.class,()->payments.deleteById(1));
        assertThrows(SecurityException.class,()->new RevenueRepository().save(new Revenue()));
        assertThrows(SecurityException.class,()->new ExpenseRepository().save(new Expense()));
        assertThrows(SecurityException.class,()->new MembershipPlanRepository().delete(1));
        assertThrows(SecurityException.class,()->new MemberMembershipRepository().cancel(1));
        assertThrows(SecurityException.class,()->DatabaseConfigManager.save(config));
        assertThrows(SecurityException.class,()->backups.backup(config,directory.resolve("denied.db")));
        assertThrows(SecurityException.class,()->backups.restore(config,directory.resolve("denied.db")));
        assertFalse(AuthorizationService.authenticate("wrong"));assertEquals(AuthorizationService.Role.OPERATOR,AuthorizationService.currentRole());
    }
    @Test void RI18_operatorCanRegisterAndCheckInMember() throws Exception {
        AuthorizationService.logout();
        Member m=new Member(0,"QA-C","New member","01000000003","","","",null,"",null,null,"Active","","");
        new MemberRepository().insert(m);assertTrue(m.getId()>0);
        CheckIn ci=new CheckIn();ci.setMemberId(m.getId());ci.setCheckInTime(LocalDateTime.now());ci.setStatus("IN");
        CheckInRepository attendance=new CheckInRepository();attendance.insert(ci);
        assertTrue(attendance.findOpenByMemberId(m.getId()).isPresent());assertTrue(attendance.checkOut(ci.getId()));
    }
    @Test void RI19_plaintextPasswordNotSerialized() throws Exception {
        config.setPassword("QA-remote-secret-never-persist");DatabaseConfigManager.save(config);
        String file=Files.readString(DatabaseConfigManager.getConfigFile());
        assertFalse(file.contains("QA-remote-secret-never-persist"));assertFalse(file.contains("password="));
        assertEquals(config.getPassword(),DatabaseConfigManager.load().getPassword());
    }
    @Test void RI20_secureTlsDefault() {assertTrue(new DatabaseConfig().isEncryptConnection());}
    @Test void RI21_cannotResetConfiguredPasswordWithoutCurrentPassword() {
        assertThrows(SecurityException.class,()->SuperAdminAuthService.setPassword("attacker-password"));
        assertThrows(SecurityException.class,()->SuperAdminAuthService.changePassword("wrong","attacker-password"));
        assertTrue(SuperAdminAuthService.verify(PASSWORD));
    }
    @Test void RI22_passwordByteLimitAndAtomicChangeRevokesApproval() {
        assertThrows(IllegalArgumentException.class,()->SuperAdminAuthService.changePassword(PASSWORD,"أ".repeat(37)));
        SuperAdminAuthService.changePassword(PASSWORD,"Replacement-QA-only-123");
        try {assertEquals(AuthorizationService.Role.OPERATOR,AuthorizationService.currentRole());assertFalse(SuperAdminAuthService.verify(PASSWORD));assertTrue(SuperAdminAuthService.verify("Replacement-QA-only-123"));}
        finally {SuperAdminAuthService.changePassword("Replacement-QA-only-123",PASSWORD);}
    }
    @Test void RI23_attachmentsUseUserDataDirectoryAndUniqueNames() throws Exception {
        Path input=directory.resolve("فاتورة.pdf");Files.writeString(input,"QA attachment");
        Path a=Path.of(RevenueAttachmentStorage.copyToApplicationStorage(input.toFile()));
        Path b=Path.of(RevenueAttachmentStorage.copyToApplicationStorage(input.toFile()));
        assertTrue(a.startsWith(AppPaths.dataDirectory()));assertNotEquals(a,b);assertEquals("QA attachment",Files.readString(a));
    }
    @Test void RI24_concurrentFinancialWritesRemainConsistent() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(4);
        try {List<Callable<Long>> tasks=new ArrayList<>();for(int i=0;i<4;i++)tasks.add(()->{Payment p=payments.save(payment(1,1L));return p.getId();});
            Set<Long> ids=new HashSet<>();for(Future<Long> f:pool.invokeAll(tasks))ids.add(f.get());assertEquals(4,ids.size());assertEquals(4,payments.countAll());
        } finally {pool.shutdownNow();}
        assertEquals(0,scalar("SELECT count(*) FROM pragma_foreign_key_check"));
    }
    @Test void RI25_failedRevenueInsertRollsBackTypeCreation() throws Exception {
        RevenueRepository repository=new RevenueRepository();Revenue v=new Revenue();v.setName("one");v.setRevenueType("existing");v.setAmount(BigDecimal.TEN);v.setRevenueDate(LocalDate.now());v.setRevenueCode("DUP");repository.save(v);
        v.setRevenueType("must rollback");assertThrows(SQLException.class,()->repository.save(v));
        assertEquals(0,scalar("SELECT count(*) FROM revenue_types WHERE name='must rollback'"));
    }
    @Test void RI26_managedConnectionCloseReleasesPermitOnce() throws Exception {
        Connection connection=DatabaseManager.getConnection(config);connection.close();connection.close();
        Path file=directory.resolve("snapshot.db");backups.backup(config,file);backups.restore(config,file);
    }
    @Test void RI27_paymentAndScheduleRollbackTogether() throws Exception {
        payments.replaceOutstandingSchedule(1,new BigDecimal("100"),3,LocalDate.now());
        assertThrows(Exception.class,()->DatabaseManager.inTransaction(()->{
            payments.save(payment(1,1L));
            payments.replaceOutstandingSchedule(1,new BigDecimal("74.50"),0,LocalDate.now());
            return null;
        }));
        assertEquals(0,payments.countAll());assertEquals(3,payments.findPaymentScheduleByMembershipId(1).size());
    }
    @Test void RI28_paymentAndScheduleCommitTogether() throws Exception {
        DatabaseManager.inTransaction(()->{
            payments.save(payment(1,1L));
            payments.replaceOutstandingSchedule(1,new BigDecimal("74.50"),2,LocalDate.now());
            return null;
        });
        assertEquals(1,payments.countAll());assertEquals(2,payments.findPaymentScheduleByMembershipId(1).size());
    }
    @Test void RI29_caughtNestedFailureStillRollsBackWholeUnitOfWork() throws Exception {
        assertThrows(SQLException.class,()->DatabaseManager.inTransaction(()->{
            payments.save(payment(1,1L));
            try {payments.replaceOutstandingSchedule(1,BigDecimal.TEN,0,LocalDate.now());} catch(SQLException expected) { }
            return null;
        }));assertEquals(0,payments.countAll());
    }
    @Test void RI30_orphanInstallmentsAreRejected() {
        assertThrows(SQLException.class,()->payments.replaceOutstandingSchedule(999,BigDecimal.TEN,2,LocalDate.now()));
    }
    @Test void RI31_unrecognizedBackupTriggerIsRejected() throws Exception {
        Path copy=directory.resolve("trigger.db");backups.backup(config,copy);
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+copy);Statement s=c.createStatement()) {
            s.execute("CREATE TRIGGER hostile AFTER INSERT ON payments BEGIN DELETE FROM expenses; END");
        }
        assertThrows(SQLException.class,()->backups.restore(config,copy));
    }
    @Test void RI32_missingRequiredBackupColumnIsRejected() throws Exception {
        Path copy=directory.resolve("missing-column.db");backups.backup(config,copy);
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+copy);Statement s=c.createStatement()) {s.execute("ALTER TABLE revenues DROP COLUMN notes");}
        assertThrows(SQLException.class,()->backups.restore(config,copy));
    }
    @Test void RI33_nonfiniteCommercialValuesRejected() {
        MemberMembership m=new MemberMembership();m.setMemberId(2);m.setMembershipPlanId(1);m.setStartDate(LocalDate.now());m.setEndDate(LocalDate.now().plusMonths(1));m.setDiscount(Double.NaN);
        assertThrows(IllegalArgumentException.class,()->new MemberMembershipRepository().insert(m));
    }
    @Test void RI34_renewalPreservesFinancialHistory() throws Exception {
        payments.save(payment(1,1L));
        assertTrue(new MemberMembershipRepository().cancel(1));
        MemberMembership next=new MemberMembership();next.setMemberId(1);next.setMembershipPlanId(1);next.setStartDate(LocalDate.of(2026,12,1));next.setEndDate(LocalDate.of(2026,12,31));next.setStatus("Active");
        new MemberMembershipRepository().insert(next);
        assertEquals(2,new MemberMembershipRepository().findByMemberId(1).size());assertEquals(1,payments.findByMemberId(1).size());
    }
    @Test void RI35_transactionCannotSilentlySwitchDatabases() throws Exception {
        DatabaseConfig different=new DatabaseConfig();different.setSqlitePath(directory.resolve("different.db").toString());
        assertThrows(SQLException.class,()->DatabaseManager.inTransaction(()->{payments.save(payment(1,1L));DatabaseManager.getConnection(different);return null;}));
        assertEquals(0,payments.countAll());assertFalse(Files.exists(Path.of(different.getSqlitePath())));
    }
    @Test void RI36_backupMissingGuardOrUniqueIndexIsRejected() throws Exception {
        Path copy=directory.resolve("missing-guard.db");backups.backup(config,copy);
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+copy);Statement s=c.createStatement()) {s.execute("DROP TRIGGER guard_payments_insert");}
        assertThrows(SQLException.class,()->backups.restore(config,copy));
        backups.backup(config,copy);
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+copy);Statement s=c.createStatement()) {s.execute("DROP INDEX idx_members_phone_nonblank_unique");}
        assertThrows(SQLException.class,()->backups.restore(config,copy));
    }
    @Test void RI37_atomicConfigWriteKeepsPrivateFileAndRoundTrips() throws Exception {
        config.setHost("QA-host");DatabaseConfigManager.save(config);
        assertEquals("QA-host",DatabaseConfigManager.load().getHost());
        if(Files.getFileStore(DatabaseConfigManager.getConfigFile()).supportsFileAttributeView("posix")) {
            assertEquals(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"),Files.getPosixFilePermissions(DatabaseConfigManager.getConfigFile()));
        }
    }
    @Test void RI38_repeatPaymentSubmissionCannotDuplicateMoney() throws Exception {
        Payment p=payment(1,1L);payments.save(p);
        assertThrows(SQLException.class,()->payments.save(p));assertEquals(1,payments.countAll());
    }
    @Test void RI39_databasePreventsTwoOpenCheckIns() throws Exception {
        String insert="INSERT INTO check_ins(member_id,membership_id,check_in_time,status) VALUES(1,1,'"+LocalDateTime.now().toString().replace('T',' ')+"','IN')";
        sql(insert);assertThrows(SQLException.class,()->sql(insert));
    }
    @Test void RI40_subcentPaymentIsRejectedInsteadOfMisreported() {
        Payment p=payment(1,1L);p.setAmount(new BigDecimal("1.005"));
        assertThrows(IllegalArgumentException.class,()->payments.save(p));
    }
    @Test void RI41_manySmallPaymentsHaveExactCentBalance() throws Exception {
        try(Connection c=DatabaseManager.getConnection(config);PreparedStatement s=c.prepareStatement("INSERT INTO payments(member_id,membership_id,amount,payment_method,payment_date,status) VALUES(1,1,0.1,'CASH','2026-11-01','PAID')")) {
            c.setAutoCommit(false);for(int i=0;i<1000;i++)s.addBatch();s.executeBatch();c.commit();
        }
        var summary=payments.findMembershipFinancialSummary(1);assertEquals(0,summary.getPaidAmount().compareTo(new BigDecimal("100.00")));assertEquals(0,summary.getBalanceDue().signum());
    }
    @Test void RI42_connectionFailureDoesNotPoisonLaterConnections() throws Exception {
        DatabaseConfig bad=new DatabaseConfig();bad.setSqlitePath(directory.toString());assertFalse(DatabaseManager.testConnection(bad));assertEquals(2,scalar("SELECT count(*) FROM members"));
    }
    @Test void RI43_externalWriterBlocksRestoreWithoutDataLoss() throws Exception {
        payments.save(payment(1,1L));Path copy=directory.resolve("before-external.db");backups.backup(config,copy);
        Path ready=directory.resolve("writer.ready"),release=directory.resolve("writer.release");
        Process process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-cp",System.getProperty("surefire.test.class.path"),"com.mycompany.lazar_app.RecoveryLockProcess",config.getSqlitePath(),ready.toString(),release.toString()).redirectErrorStream(true).redirectOutput(directory.resolve("writer.log").toFile()).start();
        try {
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(6);
            while(!Files.exists(ready)&&System.nanoTime()<deadline&&process.isAlive())Thread.sleep(20);
            assertTrue(Files.exists(ready),"External writer did not acquire lock");
            assertThrows(SQLException.class,()->backups.restore(config,copy));
        } finally {Files.writeString(release,"release");if(!process.waitFor(8,java.util.concurrent.TimeUnit.SECONDS))process.destroyForcibly();}
        assertEquals(1,payments.countAll());assertEquals(0,scalar("SELECT count(*) FROM pragma_foreign_key_check"));
    }
    @Test void RI44_validNineMigrationDatabaseUpgradesWithoutRecordLoss() throws Exception {
        DatabaseConfig legacy=new DatabaseConfig();legacy.setSqlitePath(directory.resolve("old-v9.db").toString());DatabaseConfigManager.save(legacy);
        try {
            try(Connection c=DatabaseManager.getConnection(legacy);Statement s=c.createStatement()) {
                s.execute("CREATE TABLE schema_migrations(version VARCHAR(50) PRIMARY KEY,description VARCHAR(255) NOT NULL,applied_at TEXT NOT NULL)");
                var migrations=List.<Migration>of(new M001CreateMembersTable(),new M002CreateMembershipPlansTable(),new M003CreateMemberMembershipsTable(),new M004CreateCheckInsTable(),new M005CreatePaymentsTable(),new M006EnsureSessionLimitColumns(),new M007CreateExpensesTable(),new M008CreateRevenuesTable(),new M009EnsureRevenueTables());
                for(Migration m:migrations){m.up(c);try(PreparedStatement p=c.prepareStatement("INSERT INTO schema_migrations VALUES(?,?,'2026-01-01')")){p.setString(1,m.version());p.setString(2,m.description());p.executeUpdate();}}
                s.execute("INSERT INTO members(id,member_code,name,phone) VALUES(1,'LEGACY','Legacy fixture','01011111111')");
                s.execute("INSERT INTO payments(member_id,amount,payment_method,payment_date,status) VALUES(1,12.34,'CASH','2026-01-01','PAID')");
            }
            MigrationRunner.migrate();assertEquals(1,new PaymentRepository().countAll());assertEquals(0,new PaymentRepository().getTotalPayments().compareTo(new BigDecimal("12.34")));
        } finally {DatabaseConfigManager.save(config);}
    }
    @Test void RI45_expiredApprovalAndLogoutDenyFinancialWrites() throws Exception {
        var field=AuthorizationService.class.getDeclaredField("approvedUntil");field.setAccessible(true);field.setLong(null,System.nanoTime()-1);
        assertEquals(AuthorizationService.Role.OPERATOR,AuthorizationService.currentRole());assertThrows(SecurityException.class,()->payments.save(payment(1,1L)));
        assertTrue(AuthorizationService.authenticate(PASSWORD));AuthorizationService.logout();assertThrows(SecurityException.class,()->payments.save(payment(1,1L)));
    }
    @Test void RI46_failedTransactionSubmissionCanBeRetriedOnce() throws Exception {
        Payment p=payment(1,1L);assertThrows(Exception.class,()->DatabaseManager.inTransaction(()->{payments.save(p);throw new SQLException("injected workflow failure");}));
        assertEquals(0,payments.countAll());payments.save(p);assertEquals(1,payments.countAll());assertThrows(SQLException.class,()->payments.save(p));
    }
    @Test void RI47_migrationStopsOnLegacyDuplicateAttendanceWithoutDeletingIt() throws Exception {
        sql("DROP INDEX idx_check_ins_one_open");sql("DELETE FROM schema_migrations WHERE version='013'");
        sql("INSERT INTO check_ins(member_id,check_in_time,status) VALUES(1,'2026-11-23 10:00:00','IN'),(1,'2026-11-23 10:01:00','IN')");
        assertThrows(Exception.class,MigrationRunner::migrate);assertEquals(2,scalar("SELECT count(*) FROM check_ins"));
    }
    @Test void RI48_failureAfterRecoveryDeletesRollsBackFinancialData() throws Exception {
        payments.save(payment(1,1L));Path copy=directory.resolve("schema-mismatch.db");backups.backup(config,copy);
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+copy);Statement q=c.createStatement()){q.execute("ALTER TABLE members ADD extra_unsupported TEXT");}
        assertThrows(SQLException.class,()->backups.restore(config,copy));assertEquals(1,payments.countAll());assertEquals(2,scalar("SELECT count(*) FROM members"));
    }
    @Test void RI49_killedExternalTransactionRecoversCommittedPayments() throws Exception {
        payments.save(payment(1,1L));Path ready=directory.resolve("crash.ready"),release=directory.resolve("never.release");
        Process process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-cp",System.getProperty("surefire.test.class.path"),"com.mycompany.lazar_app.RecoveryLockProcess",config.getSqlitePath(),ready.toString(),release.toString(),"delete-payments").redirectErrorStream(true).redirectOutput(directory.resolve("crash.log").toFile()).start();
        try{long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(6);while(!Files.exists(ready)&&System.nanoTime()<end&&process.isAlive())Thread.sleep(20);assertTrue(Files.exists(ready));}
        finally{process.destroyForcibly();assertTrue(process.waitFor(8,java.util.concurrent.TimeUnit.SECONDS));}
        assertEquals(1,payments.countAll());assertEquals(0,scalar("SELECT count(*) FROM pragma_foreign_key_check"));
    }
    @Test void RI50_environmentCredentialUsedWithoutDisclosure() throws Exception {
        ProcessBuilder builder=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Duser.home="+System.getProperty("user.home"),"-cp",System.getProperty("surefire.test.class.path"),"com.mycompany.lazar_app.EnvironmentCredentialProbe").redirectErrorStream(true);
        builder.environment().put("GYMFLOW_DB_PASSWORD","QA-only-environment-secret");Process process=builder.start();assertTrue(process.waitFor(8,java.util.concurrent.TimeUnit.SECONDS));String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);assertEquals(0,process.exitValue());assertFalse(output.contains("QA-only-environment-secret"));assertFalse(Files.readString(DatabaseConfigManager.getConfigFile()).contains("QA-only-environment-secret"));
    }
    @Test void RI51_repositorySearchPayloadsDoNotBypassFilters() throws Exception {
        assertTrue(new MemberRepository().findByPhone("' OR 1=1 --").isEmpty());
        var filter=new CheckInRepository.AttendanceFilter(null,null,"' OR 1=1 --",true,List.of());assertEquals(0,new CheckInRepository().findPage(filter,0,50).total());
        assertEquals(0,payments.findPage(new PaymentRepository.PaymentFilter("100%",null,null,false,List.of()),0,50).total());assertEquals(2,scalar("SELECT count(*) FROM members"));
    }
    @Test void RI52_duplicateValidationDoesNotExposePersonalDataInException() {
        Member duplicate=new Member(0,"QA-PRIVATE","QA private","01000000001","","","",null,"",null,null,"Active","","");
        SQLException exception=assertThrows(SQLException.class,()->new MemberRepository().insert(duplicate));assertFalse(exception.getMessage().contains("01000000001"));assertFalse(exception.getMessage().contains("QA private"));
    }
}

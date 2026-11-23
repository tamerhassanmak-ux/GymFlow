import com.mycompany.lazar_app.database.*;
import com.mycompany.lazar_app.database.migration.MigrationRunner;
import com.mycompany.lazar_app.repository.*;
import com.mycompany.lazar_app.model.*;
import com.mycompany.lazar_app.model.report.*;
import com.mycompany.lazar_app.service.*;
import com.mycompany.lazar_app.security.SuperAdminAuthService;
import com.mycompany.lazar_app.license.*;
import java.sql.*;
import java.time.*;
import java.math.*;
import java.nio.file.*;
import java.util.*;

public class QcHarness {
  interface Check { void run() throws Exception; }
  static int pass,fail;
  static void test(String id, Check c) { long t=System.nanoTime(); try { c.run(); pass++; System.out.printf("RESULT|%s|PASS|%.3f ms%n",id,(System.nanoTime()-t)/1e6); } catch(Throwable e) { fail++; System.out.println("RESULT|"+id+"|FAIL|"+e); e.printStackTrace(System.out); } }
  static void require(boolean b) { if(!b) throw new AssertionError("Requirement failed"); }
  static void reject(Check c) throws Exception { try { c.run(); } catch(SQLException|IllegalArgumentException e) { return; } throw new AssertionError("Invalid operation accepted"); }
  static Connection db() throws SQLException { return DatabaseManager.getConnection(); }
  static void sql(String s) throws Exception { try(Connection c=db(); Statement st=c.createStatement()) { st.execute(s); } }
  static long scalar(String s) throws Exception { try(Connection c=db(); Statement st=c.createStatement(); ResultSet r=st.executeQuery(s)) {r.next(); return r.getLong(1);} }
  static Payment payment(long member, String amount, String status) { return new Payment(0,member,null,new BigDecimal(amount),"CASH",status,LocalDateTime.of(2026,11,15,12,0),"QA-' OR 1=1 --","<script>alert(1)</script>"); }
  public static void main(String[] args) throws Exception {
    Path home=Path.of(System.getProperty("user.home"));
    require(home.getFileName().toString().startsWith("gymflow-qc-"));
    SuperAdminAuthService.setPassword("QA-disposable-only-123");
    require(com.mycompany.lazar_app.security.AuthorizationService.authenticate("QA-disposable-only-123"));
    DatabaseConfig config=new DatabaseConfig(); config.setSqlitePath(home.resolve("test.db").toString()); DatabaseConfigManager.save(config);
    test("DB-001",()->{MigrationRunner.migrate(); require(scalar("SELECT count(*) FROM schema_migrations")==14);});
    test("DB-002",()->{MigrationRunner.migrate(); require(scalar("SELECT count(*) FROM schema_migrations")==14);});
    test("DB-003",()->require(scalar("PRAGMA foreign_keys")==1));
    test("DB-004",()->{reject(()->sql("INSERT INTO payments(member_id,amount,payment_method,payment_date) VALUES(999,10,'CASH','2026-11-15')"));sql("DELETE FROM payments WHERE member_id=999");});
    sql("INSERT INTO members(id,member_code,name,phone) VALUES(1,'QA-1','QA أحمد','01000000001'),(2,'QA-2','QA English','01000000002')");
    sql("INSERT INTO membership_plans(id,code,name_ar,name_en,duration_months,price,max_sessions) VALUES(1,'QA','اختبار','QA',1,100,12)");
    sql("INSERT INTO member_memberships(id,member_id,membership_plan_id,start_date,end_date,price,final_price) VALUES(1,1,1,'2026-11-01','2026-11-30',100,100)");
    test("DB-005",()->reject(()->sql("INSERT INTO members(member_code,name) VALUES('QA-1','duplicate')")));
    test("DB-006",()->reject(()->sql("INSERT INTO members(member_code,name) VALUES('QA-null',NULL)")));
    test("DB-007",()->{try(Connection c=db(); Statement s=c.createStatement()){c.setAutoCommit(false);s.executeUpdate("INSERT INTO members(member_code,name) VALUES('QA-rollback','QA')");c.rollback();}require(scalar("SELECT count(*) FROM members WHERE member_code='QA-rollback'")==0);});
    PaymentRepository repo=new PaymentRepository(); PaymentService service=new PaymentService(repo);
    test("PAY-001",()->{Payment p=service.addPayment(payment(1,"10.10","PAID")); require(p.getId()>0);require(repo.findByMemberId(1).size()==1);p.setAmount(new BigDecimal("20.20"));service.updatePayment(p);require(repo.getTotalByStatus("PAID").compareTo(new BigDecimal("20.20"))==0);repo.deleteById(p.getId());require(repo.countAll()==0);});
    test("PAY-002",()->reject(()->service.addPayment(payment(1,"-1","PAID"))));
    test("PAY-003",()->reject(()->service.addPayment(payment(1,"0","PAID"))));
    test("PAY-004",()->reject(()->service.addPayment(null)));
    test("PAY-005",()->{Payment p=service.addPayment(payment(1,"0.10","PAID"));Payment q=service.addPayment(payment(1,"0.20","PAID"));require(repo.getTotalByStatus("PAID").subtract(new BigDecimal("0.30")).abs().compareTo(new BigDecimal("0.000001"))<0);repo.deleteById(p.getId());repo.deleteById(q.getId());});
    test("PAY-006",()->{Payment p=service.addPayment(payment(1,"10","PAID"));reject(()->sql("DELETE FROM members WHERE id=1"));repo.deleteById(p.getId());});
    test("SEC-001",()->{require(repo.search("' OR 1=1 --",null,null).isEmpty());require(scalar("SELECT count(*) FROM members")==2);});
    test("REP-001",()->{ReportSummary s=new ReportSummary();s.setTotalRevenue(new BigDecimal("100"));s.setTotalExpenses(new BigDecimal("25"));require(s.getNetProfit().compareTo(new BigDecimal("75"))==0);require(s.getProfitMargin().compareTo(new BigDecimal("75"))==0);s.setTotalRevenue(null);require(s.getProfitMargin().signum()==0);});
    test("REP-002",()->reject(()->new ReportService(new ReportRepository()).getSummary(LocalDate.of(2026,11,30),LocalDate.of(2026,11,1))));
    test("REP-003",()->{Payment p=repo.save(payment(1,"100","PAID"));Payment q=repo.save(payment(1,"50","PENDING"));ReportSummary s=new ReportRepository().getSummary(LocalDate.of(2026,11,1),LocalDate.of(2026,11,30));require(s.getTotalRevenue().compareTo(new BigDecimal("100"))==0);repo.deleteById(p.getId());repo.deleteById(q.getId());});
    CheckInRepository ci=new CheckInRepository();
    test("ATT-001",()->{CheckIn a=new CheckIn();a.setMemberId(1);a.setMembershipId(1);a.setCheckInTime(LocalDateTime.now());a.setStatus("IN");ci.insert(a);require(ci.findOpenByMemberId(1).isPresent());reject(()->ci.insert(a));require(ci.checkOut(a.getId()));require(!ci.checkOut(a.getId()));CheckIn b=new CheckIn();b.setMemberId(1);b.setMembershipId(1);b.setCheckInTime(LocalDateTime.now());b.setStatus("IN");ci.insert(b);require(ci.countByMembershipId(1)==1);require(ci.checkOut(b.getId()));});
    test("SEC-002",()->{reject(()->SuperAdminAuthService.changePassword("QA-disposable-only-123","short"));require(SuperAdminAuthService.verify("QA-disposable-only-123"));require(!SuperAdminAuthService.verify("wrong"));require(!SuperAdminAuthService.verify(null));});
    test("LIC-001",()->{require(ProgramLicenseManager.evaluate()==ProgramLicenseStatus.NOT_CONFIGURED);reject(()->ProgramLicenseManager.activatePeriod(LocalDate.now(),LocalDate.now().minusDays(1)));ProgramLicenseManager.activatePeriod(LocalDate.now().minusDays(1),LocalDate.now().plusDays(1));require(ProgramLicenseManager.isProgramAllowed());Path f=ProgramLicenseManager.getLicenseFile();Files.writeString(f,Files.readString(f).replace("mode=PERIOD","mode=LIFETIME"));require(!ProgramLicenseManager.isProgramAllowed());});
    test("SCH-001",()->{repo.replaceOutstandingSchedule(1,new BigDecimal("100"),3,LocalDate.of(2026,1,31));var rows=repo.findPaymentScheduleByMembershipId(1);require(rows.size()==3);BigDecimal total=BigDecimal.ZERO;for(var r:rows)total=total.add(r.getAmount());require(total.compareTo(new BigDecimal("100"))==0);require(rows.get(1).getDueDate().equals(LocalDate.of(2026,2,28)));});
    test("SCH-002",()->{reject(()->repo.replaceOutstandingSchedule(1,new BigDecimal("100"),0,LocalDate.now()));require(repo.findPaymentScheduleByMembershipId(1).size()==3);});
    test("REV-001",()->{RevenueRepository r=new RevenueRepository();Revenue v=new Revenue();v.setName("QA revenue");v.setRevenueType("QA type");v.setAmount(new BigDecimal("25.50"));v.setRevenueDate(LocalDate.now());r.save(v);require(v.getId()>0);v.setName("QA updated");r.update(v);require(r.search("QA updated",null).size()==1);r.deleteById(v.getId());require(r.countAll()==0);});
    test("EXP-001",()->{ExpenseService e=new ExpenseService(new ExpenseRepository());Expense v=new Expense();v.setTitle("QA expense");v.setCategory("QA");v.setAmount(new BigDecimal("25"));v.setExpenseDate(LocalDate.now());v.setPaymentMethod("CASH");v.setStatus("PAID");e.addExpense(v);require(v.getId()>0);v.setTitle("QA changed");e.updateExpense(v);require(e.getAllExpenses().size()==1);e.deleteExpense(v.getId());require(e.getTransactionCount()==0);});
    test("SUB-001",()->{MemberMembership m=new MemberMembership();m.setMemberId(2);m.setMembershipPlanId(1);m.setStartDate(LocalDate.now());m.setEndDate(LocalDate.now().plusMonths(1));m.setPrice(100);m.setDiscount(10);m.calculateFinalPrice();require(m.getFinalPrice()==90);m.setStatus("Active");MemberMembershipRepository r=new MemberMembershipRepository();r.insert(m);require(m.getId()>0);require(r.findById(m.getId()).isPresent());reject(()->r.insert(m));require(r.cancel(m.getId()));require(r.delete(m.getId()));});
    test("EXP-002",()->{Expense e=new Expense();e.setAmount(new BigDecimal("-1"));reject(()->new ExpenseService(new ExpenseRepository()).addExpense(e));});
    test("EXPORT-001",()->{for(boolean ar:new boolean[]{false,true}){if(ar)com.mycompany.lazar_app.LanguageManager.setArabic();else com.mycompany.lazar_app.LanguageManager.setEnglish();Path x=home.resolve("report-"+ar+".xlsx");new com.mycompany.lazar_app.service.export.ExcelReportExporter().export(x.toFile(),ReportType.FINANCIAL_SUMMARY,LocalDate.now(),LocalDate.now(),new ReportSummary(),List.of());try(var book=new org.apache.poi.xssf.usermodel.XSSFWorkbook(x.toFile())){require(book.getNumberOfSheets()==1);require(book.getSheetAt(0).isRightToLeft()==ar);}}});
    test("EXPORT-002",()->{for(boolean ar:new boolean[]{false,true}){if(ar)com.mycompany.lazar_app.LanguageManager.setArabic();else com.mycompany.lazar_app.LanguageManager.setEnglish();Path x=home.resolve("report-"+ar+".pdf");new com.mycompany.lazar_app.service.export.PdfReportExporter().export(x.toFile(),ReportType.FINANCIAL_SUMMARY,LocalDate.now(),LocalDate.now(),new ReportSummary(),List.of());var r=new com.lowagie.text.pdf.PdfReader(x.toString());try{require(r.getNumberOfPages()>0);}finally{r.close();}}});
    test("PAY-007",()->{Payment p=payment(2,"10","PAID");p.setMembershipId(1L);try{reject(()->repo.save(p));}finally{if(p.getId()>0)repo.deleteById(p.getId());}});
    test("PAY-008",()->{Payment p=payment(1,"10","PAID");p.setMembershipId(1L);repo.save(p);try{p.setMemberId(2);reject(()->repo.update(p));require(repo.findByMemberId(1).size()==1);}finally{repo.deleteById(p.getId());}});
    test("PAY-009",()->reject(()->repo.update(null)));
    test("DB-009",()->{try{reject(()->sql("INSERT INTO members(member_code,name,phone) VALUES('QA-dupe-phone','QA','01000000001')"));}finally{sql("DELETE FROM members WHERE member_code='QA-dupe-phone'");}});
    test("PERF-002",()->{var pool=java.util.concurrent.Executors.newFixedThreadPool(4);try{List<java.util.concurrent.Callable<Boolean>> jobs=new ArrayList<>();for(int i=0;i<4;i++){final int n=i;jobs.add(()->{for(int j=0;j<25;j++){Payment p=repo.save(payment(1,"1","PAID"));repo.deleteById(p.getId());}return true;});}long start=System.nanoTime();for(var f:pool.invokeAll(jobs))require(f.get());System.out.printf("MEASURE|4 concurrent writers 100 payment insert/delete pairs|%.3f ms%n",(System.nanoTime()-start)/1e6);require(repo.countAll()==0);}finally{pool.shutdownNow();}});
    test("DB-008",()->{require(scalar("SELECT count(*) FROM pragma_foreign_key_check")==0);try(Connection c=db();Statement s=c.createStatement();ResultSet r=s.executeQuery("PRAGMA integrity_check")){require(r.next()&&r.getString(1).equals("ok"));}});
    test("PERF-001",()->{long start=System.nanoTime();try(Connection c=db();PreparedStatement p=c.prepareStatement("INSERT INTO members(member_code,name) VALUES(?,?)")){c.setAutoCommit(false);for(int i=0;i<5000;i++){p.setString(1,"LOAD-"+i);p.setString(2,"Load member "+i);p.addBatch();}p.executeBatch();c.commit();}System.out.printf("MEASURE|5000 member inserts|%.3f ms%n",(System.nanoTime()-start)/1e6);start=System.nanoTime();require(new MemberRepository().findAll().size()==5002);System.out.printf("MEASURE|findAll 5002 members|%.3f ms%n",(System.nanoTime()-start)/1e6);start=System.nanoTime();for(int i=0;i<100;i++)try(Connection c=db()){require(!c.isClosed());}System.out.printf("MEASURE|100 connection cycles|%.3f ms%n",(System.nanoTime()-start)/1e6);});
    System.out.printf("SUMMARY|passed=%d|failed=%d%n",pass,fail);if(fail>0)System.exit(1);
  }
}

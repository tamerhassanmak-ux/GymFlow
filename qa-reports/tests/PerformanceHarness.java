import com.mycompany.lazar_app.database.*;
import com.mycompany.lazar_app.database.migration.MigrationRunner;
import com.mycompany.lazar_app.repository.*;
import com.mycompany.lazar_app.service.*;
import com.mycompany.lazar_app.security.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.lang.management.*;

public class PerformanceHarness {
 interface Action{void run()throws Exception;}
 static void measure(String label,Action a)throws Exception{long n=System.nanoTime();a.run();System.out.printf("PERF|%s|%.3f ms%n",label,(System.nanoTime()-n)/1e6);}
 static void require(boolean b){if(!b)throw new AssertionError("Fixture count mismatch");}
 public static void main(String[]args)throws Exception{
  Path home=Path.of(System.getProperty("user.home"));require(home.getFileName().toString().startsWith("gymflow-qc-"));
  SuperAdminAuthService.setPassword("Performance-QA-only-123");require(AuthorizationService.authenticate("Performance-QA-only-123"));DatabaseConfig config=new DatabaseConfig();config.setSqlitePath(home.resolve("load.db").toString());DatabaseConfigManager.save(config);
  measure("fresh startup migrations",MigrationRunner::migrate);
  measure("10000 members and subscriptions 20000 payments 50000 attendance inserts",()->{
   try(Connection c=DatabaseManager.getConnection();Statement s=c.createStatement()){
    s.execute("INSERT INTO membership_plans(id,code,name_ar,name_en,duration_months,price,max_sessions) VALUES(1,'LOAD','خطة','Plan',1,100,20)");c.setAutoCommit(false);
    try(PreparedStatement m=c.prepareStatement("INSERT INTO members(id,member_code,name,phone) VALUES(?,?,?,?)");PreparedStatement sub=c.prepareStatement("INSERT INTO member_memberships(id,member_id,membership_plan_id,start_date,end_date,final_price,allowed_sessions,status) VALUES(?,?,1,?,?,100,20,'Active')")){
     for(int i=1;i<=10000;i++){m.setInt(1,i);m.setString(2,"LOAD-"+i);m.setString(3,"أحمد Load "+i);m.setString(4,"02"+String.format(java.util.Locale.ROOT,"%09d",i));m.addBatch();sub.setInt(1,i);sub.setInt(2,i);sub.setString(3,LocalDate.now().minusDays(5).toString());sub.setString(4,LocalDate.now().plusDays(25).toString());sub.addBatch();}m.executeBatch();sub.executeBatch();
    }
    try(PreparedStatement p=c.prepareStatement("INSERT INTO payments(member_id,membership_id,amount,payment_method,status,payment_date) VALUES(?,?,50,'CASH','PAID',?)")){for(int i=0;i<20000;i++){int id=i%10000+1;p.setInt(1,id);p.setInt(2,id);p.setString(3,LocalDate.now().atTime(12,0).toString().replace('T',' '));p.addBatch();}p.executeBatch();}
    try(PreparedStatement a=c.prepareStatement("INSERT INTO check_ins(member_id,membership_id,check_in_time,check_out_time,status) VALUES(?,?,?,?, 'OUT')")){for(int i=0;i<50000;i++){int id=i%10000+1;a.setInt(1,id);a.setInt(2,id);String t=LocalDate.now().minusDays(i/10000+1).atTime(10,0).toString().replace('T',' ');a.setString(3,t);a.setString(4,t);a.addBatch();}a.executeBatch();}c.commit();
   }
  });
  measure("materialize 10000 members",()->require(new MemberRepository().findAll().size()==10000));
  measure("dashboard full data reads",()->{DashboardService d=new DashboardService();d.getSummary();d.getMonthlyData();d.getMembershipDistribution();d.getRecentMembers();d.getUpcomingExpirations();d.getRecentPayments();});
  measure("attendance page 50 of 50000",()->require(new CheckInRepository().findPage(new CheckInRepository.AttendanceFilter(null,null,"",false,List.of()),0,50).items().size()==50));
  measure("Arabic attendance search 50 of 50000",()->require(new CheckInRepository().findPage(new CheckInRepository.AttendanceFilter(null,null,"احمد",true,List.of()),0,50).items().size()==50));
  measure("payments page 50 of 20000",()->require(new PaymentRepository().findPage(new PaymentRepository.PaymentFilter("",null,null,false,List.of()),0,50).items().size()==50));
  measure("financial report with 20000 payments",()->{var s=new ReportRepository().getSummary(LocalDate.now().withDayOfMonth(1),LocalDate.now().plusDays(1));require(s.getTotalRevenue().compareTo(new java.math.BigDecimal("1000000"))==0);});
  long start=System.nanoTime();long count=0;double max=0;do{long one=System.nanoTime();try(Connection c=DatabaseManager.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT count(*) FROM members")){require(r.next()&&r.getInt(1)==10000);}max=Math.max(max,(System.nanoTime()-one)/1e6);count++;}while(System.nanoTime()-start<java.util.concurrent.TimeUnit.SECONDS.toNanos(30));
  System.out.printf("SOAK|duration_ms=%.3f|cycles=%d|max_cycle_ms=%.3f%n",(System.nanoTime()-start)/1e6,count,max);
  System.out.println("MEMORY|heap_used_bytes="+ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()+"|threads="+ManagementFactory.getThreadMXBean().getThreadCount());
  System.out.println("PERFORMANCE_SUMMARY|PASS|Bounded 30-second repeated workload; no long-duration leak certification");
 }
}

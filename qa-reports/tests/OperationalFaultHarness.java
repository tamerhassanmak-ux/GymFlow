import com.mycompany.lazar_app.database.*;
import com.mycompany.lazar_app.database.migration.MigrationRunner;
import com.mycompany.lazar_app.repository.PaymentRepository;
import com.mycompany.lazar_app.model.Payment;
import com.mycompany.lazar_app.security.*;
import java.nio.file.*;
import java.sql.*;
import java.math.BigDecimal;
import java.time.*;

/** Safe SQLite disk-full injection via a page limit, never filling the host filesystem. */
public class OperationalFaultHarness {
 public static void main(String[]args)throws Exception{
  Path home=Path.of(System.getProperty("user.home"));if(!home.getFileName().toString().startsWith("gymflow-qc-"))throw new IllegalStateException("Isolation required");
  SuperAdminAuthService.setPassword("Fault-QA-disposable-123");if(!AuthorizationService.authenticate("Fault-QA-disposable-123"))throw new AssertionError();
  DatabaseConfig config=new DatabaseConfig();config.setSqlitePath(home.resolve("fault.db").toString());DatabaseConfigManager.save(config);MigrationRunner.migrate();
  try(Connection c=DatabaseManager.getConnection();Statement s=c.createStatement()){s.execute("INSERT INTO members(id,name) VALUES(1,'Fault fixture')");}
  PaymentRepository repository=new PaymentRepository();boolean rejected=false;
  try{DatabaseManager.inTransaction(()->{
   try(Connection c=DatabaseManager.getConnection();Statement s=c.createStatement()){
    int pages;try(ResultSet r=s.executeQuery("PRAGMA page_count")){r.next();pages=r.getInt(1);}s.execute("PRAGMA max_page_count="+(pages+1));
   }
   repository.save(new Payment(0,1,null,BigDecimal.ONE,"CASH","PAID",LocalDateTime.now(),null,"x".repeat(2_000_000)));return null;
  });}catch(SQLException expected){if(!expected.getMessage().contains("FULL")&&!expected.getMessage().toLowerCase().contains("full"))throw expected;rejected=true;}
  if(!rejected||repository.countAll()!=0)throw new AssertionError("Disk-full operation partially committed");
  repository.save(new Payment(0,1,null,BigDecimal.ONE,"CASH","PAID",LocalDateTime.now(),null,null));
  if(repository.countAll()!=1)throw new AssertionError("Subsequent writes did not recover");
  System.out.println("CASE|OP-DISK-FULL|PASS|SQLITE_FULL rejected; zero partial payments; next write succeeded");
 }
}

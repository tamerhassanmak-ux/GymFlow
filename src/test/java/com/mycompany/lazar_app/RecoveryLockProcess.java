package com.mycompany.lazar_app;
import java.nio.file.*;
import java.sql.*;
/** Test-only external writer. Never packaged with the application. */
public final class RecoveryLockProcess {
 public static void main(String[] args)throws Exception {
  Path database=Path.of(args[0]);Path ready=Path.of(args[1]),release=Path.of(args[2]);
  try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+database);Statement s=c.createStatement()) {
   s.execute("BEGIN IMMEDIATE");if(args.length>3&&args[3].equals("delete-payments"))s.execute("DELETE FROM payments");
   else s.execute("UPDATE members SET name='uncommitted recovery fixture' WHERE id=1");
   Files.writeString(ready,"ready");long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
   while(!Files.exists(release)&&System.nanoTime()<deadline)Thread.sleep(25);
   s.execute("ROLLBACK");
  }
 }
}

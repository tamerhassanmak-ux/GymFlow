public class DriverProbe {
 public static void main(String[] args)throws Exception {
  Class.forName("org.sqlite.JDBC");
  try(var c=java.sql.DriverManager.getConnection("jdbc:sqlite::memory:")){System.out.println("SQLite packaged runtime connection PASS");}
 }
}

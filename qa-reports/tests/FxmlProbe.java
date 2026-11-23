import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import java.nio.file.*;
import java.util.concurrent.*;
public class FxmlProbe {
 public static void main(String[] args)throws Exception {
  if(!System.getProperty("user.home").startsWith("/tmp/gymflow-qc-"))throw new IllegalStateException("Isolation required");
  com.mycompany.lazar_app.database.DatabaseInitializer.initialize();
  com.mycompany.lazar_app.database.migration.MigrationRunner.migrate();
  var done=new CountDownLatch(1);
  var errors=new java.util.concurrent.atomic.AtomicInteger();
  Platform.startup(()->{});
  Platform.runLater(()->{int pass=0,fail=0;try{for(boolean ar:new boolean[]{false,true}){if(ar)com.mycompany.lazar_app.LanguageManager.setArabic();else com.mycompany.lazar_app.LanguageManager.setEnglish();for(String name:args){try{new FXMLLoader(FxmlProbe.class.getResource("/fxml/"+name),com.mycompany.lazar_app.LanguageManager.getBundle()).load();System.out.println("FXML|"+name+"|"+(ar?"ar":"en")+"|PASS");pass++;}catch(Throwable e){System.out.println("FXML|"+name+"|"+(ar?"ar":"en")+"|FAIL|"+e);e.printStackTrace(System.out);fail++;errors.incrementAndGet();}}}}finally{System.out.println("FXML_SUMMARY|passed="+pass+"|failed="+fail);done.countDown();}});
  if(!done.await(40,TimeUnit.SECONDS)){System.out.println("FXML_TIMEOUT");System.exit(2);}System.exit(errors.get()==0?0:1);
 }
}

import com.mycompany.lazar_app.*;
import com.mycompany.lazar_app.database.*;
import com.mycompany.lazar_app.database.migration.MigrationRunner;
import com.mycompany.lazar_app.model.*;
import com.mycompany.lazar_app.repository.*;
import com.mycompany.lazar_app.security.*;
import com.mycompany.lazar_app.settings.*;
import com.mycompany.lazar_app.ui.*;
import com.mycompany.lazar_app.ui.dialog.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.time.*;
import java.sql.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/** Bounded, real JavaFX interaction tests. No production data or external services. */
public class FinalUiHarness {
 interface Work {void run() throws Exception;}
 static Stage stage; static FXMLLoader loader; static Parent page;
 static Path home, evidence; static DatabaseConfig config;
 static int passed,failed; static final String PASSWORD="Final-UI-disposable-123";
 static final List<Throwable> uncaught=new CopyOnWriteArrayList<>();
 static <T>T fx(Callable<T> action)throws Exception {
  if(Platform.isFxApplicationThread()) return action.call();
  CompletableFuture<T> f=new CompletableFuture<>();Platform.runLater(()->{try{f.complete(action.call());}catch(Throwable t){f.completeExceptionally(t);}});
  return f.get(20,TimeUnit.SECONDS);
 }
 static <T>CompletableFuture<T> async(Callable<T> action){CompletableFuture<T> f=new CompletableFuture<>();Platform.runLater(()->{try{f.complete(action.call());}catch(Throwable t){f.completeExceptionally(t);}});return f;}
 static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
 static void test(String id,Work work){long start=System.nanoTime();try{work.run();passed++;System.out.printf("CASE|%s|PASS|%.3f ms%n",id,(System.nanoTime()-start)/1e6);}catch(Throwable t){failed++;System.out.println("CASE|"+id+"|FAIL|"+t);t.printStackTrace(System.out);try{fx(()->{for(Window w:new ArrayList<>(Window.getWindows()))if(w!=stage)w.hide();return null;});}catch(Exception ignored){}}}
 static void sql(String s)throws Exception{try(Connection c=DatabaseManager.getConnection();Statement q=c.createStatement()){q.execute(s);}}
 static long scalar(String s)throws Exception{try(Connection c=DatabaseManager.getConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery(s)){r.next();return r.getLong(1);}}
 static Object invoke(Object target,String name,Object...args)throws Exception{for(Method m:target.getClass().getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){m.setAccessible(true);try{return m.invoke(target,args);}catch(InvocationTargetException e){throw new RuntimeException(e.getCause());}}throw new NoSuchMethodException(name);}
 static void open(String name)throws Exception{fx(()->{
  if(stage.getScene()!=null)stage.getScene().setRoot(new Pane());
  loader=new FXMLLoader(FinalUiHarness.class.getResource("/fxml/"+name),LanguageManager.getBundle());page=loader.load();
  if(name.equals("expense-form.fxml"))invoke(loader.getController(),"setExpenseService",new com.mycompany.lazar_app.service.ExpenseService(new ExpenseRepository()));
  if(name.equals("revenue-form.fxml"))invoke(loader.getController(),"setRevenueRepository",new RevenueRepository());
  if(name.equals("member-details.fxml"))invoke(loader.getController(),"setMember",new MemberRepository().findById(1).orElseThrow());
  page.setNodeOrientation(LanguageManager.isArabic()?NodeOrientation.RIGHT_TO_LEFT:NodeOrientation.LEFT_TO_RIGHT);
  Parent root=PageViewport.wrap(page);Scene scene=new Scene(root,1366,768);scene.getStylesheets().add(FinalUiHarness.class.getResource("/css/app.css").toExternalForm());
  stage.setScene(scene);stage.setWidth(1366);stage.setHeight(768);stage.show();root.applyCss();root.layout();return null;});
 }
 static DialogPane dialog()throws Exception{return fx(()->{for(Window w:new ArrayList<>(Window.getWindows()))if(w!=stage&&w.isShowing()&&w.getScene()!=null){Node n=w.getScene().getRoot().lookup(".dialog-pane");if(n instanceof DialogPane d)return d;if(w.getScene().getRoot() instanceof DialogPane d)return d;}return null;});}
 static DialogPane awaitDialog()throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(System.nanoTime()<end){DialogPane p=dialog();if(p!=null)return p;Thread.sleep(30);}throw new AssertionError("Expected dialog did not appear");}
 static void close(DialogPane pane)throws Exception{fx(()->{ButtonType type=pane.getButtonTypes().stream().filter(t->t==ButtonType.CANCEL||t==ButtonType.CLOSE).findFirst().orElse(pane.getButtonTypes().get(0));((Button)pane.lookupButton(type)).fire();return null;});}
 static String label(DialogPane pane,String id)throws Exception{return fx(()->{Node n=pane.lookup("#"+id);require(n instanceof Label,"Missing visible detail: "+id);return ((Label)n).getText();});}
 static void snapshot(Scene scene,String name)throws Exception{fx(()->{scene.getRoot().applyCss();scene.getRoot().layout();WritableImage shot=scene.snapshot(null);BufferedImage image=new BufferedImage((int)shot.getWidth(),(int)shot.getHeight(),BufferedImage.TYPE_INT_ARGB);int[] pixels=new int[(int)(shot.getWidth()*shot.getHeight())];shot.getPixelReader().getPixels(0,0,(int)shot.getWidth(),(int)shot.getHeight(),PixelFormat.getIntArgbInstance(),pixels,0,(int)shot.getWidth());image.setRGB(0,0,image.getWidth(),image.getHeight(),pixels,0,image.getWidth());ImageIO.write(image,"png",evidence.resolve(name+".png").toFile());return null;});}
 static void details(boolean success,int member,String screenshot)throws Exception{
  open("checkin.fxml");Object controller=fx(()->loader.getController());Member model=new MemberRepository().findById(member).orElseThrow();
  CompletableFuture<Object> result=async(()->invoke(controller,"registerCheckInAndReturnResult",model));DialogPane pane=awaitDialog();
  if(success){require(Boolean.TRUE.equals(result.get(10,TimeUnit.SECONDS)),"Check-in rejected");require(label(pane,"checkin-member-name").equals(model.getName()),"Member identity wrong");if(member==5){long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);boolean ready=false;while(System.nanoTime()<end){ready=fx(()->((StackPane)pane.lookup("#checkin-photo")).getChildren().stream().filter(n->n instanceof ImageView).map(n->((ImageView)n).getImage()).anyMatch(i->i!=null&&!i.isError()&&i.getWidth()>0));if(ready)break;Thread.sleep(25);}require(ready,"Photo failed to load");}
   snapshot(fx(()->pane.getScene()),screenshot);}
  else require(!fx(()->pane.getContentText()==null&&pane.getContent()==null),"No validation feedback");
  close(pane);require(Boolean.valueOf(success).equals(result.get(10,TimeUnit.SECONDS)),"Unexpected attendance outcome");
 }
 static Scene childScene()throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(System.nanoTime()<end){Scene scene=fx(()->Window.getWindows().stream().filter(w->w!=stage&&w.isShowing()).map(Window::getScene).filter(Objects::nonNull).filter(sc->sc.getRoot().lookup("#saveButton")!=null||sc.getRoot().lookup("#submitPaymentButton")!=null||sc.getRoot().lookup("#submitMemberButton")!=null).findFirst().orElse(null));if(scene!=null)return scene;Thread.sleep(25);}throw new AssertionError("Expected form window did not open");}
 static void successfulSubmission(Scene scene,String buttonId)throws Exception{CompletableFuture<Void> submit=async(()->{((Button)scene.getRoot().lookup("#"+buttonId)).fire();return null;});long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(!submit.isDone()&&System.nanoTime()<end){DialogPane popup=dialog();if(popup!=null){close(popup);break;}Thread.sleep(25);}submit.get(10,TimeUnit.SECONDS);}
 static void adminDialog(boolean login)throws Exception{
  AuthorizationService.logout();CompletableFuture<Boolean> result=async(()->AdminAccess.ensure(stage));DialogPane pane=awaitDialog();
  if(login){fx(()->{((PasswordField)pane.lookup(".password-field")).setText("wrong");((Button)pane.lookupButton(ButtonType.OK)).fire();return null;});require(!result.isDone(),"Invalid credentials bypassed dialog");
   fx(()->{((PasswordField)pane.lookup(".password-field")).setText(PASSWORD);((Button)pane.lookupButton(ButtonType.OK)).fire();return null;});require(result.get(10,TimeUnit.SECONDS),"Valid login failed");
  }else{close(pane);require(!result.get(10,TimeUnit.SECONDS),"Cancelled login granted approval");}
 }
 public static void main(String[] args)throws Exception{
  home=Path.of(System.getProperty("user.home"));require(home.getFileName().toString().startsWith("gymflow-qc-"),"Isolated home required");evidence=Path.of(args[0]).toAbsolutePath();Files.createDirectories(evidence);
  SuperAdminAuthService.setPassword(PASSWORD);require(AuthorizationService.authenticate(PASSWORD),"Fixture authentication failed");config=new DatabaseConfig();config.setSqlitePath(home.resolve("ui.db").toString());DatabaseConfigManager.save(config);MigrationRunner.migrate();
  LocalDate today=LocalDate.now();
  sql("INSERT INTO members(id,member_code,name,phone) VALUES(1,'UI-1','أحمد QA Member','01000000001'),(2,'UI-2','Expired QA','01000000002'),(3,'UI-3','No subscription QA','01000000003'),(4,'UI-4','Session limit QA','01000000004'),(5,'UI-5','Photo QA','01000000005')");
  sql("INSERT INTO membership_plans(id,code,name_ar,name_en,duration_months,price,max_sessions) VALUES(1,'UI','شهري','Monthly',1,100,12)");
  for(int id:new int[]{1,2,4,5})sql("INSERT INTO member_memberships(id,member_id,membership_plan_id,start_date,end_date,price,final_price,allowed_sessions,status) VALUES("+id+","+id+",1,'"+today.minusDays(id==2?60:5)+"','"+(id==2?today.minusDays(30):today.plusDays(25))+"',100,100,"+(id==4?1:12)+",'Active')");
  sql("INSERT INTO check_ins(member_id,membership_id,check_in_time,check_out_time,status) VALUES(1,1,'"+today.minusDays(1)+" 10:00:00','"+today.minusDays(1)+" 11:00:00','OUT'),(4,4,'"+today.minusDays(1)+" 10:00:00','"+today.minusDays(1)+" 11:00:00','OUT')");
  sql("INSERT INTO payments(member_id,membership_id,amount,payment_method,status,payment_date) VALUES(1,1,25,'CASH','PAID','"+today+"'),(1,1,10,'CASH','PAID','"+today+"'),(1,1,5,'CASH','PENDING','"+today+"')");
  AppSettings settings=AppSettingsManager.load();settings.setCurrency("USD");AppSettingsManager.save(settings);
  Path photo=home.resolve("synthetic-photo.png");BufferedImage image=new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D g=image.createGraphics();g.setColor(java.awt.Color.BLUE);g.fillRect(0,0,100,100);g.dispose();ImageIO.write(image,"png",photo.toFile());sql("UPDATE members SET photo_path='"+photo.toString().replace("'","''")+"' WHERE id=5");
  Thread.setDefaultUncaughtExceptionHandler((thread,error)->{uncaught.add(error);error.printStackTrace(System.out);});Platform.startup(()->{});Platform.setImplicitExit(false);fx(()->{stage=new Stage();return null;});
  List<String> pages=Files.list(Path.of("src/main/resources/fxml")).map(p->p.getFileName().toString()).sorted().toList();
  for(boolean ar:new boolean[]{false,true}){
   fx(()->{if(ar)LanguageManager.setArabic();else LanguageManager.setEnglish();return null;});String lang=ar?"ar":"en";
   for(String name:pages)test("RENDER-"+lang+"-"+name,()->{open(name);for(int width:new int[]{1366,1024,390}){fx(()->{stage.setWidth(width);return null;});Thread.sleep(55);snapshot(fx(()->stage.getScene()),"screen-"+lang+"-"+name.replace(".fxml","")+"-"+width);}require(fx(()->page.getEffectiveNodeOrientation())==(ar?NodeOrientation.RIGHT_TO_LEFT:NodeOrientation.LEFT_TO_RIGHT),"Wrong direction");});
   for(String name:List.of("add-member.fxml","add-membership.fxml","add-payment.fxml","membership-plan-form.fxml","expense-form.fxml","revenue-form.fxml"))test("FORM-"+lang+"-"+name,()->{
    require(AuthorizationService.authenticate(PASSWORD),"Auth");open(name);long before=scalar("SELECT count(*) FROM members")+scalar("SELECT count(*) FROM payments")+scalar("SELECT count(*) FROM expenses")+scalar("SELECT count(*) FROM revenues");String id=name.equals("add-member.fxml")?"submitMemberButton":name.equals("add-payment.fxml")?"submitPaymentButton":"saveButton";
    CompletableFuture<Void> action=async(()->{Node button=page.lookup("#"+id);require(button instanceof Button,"Missing save button");((Button)button).fire();return null;});
    if(name.equals("expense-form.fxml")){action.get(10,TimeUnit.SECONDS);require(fx(()->{Label v=(Label)page.lookup("#validationLabel");return v.isVisible()&&!v.getText().isBlank();}),"Missing inline validation");}
    else{Thread.sleep(100);if(action.isCompletedExceptionally())action.get();DialogPane pane=awaitDialog();snapshot(fx(()->pane.getScene()),"validation-"+lang+"-"+name.replace(".fxml",""));close(pane);action.get(10,TimeUnit.SECONDS);}
    require(before==scalar("SELECT count(*) FROM members")+scalar("SELECT count(*) FROM payments")+scalar("SELECT count(*) FROM expenses")+scalar("SELECT count(*) FROM revenues"),"Blank form persisted data");
   });
   test("CURRENCY-SCREENS-USD-"+lang,()->{for(String name:List.of("payments.fxml","dashboard-content.fxml","memberships.fxml","reports.fxml","add-membership.fxml","add-payment.fxml","revenues.fxml")){open(name);Thread.sleep(100);require(fx(()->page.lookupAll(".label").stream().filter(n->n instanceof Label).map(n->((Label)n).getText()).noneMatch(t->t!=null&&(t.contains("EGP")||t.contains("ج.م")))),"Wrong currency on "+name);}});
   test("SETUP-LOCALE-"+lang,()->{open("super-admin-setup.fxml");String expected=LanguageManager.getBundle().getString("setup.title");require(fx(()->page.lookupAll(".label").stream().anyMatch(n->n instanceof Label&&((Label)n).getText().equals(expected))),"Setup title not localized");});
   test("DATABASE-CONTRAST-"+lang,()->{open("database-settings.fxml");String expected=LanguageManager.getBundle().getString("database.engine");require(fx(()->page.lookupAll(".label").stream().filter(n->n instanceof Label&&((Label)n).getText().equals(expected)).map(n->(Label)n).anyMatch(l->l.getTextFill() instanceof javafx.scene.paint.Color color&&color.getBrightness()<.6)),"Database caption is not readable on white");});
   test("AUTH-CANCEL-"+lang,()->adminDialog(false));test("AUTH-LOGIN-"+lang,()->adminDialog(true));
   test("DETAILS-"+lang,()->{open("checkin.fxml");Object c=fx(()->loader.getController());Member m=new MemberRepository().findById(1).orElseThrow();sql("UPDATE check_ins SET status='OUT',check_out_time=check_in_time WHERE member_id=1 AND status='IN'");CompletableFuture<Object> result=async(()->invoke(c,"registerCheckInAndReturnResult",m));DialogPane pane=awaitDialog();require(Boolean.TRUE.equals(result.get(10,TimeUnit.SECONDS)),"Check-in rejected");
    NumberFormat money=NumberFormat.getCurrencyInstance(LanguageManager.getCurrentLocale());money.setCurrency(Currency.getInstance("USD"));
    require(label(pane,"checkin-amount").equals(money.format(100).replace("\u061C", "").replace("\u200E", "").replace("\u200F", "")),"Subscription amount/currency incorrect");require(label(pane,"checkin-paid").equals(money.format(35).replace("\u061C", "").replace("\u200E", "").replace("\u200F", "")),"Paid total incorrect");require(label(pane,"checkin-balance").equals(money.format(65).replace("\u061C", "").replace("\u200E", "").replace("\u200F", "")),"Balance incorrect");require(label(pane,"checkin-sessions").equals("10"),"Remaining visits incorrect");require(label(pane,"checkin-member").contains("/ 1"),"Wrong subscription");require(label(pane,"checkin-start").equals(today.minusDays(5).toString()),"Wrong start date");require(label(pane,"checkin-end").equals(today.plusDays(25).toString()),"Wrong expiry");snapshot(fx(()->pane.getScene()),"details-"+lang);fx(()->{((ScrollPane)pane.getContent()).setVvalue(1);return null;});snapshot(fx(()->pane.getScene()),"details-"+lang+"-bottom");close(pane);
   });
   test("CHECKIN-DUPLICATE-"+lang,()->details(false,1,"unused"));test("CHECKIN-EXPIRED-"+lang,()->details(false,2,"unused"));test("CHECKIN-NONE-"+lang,()->details(false,3,"unused"));
  }
  test("CHECKIN-LIMIT",()->details(false,4,"unused"));
  test("CHECKIN-SAMEDAY-REENTRY",()->{sql("UPDATE member_memberships SET allowed_sessions=2 WHERE id=4");details(true,4,"last-session-first-entry");sql("UPDATE check_ins SET status='OUT',check_out_time=check_in_time WHERE member_id=4 AND status='IN'");details(true,4,"last-session-reentry");require(new CheckInRepository().countByMembershipId(4)==2,"Repeat used another session");});
  test("PHOTO-VALID",()->{details(true,5,"details-photo");});
  test("DETAILS-FINANCE-FAILURE",()->{Member member=new MemberRepository().findById(1).orElseThrow();var membership=new MemberMembershipRepository().findById(1).orElseThrow();sql("ALTER TABLE payments RENAME TO temporarily_unavailable_payments");try{async(()->{CheckinDetailsDialog.loadAndShow(member,membership,stage);return null;});DialogPane pane=awaitDialog();require(label(pane,"checkin-balance").equals("—"),"Financial failure displayed an invented balance");close(pane);}finally{sql("ALTER TABLE temporarily_unavailable_payments RENAME TO payments");}});
  test("CHECKIN-DB-FAILURE",()->{open("checkin.fxml");Object c=fx(()->loader.getController());Member m=new MemberRepository().findById(1).orElseThrow();Thread.sleep(250);sql("UPDATE check_ins SET status='OUT',check_out_time=check_in_time WHERE member_id=1 AND status='IN'");DatabaseConfig bad=new DatabaseConfig();bad.setSqlitePath(home.toString());DatabaseConfigManager.save(bad);try{CompletableFuture<Object> result=async(()->invoke(c,"registerCheckInAndReturnResult",m));DialogPane pane=awaitDialog();close(pane);require(Boolean.FALSE.equals(result.get(10,TimeUnit.SECONDS)),"Database failure accepted attendance");}finally{DatabaseConfigManager.save(config);}require(scalar("SELECT count(*) FROM check_ins WHERE member_id=1 AND status='IN'")==0,"Failed operation wrote attendance");});
  for(boolean ar:new boolean[]{false,true})test("KEYBOARD-"+(ar?"ar":"en"),()->{fx(()->{if(ar)LanguageManager.setArabic();else LanguageManager.setEnglish();return null;});open("add-member.fxml");fx(()->{stage.requestFocus();page.lookup("#nameField").requestFocus();return null;});Thread.sleep(100);Node previous=fx(()->stage.getScene().getFocusOwner());fx(()->{require(stage.isFocused(),"Fixture stage is not focused");var robot=new javafx.scene.robot.Robot();robot.keyPress(javafx.scene.input.KeyCode.TAB);robot.keyRelease(javafx.scene.input.KeyCode.TAB);return null;});Thread.sleep(100);require(fx(()->stage.getScene().getFocusOwner())!=null&&fx(()->stage.getScene().getFocusOwner())!=previous,"Tab did not move focus");});
  test("QR-SOFTWARE-ROUNDTRIP",()->{var qr=fx(()->com.mycompany.lazar_app.util.QrCodeGenerator.generate("UI-1",220,220));BufferedImage img=new BufferedImage(220,220,BufferedImage.TYPE_INT_RGB);for(int y=0;y<220;y++)for(int x=0;x<220;x++)img.setRGB(x,y,qr.getPixelReader().getArgb(x,y));var source=new com.google.zxing.client.j2se.BufferedImageLuminanceSource(img);var bitmap=new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));require(new com.google.zxing.MultiFormatReader().decode(bitmap).getText().equals("UI-1"),"QR round trip failed");try{com.mycompany.lazar_app.util.QrCodeGenerator.generate("",220,220);throw new AssertionError("Empty QR accepted");}catch(IllegalArgumentException expected){}});
  test("MEMBER-CREATE-UI",()->{LanguageManager.setEnglish();open("members.fxml");Object controller=fx(()->loader.getController());CompletableFuture<Object> parent=async(()->invoke(controller,"openAddMember"));Scene child=childScene();fx(()->{((TextField)child.getRoot().lookup("#nameField")).setText("GUI Registration Fixture");((TextField)child.getRoot().lookup("#phoneField")).setText("01122223333");((TextField)child.getRoot().lookup("#emailField")).setText("qa@example.test");((ComboBox<?>)child.getRoot().lookup("#genderComboBox")).getSelectionModel().selectFirst();return null;});successfulSubmission(child,"submitMemberButton");parent.get(10,TimeUnit.SECONDS);require(scalar("SELECT count(*) FROM members WHERE phone='01122223333'")==1,"Member registration not persisted");});
  test("SUBSCRIPTION-CREATE-UI",()->{open("add-membership.fxml");fx(()->{ComboBox<Member> members=(ComboBox<Member>)page.lookup("#memberComboBox");members.getSelectionModel().select(members.getItems().stream().filter(m->m.getPhone().equals("01122223333")).findFirst().orElseThrow());((ComboBox<?>)page.lookup("#planComboBox")).getSelectionModel().selectFirst();((DatePicker)page.lookup("#startDatePicker")).setValue(LocalDate.now());((DatePicker)page.lookup("#endDatePicker")).setValue(LocalDate.now().plusMonths(1).minusDays(1));return null;});successfulSubmission(fx(()->stage.getScene()),"saveButton");require(scalar("SELECT count(*) FROM member_memberships WHERE member_id=(SELECT id FROM members WHERE phone='01122223333')")==1,"Subscription not persisted");});
  test("PAYMENT-CREATE-UI",()->{require(AuthorizationService.authenticate(PASSWORD),"Auth");open("payments.fxml");Object controller=fx(()->loader.getController());CompletableFuture<Object> parent=async(()->invoke(controller,"openNewPayment"));Scene child=childScene();fx(()->{ComboBox<PaymentRepository.MemberOption> members=(ComboBox<PaymentRepository.MemberOption>)child.getRoot().lookup("#memberComboBox");members.getSelectionModel().select(members.getItems().stream().filter(m->m.getId()==1).findFirst().orElseThrow());((ComboBox<?>)child.getRoot().lookup("#membershipComboBox")).getSelectionModel().selectFirst();((TextField)child.getRoot().lookup("#amountField")).setText("10.00");return null;});successfulSubmission(child,"submitPaymentButton");parent.get(10,TimeUnit.SECONDS);require(scalar("SELECT count(*) FROM payments")==4,"Parent payment workflow did not persist exactly once");require(new PaymentRepository().findMembershipFinancialSummary(1).getBalanceDue().compareTo(new java.math.BigDecimal("55"))==0,"Partial payment balance wrong");});
  test("EXPENSE-CREATE-UI",()->{open("expenses.fxml");Object controller=fx(()->loader.getController());async(()->invoke(controller,"handleNewExpense"));Scene child=childScene();fx(()->{((TextField)child.getRoot().lookup("#titleField")).setText("GUI Expense Fixture");((TextField)child.getRoot().lookup("#amountField")).setText("12.34");return null;});successfulSubmission(child,"saveButton");require(scalar("SELECT count(*) FROM expenses WHERE title='GUI Expense Fixture'")==1,"Expense UI did not persist");});
  test("REVENUE-CREATE-UI",()->{open("revenues.fxml");Object controller=fx(()->loader.getController());async(()->invoke(controller,"openNewRevenue"));Scene child=childScene();fx(()->{((TextField)child.getRoot().lookup("#nameField")).setText("GUI Revenue Fixture");((ComboBox<?>)child.getRoot().lookup("#typeComboBox")).getEditor().setText("GUI income");((TextField)child.getRoot().lookup("#amountField")).setText("14.56");return null;});successfulSubmission(child,"saveButton");require(scalar("SELECT count(*) FROM revenues WHERE name='GUI Revenue Fixture'")==1,"Revenue UI did not persist");});
  test("NAVIGATION-CYCLES",()->{open("dashboard.fxml");List<String> ids=List.of("dashboardButton","membersButton","membershipsButton","membershipPlansButton","checkInButton","attendanceHistoryButton","paymentsButton","revenuesButton","expensesButton","reportsButton");List<Double> times=new ArrayList<>();for(int cycle=0;cycle<5;cycle++)for(String id:ids){long start=System.nanoTime();fx(()->{Node button=page.lookup("#"+id);require(button instanceof Button,"Missing navigation button "+id);((Button)button).fire();require(((StackPane)page.lookup("#contentArea")).getChildren().size()==1,"Navigation did not display page");return null;});times.add((System.nanoTime()-start)/1e6);Thread.sleep(30);}times.sort(Double::compare);System.out.printf("NAV_PERF|switches=%d|p50_ms=%.3f|p95_ms=%.3f|max_ms=%.3f%n",times.size(),times.get(times.size()/2),times.get((int)Math.floor(times.size()*.95)),times.get(times.size()-1));});
  test("UI-UNHANDLED",()->require(uncaught.isEmpty(),"Unhandled UI exceptions: "+uncaught));
  fx(()->{if(stage.getScene()!=null)stage.getScene().setRoot(new Pane());stage.close();return null;});System.out.printf("UI_SUMMARY|passed=%d|failed=%d%n",passed,failed);System.exit(failed==0?0:1);
 }
}

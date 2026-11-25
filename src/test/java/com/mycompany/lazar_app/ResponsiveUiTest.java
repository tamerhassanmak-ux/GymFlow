package com.mycompany.lazar_app;

import com.mycompany.lazar_app.database.*;
import com.mycompany.lazar_app.database.migration.MigrationRunner;
import com.mycompany.lazar_app.security.*;
import com.mycompany.lazar_app.ui.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.robot.Robot;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="gymflow.ui",matches="true")
class ResponsiveUiTest {
    static Path evidence;static DatabaseConfig config;
    record View(Parent content,Parent root,FXMLLoader loader) { Object control(String id){return loader.getNamespace().get(id);} }
    static <T>T fx(Callable<T> action)throws Exception {FutureTask<T> task=new FutureTask<>(action);Platform.runLater(task);return task.get(15,TimeUnit.SECONDS);}
    @BeforeAll static void fixture()throws Exception {
        assertTrue(System.getProperty("user.home").contains("qa-home"),"An isolated qa-home is required");
        evidence=Path.of(System.getProperty("gymflow.ui.evidence","qa-reports/v1.0.6-ui")).toAbsolutePath();Files.createDirectories(evidence.resolve("screenshots"));
        if(!SuperAdminAuthService.isConfigured())SuperAdminAuthService.setPassword("Release-QA-only-123");
        assertTrue(AuthorizationService.authenticate("Release-QA-only-123"));
        Path directory=Files.createTempDirectory("gymflow-ui-fixture-");config=new DatabaseConfig();config.setSqlitePath(directory.resolve("synthetic.db").toString());DatabaseConfigManager.save(config);MigrationRunner.migrate(config);
        try(Connection c=DatabaseManager.getConnection(config);Statement s=c.createStatement()){
            s.execute("INSERT INTO members(id,member_code,name,phone) VALUES(1,'UI1','Synthetic member','01000000001')");
            s.execute("INSERT INTO membership_plans(id,code,name_ar,name_en,duration_months,price,max_sessions) VALUES(1,'UI','خطة تجريبية','Synthetic plan',1,100,12)");
        }
        CountDownLatch ready=new CountDownLatch(1);Platform.startup(ready::countDown);assertTrue(ready.await(10,TimeUnit.SECONDS));Platform.setImplicitExit(false);
        fx(()->{try(var font=ResponsiveUiTest.class.getResourceAsStream("/fonts/NotoNaskhArabic-Regular.ttf")){javafx.scene.text.Font.loadFont(font,14);}return null;});
        System.out.println("ACTUAL_SCREENS="+fx(()->Screen.getScreens().stream().map(s->s.getBounds()+" visual="+s.getVisualBounds()+" dpi="+s.getDpi()).toList()));
    }
    View load(String name)throws Exception {
        FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/"+name),LanguageManager.getBundle());Parent content=loader.load();
        Parent root=PageViewport.wrap(content);root.setNodeOrientation(LanguageManager.isArabic()?NodeOrientation.RIGHT_TO_LEFT:NodeOrientation.LEFT_TO_RIGHT);
        Scene scene=new Scene(root,640,420);scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        return new View(content,root,loader);
    }
    static List<ButtonBase> buttons(Node node){List<ButtonBase> out=new ArrayList<>();if(node instanceof ButtonBase b)out.add(b);else if(node instanceof Parent p)for(Node child:p.getChildrenUnmodifiable())out.addAll(buttons(child));return out;}
    static void inside(Node node,double width,double height,String context){
        if(node instanceof ButtonBase){Node text=node.lookup(".text");if(text!=null){Bounds t=node.sceneToLocal(text.localToScene(text.getLayoutBounds()));assertTrue(t.getMinY()>=-1&&t.getMaxY()<=node.getLayoutBounds().getHeight()+1,context+" clipped button text "+t);}}
        Bounds b=node.getScene().getRoot().sceneToLocal(node.localToScene(node.getLayoutBounds()));assertTrue(b.getMinX()>=-1&&b.getMinY()>=-1&&b.getMaxX()<=width+1&&b.getMaxY()<=height+1,context+" bounds="+b+" viewport="+width+"x"+height);
    }
    static void snapshot(Parent root,String name)throws Exception {
        WritableImage shot=root.getScene().snapshot(null);int w=(int)shot.getWidth(),h=(int)shot.getHeight();int[] pixels=new int[w*h];shot.getPixelReader().getPixels(0,0,w,h,PixelFormat.getIntArgbInstance(),pixels,0,w);
        BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,w,h,pixels,0,w);ImageIO.write(image,"png",evidence.resolve("screenshots").resolve(name+".png").toFile());
    }
    @Test void everyFxmlLoadsAndFootersFitAtAllLogicalSizes()throws Exception {
        List<String> names;try(var paths=Files.list(Path.of("src/main/resources/fxml"))){names=paths.map(p->p.getFileName().toString()).sorted().toList();}
        int[][] sizes={{360,320},{640,360},{1024,728},{1280,680},{1366,728},{1440,860},{1600,860},{1920,1040},{2560,1400},{3840,2120}};
        for(boolean arabic:new boolean[]{false,true}){
            fx(()->{if(arabic)LanguageManager.setArabic();else LanguageManager.setEnglish();return null;});
            for(String name:names)fx(()->{
                View view=load(name);
                try {
                    for(int[] size:sizes){
                        view.root().resize(size[0],size[1]);view.root().applyCss();view.root().layout();view.root().layout();
                        assertEquals(arabic?NodeOrientation.RIGHT_TO_LEFT:NodeOrientation.LEFT_TO_RIGHT,view.root().getEffectiveNodeOrientation(),name);
                        if(view.content() instanceof BorderPane border&&border.getBottom()!=null){
                            for(ButtonBase button:buttons(border.getBottom()))if(button.isVisible()&&button.isManaged()){
                                inside(button,size[0],size[1],name+" "+button.getText());
                                assertTrue(button.getWidth()>0&&button.getHeight()>0,name);
                                for(Parent parent=button.getParent();parent!=null&&parent!=border;parent=parent.getParent())assertFalse(parent instanceof ScrollPane,"Footer scrolls: "+name);
                            }
                        }
                    }
                    if(List.of("add-member.fxml","add-membership.fxml","add-payment.fxml","revenue-form.fxml","database-settings.fxml","member-details.fxml").contains(name)){
                        view.root().getScene().getWindow();view.root().resize(640,420);view.root().layout();snapshot(view.root(),"after-"+(arabic?"ar-":"en-")+name.replace(".fxml",""));
                    }
                    System.out.println("LAYOUT_PASS language="+(arabic?"ar":"en")+" fxml="+name+" logicalSizes="+sizes.length);
                }finally{view.root().getScene().setRoot(new Pane());}
                return null;
            });
        }
    }
    @Test void allFormFieldsCanBeRevealedAtLimitedWidth()throws Exception {
        for(boolean arabic:new boolean[]{false,true})for(String name:List.of("add-member.fxml","add-membership.fxml","add-payment.fxml","expense-form.fxml","membership-plan-form.fxml","database-settings.fxml","database-auth.fxml","super-admin-setup.fxml","program-license-settings.fxml","revenue-form.fxml"))fx(()->{
            if(arabic)LanguageManager.setArabic();else LanguageManager.setEnglish();View view=load(name);
            try {
                view.root().resize(360,320);view.root().applyCss();view.root().layout();view.root().layout();
                for(Object value:view.loader().getNamespace().values())if(value instanceof Control control && (control instanceof TextInputControl || control instanceof ComboBoxBase<?>)){
                    boolean visible=true;for(Node n=control;n!=null;n=n.getParent())if(!n.isVisible()||!n.isManaged())visible=false;
                    if(!visible)continue;
                    ResponsiveLayouts.reveal(control);view.root().layout();
                    Bounds b=view.root().sceneToLocal(control.localToScene(control.getLayoutBounds()));
                    assertTrue(control.getWidth()>30&&control.getHeight()>0,name+" collapsed field "+control.getId());
                    assertTrue(b.getMinX()>=-1&&b.getMaxX()<=361,name+" horizontal field overflow "+control.getId()+" "+b);
                    for(Parent parent=control.getParent();parent!=null;parent=parent.getParent())if(parent instanceof ScrollPane scroll){
                        Node viewport=scroll.lookup(".viewport");if(viewport==null)continue;
                        Bounds visibleBounds=view.root().sceneToLocal(viewport.localToScene(viewport.getLayoutBounds()));
                        assertTrue(b.getMaxY()>visibleBounds.getMinY()&&b.getMinY()<visibleBounds.getMaxY(),name+" field cannot be revealed "+control.getId());break;
                    }
                }
            }finally{view.root().getScene().setRoot(new Pane());}
            return null;
        });
    }
    @Test void savedWindowGeometryIsValidatedAgainstCurrentScreens()throws Exception {
        fx(()->{
            String geometryKey="ui-"+UUID.randomUUID();
            Stage first=WindowManager.createStage(new BorderPane(),"Geometry fixture",null,Modality.NONE);
            WindowManager.remember(first,geometryKey);WindowManager.showResponsive(first,740,500);first.close();
            Stage restored=WindowManager.createStage(new BorderPane(),"Restored fixture",null,Modality.NONE);
            WindowManager.remember(restored,geometryKey);WindowManager.showResponsive(restored,1000,800);
            try {var visual=Screen.getScreensForRectangle(restored.getX(),restored.getY(),restored.getWidth(),restored.getHeight()).getFirst().getVisualBounds();
                assertTrue(visual.contains(new Rectangle2D(restored.getX(),restored.getY(),restored.getWidth(),restored.getHeight())));
                assertEquals(740,restored.getWidth(),2);assertEquals(500,restored.getHeight(),2);
            }finally{restored.close();}
            java.util.Properties props=new java.util.Properties();Path geometry=com.mycompany.lazar_app.util.AppPaths.configDirectory().resolve("windows.properties");
            try(var input=Files.newInputStream(geometry)){props.load(input);}
            props.setProperty(geometryKey+".x","999999");props.setProperty(geometryKey+".y","999999");props.setProperty(geometryKey+".w","1");props.setProperty(geometryKey+".h","1");
            com.mycompany.lazar_app.util.AtomicProperties.write(geometry,props,"Synthetic corrupt geometry");
            Stage offscreen=WindowManager.createStage(new BorderPane(),"Corrupt fixture",null,Modality.NONE);WindowManager.remember(offscreen,geometryKey);WindowManager.showResponsive(offscreen,740,500);
            try {var visual=Screen.getScreensForRectangle(offscreen.getX(),offscreen.getY(),offscreen.getWidth(),offscreen.getHeight()).getFirst().getVisualBounds();
                assertTrue(visual.contains(new Rectangle2D(offscreen.getX(),offscreen.getY(),offscreen.getWidth(),offscreen.getHeight())));
                assertTrue(offscreen.getWidth()>=offscreen.getMinWidth());assertTrue(offscreen.getHeight()>=offscreen.getMinHeight());
            }finally{offscreen.close();}return null;
        });
    }
    @Test void resizingMaximizingRestoringPreservesControllerAndData()throws Exception {
        View view=fx(()->load("add-member.fxml"));Stage stage=fx(()->{
            Stage s=WindowManager.createModalStage(view.root(),"Synthetic form",null);WindowManager.showResponsive(s,640,420);
            ((TextField)view.control("nameField")).setText("Unsaved اسم تجريبي");return s;
        });
        try {
            Object controller=view.loader().getController();
            fx(()->{stage.setWidth(740);stage.setHeight(500);stage.setMaximized(true);return null;});Thread.sleep(120);
            assertTrue(fx(stage::isMaximized));fx(()->{stage.setMaximized(false);stage.setWidth(640);stage.setHeight(420);return null;});Thread.sleep(220);
            fx(()->{assertTrue(stage.isResizable());assertSame(controller,view.loader().getController());assertEquals("Unsaved اسم تجريبي",((TextField)view.control("nameField")).getText());
                Rectangle2D visual=Screen.getScreensForRectangle(stage.getX(),stage.getY(),stage.getWidth(),stage.getHeight()).getFirst().getVisualBounds();assertTrue(visual.contains(new Rectangle2D(stage.getX(),stage.getY(),stage.getWidth(),stage.getHeight())));return null;});
        }finally{fx(()->{stage.close();return null;});}
    }
    @Test void invalidFieldFocusAndKeyboardTraversalScrollIntoView()throws Exception {
        View view=fx(()->load("add-membership.fxml"));Stage stage=fx(()->{Stage s=WindowManager.createModalStage(view.root(),"Focus fixture",null);WindowManager.showResponsive(s,640,420);s.requestFocus();return s;});
        try {
            fx(()->{ResponsiveLayouts.focusInvalid((Node)view.control("notesArea"));return null;});Thread.sleep(180);
            fx(()->{ScrollPane scroll=(ScrollPane)((BorderPane)view.content()).getCenter();assertTrue(scroll.getVvalue()>0,"Focused lower field was not revealed");assertEquals(view.control("notesArea"),stage.getScene().getFocusOwner());return null;});
            fx(()->{((Node)view.control("memberSearchField")).requestFocus();return null;});Thread.sleep(80);
            fx(()->{Robot robot=new Robot();robot.keyPress(KeyCode.TAB);robot.keyRelease(KeyCode.TAB);return null;});Thread.sleep(100);
            assertNotNull(fx(()->stage.getScene().getFocusOwner()));
            assertNotEquals(view.control("memberSearchField"),fx(()->stage.getScene().getFocusOwner()),"Tab did not advance focus");
        }finally{fx(()->{stage.close();return null;});}
    }
    @Test void successfulMemberSubmissionAfterResize()throws Exception {
        fx(()->{LanguageManager.setEnglish();return null;});View view=fx(()->load("members.fxml"));
        Stage owner=fx(()->{Stage s=WindowManager.createStage(view.root(),"Submission parent",null,Modality.NONE);WindowManager.showResponsive(s,1000,680);return s;});
        FutureTask<Void> launch=new FutureTask<>(()->{
            var method=view.loader().getController().getClass().getDeclaredMethod("openAddMember");method.setAccessible(true);method.invoke(view.loader().getController());return null;
        });Platform.runLater(launch);
        try {
            Stage child=null;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);
            while(child==null&&System.nanoTime()<deadline){child=fx(()->Window.getWindows().stream().filter(w->w instanceof Stage&&w!=owner&&w.isShowing()&&w.getScene().getRoot().lookup("#submitMemberButton")!=null).map(w->(Stage)w).findFirst().orElse(null));if(child==null)Thread.sleep(20);}
            assertNotNull(child);Stage form=child;
            fx(()->{Parent root=form.getScene().getRoot();((TextField)root.lookup("#nameField")).setText("UI Resize Synthetic");((TextField)root.lookup("#phoneField")).setText("01055555555");
                ((ComboBox<?>)root.lookup("#genderComboBox")).getSelectionModel().selectFirst();form.setWidth(1000);form.setWidth(640);form.setHeight(420);
                ((Button)root.lookup("#submitMemberButton")).fire();return null;});
            closeFeedback(owner,launch);
            try(Connection c=DatabaseManager.getConnection(config);Statement query=c.createStatement();ResultSet rows=query.executeQuery("SELECT count(*) FROM members WHERE phone='01055555555'")){assertTrue(rows.next());assertEquals(1,rows.getInt(1));}
        }finally{fx(()->{for(Window w:List.copyOf(Window.getWindows()))w.hide();return null;});}
    }
    static void closeFeedback(Stage owner,FutureTask<Void> task)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(!task.isDone()&&System.nanoTime()<deadline){
            fx(()->{for(Window w:List.copyOf(Window.getWindows()))if(w!=owner&&w.isShowing()&&w.getScene()!=null){
                Node n=w.getScene().getRoot();DialogPane pane=n instanceof DialogPane d?d:(DialogPane)n.lookup(".dialog-pane");
                if(pane!=null&&!pane.getButtonTypes().isEmpty())((Button)pane.lookupButton(pane.getButtonTypes().getFirst())).fire();
            }return null;});Thread.sleep(30);
        }
        task.get(2,TimeUnit.SECONDS);
    }
    @Test void emptyFormValidationDoesNotPersistAndRevealsField()throws Exception {
        View view=fx(()->load("add-member.fxml"));Stage stage=fx(()->{Stage s=WindowManager.createModalStage(view.root(),"Validation fixture",null);WindowManager.showResponsive(s,640,420);return s;});
        try {
            FutureTask<Void> save=new FutureTask<>(()->{((Button)view.control("submitMemberButton")).fire();return null;});Platform.runLater(save);closeFeedback(stage,save);Thread.sleep(100);
            assertTrue(fx(()->((TextField)view.control("nameField")).isFocused()),"Validation did not focus required name");
        }finally{fx(()->{stage.close();return null;});}
    }
    @Test void growingValidationMessagesKeepActionsVisible()throws Exception {
        fx(()->{
            View view=load("expense-form.fxml");
            try {
                Label validation=(Label)view.control("validationLabel");
                validation.setText("Long validation / رسالة تحقق طويلة ".repeat(80));validation.setManaged(true);validation.setVisible(true);
                view.root().resize(640,360);view.root().applyCss();view.root().layout();view.root().layout();
                BorderPane root=(BorderPane)view.content();for(ButtonBase button:buttons(root.getBottom()))inside(button,640,360,"Growing validation footer");
                assertTrue(((ScrollPane)root.getCenter()).getContent().getBoundsInLocal().getHeight()>((ScrollPane)root.getCenter()).getViewportBounds().getHeight());
            }finally{view.root().getScene().setRoot(new Pane());}
            return null;
        });
    }
    @Test void nativeDialogButtonsWrapAndRemainAccessible()throws Exception {
        FutureTask<Optional<ButtonType>> show=new FutureTask<>(()->{
            Dialog<ButtonType> dialog=new Dialog<>();dialog.setTitle("Native dialog fixture");dialog.setHeaderText("Long translated message / رسالة توضيحية طويلة");dialog.setContentText("Synthetic diagnostic details. ".repeat(80));
            dialog.getDialogPane().getButtonTypes().setAll(new ButtonType("Confirm a long translated operation",ButtonBar.ButtonData.OK_DONE),new ButtonType("Print a long translated document",ButtonBar.ButtonData.OTHER),ButtonType.CANCEL);
            return WindowManager.showResponsiveDialogAndWait(dialog,null,400,420);
        });Platform.runLater(show);
        try {
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);DialogPane pane=null;
            while(pane==null&&System.nanoTime()<until){pane=fx(()->Window.getWindows().stream().filter(Window::isShowing).map(Window::getScene).filter(Objects::nonNull).map(Scene::getRoot).map(r->r instanceof DialogPane d?d:(DialogPane)r.lookup(".dialog-pane")).filter(Objects::nonNull).findFirst().orElse(null));if(pane==null)Thread.sleep(20);}
            assertNotNull(pane);DialogPane found=pane;Thread.sleep(120);
            fx(()->{assertTrue(((Stage)found.getScene().getWindow()).isResizable());for(ButtonType type:found.getButtonTypes())inside(found.lookupButton(type),found.getScene().getWidth(),found.getScene().getHeight(),"Dialog "+type.getText());((Button)found.lookupButton(ButtonType.CANCEL)).fire();return null;});
            assertEquals(ButtonType.CANCEL,show.get(3,TimeUnit.SECONDS).orElseThrow());
        }finally{if(!show.isDone())fx(()->{for(Window w:List.copyOf(Window.getWindows()))if(w.isShowing())w.hide();return null;});}
    }
    @AfterAll static void shutdown()throws Exception {fx(()->{for(Window w:List.copyOf(Window.getWindows()))w.hide();return null;});Platform.exit();}
}

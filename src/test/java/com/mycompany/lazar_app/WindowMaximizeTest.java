package com.mycompany.lazar_app;

import com.mycompany.lazar_app.ui.WindowManager;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.robot.Robot;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="gymflow.ui",matches="true")
class WindowMaximizeTest {
    private static <T>T fx(Callable<T> call)throws Exception {FutureTask<T> task=new FutureTask<>(call);Platform.runLater(task);return task.get(10,TimeUnit.SECONDS);}
    @BeforeAll static void ready()throws Exception {CountDownLatch ready=new CountDownLatch(1);Platform.startup(ready::countDown);assertTrue(ready.await(10,TimeUnit.SECONDS));Platform.setImplicitExit(false);}
    private void doubleClickTitle(Stage stage)throws Exception {
        fx(()->{stage.toFront();stage.requestFocus();Robot r=new Robot();r.mouseMove(stage.getX()+stage.getWidth()/2,stage.getY()+Math.max(8,stage.getScene().getY()/2));r.mouseClick(MouseButton.PRIMARY);return null;});
        Thread.sleep(85);
        fx(()->{new Robot().mouseClick(MouseButton.PRIMARY);return null;});
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value=Modality.class,names={"NONE","WINDOW_MODAL"})
    void nativeMaximizeAndRestoreChangeActualBoundsWithoutLosingInput(Modality modality)throws Exception {
        TextField input=fx(()->new TextField("Unsaved input نص غير محفوظ"));
        Stage owner=fx(()->{Stage s=WindowManager.createStage(new BorderPane(),"GymFlow test owner",null,Modality.NONE);WindowManager.showResponsive(s,900,650);return s;});
        Stage stage=fx(()->{Stage s=WindowManager.createStage(new BorderPane(input),"GymFlow maximize regression",modality==Modality.NONE?null:owner,modality);WindowManager.showResponsive(s,740,500);return s;});
        try {
            Thread.sleep(250);
            Rectangle2D normal=fx(()->new Rectangle2D(stage.getX(),stage.getY(),stage.getWidth(),stage.getHeight()));
            Rectangle2D visual=fx(()->Screen.getScreensForRectangle(stage.getX(),stage.getY(),stage.getWidth(),stage.getHeight()).getFirst().getVisualBounds());
            if(modality==Modality.NONE)doubleClickTitle(stage);
            else fx(()->{stage.setMaximized(true);return null;});
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
            while(!fx(()->stage.getWidth()>=visual.getWidth()-24&&stage.getHeight()>=visual.getHeight()-48)&&System.nanoTime()<until)Thread.sleep(40);
            fx(()->{System.out.println("MAXIMIZE actual="+stage.getWidth()+"x"+stage.getHeight()+" visual="+visual+" flag="+stage.isMaximized());
                assertTrue(stage.isMaximized(),"Native title-bar maximize was rejected");
                assertTrue(stage.getWidth()>=visual.getWidth()-24,"Maximize flag changed without expanding window width");
                assertTrue(stage.getHeight()>=visual.getHeight()-48,"Maximize flag changed without expanding window height");return null;});
            fx(()->{stage.setMaximized(false);return null;});until=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
            while(fx(()->stage.isMaximized()||Math.abs(stage.getWidth()-normal.getWidth())>8||Math.abs(stage.getHeight()-normal.getHeight())>8)&&System.nanoTime()<until)Thread.sleep(40);
            fx(()->{assertFalse(stage.isMaximized());assertEquals(normal.getWidth(),stage.getWidth(),8);assertEquals(normal.getHeight(),stage.getHeight(),8);assertEquals("Unsaved input نص غير محفوظ",input.getText());return null;});
        }finally{fx(()->{stage.close();owner.close();return null;});}
    }
    @Test
    void managerMaximizedEntryPointStartsAtWorkAreaAndKeepsRestoreAvailable()throws Exception {
        Stage stage=fx(()->WindowManager.createStage(new BorderPane(new TextField("Unsaved")),"GymFlow managed maximize",null,Modality.NONE));
        try {
            fx(()->{WindowManager.showMaximized(stage);return null;});
            Rectangle2D visual=fx(()->Screen.getScreensForRectangle(stage.getX(),stage.getY(),stage.getWidth(),stage.getHeight()).getFirst().getVisualBounds());
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
            while(!fx(()->stage.isMaximized()&&stage.getWidth()>=visual.getWidth()-24&&stage.getHeight()>=visual.getHeight()-48)&&System.nanoTime()<until)Thread.sleep(40);
            fx(()->{assertTrue(stage.isShowing());assertTrue(stage.isResizable());assertTrue(stage.isMaximized());assertTrue(stage.getWidth()>=visual.getWidth()-24);assertTrue(stage.getHeight()>=visual.getHeight()-48);return null;});
            fx(()->{stage.setMaximized(false);return null;});
            until=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
            while(fx(stage::isMaximized)&&System.nanoTime()<until)Thread.sleep(40);
            fx(()->{assertFalse(stage.isMaximized());assertTrue(stage.isResizable());return null;});
        } finally {fx(()->{stage.close();return null;});}
    }
    @AfterAll static void stop(){Platform.exit();}
}

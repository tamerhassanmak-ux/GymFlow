package com.mycompany.lazar_app.ui;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.util.AppPaths;
import com.mycompany.lazar_app.util.AtomicProperties;
import com.mycompany.lazar_app.ui.dialog.DialogStyler;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;
import java.util.*;
import java.nio.file.*;

/** Single policy for forms, main windows and native dialogs. Sizes are JavaFX logical units. */
public final class WindowManager {
    public static final double STANDARD_DIALOG_WIDTH=960, STANDARD_DIALOG_HEIGHT=680;
    private static final String STATE="gymflow.window.state";
    private WindowManager() {}
    public static Stage createStage(Parent root,String title,Window owner,Modality modality) {
        orient(root);ResponsiveLayouts.prepare(root);
        Stage stage=new Stage();stage.setTitle(title==null?"":title);
        if(owner!=null)stage.initOwner(owner);
        stage.initModality(owner==null && modality==Modality.WINDOW_MODAL?Modality.APPLICATION_MODAL:modality);
        Scene scene=root.getScene();
        if(scene==null || scene.getRoot()!=root)scene=new Scene(root);
        else if(scene.getWindow()!=null)throw new IllegalArgumentException("Root already belongs to a window.");
        css(scene);stage.setScene(scene);stage.setResizable(true);return stage;
    }
    public static Stage createModalStage(Parent root,String title,Window owner){return createStage(root,title,owner,owner==null?Modality.APPLICATION_MODAL:Modality.WINDOW_MODAL);}
    public static void showResponsiveAndWait(Stage stage,double width,double height){configure(stage,width,height);stage.showAndWait();}
    public static void showResponsive(Stage stage){showResponsive(stage,STANDARD_DIALOG_WIDTH,STANDARD_DIALOG_HEIGHT);}
    public static void showResponsive(Stage stage,double width,double height){configure(stage,width,height);stage.show();}
    /** Shows a form at the native work-area size while retaining the native restore control. */
    public static void showMaximizedAndWait(Stage stage){showMaximizedAndWait(stage,STANDARD_DIALOG_WIDTH,STANDARD_DIALOG_HEIGHT);}
    public static void showMaximizedAndWait(Stage stage,double width,double height){
        configure(stage,width,height);
        maximizeWhenShown(stage);
        stage.showAndWait();
    }
    public static void showMaximized(Stage stage){
        configure(stage,1200,800);
        maximizeWhenShown(stage);
        stage.show();
    }
    private static void maximizeWhenShown(Stage stage){
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN,new javafx.event.EventHandler<>(){
            @Override public void handle(WindowEvent event){
                stage.removeEventHandler(WindowEvent.WINDOW_SHOWN,this);
                Platform.runLater(()->{if(stage.isShowing())stage.setMaximized(true);});
            }
        });
    }

    public static void configure(Stage stage,double preferredWidth,double preferredHeight) {
        Objects.requireNonNull(stage);if(stage.getScene()==null)return;
        orient(stage.getScene().getRoot());ResponsiveLayouts.prepare(stage.getScene().getRoot());css(stage.getScene());
        Managed state=(Managed)stage.getProperties().get(STATE);
        if(state==null){state=new Managed(stage);stage.getProperties().put(STATE,state);}
        Rectangle2D screen=visualFor(stage.getOwner()!=null?stage.getOwner():stage);
        double width=Math.min(preferredWidth,Math.max(1,screen.getWidth()-16));
        double height=Math.min(preferredHeight,Math.max(1,screen.getHeight()-16));
        Window owner=stage.getOwner();
        double x=owner!=null&&owner.isShowing()?owner.getX()+(owner.getWidth()-width)/2:screen.getMinX()+(screen.getWidth()-width)/2;
        double y=owner!=null&&owner.isShowing()?owner.getY()+(owner.getHeight()-height)/2:screen.getMinY()+(screen.getHeight()-height)/2;
        Rectangle2D desired=new Rectangle2D(x,y,Math.max(1,width),Math.max(1,height));
        if(state.restored!=null){desired=state.restored;screen=WindowGeometry.screenFor(desired,screens());state.restored=null;}
        stage.setResizable(true);state.limits(screen);
        desired=new Rectangle2D(desired.getMinX(),desired.getMinY(),Math.max(stage.getMinWidth(),desired.getWidth()),Math.max(stage.getMinHeight(),desired.getHeight()));
        apply(stage,WindowGeometry.fit(desired,screen));
        state.normal=WindowGeometry.fit(desired,screen);
    }
    /** Geometry is persisted only for explicitly named management windows, never member-specific titles. */
    public static void remember(Stage stage,String key) {
        if(!key.matches("[A-Za-z0-9._-]{1,64}"))throw new IllegalArgumentException("Stable window key required.");
        Managed state=(Managed)stage.getProperties().get(STATE);
        if(state==null){state=new Managed(stage);stage.getProperties().put(STATE,state);}
        state.key=key;
        try {
            Properties p=new Properties();Path file=geometryFile();if(Files.exists(file))try(var in=Files.newInputStream(file)){p.load(in);}
            if(p.containsKey(key+".w"))state.restored=new Rectangle2D(Double.parseDouble(p.getProperty(key+".x")),Double.parseDouble(p.getProperty(key+".y")),
                Math.max(1,Double.parseDouble(p.getProperty(key+".w"))),Math.max(1,Double.parseDouble(p.getProperty(key+".h"))));
        }catch(Exception ignored){state.restored=null;}
    }
    public static void enableFullScreen(Stage stage){
        if(stage.getProperties().putIfAbsent("gymflow.fullscreen",true)!=null)return;
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED,event->{if(event.getCode()==KeyCode.F11){stage.setFullScreen(!stage.isFullScreen());event.consume();}});
    }
    private static Path geometryFile(){return AppPaths.configDirectory().resolve("windows.properties");}
    private static List<Rectangle2D> screens(){return Screen.getScreens().stream().map(Screen::getVisualBounds).toList();}
    private static Rectangle2D rect(Window w){return new Rectangle2D(Double.isFinite(w.getX())?w.getX():0,Double.isFinite(w.getY())?w.getY():0,
        Double.isFinite(w.getWidth())?Math.max(1,w.getWidth()):1,Double.isFinite(w.getHeight())?Math.max(1,w.getHeight()):1);}
    private static Rectangle2D visualFor(Window window){return window==null?Screen.getPrimary().getVisualBounds():WindowGeometry.screenFor(rect(window),screens());}
    private static void apply(Stage stage,Rectangle2D rect){
        if(Math.abs(stage.getWidth()-rect.getWidth())>.5||!Double.isFinite(stage.getWidth()))stage.setWidth(rect.getWidth());
        if(Math.abs(stage.getHeight()-rect.getHeight())>.5||!Double.isFinite(stage.getHeight()))stage.setHeight(rect.getHeight());
        if(Math.abs(stage.getX()-rect.getMinX())>.5||!Double.isFinite(stage.getX()))stage.setX(rect.getMinX());
        if(Math.abs(stage.getY()-rect.getMinY())>.5||!Double.isFinite(stage.getY()))stage.setY(rect.getMinY());
    }
    private static void css(Scene scene){var css=WindowManager.class.getResource("/css/app.css");if(css!=null&&!scene.getStylesheets().contains(css.toExternalForm()))scene.getStylesheets().add(css.toExternalForm());}
    private static void orient(Parent root){root.setNodeOrientation(LanguageManager.isArabic()?NodeOrientation.RIGHT_TO_LEFT:NodeOrientation.LEFT_TO_RIGHT);}
    private static final class Managed {
        final Stage stage;final PauseTransition settle=new PauseTransition(Duration.millis(300));
        Rectangle2D normal,restored;String key;boolean adjusting,watching;
        final ListChangeListener<Screen> monitors=change->Platform.runLater(this::clamp);
        Managed(Stage stage){
            this.stage=stage;settle.setOnFinished(event->clamp());
            javafx.beans.InvalidationListener moved=obs->{if(!adjusting&&stage.isShowing())settle.playFromStart();};
            stage.xProperty().addListener(moved);stage.yProperty().addListener(moved);stage.widthProperty().addListener(moved);stage.heightProperty().addListener(moved);
            stage.outputScaleXProperty().addListener(moved);stage.outputScaleYProperty().addListener(moved);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN,event->{if(!watching){Screen.getScreens().addListener(monitors);watching=true;}Platform.runLater(this::clamp);});
            stage.addEventHandler(WindowEvent.WINDOW_HIDDEN,event->{settle.stop();if(watching){Screen.getScreens().removeListener(monitors);watching=false;}save();});
        }
        void limits(Rectangle2D visual){
            stage.setMinWidth(Math.min(360,visual.getWidth()));stage.setMinHeight(Math.min(240,visual.getHeight()));
            // Native window managers own maximized geometry, including decoration extents.
            // Finite maximum-size hints can disable native maximize/restore on GTK/Windows.
            stage.setMaxWidth(Double.MAX_VALUE);stage.setMaxHeight(Double.MAX_VALUE);
        }
        void clamp(){
            if(!stage.isShowing() || stage.isMaximized() || stage.isFullScreen())return;
            adjusting=true;
            try {
                var screen=visualFor(stage);limits(screen);
                if(!stage.isMaximized()&&!stage.isFullScreen()){var fit=WindowGeometry.fit(rect(stage),screen);apply(stage,fit);normal=fit;}
                if(stage.getScene().getRoot() instanceof Region region)region.requestLayout();
            }finally{adjusting=false;}
        }
        void save(){
            if(key==null||normal==null)return;
            try {
                Properties p=new Properties();Path file=geometryFile();if(Files.exists(file))try(var in=Files.newInputStream(file)){p.load(in);}
                p.setProperty(key+".x",""+normal.getMinX());p.setProperty(key+".y",""+normal.getMinY());p.setProperty(key+".w",""+normal.getWidth());p.setProperty(key+".h",""+normal.getHeight());
                AtomicProperties.write(file,p,"GymFlow window geometry (logical coordinates)");
            }catch(Exception ignored){System.getLogger(WindowManager.class.getName()).log(System.Logger.Level.WARNING,"Window geometry could not be saved.");}
        }
    }
    public static <R> Optional<R> showResponsiveDialogAndWait(Dialog<R> dialog,Window owner,double width,double height){
        if(dialog==null)return Optional.empty();
        if(owner!=null&&dialog.getOwner()==null)dialog.initOwner(owner);
        DialogPane pane=dialog.getDialogPane();DialogStyler.style(dialog);orient(pane);
        pane.getStyleClass().add("gymflow-responsive-dialog");pane.setMinSize(0,0);pane.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        if(dialog instanceof Alert alert&&pane.getContent()==null){
            Label message=new Label();message.setWrapText(true);message.textProperty().bind(alert.contentTextProperty());pane.setContent(message);
        }
        Node content=pane.getContent();if(content!=null){ResponsiveLayouts.adapt(content);if(!(content instanceof ScrollPane))pane.setContent(ResponsiveLayouts.scroll(content));}
        var visual=visualFor(dialog.getOwner());pane.setPrefSize(Math.min(width,visual.getWidth()-16),Math.min(height,visual.getHeight()-48));
        dialog.setResizable(true);
        javafx.event.EventHandler<DialogEvent> shown=event->{
            pane.applyCss();
            if(pane.getScene()!=null&&pane.getScene().getWindow() instanceof Stage stage){
                configure(stage,width,height);ResponsiveLayouts.installFocusReveal(pane.getScene());
                for(Node node:pane.lookupAll(".button-bar"))if(node instanceof ButtonBar bar)bar.setSkin(new WrappingButtonBarSkin(bar));
            }
        };
        dialog.addEventHandler(DialogEvent.DIALOG_SHOWN,shown);
        try{return dialog.showAndWait();}finally{dialog.removeEventHandler(DialogEvent.DIALOG_SHOWN,shown);}
    }
}

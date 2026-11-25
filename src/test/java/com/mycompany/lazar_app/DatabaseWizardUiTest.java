package com.mycompany.lazar_app;

import com.mycompany.lazar_app.database.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="gymflow.ui", matches="true")
class DatabaseWizardUiTest {
    @BeforeAll static void toolkit() throws Exception {
        CountDownLatch ready=new CountDownLatch(1); Platform.startup(ready::countDown);
        assertTrue(ready.await(10,TimeUnit.SECONDS)); Platform.setImplicitExit(false);
    }
    private static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task=new FutureTask<>(action);Platform.runLater(task);return task.get(8,TimeUnit.SECONDS);
    }
    private record Wizard(Parent root, Map<String,Object> controls) {
        Object lookup(String selector) { return controls.get(selector.substring(1)); }
    }
    private Wizard load(Locale locale) throws Exception {
        FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/database-settings.fxml"), ResourceBundle.getBundle("i18n.messages",locale));
        Parent root=loader.load();new Scene(root);
        return new Wizard(root,new HashMap<>(loader.getNamespace()));
    }
    @Test void bilingualWizardLoads() throws Exception {
        for(String language:List.of("en","ar")) fx(() -> {
            Wizard root=load(Locale.forLanguageTag(language));
            assertNotNull(root.lookup("#trustStoreField"));assertNotNull(root.lookup("#platformCredentialCheckBox"));
            assertNotNull(root.lookup("#testButton"));assertNotNull(root.lookup("#saveButton")); return null;
        });
    }
    @Test void stalledMysqlHandshakeDoesNotBlockFxThread() throws Exception {
        try(ServerSocket listener=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))) {
            listener.setSoTimeout(10000);
            Wizard root=fx(() -> load(Locale.ENGLISH));
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
            while(fx(() -> ((Button)root.lookup("#testButton")).isDisabled()) && System.nanoTime()<until)Thread.sleep(20);
            fx(() -> {
                ((ComboBox<DatabaseType>)root.lookup("#databaseTypeCombo")).setValue(DatabaseType.MYSQL);
                ((TextField)root.lookup("#hostField")).setText("127.0.0.1");
                ((TextField)root.lookup("#portField")).setText(""+listener.getLocalPort());
                ((TextField)root.lookup("#databaseNameField")).setText("fixture");
                ((Button)root.lookup("#testButton")).fire();return null;
            });
            try(Socket accepted=listener.accept()) {
                CountDownLatch responsive=new CountDownLatch(1);Platform.runLater(responsive::countDown);
                assertTrue(responsive.await(1,TimeUnit.SECONDS),"Network work blocked JavaFX");
                assertTrue(fx(() -> ((Button)root.lookup("#testButton")).isDisabled()));
            }
        }
    }
    @Test void failedStartupShowsRecoveryWithoutCreatingReplacementDatabase() throws Exception {
        java.nio.file.Path file=DatabaseConfigManager.getConfigFile();
        byte[] original=java.nio.file.Files.exists(file)?java.nio.file.Files.readAllBytes(file):null;
        boolean defaultExisted=java.nio.file.Files.exists(com.mycompany.lazar_app.util.AppPaths.databaseFile());
        javafx.stage.Stage stage=fx(javafx.stage.Stage::new);
        try {
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.writeString(file,"type=MYSQL\nhost=remote.example.org\nport=3306\ndatabaseName=gymflow\nencryptConnection=false\n");
            fx(()->{new Lazar_APP().start(stage);return null;});
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
            while(!fx(()->stage.getTitle().contains("Database recovery")) && System.nanoTime()<until)Thread.sleep(20);
            assertTrue(fx(()->stage.isShowing() && stage.getTitle().contains("Database recovery")));
            fx(()->{
                stage.setWidth(360);stage.setHeight(360);var root=(javafx.scene.layout.BorderPane)stage.getScene().getRoot();root.applyCss();root.layout();root.layout();
                var footer=(javafx.scene.layout.Pane)root.getBottom();
                assertEquals(5,footer.getChildren().size(),"Recovery must use one shared footer");
                for(var button:footer.getChildren()){
                    var bounds=root.sceneToLocal(button.localToScene(button.getLayoutBounds()));
                    assertTrue(bounds.getMinX()>=-1&&bounds.getMaxX()<=root.getWidth()+1&&bounds.getMinY()>=-1&&bounds.getMaxY()<=root.getHeight()+1,"Hidden recovery action: "+bounds);
                }return null;
            });
            if(!defaultExisted)assertFalse(java.nio.file.Files.exists(com.mycompany.lazar_app.util.AppPaths.databaseFile()));
        } finally {
            fx(()->{stage.close();return null;});
            if(original==null)java.nio.file.Files.deleteIfExists(file);else java.nio.file.Files.write(file,original);
        }
    }
    @AfterAll static void stop() { Platform.exit(); }
}

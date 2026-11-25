import java.nio.file.*;
import java.net.*;
import java.util.*;

/** Test-only native launcher. Uses the application package's exact runtime and released JAR. */
public final class UiQaLauncher {
    public static void main(String[] args)throws Exception {
        Path directory=Path.of(UiQaLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        List<URL> jars=new ArrayList<>();
        try(var files=Files.walk(directory)){for(Path file:files.filter(p->p.toString().endsWith(".jar")).toList())jars.add(file.toUri().toURL());}
        try(var loader=new URLClassLoader("gymflow-ui-qa",jars.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())){
            Thread.currentThread().setContextClassLoader(loader);
            Class.forName("org.junit.platform.console.ConsoleLauncher",true,loader).getMethod("main",String[].class).invoke(null,(Object)args);
        }
    }
}

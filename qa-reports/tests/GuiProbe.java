public class GuiProbe {
    public static void main(String[] args) {
        javafx.application.Platform.startup(() -> {System.out.println("JavaFX toolkit started"); javafx.application.Platform.exit();});
    }
}

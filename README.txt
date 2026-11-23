GymFlow central dialog fix
==========================
Replace WindowManager.java and AppDialog.java at the included paths.
The CSS file is a stylesheet FRAGMENT, not a replacement for existing app.css.
Append its contents to src/main/resources/css/app.css (only once).
Run: mvn clean compile && mvn javafx:run
Scope: JavaFX Dialog/Alert instances using WindowManager.showResponsiveDialogAndWait and AppDialog. Other Stage windows and controllers that call showAndWait directly require separate review.

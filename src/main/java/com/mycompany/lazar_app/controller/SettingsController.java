package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.database.DatabaseConfig;
import com.mycompany.lazar_app.database.DatabaseConfigManager;
import com.mycompany.lazar_app.database.DatabaseType;
import com.mycompany.lazar_app.settings.AppSettings;
import com.mycompany.lazar_app.settings.AppSettingsManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class SettingsController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Button saveChangesButton;

    @FXML private Label generalTitleLabel;
    @FXML private Label gymNameLabel;
    @FXML private TextField gymNameField;
    @FXML private Label phoneLabel;
    @FXML private TextField phoneField;
    @FXML private Label emailLabel;
    @FXML private TextField emailField;
    @FXML private Label addressLabel;
    @FXML private TextField addressField;

    @FXML private Label regionalTitleLabel;
    @FXML private Label languageLabel;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private Label currencyLabel;
    @FXML private ComboBox<String> currencyComboBox;
    @FXML private Label dateFormatLabel;
    @FXML private ComboBox<String> dateFormatComboBox;
    @FXML private Label timeZoneLabel;
    @FXML private ComboBox<String> timeZoneComboBox;

    @FXML private Label membershipSettingsTitleLabel;
    @FXML private Label defaultMembershipLabel;
    @FXML private ComboBox<String> defaultMembershipComboBox;
    @FXML private Label gracePeriodLabel;
    @FXML private TextField gracePeriodField;
    @FXML private CheckBox autoExpireCheckBox;
    @FXML private CheckBox renewBeforeExpiryCheckBox;

    @FXML private Label notificationsTitleLabel;
    @FXML private CheckBox expiryAlertsCheckBox;
    @FXML private CheckBox paymentRemindersCheckBox;
    @FXML private CheckBox newMemberNotificationCheckBox;
    @FXML private CheckBox expenseNotificationsCheckBox;
    @FXML private Label expiryWarningLabel;
    @FXML private TextField expiryWarningField;

    @FXML private Label appearanceTitleLabel;
    @FXML private Label darkModeLabel;
    @FXML private Label darkModeDescriptionLabel;
    @FXML private ToggleButton darkModeToggle;

    @FXML private Label securityTitleLabel;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button changePasswordButton;

    @FXML private Label backupTitleLabel;
    @FXML private Label lastBackupLabel;
    @FXML private Button backupDatabaseButton;
    @FXML private Button restoreBackupButton;
    @FXML private Button databaseConfigurationButton;

    private AppSettings settings;

    @FXML
    public void initialize() {
        setupComboBoxes();
        settings = AppSettingsManager.load();
        loadSettingsIntoForm();
        configureActions();
        applyLanguageFromSettings();
        applyTheme(settings.isDarkMode());
        updateLanguage();
        updateLastBackupLabel();
    }

    private void setupComboBoxes() {
        languageComboBox.getItems().setAll("English", "العربية");
        currencyComboBox.getItems().setAll("EGP", "USD", "SAR");
        dateFormatComboBox.getItems().setAll("DD/MM/YYYY", "MM/DD/YYYY", "YYYY-MM-DD");
        timeZoneComboBox.getItems().setAll("Africa/Cairo", "Asia/Riyadh", "UTC");
        defaultMembershipComboBox.getItems().setAll("Monthly", "3 Months", "6 Months", "Yearly");
    }

    private void configureActions() {
        saveChangesButton.setOnAction(e -> saveChanges());
        darkModeToggle.setOnAction(e -> {
            applyTheme(darkModeToggle.isSelected());
            updateLanguage();
        });
        backupDatabaseButton.setOnAction(e -> backupDatabase());
        restoreBackupButton.setOnAction(e -> restoreDatabase());
        changePasswordButton.setOnAction(e -> showPasswordManagementInfo());

        if (databaseConfigurationButton != null) {
            databaseConfigurationButton.setOnAction(e -> openDatabaseAuthentication());
        }
    }

    private void loadSettingsIntoForm() {
        gymNameField.setText(settings.getGymName());
        phoneField.setText(settings.getPhone());
        emailField.setText(settings.getEmail());
        addressField.setText(settings.getAddress());

        languageComboBox.setValue(settings.getLanguage());
        currencyComboBox.setValue(settings.getCurrency());
        dateFormatComboBox.setValue(settings.getDateFormat());
        timeZoneComboBox.setValue(settings.getTimeZone());

        defaultMembershipComboBox.setValue(settings.getDefaultMembership());
        gracePeriodField.setText(String.valueOf(settings.getGracePeriodDays()));
        autoExpireCheckBox.setSelected(settings.isAutoExpireMemberships());
        renewBeforeExpiryCheckBox.setSelected(settings.isRenewBeforeExpiry());

        expiryAlertsCheckBox.setSelected(settings.isExpiryAlerts());
        paymentRemindersCheckBox.setSelected(settings.isPaymentReminders());
        newMemberNotificationCheckBox.setSelected(settings.isNewMemberNotification());
        expenseNotificationsCheckBox.setSelected(settings.isExpenseNotifications());
        expiryWarningField.setText(String.valueOf(settings.getExpiryWarningDays()));

        darkModeToggle.setSelected(settings.isDarkMode());
    }

    private void saveChanges() {
        try {
            int grace = nonNegativeInteger(gracePeriodField, 0);
            int warning = nonNegativeInteger(expiryWarningField, 3);

            String email = text(emailField);
            if (!email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                show(Alert.AlertType.WARNING,
                        ar("تنبيه", "Validation"),
                        ar("يرجى إدخال بريد إلكتروني صحيح.", "Please enter a valid email address."));
                emailField.requestFocus();
                return;
            }

            settings.setGymName(text(gymNameField));
            settings.setPhone(text(phoneField));
            settings.setEmail(email);
            settings.setAddress(text(addressField));

            settings.setLanguage(value(languageComboBox, "English"));
            settings.setCurrency(value(currencyComboBox, "EGP"));
            settings.setDateFormat(value(dateFormatComboBox, "DD/MM/YYYY"));
            settings.setTimeZone(value(timeZoneComboBox, "Africa/Cairo"));

            settings.setDefaultMembership(value(defaultMembershipComboBox, "Monthly"));
            settings.setGracePeriodDays(grace);
            settings.setAutoExpireMemberships(autoExpireCheckBox.isSelected());
            settings.setRenewBeforeExpiry(renewBeforeExpiryCheckBox.isSelected());

            settings.setExpiryAlerts(expiryAlertsCheckBox.isSelected());
            settings.setPaymentReminders(paymentRemindersCheckBox.isSelected());
            settings.setNewMemberNotification(newMemberNotificationCheckBox.isSelected());
            settings.setExpenseNotifications(expenseNotificationsCheckBox.isSelected());
            settings.setExpiryWarningDays(warning);

            settings.setDarkMode(darkModeToggle.isSelected());

            AppSettingsManager.save(settings);
            applyLanguageFromSettings();
            applyTheme(settings.isDarkMode());
            updateLanguage();
            applyPageDirection();

            show(Alert.AlertType.INFORMATION,
                    ar("تم الحفظ", "Saved"),
                    ar("تم حفظ إعدادات GymFlow بنجاح.",
                       "GymFlow settings were saved successfully."));

        } catch (NumberFormatException ex) {
            show(Alert.AlertType.WARNING,
                    ar("بيانات غير صحيحة", "Invalid Data"),
                    ar("فترة السماح وأيام التنبيه يجب أن تكون أرقامًا صحيحة غير سالبة.",
                       "Grace period and warning days must be non-negative whole numbers."));
        } catch (Exception ex) {
            ex.printStackTrace();
            show(Alert.AlertType.ERROR,
                    ar("خطأ", "Error"),
                    ar("تعذر حفظ الإعدادات.", "Could not save settings."));
        }
    }

    private void applyLanguageFromSettings() {
        if ("العربية".equals(settings.getLanguage())) {
            LanguageManager.setArabic();
        } else {
            LanguageManager.setEnglish();
        }
        applyPageDirection();
    }

    private void applyPageDirection() {
        if (saveChangesButton == null || saveChangesButton.getScene() == null) return;
        Parent root = saveChangesButton.getScene().getRoot();
        root.setNodeOrientation(LanguageManager.isArabic()
                ? NodeOrientation.RIGHT_TO_LEFT
                : NodeOrientation.LEFT_TO_RIGHT);
    }

    private void applyTheme(boolean dark) {
        if (saveChangesButton == null || saveChangesButton.getScene() == null) return;
        Parent root = saveChangesButton.getScene().getRoot();
        root.getStyleClass().remove("dark-theme");
        if (dark) root.getStyleClass().add("dark-theme");
    }

    private void backupDatabase() {
        DatabaseConfig config = DatabaseConfigManager.load();
        if (config.getType() != DatabaseType.SQLITE) {
            show(Alert.AlertType.INFORMATION,
                    ar("النسخ الاحتياطي", "Backup"),
                    ar("النسخ الاحتياطي من داخل البرنامج متاح حاليًا لقاعدة SQLite فقط.",
                       "In-app backup currently supports SQLite only."));
            return;
        }

        Path source = Path.of(config.getSqlitePath()).toAbsolutePath().normalize();
        if (!Files.exists(source)) {
            show(Alert.AlertType.ERROR, ar("خطأ", "Error"),
                    ar("ملف قاعدة البيانات غير موجود.", "Database file was not found."));
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(ar("حفظ نسخة احتياطية", "Save Database Backup"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("SQLite Backup (*.db)", "*.db"));
        chooser.setInitialFileName("gymflow-backup-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + ".db");

        File target = chooser.showSaveDialog(owner());
        if (target == null) return;

        try {
            Files.copy(source, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            settings.setLastBackup(LocalDateTime.now().toString());
            AppSettingsManager.save(settings);
            updateLastBackupLabel();
            show(Alert.AlertType.INFORMATION, ar("نجاح", "Success"),
                    ar("تم إنشاء النسخة الاحتياطية بنجاح.",
                       "Database backup created successfully."));
        } catch (IOException ex) {
            ex.printStackTrace();
            show(Alert.AlertType.ERROR, ar("خطأ", "Error"),
                    ar("تعذر إنشاء النسخة الاحتياطية.", "Could not create database backup."));
        }
    }

    private void restoreDatabase() {
        DatabaseConfig config = DatabaseConfigManager.load();
        if (config.getType() != DatabaseType.SQLITE) {
            show(Alert.AlertType.INFORMATION, ar("الاستعادة", "Restore"),
                    ar("الاستعادة من داخل البرنامج متاحة حاليًا لقاعدة SQLite فقط.",
                       "In-app restore currently supports SQLite only."));
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(ar("اختيار نسخة احتياطية", "Select Database Backup"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("SQLite Database (*.db)", "*.db"));
        File source = chooser.showOpenDialog(owner());
        if (source == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(owner());
        confirm.setTitle(ar("تأكيد الاستعادة", "Confirm Restore"));
        confirm.setHeaderText(ar("سيتم استبدال قاعدة البيانات الحالية.",
                                 "The current database will be replaced."));
        confirm.setContentText(ar("يُنصح بعمل نسخة احتياطية أولًا. هل تريد المتابعة؟",
                                  "Create a backup first. Do you want to continue?"));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        Path target = Path.of(config.getSqlitePath()).toAbsolutePath().normalize();

        try {
            if (target.getParent() != null) Files.createDirectories(target.getParent());

            Path safety = target.resolveSibling(
                    "pre-restore-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) +
                    ".db");
            if (Files.exists(target)) {
                Files.copy(target, safety, StandardCopyOption.REPLACE_EXISTING);
            }

            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

            show(Alert.AlertType.INFORMATION,
                    ar("تمت الاستعادة", "Restore Complete"),
                    ar("تمت استعادة قاعدة البيانات. أغلق GymFlow وافتحه مرة أخرى قبل متابعة العمل.",
                       "Database restored. Close and reopen GymFlow before continuing."));
        } catch (IOException ex) {
            ex.printStackTrace();
            show(Alert.AlertType.ERROR, ar("خطأ", "Error"),
                    ar("تعذر استعادة قاعدة البيانات.", "Could not restore database."));
        }
    }

    private void updateLastBackupLabel() {
        String raw = settings == null ? "" : settings.getLastBackup();
        if (raw == null || raw.isBlank()) {
            lastBackupLabel.setText(ar("آخر نسخة احتياطية: لا توجد",
                                       "Last backup: None"));
            return;
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(raw);
            String formatted = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            lastBackupLabel.setText(ar("آخر نسخة احتياطية: " + formatted,
                                       "Last backup: " + formatted));
        } catch (Exception e) {
            lastBackupLabel.setText(ar("آخر نسخة احتياطية: " + raw,
                                       "Last backup: " + raw));
        }
    }

    private void showPasswordManagementInfo() {
        show(Alert.AlertType.INFORMATION,
                ar("الأمان", "Security"),
                ar("تغيير كلمة مرور السوبر أدمن يجب أن يتم من خلال خدمة SuperAdminAuthService بعد إضافة API مخصص لتغيير كلمة المرور. لم يتم تعديل بيانات الدخول بصورة غير آمنة.",
                   "Super Admin password changes must go through SuperAdminAuthService after adding a dedicated password-change API. Login credentials were not modified insecurely."));
    }

    private int nonNegativeInteger(TextField field, int fallback) {
        String s = text(field);
        if (s.isBlank()) return fallback;
        int v = Integer.parseInt(s);
        if (v < 0) throw new NumberFormatException();
        return v;
    }

    private String text(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private String value(ComboBox<String> box, String fallback) {
        return box == null || box.getValue() == null ? fallback : box.getValue();
    }

    private String ar(String arabic, String english) {
        return LanguageManager.isArabic() ? arabic : english;
    }

    private Window owner() {
        return saveChangesButton != null && saveChangesButton.getScene() != null
                ? saveChangesButton.getScene().getWindow()
                : null;
    }

    private void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        if (owner() != null) alert.initOwner(owner());
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void updateLanguage() {

        if (LanguageManager.isArabic()) {

            // Header
            titleLabel.setText("الإعدادات");
            subtitleLabel.setText(
                    "إدارة إعدادات التطبيق والصالة الرياضية"
            );
            saveChangesButton.setText("حفظ التغييرات");


            // General
            generalTitleLabel.setText("المعلومات العامة");

            gymNameLabel.setText("اسم الصالة الرياضية");
            gymNameField.setPromptText("أدخل اسم الصالة");

            phoneLabel.setText("رقم الهاتف");

            emailLabel.setText("البريد الإلكتروني");

            addressLabel.setText("العنوان");
            addressField.setPromptText("أدخل عنوان الصالة");


            // Regional
            regionalTitleLabel.setText("الإعدادات الإقليمية");

            languageLabel.setText("اللغة");
            currencyLabel.setText("العملة");
            dateFormatLabel.setText("تنسيق التاريخ");
            timeZoneLabel.setText("المنطقة الزمنية");


            // Membership
            membershipSettingsTitleLabel.setText(
                    "إعدادات العضويات"
            );

            defaultMembershipLabel.setText(
                    "العضوية الافتراضية"
            );

            gracePeriodLabel.setText("فترة السماح");
            gracePeriodField.setPromptText("أيام");

            autoExpireCheckBox.setText(
                    "تحديد العضويات المنتهية تلقائيًا"
            );

            renewBeforeExpiryCheckBox.setText(
                    "السماح بتجديد العضوية قبل انتهائها"
            );


            // Notifications
            notificationsTitleLabel.setText("الإشعارات");

            expiryAlertsCheckBox.setText(
                    "تنبيهات انتهاء العضويات"
            );

            paymentRemindersCheckBox.setText(
                    "تذكيرات الدفع"
            );

            newMemberNotificationCheckBox.setText(
                    "إشعار العضو الجديد"
            );

            expenseNotificationsCheckBox.setText(
                    "إشعارات المصروفات"
            );

            expiryWarningLabel.setText(
                    "تنبيه انتهاء العضوية"
            );

            expiryWarningField.setPromptText(
                    "قبل انتهاء العضوية بـ 3 أيام"
            );


            // Appearance
            appearanceTitleLabel.setText("المظهر");

            darkModeLabel.setText("الوضع الداكن");

            darkModeDescriptionLabel.setText(
                    "استخدام المظهر الداكن للتطبيق"
            );

            darkModeToggle.setText(
                    darkModeToggle.isSelected()
                            ? "تشغيل"
                            : "إيقاف"
            );


            // Security
            securityTitleLabel.setText("الأمان");

            currentPasswordField.setPromptText(
                    "كلمة المرور الحالية"
            );

            newPasswordField.setPromptText(
                    "كلمة المرور الجديدة"
            );

            confirmPasswordField.setPromptText(
                    "تأكيد كلمة المرور"
            );

            changePasswordButton.setText(
                    "تغيير كلمة المرور"
            );


            // Backup
            backupTitleLabel.setText(
                    "البيانات والنسخ الاحتياطي"
            );

            lastBackupLabel.setText(
                    "آخر نسخة احتياطية: اليوم، 10:30 ص"
            );

            backupDatabaseButton.setText(
                    "نسخ احتياطي لقاعدة البيانات"
            );

            restoreBackupButton.setText(
                    "استعادة النسخة الاحتياطية"
            );

            if (databaseConfigurationButton != null) {
                databaseConfigurationButton.setText("إعداد قاعدة البيانات 🔒");
            }

        } else {

            // Header
            titleLabel.setText("Settings");

            subtitleLabel.setText(
                    "Manage application and gym settings"
            );

            saveChangesButton.setText("Save Changes");


            // General
            generalTitleLabel.setText("General Information");

            gymNameLabel.setText("Gym Name");
            gymNameField.setPromptText("Enter gym name");

            phoneLabel.setText("Phone");

            emailLabel.setText("Email");

            addressLabel.setText("Address");
            addressField.setPromptText("Enter gym address");


            // Regional
            regionalTitleLabel.setText("Regional Settings");

            languageLabel.setText("Language");
            currencyLabel.setText("Currency");
            dateFormatLabel.setText("Date Format");
            timeZoneLabel.setText("Time Zone");


            // Membership
            membershipSettingsTitleLabel.setText(
                    "Membership Settings"
            );

            defaultMembershipLabel.setText(
                    "Default Membership"
            );

            gracePeriodLabel.setText("Grace Period");
            gracePeriodField.setPromptText("Days");

            autoExpireCheckBox.setText(
                    "Automatically mark expired memberships"
            );

            renewBeforeExpiryCheckBox.setText(
                    "Allow membership renewal before expiry"
            );


            // Notifications
            notificationsTitleLabel.setText("Notifications");

            expiryAlertsCheckBox.setText(
                    "Membership expiry alerts"
            );

            paymentRemindersCheckBox.setText(
                    "Payment reminders"
            );

            newMemberNotificationCheckBox.setText(
                    "New member notification"
            );

            expenseNotificationsCheckBox.setText(
                    "Expense notifications"
            );

            expiryWarningLabel.setText("Expiry Warning");

            expiryWarningField.setPromptText(
                    "3 days before expiry"
            );


            // Appearance
            appearanceTitleLabel.setText("Appearance");

            darkModeLabel.setText("Dark Mode");

            darkModeDescriptionLabel.setText(
                    "Use dark application theme"
            );

            darkModeToggle.setText(
                    darkModeToggle.isSelected()
                            ? "ON"
                            : "OFF"
            );


            // Security
            securityTitleLabel.setText("Security");

            currentPasswordField.setPromptText(
                    "Current Password"
            );

            newPasswordField.setPromptText(
                    "New Password"
            );

            confirmPasswordField.setPromptText(
                    "Confirm Password"
            );

            changePasswordButton.setText(
                    "Change Password"
            );


            // Backup
            backupTitleLabel.setText("Data & Backup");

            lastBackupLabel.setText(
                    "Last backup: Today, 10:30 AM"
            );

            backupDatabaseButton.setText(
                    "Backup Database"
            );

            restoreBackupButton.setText(
                    "Restore Backup"
            );

            if (databaseConfigurationButton != null) {
                databaseConfigurationButton.setText("Database Configuration 🔒");
            }
        }
    }


    private void openDatabaseAuthentication() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/database-auth.fxml")
            );

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(
                    LanguageManager.isArabic()
                            ? "التحقق من صلاحية السوبر أدمن"
                            : "Super Admin Authentication"
            );
            stage.initModality(Modality.APPLICATION_MODAL);

            Window owner = databaseConfigurationButton.getScene().getWindow();
            if (owner != null) stage.initOwner(owner);

            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.showAndWait();

        } catch (IOException exception) {
            exception.printStackTrace();
            show(Alert.AlertType.ERROR,
                    ar("خطأ", "Error"),
                    ar("تعذر فتح إعدادات قاعدة البيانات.",
                       "Could not open database configuration."));
        }
    }
}

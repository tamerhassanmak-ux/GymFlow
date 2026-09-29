package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.license.ProgramLicense;
import com.mycompany.lazar_app.license.ProgramLicenseManager;
import com.mycompany.lazar_app.license.ProgramLicenseStatus;
import com.mycompany.lazar_app.security.SuperAdminAuthService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ProgramLicenseSettingsController {

    private enum TermOption {
        ONE_MONTH,
        THREE_MONTHS,
        SIX_MONTHS,
        ONE_YEAR,
        CUSTOM,
        LIFETIME
    }

    @FXML private VBox rootContainer;

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label statusTitleLabel;
    @FXML private Label statusValueLabel;

    @FXML private Label termLabel;
    @FXML private ComboBox<TermOption> termComboBox;

    @FXML private Label fromLabel;
    @FXML private DatePicker fromDatePicker;

    @FXML private Label toLabel;
    @FXML private DatePicker toDatePicker;

    @FXML private Label passwordLabel;
    @FXML private PasswordField superAdminPasswordField;

    @FXML private Label messageLabel;

    @FXML private Button activateButton;
    @FXML private Button cancelButton;

    private boolean startupLockMode;
    private boolean activated;

    private final DateTimeFormatter displayDate =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {

        configureTermComboBox();
        loadCurrentLicense();
        updateLanguage();
        updateDateControls();
        updateStatusDisplay();

        termComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    applyPresetDates(newValue);
                    updateDateControls();
                }
        );

        fromDatePicker.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    TermOption term = termComboBox.getValue();

                    if (term != TermOption.CUSTOM
                            && term != TermOption.LIFETIME) {
                        applyPresetDates(term);
                    }
                }
        );

        activateButton.setOnAction(event -> activateLicense());
        cancelButton.setOnAction(event -> closeWindow());
    }

    public void setStartupLockMode(
            boolean startupLockMode
    ) {
        this.startupLockMode = startupLockMode;
        updateLanguage();
        updateStatusDisplay();
    }

    public boolean isActivated() {
        return activated;
    }

    private void configureTermComboBox() {

        termComboBox.getItems().setAll(
                TermOption.ONE_MONTH,
                TermOption.THREE_MONTHS,
                TermOption.SIX_MONTHS,
                TermOption.ONE_YEAR,
                TermOption.CUSTOM,
                TermOption.LIFETIME
        );

        termComboBox.setCellFactory(
                listView -> new ListCell<>() {
                    @Override
                    protected void updateItem(
                            TermOption item,
                            boolean empty
                    ) {
                        super.updateItem(item, empty);
                        setText(empty || item == null
                                ? null
                                : termText(item));
                    }
                }
        );

        termComboBox.setButtonCell(
                new ListCell<>() {
                    @Override
                    protected void updateItem(
                            TermOption item,
                            boolean empty
                    ) {
                        super.updateItem(item, empty);
                        setText(empty || item == null
                                ? null
                                : termText(item));
                    }
                }
        );
    }

    private void loadCurrentLicense() {

        ProgramLicense license =
                ProgramLicenseManager.load();

        if (license.isConfigured()
                && license.isIntegrityValid()) {

            if (license.isLifetime()) {
                termComboBox.setValue(TermOption.LIFETIME);
                fromDatePicker.setValue(null);
                toDatePicker.setValue(null);

            } else {
                termComboBox.setValue(TermOption.CUSTOM);
                fromDatePicker.setValue(license.getStartDate());
                toDatePicker.setValue(license.getEndDate());
            }

        } else {
            termComboBox.setValue(TermOption.ONE_MONTH);
            fromDatePicker.setValue(LocalDate.now());
            applyPresetDates(TermOption.ONE_MONTH);
        }
    }

    private void applyPresetDates(
            TermOption option
    ) {

        if (option == null) {
            return;
        }

        if (option == TermOption.LIFETIME) {
            fromDatePicker.setValue(null);
            toDatePicker.setValue(null);
            return;
        }

        if (fromDatePicker.getValue() == null) {
            fromDatePicker.setValue(LocalDate.now());
        }

        if (option == TermOption.CUSTOM) {
            if (toDatePicker.getValue() == null) {
                toDatePicker.setValue(
                        fromDatePicker.getValue()
                                .plusMonths(1)
                                .minusDays(1)
                );
            }
            return;
        }

        LocalDate startDate = fromDatePicker.getValue();
        LocalDate endDate;

        switch (option) {
            case ONE_MONTH ->
                    endDate = startDate.plusMonths(1).minusDays(1);

            case THREE_MONTHS ->
                    endDate = startDate.plusMonths(3).minusDays(1);

            case SIX_MONTHS ->
                    endDate = startDate.plusMonths(6).minusDays(1);

            case ONE_YEAR ->
                    endDate = startDate.plusYears(1).minusDays(1);

            default -> {
                return;
            }
        }

        toDatePicker.setValue(endDate);
    }

    private void updateDateControls() {

        TermOption option = termComboBox.getValue();

        boolean lifetime = option == TermOption.LIFETIME;
        boolean custom = option == TermOption.CUSTOM;

        fromDatePicker.setDisable(lifetime);
        toDatePicker.setDisable(lifetime || !custom);

        fromDatePicker.setEditable(!lifetime);
        toDatePicker.setEditable(!lifetime && custom);
    }

    private void activateLicense() {

        clearMessage();

        if (!SuperAdminAuthService.isConfigured()) {
            showMessage(
                    ar(
                            "لم يتم إعداد كلمة مرور السوبر أدمن بعد.",
                            "The Super Admin password has not been configured yet."
                    ),
                    true
            );
            return;
        }

        String password =
                superAdminPasswordField.getText() == null
                        ? ""
                        : superAdminPasswordField.getText();

        if (!SuperAdminAuthService.verify(password)) {
            showMessage(
                    ar(
                            "كلمة مرور السوبر أدمن غير صحيحة.",
                            "Invalid Super Admin password."
                    ),
                    true
            );

            superAdminPasswordField.clear();
            superAdminPasswordField.requestFocus();
            return;
        }

        TermOption option = termComboBox.getValue();

        if (option == null) {
            showMessage(
                    ar(
                            "يرجى اختيار مدة تشغيل البرنامج.",
                            "Please select a program license term."
                    ),
                    true
            );
            return;
        }

        try {

            if (option == TermOption.LIFETIME) {

                ProgramLicenseManager.activateLifetime();

            } else {

                LocalDate startDate = fromDatePicker.getValue();
                LocalDate endDate = toDatePicker.getValue();

                if (startDate == null || endDate == null) {
                    showMessage(
                            ar(
                                    "تاريخ البداية وتاريخ النهاية مطلوبان.",
                                    "Start and end dates are required."
                            ),
                            true
                    );
                    return;
                }

                if (endDate.isBefore(startDate)) {
                    showMessage(
                            ar(
                                    "تاريخ النهاية لا يمكن أن يسبق تاريخ البداية.",
                                    "The end date cannot be before the start date."
                            ),
                            true
                    );
                    return;
                }

                /*
                 * A saved activation should be usable immediately.
                 * This prevents a Super Admin from accidentally saving
                 * a range that immediately locks the application again.
                 */
                LocalDate today = LocalDate.now();

                if (today.isBefore(startDate)
                        || today.isAfter(endDate)) {

                    showMessage(
                            ar(
                                    "يجب أن يكون تاريخ اليوم داخل مدة التشغيل الجديدة.",
                                    "Today's date must be inside the new activation period."
                            ),
                            true
                    );
                    return;
                }

                ProgramLicenseManager.activatePeriod(
                        startDate,
                        endDate
                );
            }

            ProgramLicenseStatus status =
                    ProgramLicenseManager.evaluate();

            if (!status.isAllowed()) {
                showMessage(
                        ar(
                                "تم الحفظ ولكن الترخيص غير صالح للتشغيل الآن.",
                                "The license was saved but is not valid for use now."
                        ),
                        true
                );
                updateStatusDisplay();
                return;
            }

            ProgramLicenseManager.recordSuccessfulOpen();

            activated = true;
            superAdminPasswordField.clear();
            updateStatusDisplay();

            showMessage(
                    ar(
                            "تم تفعيل ترخيص تشغيل GymFlow بنجاح.",
                            "GymFlow program license activated successfully."
                    ),
                    false
            );

            closeWindow();

        } catch (Exception exception) {
            exception.printStackTrace();

            showMessage(
                    ar(
                            "تعذر حفظ ترخيص تشغيل البرنامج: "
                                    + safe(exception.getMessage()),
                            "Could not save the program license: "
                                    + safe(exception.getMessage())
                    ),
                    true
            );
        }
    }

    private void updateStatusDisplay() {

        if (statusValueLabel == null) {
            return;
        }

        ProgramLicense license =
                ProgramLicenseManager.load();

        ProgramLicenseStatus status =
                ProgramLicenseManager.evaluate();

        String text;
        String color;

        switch (status) {

            case LIFETIME -> {
                text = ar(
                        "مفعّل للأبد — لا يوجد تاريخ انتهاء",
                        "Lifetime — no expiry date"
                );
                color = "#15803D";
            }

            case ACTIVE -> {
                long remaining =
                        ProgramLicenseManager.daysRemaining() + 1;

                text = ar(
                        "نشط من "
                                + format(license.getStartDate())
                                + " إلى "
                                + format(license.getEndDate())
                                + " — متبقي "
                                + remaining
                                + " يوم",
                        "Active from "
                                + format(license.getStartDate())
                                + " to "
                                + format(license.getEndDate())
                                + " — "
                                + remaining
                                + " day(s) remaining"
                );
                color = "#15803D";
            }

            case EXPIRED -> {
                text = ar(
                        "انتهت مدة التشغيل في "
                                + format(license.getEndDate()),
                        "Program license expired on "
                                + format(license.getEndDate())
                );
                color = "#B91C1C";
            }

            case NOT_STARTED -> {
                text = ar(
                        "مدة التشغيل تبدأ في "
                                + format(license.getStartDate()),
                        "Program license starts on "
                                + format(license.getStartDate())
                );
                color = "#B45309";
            }

            case CLOCK_ROLLBACK -> {
                text = ar(
                        "تم اكتشاف رجوع في تاريخ الجهاز. يلزم تفعيل السوبر أدمن.",
                        "A system-date rollback was detected. Super Admin activation is required."
                );
                color = "#B91C1C";
            }

            case INVALID -> {
                text = ar(
                        "ملف ترخيص التشغيل غير صالح أو تم تعديله.",
                        "The program license file is invalid or was modified."
                );
                color = "#B91C1C";
            }

            case NOT_CONFIGURED -> {
                text = ar(
                        "لم يتم تفعيل ترخيص تشغيل البرنامج بعد.",
                        "The program license has not been activated yet."
                );
                color = "#B45309";
            }

            default -> {
                text = "-";
                color = "#374151";
            }
        }

        statusValueLabel.setText(text);
        statusValueLabel.setStyle(
                "-fx-font-weight: bold;"
                        + "-fx-text-fill: " + color + ";"
                        + "-fx-wrap-text: true;"
        );

        if (startupLockMode && !status.isAllowed()) {
            subtitleLabel.setText(
                    ar(
                            "البرنامج مقفل حتى يقوم السوبر أدمن بتجديد مدة التشغيل أو اختيار فتح للأبد.",
                            "GymFlow is locked until the Super Admin renews the program period or selects Lifetime."
                    )
            );
        }
    }

    public void updateLanguage() {

        boolean arabic = LanguageManager.isArabic();

        if (rootContainer != null) {
            rootContainer.setNodeOrientation(
                    arabic
                            ? NodeOrientation.RIGHT_TO_LEFT
                            : NodeOrientation.LEFT_TO_RIGHT
            );
        }

        titleLabel.setText(
                ar(
                        startupLockMode
                                ? "تجديد ترخيص تشغيل GymFlow"
                                : "ترخيص تشغيل GymFlow",
                        startupLockMode
                                ? "Renew GymFlow Program License"
                                : "GymFlow Program License"
                )
        );

        subtitleLabel.setText(
                ar(
                        "هذه الإعدادات متاحة للسوبر أدمن فقط.",
                        "These settings are available to the Super Admin only."
                )
        );

        statusTitleLabel.setText(
                ar("حالة الترخيص", "License Status")
        );

        termLabel.setText(
                ar("مدة تشغيل البرنامج", "Program Term")
        );

        fromLabel.setText(
                ar("من تاريخ", "From Date")
        );

        toLabel.setText(
                ar("إلى تاريخ", "To Date")
        );

        passwordLabel.setText(
                ar(
                        "كلمة مرور السوبر أدمن",
                        "Super Admin Password"
                )
        );

        superAdminPasswordField.setPromptText(
                ar(
                        "أدخل كلمة مرور السوبر أدمن للتفعيل",
                        "Enter Super Admin password to activate"
                )
        );

        activateButton.setText(
                ar(
                        startupLockMode
                                ? "تفعيل وفتح البرنامج"
                                : "حفظ وتفعيل",
                        startupLockMode
                                ? "Activate & Open GymFlow"
                                : "Save & Activate"
                )
        );

        cancelButton.setText(
                ar(
                        startupLockMode
                                ? "إغلاق البرنامج"
                                : "إلغاء",
                        startupLockMode
                                ? "Exit Application"
                                : "Cancel"
                )
        );

        TermOption selected = termComboBox.getValue();

        termComboBox.setCellFactory(
                listView -> new ListCell<>() {
                    @Override
                    protected void updateItem(
                            TermOption item,
                            boolean empty
                    ) {
                        super.updateItem(item, empty);
                        setText(empty || item == null
                                ? null
                                : termText(item));
                    }
                }
        );

        termComboBox.setButtonCell(
                new ListCell<>() {
                    @Override
                    protected void updateItem(
                            TermOption item,
                            boolean empty
                    ) {
                        super.updateItem(item, empty);
                        setText(empty || item == null
                                ? null
                                : termText(item));
                    }
                }
        );
        termComboBox.setValue(selected);

        updateStatusDisplay();
    }

    private String termText(
            TermOption option
    ) {

        if (option == null) {
            return "";
        }

        return switch (option) {
            case ONE_MONTH ->
                    ar("شهر واحد", "1 Month");

            case THREE_MONTHS ->
                    ar("3 أشهر", "3 Months");

            case SIX_MONTHS ->
                    ar("6 أشهر", "6 Months");

            case ONE_YEAR ->
                    ar("سنة واحدة", "1 Year");

            case CUSTOM ->
                    ar("مدة مخصصة من / إلى", "Custom From / To");

            case LIFETIME ->
                    ar("فتح للأبد", "Lifetime");
        };
    }

    private void showMessage(
            String message,
            boolean error
    ) {

        if (messageLabel == null) {
            return;
        }

        messageLabel.setText(message == null ? "" : message);
        messageLabel.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: "
                        + (error ? "#B91C1C" : "#15803D")
                        + ";"
                        + "-fx-wrap-text: true;"
        );
    }

    private void clearMessage() {
        showMessage("", false);
    }

    private String format(
            LocalDate value
    ) {
        return value == null ? "-" : value.format(displayDate);
    }

    private String safe(
            String value
    ) {
        return value == null ? "" : value.trim();
    }

    private String ar(
            String arabic,
            String english
    ) {
        return LanguageManager.isArabic()
                ? arabic
                : english;
    }

    private void closeWindow() {

        if (cancelButton != null
                && cancelButton.getScene() != null
                && cancelButton.getScene().getWindow() instanceof Stage stage) {

            stage.close();
        }
    }
}

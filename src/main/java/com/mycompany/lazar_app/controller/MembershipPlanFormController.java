package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.ui.dialog.DialogStyler;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.MembershipPlan;
import com.mycompany.lazar_app.repository.MembershipPlanRepository;

import javafx.fxml.FXML;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import javafx.stage.Stage;

import java.sql.SQLException;

import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;


public class MembershipPlanFormController {

    private final MembershipPlanRepository repository =
            new MembershipPlanRepository();


    private MembershipPlan editingPlan;

    private boolean saved;


    @FXML
    private Label titleLabel;

    @FXML
    private Label codeLabel;

    @FXML
    private Label nameArLabel;

    @FXML
    private Label nameEnLabel;

    @FXML
    private Label durationLabel;

    @FXML
    private Label maxSessionsLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label descriptionLabel;


    @FXML
    private TextField codeField;

    @FXML
    private TextField nameArField;

    @FXML
    private TextField nameEnField;

    @FXML
    private TextField durationField;

    @FXML
    private TextField maxSessionsField;

    @FXML
    private TextField priceField;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private TextArea descriptionArea;


    @FXML
    private Button cancelButton;

    @FXML
    private Button saveButton;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        configureLanguage();

        configureNumericFields();

        statusComboBox
                .getItems()
                .setAll(
                        "Active",
                        "Inactive"
                );


        statusComboBox.setValue(
                "Active"
        );
    }


    // =========================================================
    // LANGUAGE
    // =========================================================

    private void configureLanguage() {

        if (LanguageManager.isArabic()) {

            titleLabel.setText(
                    "إضافة خطة اشتراك"
            );

            codeLabel.setText(
                    "كود الخطة"
            );

            nameArLabel.setText(
                    "الاسم العربي"
            );

            nameEnLabel.setText(
                    "الاسم الإنجليزي"
            );

            durationLabel.setText(
                    "المدة بالشهور"
            );

            maxSessionsLabel.setText(
                    "عدد الجلسات"
            );

            priceLabel.setText(
                    "السعر"
            );

            statusLabel.setText(
                    "الحالة"
            );

            descriptionLabel.setText(
                    "الوصف"
            );

            codeField.setPromptText(
                    "مثال: MONTHLY"
            );

            nameArField.setPromptText(
                    "مثال: اشتراك شهري"
            );

            nameEnField.setPromptText(
                    "Example: Monthly"
            );

            durationField.setPromptText(
                    "مثال: 1"
            );

            maxSessionsField.setPromptText(
                    "مثال: 12"
            );

            priceField.setPromptText(
                    "مثال: 500"
            );

            descriptionArea.setPromptText(
                    "وصف اختياري للخطة"
            );

            saveButton.setText(
                    "حفظ"
            );

            cancelButton.setText(
                    "إلغاء"
            );

        } else {

            titleLabel.setText(
                    "Add Membership Plan"
            );

            codeLabel.setText(
                    "Plan Code"
            );

            nameArLabel.setText(
                    "Arabic Name"
            );

            nameEnLabel.setText(
                    "English Name"
            );

            durationLabel.setText(
                    "Duration (Months)"
            );

            maxSessionsLabel.setText(
                    "Max Sessions"
            );

            priceLabel.setText(
                    "Price"
            );

            statusLabel.setText(
                    "Status"
            );

            descriptionLabel.setText(
                    "Description"
            );

            codeField.setPromptText(
                    "Example: MONTHLY"
            );

            nameArField.setPromptText(
                    "Example: اشتراك شهري"
            );

            nameEnField.setPromptText(
                    "Example: Monthly"
            );

            durationField.setPromptText(
                    "Example: 1"
            );

            maxSessionsField.setPromptText(
                    "Example: 12"
            );

            priceField.setPromptText(
                    "Example: 500"
            );

            descriptionArea.setPromptText(
                    "Optional plan description"
            );

            saveButton.setText(
                    "Save"
            );

            cancelButton.setText(
                    "Cancel"
            );
        }
    }


    // =========================================================
    // NUMERIC FIELDS
    // =========================================================

    private void configureNumericFields() {

        UnaryOperator<TextFormatter.Change> integerFilter =
                change ->
                        change
                                .getControlNewText()
                                .matches("\\d*")
                                ? change
                                : null;


        durationField.setTextFormatter(
                new TextFormatter<>(
                        integerFilter
                )
        );


        maxSessionsField.setTextFormatter(
                new TextFormatter<>(
                        integerFilter
                )
        );


        UnaryOperator<TextFormatter.Change> decimalFilter =
                change ->
                        change
                                .getControlNewText()
                                .matches(
                                        "\\d*(\\.\\d{0,2})?"
                                )
                                ? change
                                : null;


        priceField.setTextFormatter(
                new TextFormatter<>(
                        decimalFilter
                )
        );
    }


    // =========================================================
    // SET PLAN
    // =========================================================

    public void setPlan(
            MembershipPlan plan
    ) {

        editingPlan =
                plan;


        if (plan == null) {

            titleLabel.setText(
                    LanguageManager.isArabic()
                            ? "إضافة خطة اشتراك جديدة"
                            : "Add Membership Plan"
            );

            return;
        }


        titleLabel.setText(
                LanguageManager.isArabic()
                        ? "تعديل خطة الاشتراك"
                        : "Edit Membership Plan"
        );


        codeField.setText(
                safe(
                        plan.getCode()
                )
        );


        nameArField.setText(
                safe(
                        plan.getNameAr()
                )
        );


        nameEnField.setText(
                safe(
                        plan.getNameEn()
                )
        );


        durationField.setText(
                String.valueOf(
                        plan.getDurationMonths()
                )
        );


        maxSessionsField.setText(
                String.valueOf(
                        plan.getMaxSessions()
                )
        );


        priceField.setText(
                String.format(
                        Locale.US,
                        "%.2f",
                        plan.getPrice()
                )
        );


        statusComboBox.setValue(
                plan.isActive()
                        ? "Active"
                        : "Inactive"
        );


        descriptionArea.setText(
                safe(
                        plan.getDescription()
                )
        );
    }


    // =========================================================
    // SAVE
    // =========================================================

    @FXML
    private void save() {

        String code =
                safe(
                        codeField.getText()
                )
                        .toUpperCase(
                                Locale.ROOT
                        );


        String nameAr =
                safe(
                        nameArField.getText()
                );


        String nameEn =
                safe(
                        nameEnField.getText()
                );


        String durationText =
                safe(
                        durationField.getText()
                );


        String maxSessionsText =
                safe(
                        maxSessionsField.getText()
                );


        String priceText =
                safe(
                        priceField.getText()
                );


        String status =
                statusComboBox.getValue();


        String description =
                safe(
                        descriptionArea.getText()
                );


        // =====================================================
        // VALIDATION
        // =====================================================

        if (code.isBlank()) {

            warning(
                    LanguageManager.isArabic()
                            ? "يرجى إدخال كود الخطة."
                            : "Plan code is required."
            );

            codeField.requestFocus();

            return;
        }


        if (!code.matches(
                "[A-Z0-9_-]+"
        )) {

            warning(
                    LanguageManager.isArabic()
                            ? "كود الخطة يسمح بالحروف الإنجليزية والأرقام و - و _ فقط."
                            : "Plan code may contain English letters, numbers, - and _ only."
            );

            codeField.requestFocus();

            return;
        }


        if (nameAr.isBlank()) {

            warning(
                    LanguageManager.isArabic()
                            ? "يرجى إدخال الاسم العربي."
                            : "Arabic name is required."
            );

            nameArField.requestFocus();

            return;
        }


        if (nameEn.isBlank()) {

            warning(
                    LanguageManager.isArabic()
                            ? "يرجى إدخال الاسم الإنجليزي."
                            : "English name is required."
            );

            nameEnField.requestFocus();

            return;
        }


        if (durationText.isBlank()) {

            warning(
                    LanguageManager.isArabic()
                            ? "يرجى إدخال مدة الاشتراك."
                            : "Membership duration is required."
            );

            durationField.requestFocus();

            return;
        }


        int duration;


        try {

            duration =
                    Integer.parseInt(
                            durationText
                    );


        } catch (NumberFormatException exception) {

            warning(
                    LanguageManager.isArabic()
                            ? "مدة الاشتراك غير صحيحة."
                            : "Invalid membership duration."
            );

            return;
        }


        if (duration <= 0) {

            warning(
                    LanguageManager.isArabic()
                            ? "مدة الاشتراك يجب أن تكون أكبر من صفر."
                            : "Duration must be greater than zero."
            );

            return;
        }


        if (maxSessionsText.isBlank()) {

            warning(
                    LanguageManager.isArabic()
                            ? "يرجى إدخال عدد الجلسات المسموح بها."
                            : "Maximum sessions is required."
            );

            maxSessionsField.requestFocus();

            return;
        }


        int maxSessions;


        try {

            maxSessions =
                    Integer.parseInt(
                            maxSessionsText
                    );


        } catch (NumberFormatException exception) {

            warning(
                    LanguageManager.isArabic()
                            ? "عدد الجلسات غير صحيح."
                            : "Invalid maximum sessions."
            );

            maxSessionsField.requestFocus();

            return;
        }


        if (maxSessions <= 0) {

            warning(
                    LanguageManager.isArabic()
                            ? "عدد الجلسات يجب أن يكون أكبر من صفر."
                            : "Maximum sessions must be greater than zero."
            );

            maxSessionsField.requestFocus();

            return;
        }


        if (priceText.isBlank()) {

            warning(
                    LanguageManager.isArabic()
                            ? "يرجى إدخال السعر."
                            : "Price is required."
            );

            priceField.requestFocus();

            return;
        }


        double price;


        try {

            price =
                    Double.parseDouble(
                            priceText
                    );


        } catch (NumberFormatException exception) {

            warning(
                    LanguageManager.isArabic()
                            ? "السعر غير صحيح."
                            : "Invalid price."
            );

            return;
        }


        if (price < 0) {

            warning(
                    LanguageManager.isArabic()
                            ? "السعر لا يمكن أن يكون سالبًا."
                            : "Price cannot be negative."
            );

            return;
        }


        // =====================================================
        // DATABASE
        // =====================================================

        try {

            Optional<MembershipPlan> sameCode =
                    repository.findByCode(
                            code
                    );


            if (sameCode.isPresent()) {

                boolean sameRecord =
                        editingPlan != null
                        && sameCode
                                .get()
                                .getId()
                                == editingPlan.getId();


                if (!sameRecord) {

                    warning(
                            LanguageManager.isArabic()
                                    ? "كود الخطة مستخدم بالفعل."
                                    : "Plan code already exists."
                    );

                    codeField.requestFocus();

                    return;
                }
            }


            MembershipPlan plan;


            if (editingPlan == null) {

                plan =
                        new MembershipPlan();

            } else {

                plan =
                        editingPlan;
            }


            plan.setCode(
                    code
            );


            plan.setNameAr(
                    nameAr
            );


            plan.setNameEn(
                    nameEn
            );


            plan.setDurationMonths(
                    duration
            );


            plan.setMaxSessions(
                    maxSessions
            );


            plan.setPrice(
                    price
            );


            plan.setStatus(
                    status == null
                            ? "Active"
                            : status
            );


            plan.setDescription(
                    description
            );


            if (editingPlan == null) {

                repository.insert(
                        plan
                );

            } else {

                repository.update(
                        plan
                );
            }


            saved =
                    true;


            success(
                    LanguageManager.isArabic()
                            ? "تم حفظ خطة الاشتراك بنجاح."
                            : "Membership plan saved successfully."
            );


            close();


        } catch (SQLException exception) {

            exception.printStackTrace();


            error(
                    LanguageManager.isArabic()
                            ? "تعذر حفظ خطة الاشتراك."
                            : "Could not save membership plan.",
                    exception
            );
        }
    }


    // =========================================================
    // CANCEL
    // =========================================================

    @FXML
    private void cancel() {

        close();
    }


    // =========================================================
    // SAVED
    // =========================================================

    public boolean isSaved() {

        return saved;
    }


    // =========================================================
    // CLOSE
    // =========================================================

    private void close() {

        Stage stage =
                (Stage) saveButton
                        .getScene()
                        .getWindow();


        stage.close();
    }


    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }


    private void warning(
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.WARNING
                );


        alert.setTitle(
                LanguageManager.isArabic()
                        ? "تنبيه"
                        : "Warning"
        );


        alert.setHeaderText(
                null
        );


        alert.setContentText(
                message
        );


        DialogStyler.style(alert);


        alert.showAndWait();
    }


    private void success(
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );


        alert.setTitle(
                LanguageManager.isArabic()
                        ? "تم بنجاح"
                        : "Success"
        );


        alert.setHeaderText(
                null
        );


        alert.setContentText(
                message
        );


        DialogStyler.style(alert);


        alert.showAndWait();
    }


    private void error(
            String message,
            Exception exception
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );


        alert.setTitle(
                LanguageManager.isArabic()
                        ? "خطأ"
                        : "Error"
        );


        alert.setHeaderText(
                message
        );


        alert.setContentText(
                exception == null
                        || exception.getMessage() == null
                        ? ""
                        : exception.getMessage()
        );


        DialogStyler.style(alert);


        alert.showAndWait();
    }
}
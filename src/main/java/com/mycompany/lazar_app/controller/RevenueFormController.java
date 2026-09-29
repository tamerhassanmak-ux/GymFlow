package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.ui.WindowManager;
import com.mycompany.lazar_app.ui.dialog.DialogStyler;
import com.mycompany.lazar_app.model.Revenue;
import com.mycompany.lazar_app.repository.RevenueRepository;
import com.mycompany.lazar_app.util.RevenueAttachmentStorage;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class RevenueFormController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label nameLabel;

    @FXML
    private TextField nameField;

    @FXML
    private Label typeLabel;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private Label amountLabel;

    @FXML
    private TextField amountField;

    @FXML
    private Label dateLabel;

    @FXML
    private DatePicker revenueDatePicker;

    @FXML
    private Label attachmentTitleLabel;

    @FXML
    private Label attachmentNameLabel;

    @FXML
    private Button chooseAttachmentButton;

    @FXML
    private Button removeAttachmentButton;

    @FXML
    private Label notesLabel;

    @FXML
    private TextArea notesArea;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private RevenueRepository revenueRepository;
    private Revenue revenue;
    private File selectedAttachment;
    private boolean removeExistingAttachment;
    private Runnable onSaved;

    @FXML
    public void initialize() {

        revenueDatePicker.setValue(LocalDate.now());
        typeComboBox.setEditable(true);

        configureLanguage();

        Platform.runLater(() -> {
            if (nameField != null) {
                nameField.requestFocus();
            }
        });
    }

    public void setRevenueRepository(
            RevenueRepository revenueRepository
    ) {
        this.revenueRepository = revenueRepository;
        loadRevenueTypes();
    }

    public void setRevenue(Revenue revenue) {

        this.revenue = revenue;
        this.removeExistingAttachment = false;
        this.selectedAttachment = null;

        if (revenue == null) {
            configureLanguage();
            return;
        }

        nameField.setText(safe(revenue.getName()));
        typeComboBox.setValue(safe(revenue.getRevenueType()));

        if (revenue.getAmount() != null) {
            amountField.setText(revenue.getAmount().toPlainString());
        }

        if (revenue.getRevenueDate() != null) {
            revenueDatePicker.setValue(revenue.getRevenueDate());
        }

        notesArea.setText(safe(revenue.getNotes()));
        updateAttachmentLabel(revenue.getAttachmentPath());
        configureLanguage();
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    private void loadRevenueTypes() {

        if (revenueRepository == null || typeComboBox == null) {
            return;
        }

        try {
            List<String> types = revenueRepository.findRevenueTypes();
            typeComboBox.setItems(
                    FXCollections.observableArrayList(types)
            );
        } catch (Exception exception) {
            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تحميل أنواع الإيرادات"
                            : "Unable to load revenue types",
                    exception.getMessage()
            );
        }
    }

    private void configureLanguage() {

        boolean arabic = LanguageManager.isArabic();

        if (titleLabel != null) {
            titleLabel.setText(
                    revenue == null
                            ? (arabic ? "إضافة إيراد جديد" : "New Revenue")
                            : (arabic ? "تعديل الإيراد" : "Edit Revenue")
            );
        }

        if (subtitleLabel != null) {
            subtitleLabel.setText(
                    arabic
                            ? "سجل الإيراد وارفق مستند المصدر عند الحاجة"
                            : "Record the revenue and optionally attach its source document"
            );
        }

        setText(nameLabel, arabic ? "اسم الإيراد" : "Revenue Name");
        setText(typeLabel, arabic ? "نوع الإيراد" : "Revenue Type");
        setText(amountLabel, arabic ? "المبلغ" : "Amount");
        setText(dateLabel, arabic ? "تاريخ الإيراد" : "Revenue Date");
        setText(attachmentTitleLabel, arabic ? "مستند مصدر الدخل" : "Income Source Document");
        setText(notesLabel, arabic ? "ملاحظات" : "Notes");
        setText(chooseAttachmentButton, arabic ? "رفع ملف" : "Choose File");
        setText(removeAttachmentButton, arabic ? "إزالة المرفق" : "Remove Attachment");
        setText(saveButton, arabic ? "حفظ الإيراد" : "Save Revenue");
        setText(cancelButton, arabic ? "إلغاء" : "Cancel");

        nameField.setPromptText(
                arabic ? "مثال: بيع مشروبات" : "Example: Beverage sales"
        );

        typeComboBox.setPromptText(
                arabic
                        ? "اختر نوعًا سابقًا أو اكتب نوعًا جديدًا"
                        : "Select an existing type or type a new one"
        );

        amountField.setPromptText(arabic ? "0.00" : "0.00");
        notesArea.setPromptText(
                arabic ? "ملاحظات اختيارية..." : "Optional notes..."
        );

        NodeOrientation orientation = arabic
                ? NodeOrientation.RIGHT_TO_LEFT
                : NodeOrientation.LEFT_TO_RIGHT;

        if (titleLabel != null && titleLabel.getScene() != null) {
            titleLabel.getScene().getRoot().setNodeOrientation(orientation);
        }

        if (attachmentNameLabel != null
                && (attachmentNameLabel.getText() == null
                || attachmentNameLabel.getText().isBlank())) {
            attachmentNameLabel.setText(
                    arabic ? "لا يوجد ملف مرفق" : "No attachment selected"
            );
        }
    }

    @FXML
    private void chooseAttachment() {

        FileChooser chooser = new FileChooser();
        chooser.setTitle(
                LanguageManager.isArabic()
                        ? "اختيار مستند مصدر الإيراد"
                        : "Choose Revenue Source Document"
        );

        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        "Documents & Images",
                        "*.pdf",
                        "*.png",
                        "*.jpg",
                        "*.jpeg",
                        "*.webp",
                        "*.doc",
                        "*.docx",
                        "*.xls",
                        "*.xlsx"
                ),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = getStage();
        File file = chooser.showOpenDialog(stage);

        if (file == null) {
            return;
        }

        selectedAttachment = file;
        removeExistingAttachment = false;
        attachmentNameLabel.setText(file.getName());
    }

    @FXML
    private void removeAttachment() {
        selectedAttachment = null;
        removeExistingAttachment = true;
        attachmentNameLabel.setText(
                LanguageManager.isArabic()
                        ? "لا يوجد ملف مرفق"
                        : "No attachment selected"
        );
    }

    @FXML
    private void saveRevenue() {

        if (revenueRepository == null) {
            showError(
                    LanguageManager.isArabic()
                            ? "خطأ في الإيرادات"
                            : "Revenue Error",
                    "RevenueRepository is not configured."
            );
            return;
        }

        try {
            String name = nameField.getText() == null
                    ? ""
                    : nameField.getText().trim();

            String type = getRevenueTypeText();
            String amountText = amountField.getText() == null
                    ? ""
                    : amountField.getText().trim().replace(",", "");

            if (name.isBlank()) {
                throw new IllegalArgumentException(
                        LanguageManager.isArabic()
                                ? "اسم الإيراد مطلوب."
                                : "Revenue name is required."
                );
            }

            if (type.isBlank()) {
                throw new IllegalArgumentException(
                        LanguageManager.isArabic()
                                ? "نوع الإيراد مطلوب."
                                : "Revenue type is required."
                );
            }

            BigDecimal amount;

            try {
                amount = new BigDecimal(amountText);
            } catch (Exception exception) {
                throw new IllegalArgumentException(
                        LanguageManager.isArabic()
                                ? "أدخل مبلغًا صحيحًا."
                                : "Enter a valid amount."
                );
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        LanguageManager.isArabic()
                                ? "المبلغ يجب أن يكون أكبر من صفر."
                                : "Amount must be greater than zero."
                );
            }

            LocalDate revenueDate = revenueDatePicker.getValue();

            if (revenueDate == null) {
                throw new IllegalArgumentException(
                        LanguageManager.isArabic()
                                ? "تاريخ الإيراد مطلوب."
                                : "Revenue date is required."
                );
            }

            String attachmentPath = revenue == null
                    ? null
                    : revenue.getAttachmentPath();

            if (removeExistingAttachment) {
                attachmentPath = null;
            }

            if (selectedAttachment != null) {
                attachmentPath = RevenueAttachmentStorage
                        .copyToApplicationStorage(selectedAttachment);
            }

            Revenue target = revenue == null
                    ? new Revenue()
                    : revenue;

            target.setName(name);
            target.setRevenueType(type);
            target.setAmount(amount);
            target.setRevenueDate(revenueDate);
            target.setAttachmentPath(attachmentPath);
            target.setNotes(notesArea.getText());

            if (target.getId() <= 0) {
                revenueRepository.save(target);
            } else {
                revenueRepository.update(target);
            }

            if (onSaved != null) {
                onSaved.run();
            }

            closeWindow();

        } catch (Exception exception) {
            showError(
                    LanguageManager.isArabic()
                            ? "تعذر حفظ الإيراد"
                            : "Unable to save revenue",
                    exception.getMessage()
            );
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private String getRevenueTypeText() {

        String editorText = typeComboBox.getEditor() == null
                ? null
                : typeComboBox.getEditor().getText();

        if (editorText != null && !editorText.isBlank()) {
            return editorText.trim();
        }

        String value = typeComboBox.getValue();
        return value == null ? "" : value.trim();
    }

    private void updateAttachmentLabel(String path) {

        if (path == null || path.isBlank()) {
            attachmentNameLabel.setText(
                    LanguageManager.isArabic()
                            ? "لا يوجد ملف مرفق"
                            : "No attachment selected"
            );
            return;
        }

        try {
            Path file = Path.of(path);
            attachmentNameLabel.setText(
                    file.getFileName() == null
                            ? path
                            : file.getFileName().toString()
            );
        } catch (Exception exception) {
            attachmentNameLabel.setText(path);
        }
    }

    private void setText(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private void setText(Button button, String value) {
        if (button != null) {
            button.setText(value);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private Stage getStage() {
        if (saveButton == null || saveButton.getScene() == null) {
            return null;
        }
        return (Stage) saveButton.getScene().getWindow();
    }

    private void closeWindow() {
        Stage stage = getStage();
        if (stage != null) {
            stage.close();
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(
                message == null || message.isBlank()
                        ? (LanguageManager.isArabic()
                                ? "حدث خطأ غير متوقع."
                                : "Unexpected error.")
                        : message
        );

        DialogStyler.style(alert);

        WindowManager.showResponsiveDialogAndWait(
                alert,
                getStage(),
                520.0,
                320.0
        );
    }
}

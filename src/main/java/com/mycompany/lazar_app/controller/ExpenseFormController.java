package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.ui.dialog.DialogStyler;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.Expense;
import com.mycompany.lazar_app.service.ExpenseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ExpenseFormController {

    // =========================================================
    // ROOT
    // =========================================================

    @FXML
    private BorderPane rootPane;


    // =========================================================
    // HEADER
    // =========================================================

    @FXML
    private Label formTitleLabel;

    @FXML
    private Label formSubtitleLabel;

    @FXML
    private Label expenseCodeTitleLabel;

    @FXML
    private Label expenseCodeLabel;


    // =========================================================
    // SECTION TITLES
    // =========================================================

    @FXML
    private Label informationTitleLabel;

    @FXML
    private Label informationSubtitleLabel;

    @FXML
    private Label paymentDetailsTitleLabel;

    @FXML
    private Label paymentDetailsSubtitleLabel;


    // =========================================================
    // FIELD LABELS
    // =========================================================

    @FXML
    private Label titleLabel;

    @FXML
    private Label categoryLabel;

    @FXML
    private Label amountLabel;

    @FXML
    private Label expenseDateLabel;

    @FXML
    private Label paymentMethodLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label beneficiaryLabel;

    @FXML
    private Label referenceNumberLabel;

    @FXML
    private Label notesLabel;


    // =========================================================
    // FORM FIELDS
    // =========================================================

    @FXML
    private TextField titleField;

    @FXML
    private ComboBox<LocalizedOption> categoryComboBox;

    @FXML
    private TextField amountField;

    @FXML
    private DatePicker expenseDatePicker;

    @FXML
    private ComboBox<LocalizedOption> paymentMethodComboBox;

    @FXML
    private ComboBox<LocalizedOption> statusComboBox;

    @FXML
    private TextField beneficiaryField;

    @FXML
    private TextField referenceNumberField;

    @FXML
    private TextArea notesArea;

    @FXML
    private Label validationLabel;


    // =========================================================
    // BUTTONS
    // =========================================================

    @FXML
    private Button cancelButton;

    @FXML
    private Button saveButton;


    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private ExpenseService expenseService;

    private Expense expense;

    private Runnable onSaved;

    private ResourceBundle bundle;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    private void initialize() {

        bundle = LanguageManager.getBundle();

        applyLanguageDirection();

        configureStaticTexts();

        configureCategories();

        configurePaymentMethods();

        configureStatuses();

        configureComboBoxRendering();

        configureAmountField();

        setDefaults();

        configureCreateMode();

        clearValidation();
    }


    // =========================================================
    // LANGUAGE DIRECTION
    // =========================================================

    private void applyLanguageDirection() {

        if (rootPane == null) {
            return;
        }

        rootPane.setNodeOrientation(
                LanguageManager.isArabic()
                        ? NodeOrientation.RIGHT_TO_LEFT
                        : NodeOrientation.LEFT_TO_RIGHT
        );
    }


    // =========================================================
    // STATIC TRANSLATIONS
    // =========================================================

    private void configureStaticTexts() {

        expenseCodeTitleLabel.setText(
                text("expense.code", "Expense Code")
        );

        informationTitleLabel.setText(
                text(
                        "expense.information",
                        "Expense Information"
                )
        );

        informationSubtitleLabel.setText(
                text(
                        "expense.informationSubtitle",
                        "Enter the basic expense details"
                )
        );

        paymentDetailsTitleLabel.setText(
                text(
                        "expense.paymentDetails",
                        "Payment Details"
                )
        );

        paymentDetailsSubtitleLabel.setText(
                text(
                        "expense.paymentDetailsSubtitle",
                        "Optional payment and beneficiary information"
                )
        );

        titleLabel.setText(
                text("expense.title", "Expense Title") + " *"
        );

        categoryLabel.setText(
                text("expense.category", "Category") + " *"
        );

        amountLabel.setText(
                text("expense.amount", "Amount") + " *"
        );

        expenseDateLabel.setText(
                text("expense.date", "Expense Date") + " *"
        );

        paymentMethodLabel.setText(
                text(
                        "expense.payment_method",
                        "Payment Method"
                ) + " *"
        );

        statusLabel.setText(
                text("expense.status", "Status") + " *"
        );

        beneficiaryLabel.setText(
                text(
                        "expense.beneficiary",
                        "Beneficiary"
                )
        );

        referenceNumberLabel.setText(
                text(
                        "expense.reference_number",
                        "Reference Number"
                )
        );

        notesLabel.setText(
                text("expense.notes", "Notes")
        );

        titleField.setPromptText(
                text(
                        "expense.titlePlaceholder",
                        "Example: Monthly electricity bill"
                )
        );

        categoryComboBox.setPromptText(
                text(
                        "expense.selectCategory",
                        "Select category"
                )
        );

        amountField.setPromptText(
                text(
                        "expense.amountPlaceholder",
                        "0.00"
                )
        );

        paymentMethodComboBox.setPromptText(
                text(
                        "expense.selectPaymentMethod",
                        "Select payment method"
                )
        );

        statusComboBox.setPromptText(
                text(
                        "expense.selectStatus",
                        "Select status"
                )
        );

        beneficiaryField.setPromptText(
                text(
                        "expense.beneficiaryPlaceholder",
                        "Supplier or beneficiary"
                )
        );

        referenceNumberField.setPromptText(
                text(
                        "expense.referencePlaceholder",
                        "Invoice / receipt number"
                )
        );

        notesArea.setPromptText(
                text(
                        "expense.notesPlaceholder",
                        "Additional notes..."
                )
        );

        cancelButton.setText(
                text(
                        "expense.cancel",
                        "Cancel"
                )
        );

        saveButton.setText(
                text(
                        "expense.save",
                        "Save Expense"
                )
        );
    }


    // =========================================================
    // SERVICE
    // =========================================================

    public void setExpenseService(
            ExpenseService expenseService) {

        this.expenseService = expenseService;
    }


    // =========================================================
    // SET EXPENSE
    // =========================================================

    public void setExpense(
            Expense expense) {

        this.expense = expense;

        if (expense == null) {

            configureCreateMode();

        } else {

            configureEditMode();

            populateForm(expense);
        }
    }


    // =========================================================
    // CALLBACK
    // =========================================================

    public void setOnSaved(
            Runnable onSaved) {

        this.onSaved = onSaved;
    }


    // =========================================================
    // CREATE MODE
    // =========================================================

    private void configureCreateMode() {

        formTitleLabel.setText(
                text(
                        "expense.new",
                        "New Expense"
                )
        );

        formSubtitleLabel.setText(
                text(
                        "expense.newSubtitle",
                        "Record a new operating expense"
                )
        );

        expenseCodeLabel.setText(
                text(
                        "expense.codeAuto",
                        "Generated automatically"
                )
        );
    }


    // =========================================================
    // EDIT MODE
    // =========================================================

    private void configureEditMode() {

        formTitleLabel.setText(
                text(
                        "expense.edit",
                        "Edit Expense"
                )
        );

        formSubtitleLabel.setText(
                text(
                        "expense.editSubtitle",
                        "Update expense information"
                )
        );
    }


    // =========================================================
    // POPULATE FORM
    // =========================================================

    private void populateForm(
            Expense expense) {

        expenseCodeLabel.setText(
                safe(expense.getExpenseCode())
        );

        titleField.setText(
                safe(expense.getTitle())
        );

        selectOptionByValue(
                categoryComboBox,
                expense.getCategory()
        );

        if (expense.getAmount() != null) {

            amountField.setText(
                    expense.getAmount()
                            .toPlainString()
            );
        }

        expenseDatePicker.setValue(
                expense.getExpenseDate()
        );

        selectOptionByValue(
                paymentMethodComboBox,
                expense.getPaymentMethod()
        );

        selectOptionByValue(
                statusComboBox,
                expense.getStatus()
        );

        beneficiaryField.setText(
                safe(expense.getBeneficiary())
        );

        referenceNumberField.setText(
                safe(expense.getReferenceNumber())
        );

        notesArea.setText(
                safe(expense.getNotes())
        );
    }


    // =========================================================
    // CATEGORIES
    // =========================================================

    private void configureCategories() {

        categoryComboBox.setItems(
                FXCollections.observableArrayList(
                        new LocalizedOption(
                                "Rent",
                                text(
                                        "expense.category.rent",
                                        "Rent"
                                )
                        ),
                        new LocalizedOption(
                                "Electricity",
                                text(
                                        "expense.category.electricity",
                                        "Electricity"
                                )
                        ),
                        new LocalizedOption(
                                "Water",
                                text(
                                        "expense.category.water",
                                        "Water"
                                )
                        ),
                        new LocalizedOption(
                                "Maintenance",
                                text(
                                        "expense.category.maintenance",
                                        "Maintenance"
                                )
                        ),
                        new LocalizedOption(
                                "Equipment",
                                text(
                                        "expense.category.equipment",
                                        "Equipment"
                                )
                        ),
                        new LocalizedOption(
                                "Salaries",
                                text(
                                        "expense.category.salaries",
                                        "Salaries"
                                )
                        ),
                        new LocalizedOption(
                                "Cleaning",
                                text(
                                        "expense.category.cleaning",
                                        "Cleaning"
                                )
                        ),
                        new LocalizedOption(
                                "Marketing",
                                text(
                                        "expense.category.marketing",
                                        "Marketing"
                                )
                        ),
                        new LocalizedOption(
                                "Internet",
                                text(
                                        "expense.category.internet",
                                        "Internet"
                                )
                        ),
                        new LocalizedOption(
                                "Transportation",
                                text(
                                        "expense.category.transportation",
                                        "Transportation"
                                )
                        ),
                        new LocalizedOption(
                                "Supplies",
                                text(
                                        "expense.category.supplies",
                                        "Supplies"
                                )
                        ),
                        new LocalizedOption(
                                "Taxes",
                                text(
                                        "expense.category.taxes",
                                        "Taxes"
                                )
                        ),
                        new LocalizedOption(
                                "Insurance",
                                text(
                                        "expense.category.insurance",
                                        "Insurance"
                                )
                        ),
                        new LocalizedOption(
                                "Other",
                                text(
                                        "expense.category.other",
                                        "Other"
                                )
                        )
                )
        );
    }


    // =========================================================
    // PAYMENT METHODS
    // =========================================================

    private void configurePaymentMethods() {

        paymentMethodComboBox.setItems(
                FXCollections.observableArrayList(
                        new LocalizedOption(
                                "CASH",
                                text(
                                        "expense.payment.cash",
                                        "Cash"
                                )
                        ),
                        new LocalizedOption(
                                "CARD",
                                text(
                                        "expense.payment.card",
                                        "Card"
                                )
                        ),
                        new LocalizedOption(
                                "BANK_TRANSFER",
                                text(
                                        "expense.payment.bank_transfer",
                                        "Bank Transfer"
                                )
                        ),
                        new LocalizedOption(
                                "CHEQUE",
                                text(
                                        "expense.payment.cheque",
                                        "Cheque"
                                )
                        ),
                        new LocalizedOption(
                                "OTHER",
                                text(
                                        "expense.payment.other",
                                        "Other"
                                )
                        )
                )
        );
    }


    // =========================================================
    // STATUSES
    // =========================================================

    private void configureStatuses() {

        statusComboBox.setItems(
                FXCollections.observableArrayList(
                        new LocalizedOption(
                                "PAID",
                                text(
                                        "expense.status.paid",
                                        "Paid"
                                )
                        ),
                        new LocalizedOption(
                                "PENDING",
                                text(
                                        "expense.status.pending",
                                        "Pending"
                                )
                        ),
                        new LocalizedOption(
                                "CANCELLED",
                                text(
                                        "expense.status.cancelled",
                                        "Cancelled"
                                )
                        )
                )
        );
    }


    // =========================================================
    // COMBOBOX RENDERING
    // =========================================================

    private void configureComboBoxRendering() {

        configureComboBox(categoryComboBox);

        configureComboBox(paymentMethodComboBox);

        configureComboBox(statusComboBox);
    }

    private void configureComboBox(
            ComboBox<LocalizedOption> comboBox) {

        comboBox.setCellFactory(
                listView ->
                        new ListCell<>() {

                            @Override
                            protected void updateItem(
                                    LocalizedOption item,
                                    boolean empty) {

                                super.updateItem(
                                        item,
                                        empty
                                );

                                setText(
                                        empty || item == null
                                                ? null
                                                : item.label()
                                );
                            }
                        }
        );

        comboBox.setButtonCell(
                new ListCell<>() {

                    @Override
                    protected void updateItem(
                            LocalizedOption item,
                            boolean empty) {

                        super.updateItem(
                                item,
                                empty
                        );

                        setText(
                                empty || item == null
                                        ? null
                                        : item.label()
                        );
                    }
                }
        );
    }


    // =========================================================
    // DEFAULT VALUES
    // =========================================================

    private void setDefaults() {

        selectOptionByValue(
                categoryComboBox,
                "Other"
        );

        expenseDatePicker.setValue(
                LocalDate.now()
        );

        selectOptionByValue(
                paymentMethodComboBox,
                "CASH"
        );

        selectOptionByValue(
                statusComboBox,
                "PAID"
        );
    }


    // =========================================================
    // SELECT OPTION
    // =========================================================

    private void selectOptionByValue(
            ComboBox<LocalizedOption> comboBox,
            String value) {

        if (value == null) {
            comboBox.setValue(null);
            return;
        }

        for (LocalizedOption option
                : comboBox.getItems()) {

            if (option.value()
                    .equalsIgnoreCase(value)) {

                comboBox.setValue(option);

                return;
            }
        }

        comboBox.setValue(null);
    }


    // =========================================================
    // AMOUNT INPUT
    // =========================================================

    private void configureAmountField() {

        amountField.textProperty()
                .addListener(
                        (observable,
                         oldValue,
                         newValue) -> {

                            if (newValue == null
                                    || newValue.isEmpty()) {

                                return;
                            }

                            if (!newValue.matches(
                                    "\\d*(\\.\\d{0,2})?"
                            )) {

                                amountField.setText(
                                        oldValue
                                );
                            }
                        }
                );
    }


    // =========================================================
    // SAVE
    // =========================================================

    @FXML
    private void handleSave() {

        clearValidation();

        try {

            if (expenseService == null) {

                throw new IllegalStateException(
                        "Expense service is not configured."
                );
            }

            boolean createMode =
                    expense == null;

            Expense target =
                    createMode
                            ? new Expense()
                            : expense;

            readForm(target);

            if (createMode) {

                expense =
                        expenseService
                                .addExpense(target);

            } else {

                expenseService
                        .updateExpense(target);
            }

            if (onSaved != null) {
                onSaved.run();
            }

            showSuccess(
                    createMode
                            ? text(
                                    "expense.message.created",
                                    "Expense created successfully."
                            )
                            : text(
                                    "expense.message.updated",
                                    "Expense updated successfully."
                            )
            );

            closeWindow();

        } catch (IllegalArgumentException exception) {

            showValidation(
                    exception.getMessage()
            );

        } catch (Exception exception) {

            showError(
                    text(
                            "expense.message.saveError",
                            "Unable to save expense"
                    ),
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // READ FORM
    // =========================================================

    private void readForm(
            Expense target) {

        target.setTitle(
                titleField.getText()
        );

        target.setCategory(
                getSelectedValue(
                        categoryComboBox
                )
        );

        target.setAmount(
                parseAmount()
        );

        target.setExpenseDate(
                expenseDatePicker.getValue()
        );

        target.setPaymentMethod(
                getSelectedValue(
                        paymentMethodComboBox
                )
        );

        target.setStatus(
                getSelectedValue(
                        statusComboBox
                )
        );

        target.setBeneficiary(
                beneficiaryField.getText()
        );

        target.setReferenceNumber(
                referenceNumberField.getText()
        );

        target.setNotes(
                notesArea.getText()
        );
    }


    // =========================================================
    // SELECTED INTERNAL VALUE
    // =========================================================

    private String getSelectedValue(
            ComboBox<LocalizedOption> comboBox) {

        LocalizedOption selected =
                comboBox.getValue();

        return selected == null
                ? null
                : selected.value();
    }


    // =========================================================
    // PARSE AMOUNT
    // =========================================================

    private BigDecimal parseAmount() {

        String value =
                amountField.getText();

        if (value == null
                || value.isBlank()) {

            return null;
        }

        try {

            return new BigDecimal(
                    value.trim()
            );

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    LanguageManager.isArabic()
                            ? "يرجى إدخال مبلغ صحيح."
                            : "Please enter a valid expense amount."
            );
        }
    }


    // =========================================================
    // CANCEL
    // =========================================================

    @FXML
    private void handleCancel() {

        closeWindow();
    }


    // =========================================================
    // CLOSE WINDOW
    // =========================================================

    private void closeWindow() {

        Node node = titleField;

        if (node.getScene() == null
                || node.getScene()
                        .getWindow() == null) {

            return;
        }

        Stage stage =
                (Stage) node
                        .getScene()
                        .getWindow();

        stage.close();
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void showValidation(
            String message) {

        validationLabel.setText(
                message == null
                        ? text(
                                "expense.validation.required",
                                "Please check the form."
                        )
                        : translateValidationMessage(
                                message
                        )
        );

        validationLabel.setVisible(true);

        validationLabel.setManaged(true);
    }

    private void clearValidation() {

        validationLabel.setText("");

        validationLabel.setVisible(false);

        validationLabel.setManaged(false);
    }


    // =========================================================
    // SERVICE VALIDATION TRANSLATION
    // =========================================================

    private String translateValidationMessage(
            String message) {

        if (!LanguageManager.isArabic()
                || message == null) {

            return message;
        }

        return switch (message) {

            case "Expense title is required." ->
                    "اسم المصروف مطلوب.";

            case "Expense category is required." ->
                    "تصنيف المصروف مطلوب.";

            case "Expense amount is required." ->
                    "مبلغ المصروف مطلوب.";

            case "Expense amount must be greater than zero." ->
                    "يجب أن يكون مبلغ المصروف أكبر من صفر.";

            case "Expense date is required." ->
                    "تاريخ المصروف مطلوب.";

            case "Payment method is required." ->
                    "طريقة الدفع مطلوبة.";

            case "Expense status is required." ->
                    "حالة المصروف مطلوبة.";

            case "Invalid expense status." ->
                    "حالة المصروف غير صحيحة.";

            default -> message;
        };
    }


    // =========================================================
    // SUCCESS
    // =========================================================

    private void showSuccess(
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        alert.setTitle(
                text(
                        "expenses.title",
                        "Expenses"
                )
        );

        alert.setHeaderText(null);

        alert.setContentText(message);

        applyAlertDirection(alert);

        DialogStyler.style(alert);

        alert.showAndWait();
    }


    // =========================================================
    // ERROR
    // =========================================================

    private void showError(
            String title,
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(
                message == null
                        ? LanguageManager.isArabic()
                                ? "حدث خطأ غير متوقع."
                                : "Unexpected error."
                        : message
        );

        applyAlertDirection(alert);

        DialogStyler.style(alert);

        alert.showAndWait();
    }


    // =========================================================
    // ALERT DIRECTION
    // =========================================================

    private void applyAlertDirection(
            Alert alert) {

        if (alert.getDialogPane() != null) {

            alert.getDialogPane()
                    .setNodeOrientation(
                            LanguageManager.isArabic()
                                    ? NodeOrientation.RIGHT_TO_LEFT
                                    : NodeOrientation.LEFT_TO_RIGHT
                    );
        }
    }


    // =========================================================
    // RESOURCE BUNDLE
    // =========================================================

    private String text(
            String key,
            String fallback) {

        if (bundle != null
                && bundle.containsKey(key)) {

            return bundle.getString(key);
        }

        return fallback;
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }


    // =========================================================
    // LOCALIZED OPTION
    // =========================================================

    private record LocalizedOption(
            String value,
            String label) {

        @Override
        public String toString() {
            return label;
        }
    }
}
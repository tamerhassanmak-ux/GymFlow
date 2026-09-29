package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.ui.dialog.DialogStyler;
import com.mycompany.lazar_app.ui.WindowManager;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.Expense;
import com.mycompany.lazar_app.repository.ExpenseRepository;
import com.mycompany.lazar_app.service.ExpenseService;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class ExpensesController implements Initializable {

    // =========================================================
    // SERVICE
    // =========================================================

    private final ExpenseService expenseService =
            new ExpenseService(
                    new ExpenseRepository()
            );


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<Expense> expenses =
            FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final NumberFormat MONEY_FORMAT =
            NumberFormat.getNumberInstance(Locale.US);

    private ResourceBundle bundle;


    // =========================================================
    // ROOT
    // =========================================================

    @FXML
    private BorderPane rootPane;


    // =========================================================
    // PAGE HEADER
    // =========================================================

    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label pageSubtitleLabel;

    @FXML
    private Button refreshButton;

    @FXML
    private Button newExpenseButton;


    // =========================================================
    // KPI TITLES
    // =========================================================

    @FXML
    private Label totalExpensesTitleLabel;

    @FXML
    private Label monthExpensesTitleLabel;

    @FXML
    private Label pendingExpensesTitleLabel;

    @FXML
    private Label transactionsTitleLabel;


    // =========================================================
    // KPI VALUES
    // =========================================================

    @FXML
    private Label totalExpensesLabel;

    @FXML
    private Label monthExpensesLabel;

    @FXML
    private Label pendingExpensesLabel;

    @FXML
    private Label transactionsCountLabel;


    // =========================================================
    // FILTERS
    // =========================================================

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<FilterOption> categoryFilter;

    @FXML
    private ComboBox<FilterOption> statusFilter;

    @FXML
    private Button resetButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;


    // =========================================================
    // TABLE
    // =========================================================

    @FXML
    private TableView<Expense> expensesTable;

    @FXML
    private TableColumn<Expense, String> codeColumn;

    @FXML
    private TableColumn<Expense, String> titleColumn;

    @FXML
    private TableColumn<Expense, String> categoryColumn;

    @FXML
    private TableColumn<Expense, BigDecimal> amountColumn;

    @FXML
    private TableColumn<Expense, String> dateColumn;

    @FXML
    private TableColumn<Expense, String> paymentMethodColumn;

    @FXML
    private TableColumn<Expense, String> beneficiaryColumn;

    @FXML
    private TableColumn<Expense, String> statusColumn;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @Override
    public void initialize(
            URL location,
            ResourceBundle resources) {

        bundle = LanguageManager.getBundle();

        applyLanguage();

        configurePageTexts();

        configureTable();

        configureFilters();

        configureSmartSearch();

        configureDoubleClick();

        refreshAll();
    }


    // =========================================================
    // LANGUAGE
    // =========================================================

    private void applyLanguage() {

        rootPane.setNodeOrientation(
                LanguageManager.isArabic()
                        ? NodeOrientation.RIGHT_TO_LEFT
                        : NodeOrientation.LEFT_TO_RIGHT
        );
    }


    // =========================================================
    // PAGE TEXTS
    // =========================================================

    private void configurePageTexts() {

        pageTitleLabel.setText(
                text(
                        "expenses.title",
                        "Expenses"
                )
        );

        pageSubtitleLabel.setText(
                text(
                        "expenses.subtitle",
                        "Manage and track gym expenses"
                )
        );

        newExpenseButton.setText(
                text(
                        "expenses.add",
                        "+ Add Expense"
                )
        );

        refreshButton.setText(
                text(
                        "common.refresh",
                        "Refresh"
                )
        );

        totalExpensesTitleLabel.setText(
                text(
                        "expenses.stats.total",
                        "Total Expenses"
                )
        );

        monthExpensesTitleLabel.setText(
                text(
                        "expenses.stats.month",
                        "This Month"
                )
        );

        pendingExpensesTitleLabel.setText(
                text(
                        "expenses.stats.pending",
                        "Pending Payments"
                )
        );

        transactionsTitleLabel.setText(
                text(
                        "expenses.stats.transactions",
                        "Transactions"
                )
        );

        searchField.setPromptText(
                text(
                        "expenses.search",
                        "Search expenses..."
                )
        );

        resetButton.setText(
                LanguageManager.isArabic()
                        ? "إعادة تعيين"
                        : "Reset"
        );

        editButton.setText(
                LanguageManager.isArabic()
                        ? "تعديل"
                        : "Edit"
        );

        deleteButton.setText(
                LanguageManager.isArabic()
                        ? "حذف"
                        : "Delete"
        );

        codeColumn.setText(
                LanguageManager.isArabic()
                        ? "الكود"
                        : "Code"
        );

        titleColumn.setText(
                text(
                        "expenses.table.expense",
                        "Expense"
                )
        );

        categoryColumn.setText(
                text(
                        "expenses.table.category",
                        "Category"
                )
        );

        amountColumn.setText(
                text(
                        "expenses.table.amount",
                        "Amount"
                )
        );

        dateColumn.setText(
                text(
                        "expenses.table.date",
                        "Date"
                )
        );

        paymentMethodColumn.setText(
                text(
                        "expenses.table.paymentMethod",
                        "Payment Method"
                )
        );

        beneficiaryColumn.setText(
                LanguageManager.isArabic()
                        ? "المستفيد"
                        : "Beneficiary"
        );

        statusColumn.setText(
                text(
                        "expenses.table.status",
                        "Status"
                )
        );
    }


    // =========================================================
    // TABLE
    // =========================================================

    private void configureTable() {

        expensesTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        codeColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                safe(
                                        data.getValue()
                                                .getExpenseCode()
                                )
                        )
        );

        titleColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                safe(
                                        data.getValue()
                                                .getTitle()
                                )
                        )
        );

        // Category translated for display only.
        categoryColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                translateCategory(
                                        data.getValue()
                                                .getCategory()
                                )
                        )
        );

        amountColumn.setCellValueFactory(
                data ->
                        new SimpleObjectProperty<>(
                                data.getValue()
                                        .getAmount()
                        )
        );

        amountColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    BigDecimal amount,
                                    boolean empty) {

                                super.updateItem(
                                        amount,
                                        empty
                                );

                                if (empty || amount == null) {

                                    setText(null);

                                } else {

                                    setText(
                                            MONEY_FORMAT.format(
                                                    amount
                                            )
                                    );
                                }
                            }
                        }
        );

        dateColumn.setCellValueFactory(
                data -> {

                    if (data.getValue()
                            .getExpenseDate() == null) {

                        return new SimpleStringProperty("");
                    }

                    return new SimpleStringProperty(
                            data.getValue()
                                    .getExpenseDate()
                                    .format(DATE_FORMAT)
                    );
                }
        );

        // Payment method translated for display only.
        paymentMethodColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                translatePaymentMethod(
                                        data.getValue()
                                                .getPaymentMethod()
                                )
                        )
        );

        beneficiaryColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                safe(
                                        data.getValue()
                                                .getBeneficiary()
                                )
                        )
        );

        // Status translated for display only.
        statusColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                translateStatus(
                                        data.getValue()
                                                .getStatus()
                                )
                        )
        );

        expensesTable.setItems(expenses);

        expensesTable.setPlaceholder(
                new Label(
                        LanguageManager.isArabic()
                                ? "لا توجد مصروفات"
                                : "No expenses found"
                )
        );
    }


    // =========================================================
    // FILTERS
    // =========================================================

    private void configureFilters() {

        categoryFilter.setItems(
                FXCollections.observableArrayList(

                        new FilterOption(
                                "ALL",
                                text(
                                        "expenses.category.all",
                                        "All Categories"
                                )
                        ),

                        new FilterOption(
                                "Rent",
                                translateCategory("Rent")
                        ),

                        new FilterOption(
                                "Electricity",
                                translateCategory("Electricity")
                        ),

                        new FilterOption(
                                "Water",
                                translateCategory("Water")
                        ),

                        new FilterOption(
                                "Maintenance",
                                translateCategory("Maintenance")
                        ),

                        new FilterOption(
                                "Equipment",
                                translateCategory("Equipment")
                        ),

                        new FilterOption(
                                "Salaries",
                                translateCategory("Salaries")
                        ),

                        new FilterOption(
                                "Cleaning",
                                translateCategory("Cleaning")
                        ),

                        new FilterOption(
                                "Marketing",
                                translateCategory("Marketing")
                        ),

                        new FilterOption(
                                "Internet",
                                translateCategory("Internet")
                        ),

                        new FilterOption(
                                "Transportation",
                                translateCategory("Transportation")
                        ),

                        new FilterOption(
                                "Supplies",
                                translateCategory("Supplies")
                        ),

                        new FilterOption(
                                "Taxes",
                                translateCategory("Taxes")
                        ),

                        new FilterOption(
                                "Insurance",
                                translateCategory("Insurance")
                        ),

                        new FilterOption(
                                "Other",
                                translateCategory("Other")
                        )
                )
        );

        statusFilter.setItems(
                FXCollections.observableArrayList(

                        new FilterOption(
                                "ALL",
                                text(
                                        "expenses.status.all",
                                        "All Status"
                                )
                        ),

                        new FilterOption(
                                "PAID",
                                translateStatus("PAID")
                        ),

                        new FilterOption(
                                "PENDING",
                                translateStatus("PENDING")
                        ),

                        new FilterOption(
                                "CANCELLED",
                                translateStatus("CANCELLED")
                        )
                )
        );

        configureFilterRendering(categoryFilter);

        configureFilterRendering(statusFilter);

        selectFilterValue(
                categoryFilter,
                "ALL"
        );

        selectFilterValue(
                statusFilter,
                "ALL"
        );

        categoryFilter.valueProperty()
                .addListener(
                        (observable,
                         oldValue,
                         newValue) ->
                                applySearch()
                );

        statusFilter.valueProperty()
                .addListener(
                        (observable,
                         oldValue,
                         newValue) ->
                                applySearch()
                );
    }


    // =========================================================
    // FILTER RENDERING
    // =========================================================

    private void configureFilterRendering(
            ComboBox<FilterOption> comboBox) {

        comboBox.setCellFactory(
                listView ->
                        new ListCell<>() {

                            @Override
                            protected void updateItem(
                                    FilterOption item,
                                    boolean empty) {

                                super.updateItem(item, empty);

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
                            FilterOption item,
                            boolean empty) {

                        super.updateItem(item, empty);

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
    // SMART SEARCH
    // =========================================================

    private void configureSmartSearch() {

        searchField.textProperty()
                .addListener(
                        (observable,
                         oldValue,
                         newValue) ->
                                applySearch()
                );
    }


    // =========================================================
    // SEARCH
    // =========================================================

    private void applySearch() {

        try {

            String keyword =
                    searchField.getText();

            String category =
                    getFilterValue(
                            categoryFilter
                    );

            String status =
                    getFilterValue(
                            statusFilter
                    );

            List<Expense> result =
                    expenseService.search(
                            keyword,
                            category,
                            status
                    );

            expenses.setAll(result);

        } catch (Exception exception) {

            showError(
                    LanguageManager.isArabic()
                            ? "خطأ في البحث"
                            : "Search Error",
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // REFRESH
    // =========================================================

    @FXML
    private void refreshAll() {

        try {

            /*
             * Keep current filters when refreshing.
             */
            if (categoryFilter != null
                    && statusFilter != null
                    && categoryFilter.getValue() != null
                    && statusFilter.getValue() != null) {

                applySearch();

            } else {

                loadExpenses();
            }

            loadStatistics();

        } catch (Exception exception) {

            showError(
                    LanguageManager.isArabic()
                            ? "خطأ في المصروفات"
                            : "Expenses Error",
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // LOAD EXPENSES
    // =========================================================

    private void loadExpenses() {

        List<Expense> result =
                expenseService.getAllExpenses();

        expenses.setAll(result);
    }


    // =========================================================
    // STATISTICS
    // =========================================================

    private void loadStatistics() {

        BigDecimal total =
                expenseService.getTotalExpenses();

        BigDecimal month =
                expenseService
                        .getCurrentMonthExpenses();

        BigDecimal pending =
                expenseService
                        .getPendingAmount();

        long transactions =
                expenseService
                        .getTransactionCount();

        totalExpensesLabel.setText(
                formatMoney(total)
        );

        monthExpensesLabel.setText(
                formatMoney(month)
        );

        pendingExpensesLabel.setText(
                formatMoney(pending)
        );

        transactionsCountLabel.setText(
                String.valueOf(transactions)
        );
    }


    // =========================================================
    // NEW
    // =========================================================

    @FXML
    private void handleNewExpense() {

        openExpenseForm(null);
    }


    // =========================================================
    // EDIT
    // =========================================================

    @FXML
    private void handleEditExpense() {

        Expense selected =
                expensesTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (selected == null) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "اختيار مصروف"
                            : "Select Expense",

                    LanguageManager.isArabic()
                            ? "يرجى اختيار المصروف المراد تعديله."
                            : "Please select an expense to edit."
            );

            return;
        }

        openExpenseForm(selected);
    }


    // =========================================================
    // DELETE
    // =========================================================

    @FXML
    private void handleDeleteExpense() {

        Expense selected =
                expensesTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (selected == null) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "اختيار مصروف"
                            : "Select Expense",

                    LanguageManager.isArabic()
                            ? "يرجى اختيار المصروف المراد حذفه."
                            : "Please select an expense to delete."
            );

            return;
        }

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                LanguageManager.isArabic()
                        ? "حذف المصروف"
                        : "Delete Expense"
        );

        confirmation.setHeaderText(
                LanguageManager.isArabic()
                        ? "حذف المصروف "
                                + selected.getExpenseCode()
                                + "؟"
                        : "Delete "
                                + selected.getExpenseCode()
                                + "?"
        );

        confirmation.setContentText(
                LanguageManager.isArabic()
                        ? "لا يمكن التراجع عن هذه العملية."
                        : "This operation cannot be undone."
        );

        applyAlertDirection(confirmation);

        DialogStyler.style(confirmation);

        Optional<ButtonType> result =
                WindowManager.showResponsiveDialogAndWait(
                        confirmation,
                        getOwnerWindow(),
                        520.0,
                        320.0
                );

        if (result.isEmpty()
                || result.get() != ButtonType.OK) {

            return;
        }

        try {

            expenseService.deleteExpense(
                    selected.getId()
            );

            refreshAll();

        } catch (Exception exception) {

            showError(
                    LanguageManager.isArabic()
                            ? "خطأ في الحذف"
                            : "Delete Error",
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // RESET FILTERS
    // =========================================================

    @FXML
    private void handleResetFilters() {

        searchField.clear();

        selectFilterValue(
                categoryFilter,
                "ALL"
        );

        selectFilterValue(
                statusFilter,
                "ALL"
        );

        refreshAll();
    }


    // =========================================================
    // DOUBLE CLICK
    // =========================================================

    private void configureDoubleClick() {

        expensesTable.setRowFactory(
                table -> {

                    TableRow<Expense> row =
                            new TableRow<>();

                    row.setOnMouseClicked(
                            event -> {

                                if (event.getClickCount() == 2
                                        && !row.isEmpty()) {

                                    openExpenseForm(
                                            row.getItem()
                                    );
                                }
                            }
                    );

                    return row;
                }
        );
    }


    // =========================================================
    // OPEN EXPENSE FORM
    // =========================================================

    private void openExpenseForm(
            Expense expense) {

        try {

            URL resource =
                    getClass().getResource(
                            "/fxml/expense-form.fxml"
                    );

            if (resource == null) {

                throw new IOException(
                        "FXML file not found: /fxml/expense-form.fxml"
                );
            }

            FXMLLoader loader =
                    new FXMLLoader(resource);

            Parent root =
                    loader.load();

            ExpenseFormController controller =
                    loader.getController();

            controller.setExpenseService(
                    expenseService
            );

            controller.setExpense(
                    expense
            );

            controller.setOnSaved(
                    this::refreshAll
            );

            String windowTitle =
                    expense == null
                            ? text(
                                    "expense.new",
                                    "New Expense"
                            )
                            : text(
                                    "expense.edit",
                                    "Edit Expense"
                            );

            Stage stage =
                    WindowManager.createModalStage(
                            root,
                            windowTitle,
                            getOwnerWindow()
                    );

            WindowManager.showMaximizedAndWait(
                    stage,
                    900.0,
                    650.0
            );

        } catch (IOException exception) {

            showError(
                    LanguageManager.isArabic()
                            ? "فتح نموذج المصروف"
                            : "Open Expense Form",
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // CATEGORY TRANSLATION
    // =========================================================

    private String translateCategory(
            String value) {

        if (value == null) {
            return "";
        }

        return switch (value.toLowerCase(Locale.ROOT)) {

            case "rent" ->
                    text(
                            "expense.category.rent",
                            "Rent"
                    );

            case "electricity" ->
                    text(
                            "expense.category.electricity",
                            "Electricity"
                    );

            case "water" ->
                    text(
                            "expense.category.water",
                            "Water"
                    );

            case "maintenance" ->
                    text(
                            "expense.category.maintenance",
                            "Maintenance"
                    );

            case "equipment" ->
                    text(
                            "expense.category.equipment",
                            "Equipment"
                    );

            case "salaries" ->
                    text(
                            "expense.category.salaries",
                            "Salaries"
                    );

            case "cleaning" ->
                    text(
                            "expense.category.cleaning",
                            "Cleaning"
                    );

            case "marketing" ->
                    text(
                            "expense.category.marketing",
                            "Marketing"
                    );

            case "internet" ->
                    text(
                            "expense.category.internet",
                            "Internet"
                    );

            case "transportation" ->
                    text(
                            "expense.category.transportation",
                            "Transportation"
                    );

            case "supplies" ->
                    text(
                            "expense.category.supplies",
                            "Supplies"
                    );

            case "taxes" ->
                    text(
                            "expense.category.taxes",
                            "Taxes"
                    );

            case "insurance" ->
                    text(
                            "expense.category.insurance",
                            "Insurance"
                    );

            case "other" ->
                    text(
                            "expense.category.other",
                            "Other"
                    );

            default -> value;
        };
    }


    // =========================================================
    // PAYMENT METHOD TRANSLATION
    // =========================================================

    private String translatePaymentMethod(
            String value) {

        if (value == null) {
            return "";
        }

        return switch (value.toUpperCase(Locale.ROOT)) {

            case "CASH" ->
                    text(
                            "expense.payment.cash",
                            "Cash"
                    );

            case "CARD" ->
                    text(
                            "expense.payment.card",
                            "Card"
                    );

            case "BANK_TRANSFER" ->
                    text(
                            "expense.payment.bank_transfer",
                            "Bank Transfer"
                    );

            case "CHEQUE" ->
                    text(
                            "expense.payment.cheque",
                            "Cheque"
                    );

            case "OTHER" ->
                    text(
                            "expense.payment.other",
                            "Other"
                    );

            default -> value;
        };
    }


    // =========================================================
    // STATUS TRANSLATION
    // =========================================================

    private String translateStatus(
            String value) {

        if (value == null) {
            return "";
        }

        return switch (value.toUpperCase(Locale.ROOT)) {

            case "PAID" ->
                    text(
                            "expense.status.paid",
                            "Paid"
                    );

            case "PENDING" ->
                    text(
                            "expense.status.pending",
                            "Pending"
                    );

            case "CANCELLED" ->
                    text(
                            "expense.status.cancelled",
                            "Cancelled"
                    );

            default -> value;
        };
    }


    // =========================================================
    // FILTER VALUES
    // =========================================================

    private String getFilterValue(
            ComboBox<FilterOption> comboBox) {

        FilterOption option =
                comboBox.getValue();

        return option == null
                ? "ALL"
                : option.value();
    }

    private void selectFilterValue(
            ComboBox<FilterOption> comboBox,
            String value) {

        for (FilterOption option
                : comboBox.getItems()) {

            if (option.value()
                    .equalsIgnoreCase(value)) {

                comboBox.setValue(option);

                return;
            }
        }
    }


    // =========================================================
    // MONEY
    // =========================================================

    private String formatMoney(
            BigDecimal amount) {

        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        return MONEY_FORMAT.format(amount);
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
    // SAFE
    // =========================================================

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }


    // =========================================================
    // ALERT DIRECTION
    // =========================================================

    private void applyAlertDirection(
            Alert alert) {

        alert.getDialogPane()
                .setNodeOrientation(
                        LanguageManager.isArabic()
                                ? NodeOrientation.RIGHT_TO_LEFT
                                : NodeOrientation.LEFT_TO_RIGHT
                );
    }


    // =========================================================
    // WARNING
    // =========================================================

    private void showWarning(
            String title,
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.WARNING
                );

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(message);

        applyAlertDirection(alert);

        DialogStyler.style(alert);

        WindowManager.showResponsiveDialogAndWait(
                alert,
                getOwnerWindow(),
                520.0,
                320.0
        );
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

        WindowManager.showResponsiveDialogAndWait(
                alert,
                getOwnerWindow(),
                520.0,
                320.0
        );
    }


    // =========================================================
    // OWNER WINDOW
    // =========================================================

    private Window getOwnerWindow() {

        if (expensesTable == null
                || expensesTable.getScene() == null) {
            return null;
        }

        return expensesTable
                .getScene()
                .getWindow();
    }


    // =========================================================
    // FILTER OPTION
    // =========================================================

    private record FilterOption(
            String value,
            String label) {

        @Override
        public String toString() {
            return label;
        }
    }
}
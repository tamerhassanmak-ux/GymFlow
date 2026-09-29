package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.ui.dialog.DialogStyler;
import com.mycompany.lazar_app.ui.WindowManager;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.report.FinancialActivity;
import com.mycompany.lazar_app.model.report.MonthlyFinancialSummary;
import com.mycompany.lazar_app.model.report.ReportSummary;
import com.mycompany.lazar_app.model.report.ReportType;
import com.mycompany.lazar_app.repository.ReportRepository;
import com.mycompany.lazar_app.service.ReportService;
import com.mycompany.lazar_app.service.export.ExcelReportExporter;
import com.mycompany.lazar_app.service.export.PdfReportExporter;

import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class ReportsController {

    // =========================================================
    // SERVICES
    // =========================================================

    private final ReportService reportService =
            new ReportService(
                    new ReportRepository()
            );


    // =========================================================
    // STATE
    // =========================================================

    private boolean showAllActivity = false;


    // =========================================================
    // HEADER
    // =========================================================

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Button exportButton;


    // =========================================================
    // FILTERS
    // =========================================================

    @FXML
    private Label reportLabel;

    @FXML
    private ComboBox<ReportType> reportTypeComboBox;

    @FXML
    private Label fromLabel;

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private Label toLabel;

    @FXML
    private DatePicker toDatePicker;

    @FXML
    private Button generateButton;


    // =========================================================
    // KPI CARDS
    // =========================================================

    @FXML
    private Label totalRevenueTitleLabel;

    @FXML
    private Label totalRevenueValueLabel;

    @FXML
    private Label revenueChangeLabel;

    @FXML
    private Label totalExpensesTitleLabel;

    @FXML
    private Label totalExpensesValueLabel;

    @FXML
    private Label transactionsLabel;

    @FXML
    private Label netProfitTitleLabel;

    @FXML
    private Label netProfitValueLabel;

    @FXML
    private Label revenueMinusExpensesLabel;

    @FXML
    private Label activeMembersTitleLabel;

    @FXML
    private Label activeMembersValueLabel;

    @FXML
    private Label newMembersLabel;


    // =========================================================
    // CHART
    // =========================================================

    @FXML
    private Label revenueExpensesTitleLabel;

    @FXML
    private BarChart<String, Number> financialChart;

    @FXML
    private CategoryAxis monthAxis;

    @FXML
    private NumberAxis amountAxis;


    // =========================================================
    // REPORT SUMMARY
    // =========================================================

    @FXML
    private Label summaryTitleLabel;

    @FXML
    private Label membershipRevenueLabel;

    @FXML
    private Label membershipRevenueValueLabel;

    @FXML
    private Label otherRevenueLabel;

    @FXML
    private Label otherRevenueValueLabel;

    @FXML
    private Label operatingExpensesLabel;

    @FXML
    private Label operatingExpensesValueLabel;

    @FXML
    private Label profitMarginLabel;

    @FXML
    private Label profitMarginValueLabel;


    // =========================================================
    // FINANCIAL ACTIVITY
    // =========================================================

    @FXML
    private Label recentActivityTitleLabel;

    @FXML
    private Button viewAllButton;

    @FXML
    private TableView<FinancialActivity> financialActivityTable;

    @FXML
    private TableColumn<FinancialActivity, String> dateColumn;

    @FXML
    private TableColumn<FinancialActivity, String> descriptionColumn;

    @FXML
    private TableColumn<FinancialActivity, String> typeColumn;

    @FXML
    private TableColumn<FinancialActivity, BigDecimal> amountColumn;

    @FXML
    private TableColumn<FinancialActivity, String> paymentMethodColumn;

    @FXML
    private TableColumn<FinancialActivity, String> statusColumn;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        configureLanguage();

        configureReportTypes();

        configureDateRange();

        configureTable();

        configureActions();

        generateReport();
    }


    // =========================================================
    // UPDATE LANGUAGE
    // =========================================================

    public void updateLanguage() {

        configureLanguage();

        configureReportTypes();

        configureTable();

        generateReport();
    }


    // =========================================================
    // LANGUAGE
    // =========================================================

    private void configureLanguage() {

        boolean arabic =
                LanguageManager.isArabic();


        setText(
                titleLabel,
                arabic
                        ? "التقارير والتحليلات"
                        : "Reports & Analytics"
        );

        setText(
                subtitleLabel,
                arabic
                        ? "متابعة الأداء المالي وأداء العضويات في الصالة الرياضية"
                        : "Monitor gym financial and membership performance"
        );

        setText(
                exportButton,
                arabic
                        ? "تصدير التقرير"
                        : "Export Report"
        );

        setText(
                reportLabel,
                arabic
                        ? "التقرير:"
                        : "Report:"
        );

        if (reportTypeComboBox != null) {

            reportTypeComboBox.setPromptText(
                    arabic
                            ? "نوع التقرير"
                            : "Report Type"
            );
        }

        setText(
                fromLabel,
                arabic
                        ? "من:"
                        : "From:"
        );

        setText(
                toLabel,
                arabic
                        ? "إلى:"
                        : "To:"
        );

        setText(
                generateButton,
                arabic
                        ? "إنشاء التقرير"
                        : "Generate"
        );


        // =====================================================
        // KPI
        // =====================================================

        setText(
                totalRevenueTitleLabel,
                arabic
                        ? "إجمالي الإيرادات"
                        : "Total Revenue"
        );

        setText(
                totalExpensesTitleLabel,
                arabic
                        ? "إجمالي المصروفات"
                        : "Total Expenses"
        );

        setText(
                netProfitTitleLabel,
                arabic
                        ? "صافي الربح"
                        : "Net Profit"
        );

        setText(
                revenueMinusExpensesLabel,
                arabic
                        ? "الإيرادات - المصروفات"
                        : "Revenue - Expenses"
        );

        setText(
                activeMembersTitleLabel,
                arabic
                        ? "الأعضاء النشطون"
                        : "Active Members"
        );


        // =====================================================
        // CHART
        // =====================================================

        setText(
                revenueExpensesTitleLabel,
                arabic
                        ? "الإيرادات والمصروفات"
                        : "Revenue & Expenses"
        );

        if (financialChart != null) {

            financialChart.setTitle(
                    arabic
                            ? "الأداء المالي الشهري"
                            : "Monthly Financial Performance"
            );
        }

        if (monthAxis != null) {

            monthAxis.setLabel(
                    arabic
                            ? "الشهر"
                            : "Month"
            );
        }

        if (amountAxis != null) {

            amountAxis.setLabel(
                    arabic
                            ? "المبلغ (ج.م)"
                            : "Amount (EGP)"
            );
        }


        // =====================================================
        // SUMMARY
        // =====================================================

        setText(
                summaryTitleLabel,
                arabic
                        ? "ملخص التقرير"
                        : "Report Summary"
        );

        setText(
                membershipRevenueLabel,
                arabic
                        ? "إيرادات العضويات"
                        : "Membership Revenue"
        );

        setText(
                otherRevenueLabel,
                arabic
                        ? "إيرادات أخرى"
                        : "Other Revenue"
        );

        setText(
                operatingExpensesLabel,
                arabic
                        ? "المصروفات التشغيلية"
                        : "Operating Expenses"
        );

        setText(
                profitMarginLabel,
                arabic
                        ? "هامش الربح"
                        : "Profit Margin"
        );


        // =====================================================
        // ACTIVITY
        // =====================================================

        setText(
                recentActivityTitleLabel,
                arabic
                        ? "أحدث الأنشطة المالية"
                        : "Recent Financial Activity"
        );

        updateViewAllButton();


        // =====================================================
        // TABLE
        // =====================================================

        if (dateColumn != null) {

            dateColumn.setText(
                    arabic
                            ? "التاريخ"
                            : "Date"
            );
        }

        if (descriptionColumn != null) {

            descriptionColumn.setText(
                    arabic
                            ? "الوصف"
                            : "Description"
            );
        }

        if (typeColumn != null) {

            typeColumn.setText(
                    arabic
                            ? "النوع"
                            : "Type"
            );
        }

        if (amountColumn != null) {

            amountColumn.setText(
                    arabic
                            ? "المبلغ"
                            : "Amount"
            );
        }

        if (paymentMethodColumn != null) {

            paymentMethodColumn.setText(
                    arabic
                            ? "طريقة الدفع"
                            : "Payment Method"
            );
        }

        if (statusColumn != null) {

            statusColumn.setText(
                    arabic
                            ? "الحالة"
                            : "Status"
            );
        }


        if (financialActivityTable != null) {

            financialActivityTable.setNodeOrientation(
                    arabic
                            ? NodeOrientation.RIGHT_TO_LEFT
                            : NodeOrientation.LEFT_TO_RIGHT
            );
        }
    }


    // =========================================================
    // REPORT TYPES
    // =========================================================

    private void configureReportTypes() {

        if (reportTypeComboBox == null) {
            return;
        }


        ReportType current =
                reportTypeComboBox.getValue();


        reportTypeComboBox.setItems(
                FXCollections.observableArrayList(
                        ReportType.values()
                )
        );


        reportTypeComboBox.setCellFactory(
                list ->
                        new ListCell<>() {

                            @Override
                            protected void updateItem(
                                    ReportType item,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        item,
                                        empty
                                );

                                setText(
                                        empty || item == null
                                                ? null
                                                : item.getDisplayName()
                                );
                            }
                        }
        );


        reportTypeComboBox.setButtonCell(
                new ListCell<>() {

                    @Override
                    protected void updateItem(
                            ReportType item,
                            boolean empty
                    ) {

                        super.updateItem(
                                item,
                                empty
                        );

                        setText(
                                empty || item == null
                                        ? null
                                        : item.getDisplayName()
                        );
                    }
                }
        );


        if (current != null) {

            reportTypeComboBox.setValue(
                    current
            );

        } else {

            reportTypeComboBox
                    .getSelectionModel()
                    .select(
                            ReportType.FINANCIAL_SUMMARY
                    );
        }
    }


    // =========================================================
    // DATE RANGE
    // =========================================================

    private void configureDateRange() {

        LocalDate today =
                LocalDate.now();


        if (fromDatePicker != null) {

            fromDatePicker.setValue(
                    today.withDayOfMonth(1)
            );
        }


        if (toDatePicker != null) {

            toDatePicker.setValue(
                    today
            );
        }
    }


    // =========================================================
    // TABLE
    // =========================================================

    private void configureTable() {

        if (financialActivityTable == null) {
            return;
        }


        financialActivityTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );


        dateColumn.setCellValueFactory(
                cell -> {

                    LocalDateTime date =
                            cell.getValue()
                                    .getDate();


                    String value =
                            date == null
                                    ? "-"
                                    : date.format(
                                            DateTimeFormatter.ofPattern(
                                                    "yyyy-MM-dd HH:mm"
                                            )
                                    );


                    return new ReadOnlyStringWrapper(
                            value
                    );
                }
        );


        descriptionColumn.setCellValueFactory(
                cell ->
                        new ReadOnlyStringWrapper(
                                safe(
                                        cell.getValue()
                                                .getDescription()
                                )
                        )
        );


        typeColumn.setCellValueFactory(
                cell ->
                        new ReadOnlyStringWrapper(
                                translateType(
                                        cell.getValue()
                                                .getType()
                                )
                        )
        );


        amountColumn.setCellValueFactory(
                cell ->
                        new ReadOnlyObjectWrapper<>(
                                cell.getValue()
                                        .getAmount()
                        )
        );


        amountColumn.setCellFactory(
                column ->
                        new javafx.scene.control.TableCell<>() {

                            @Override
                            protected void updateItem(
                                    BigDecimal amount,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        amount,
                                        empty
                                );


                                if (empty
                                        || amount == null) {

                                    setText(null);

                                } else {

                                    setText(
                                            formatMoney(
                                                    amount
                                            )
                                    );
                                }
                            }
                        }
        );


        paymentMethodColumn.setCellValueFactory(
                cell ->
                        new ReadOnlyStringWrapper(
                                translatePaymentMethod(
                                        cell.getValue()
                                                .getPaymentMethod()
                                )
                        )
        );


        statusColumn.setCellValueFactory(
                cell ->
                        new ReadOnlyStringWrapper(
                                translateStatus(
                                        cell.getValue()
                                                .getStatus()
                                )
                        )
        );


        financialActivityTable.setPlaceholder(
                new Label(
                        LanguageManager.isArabic()
                                ? "لا توجد عمليات مالية خلال الفترة المحددة"
                                : "No financial activity for the selected period"
                )
        );
    }


    // =========================================================
    // ACTIONS
    // =========================================================

    private void configureActions() {

        if (generateButton != null) {

            generateButton.setOnAction(
                    event ->
                            generateReport()
            );
        }


        if (viewAllButton != null) {

            viewAllButton.setOnAction(
                    event -> {

                        showAllActivity =
                                !showAllActivity;

                        loadFinancialActivity();

                        updateViewAllButton();
                    }
            );
        }


        if (reportTypeComboBox != null) {

            reportTypeComboBox.setOnAction(
                    event ->
                            generateReport()
            );
        }


        if (fromDatePicker != null) {

            fromDatePicker.setOnAction(
                    event ->
                            generateReport()
            );
        }


        if (toDatePicker != null) {

            toDatePicker.setOnAction(
                    event ->
                            generateReport()
            );
        }


        if (exportButton != null) {

            exportButton.setOnAction(
                    event ->
                            exportReport()
            );
        }
    }


    // =========================================================
    // GENERATE
    // =========================================================

    @FXML
    private void handleGenerateReport() {

        generateReport();
    }


    private void generateReport() {

        try {

            LocalDate from =
                    fromDatePicker.getValue();

            LocalDate to =
                    toDatePicker.getValue();


            validateDateRange(
                    from,
                    to
            );


            ReportType reportType =
                    getSelectedReportType();


            ReportSummary summary =
                    reportService.getSummary(
                            from,
                            to,
                            reportType
                    );


            updateSummary(
                    summary
            );


            loadChart(
                    from,
                    to,
                    reportType
            );


            loadFinancialActivity();


        } catch (Exception exception) {

            exception.printStackTrace();

            showError(
                    LanguageManager.isArabic()
                            ? "تعذر إنشاء التقرير"
                            : "Unable to generate report",

                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // SELECTED REPORT TYPE
    // =========================================================

    private ReportType getSelectedReportType() {

        if (reportTypeComboBox == null
                || reportTypeComboBox.getValue() == null) {

            return ReportType.FINANCIAL_SUMMARY;
        }


        return reportTypeComboBox.getValue();
    }


    // =========================================================
    // SUMMARY
    // =========================================================

    private void updateSummary(
            ReportSummary summary
    ) {

        totalRevenueValueLabel.setText(
                formatMoney(
                        summary.getTotalRevenue()
                )
        );


        totalExpensesValueLabel.setText(
                formatMoney(
                        summary.getTotalExpenses()
                )
        );


        netProfitValueLabel.setText(
                formatMoney(
                        summary.getNetProfit()
                )
        );


        activeMembersValueLabel.setText(
                NumberFormat
                        .getIntegerInstance(
                                LanguageManager.isArabic()
                                        ? Locale.forLanguageTag("ar")
                                        : Locale.US
                        )
                        .format(
                                summary.getActiveMembers()
                        )
        );


        membershipRevenueValueLabel.setText(
                formatMoney(
                        summary.getMembershipRevenue()
                )
        );


        otherRevenueValueLabel.setText(
                formatMoney(
                        summary.getOtherRevenue()
                )
        );


        operatingExpensesValueLabel.setText(
                formatMoney(
                        summary.getTotalExpenses()
                )
        );


        profitMarginValueLabel.setText(
                String.format(
                        Locale.US,
                        "%.2f%%",
                        summary.getProfitMargin()
                                .doubleValue()
                )
        );


        if (LanguageManager.isArabic()) {

            transactionsLabel.setText(
                    summary.getTransactions()
                            + " معاملة"
            );

            newMembersLabel.setText(
                    "+"
                            + summary.getNewMembers()
                            + " عضو جديد"
            );

            revenueChangeLabel.setText(
                    "الإيرادات المحققة خلال الفترة"
            );

        } else {

            transactionsLabel.setText(
                    summary.getTransactions()
                            + " transactions"
            );

            newMembersLabel.setText(
                    "+"
                            + summary.getNewMembers()
                            + " new members"
            );

            revenueChangeLabel.setText(
                    "Realized revenue for selected period"
            );
        }


        updateProfitStyle(
                summary.getNetProfit()
        );
    }


    // =========================================================
    // PROFIT STYLE
    // =========================================================

    private void updateProfitStyle(
            BigDecimal profit
    ) {

        if (profit == null) {
            return;
        }


        if (profit.compareTo(
                BigDecimal.ZERO
        ) >= 0) {

            netProfitValueLabel.setStyle(
                    "-fx-font-size: 22px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #2563EB;"
            );

            profitMarginValueLabel.setStyle(
                    "-fx-font-size: 22px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #16A34A;"
            );

        } else {

            netProfitValueLabel.setStyle(
                    "-fx-font-size: 22px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #DC2626;"
            );

            profitMarginValueLabel.setStyle(
                    "-fx-font-size: 22px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #DC2626;"
            );
        }
    }


    // =========================================================
    // CHART
    // =========================================================

    private void loadChart(
            LocalDate from,
            LocalDate to,
            ReportType reportType
    ) throws Exception {

        List<MonthlyFinancialSummary> data =
                reportService
                        .getMonthlyFinancialSummary(
                                from,
                                to,
                                reportType
                        );


        financialChart
                .getData()
                .clear();


        XYChart.Series<String, Number> revenueSeries =
                new XYChart.Series<>();

        XYChart.Series<String, Number> expenseSeries =
                new XYChart.Series<>();


        revenueSeries.setName(
                LanguageManager.isArabic()
                        ? "الإيرادات"
                        : "Revenue"
        );

        expenseSeries.setName(
                LanguageManager.isArabic()
                        ? "المصروفات"
                        : "Expenses"
        );


        Locale locale =
                LanguageManager.isArabic()
                        ? Locale.forLanguageTag("ar")
                        : Locale.ENGLISH;


        for (MonthlyFinancialSummary item
                : data) {

            String month =
                    item.getMonth()
                            .getMonth()
                            .getDisplayName(
                                    TextStyle.SHORT,
                                    locale
                            );


            String label =
                    month
                    + " "
                    + item.getMonth()
                            .getYear();


            revenueSeries
                    .getData()
                    .add(
                            new XYChart.Data<>(
                                    label,
                                    item.getRevenue()
                            )
                    );


            expenseSeries
                    .getData()
                    .add(
                            new XYChart.Data<>(
                                    label,
                                    item.getExpenses()
                            )
                    );
        }


        switch (reportType) {

            case REVENUE, MEMBERSHIPS ->

                financialChart
                        .getData()
                        .add(
                                revenueSeries
                        );


            case EXPENSES ->

                financialChart
                        .getData()
                        .add(
                                expenseSeries
                        );


            case FINANCIAL_SUMMARY ->

                financialChart
                        .getData()
                        .addAll(
                                revenueSeries,
                                expenseSeries
                        );
        }
    }


    // =========================================================
    // FINANCIAL ACTIVITY
    // =========================================================

    private void loadFinancialActivity() {

        try {

            LocalDate from =
                    fromDatePicker.getValue();

            LocalDate to =
                    toDatePicker.getValue();


            if (from == null
                    || to == null) {

                return;
            }


            ReportType reportType =
                    getSelectedReportType();


            List<FinancialActivity> activities;


            if (showAllActivity) {

                activities =
                        reportService.getAllActivity(
                                from,
                                to,
                                reportType
                        );

            } else {

                activities =
                        reportService.getRecentActivity(
                                from,
                                to,
                                reportType
                        );
            }


            financialActivityTable.setItems(
                    FXCollections.observableArrayList(
                            activities
                    )
            );


        } catch (Exception exception) {

            exception.printStackTrace();

            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تحميل العمليات المالية"
                            : "Unable to load financial activity",

                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // EXPORT
    // =========================================================

    private void exportReport() {

        try {

            boolean arabic =
                    LanguageManager.isArabic();


            LocalDate from =
                    fromDatePicker.getValue();

            LocalDate to =
                    toDatePicker.getValue();


            validateDateRange(
                    from,
                    to
            );


            ReportType reportType =
                    getSelectedReportType();


            ChoiceDialog<String> dialog =
                    new ChoiceDialog<>(
                            "Excel (.xlsx)",
                            List.of(
                                    "Excel (.xlsx)",
                                    "PDF (.pdf)"
                            )
                    );


            dialog.setTitle(
                    arabic
                            ? "تصدير التقرير"
                            : "Export Report"
            );

            dialog.setHeaderText(
                    arabic
                            ? "اختر صيغة التقرير"
                            : "Select export format"
            );

            dialog.setContentText(
                    arabic
                            ? "الصيغة:"
                            : "Format:"
            );


            DialogStyler.style(dialog);

            dialog.getDialogPane().setNodeOrientation(
                    arabic
                            ? NodeOrientation.RIGHT_TO_LEFT
                            : NodeOrientation.LEFT_TO_RIGHT
            );

            /*
             * Centralized dialog sizing + centering.
             * The export selector is centered over the Reports page owner,
             * not over the whole monitor.
             */
            var choice =
                    WindowManager.showResponsiveDialogAndWait(
                            dialog,
                            getOwnerWindow(),
                            560.0,
                            360.0
                    );


            if (choice.isEmpty()) {
                return;
            }


            String format =
                    choice.get();


            FileChooser chooser =
                    new FileChooser();


            chooser.setTitle(
                    arabic
                            ? "حفظ التقرير"
                            : "Save Report"
            );


            String baseName =
                    createReportFileName(
                            reportType,
                            from,
                            to
                    );


            boolean excel =
                    format.startsWith(
                            "Excel"
                    );


            if (excel) {

                chooser.getExtensionFilters()
                        .add(
                                new FileChooser.ExtensionFilter(
                                        "Excel Workbook (*.xlsx)",
                                        "*.xlsx"
                                )
                        );

                chooser.setInitialFileName(
                        baseName + ".xlsx"
                );

            } else {

                chooser.getExtensionFilters()
                        .add(
                                new FileChooser.ExtensionFilter(
                                        "PDF Document (*.pdf)",
                                        "*.pdf"
                                )
                        );

                chooser.setInitialFileName(
                        baseName + ".pdf"
                );
            }


            Window owner =
                    exportButton != null
                    && exportButton.getScene() != null
                            ? exportButton
                                    .getScene()
                                    .getWindow()
                            : null;


            File file =
                    chooser.showSaveDialog(
                            owner
                    );


            if (file == null) {
                return;
            }


            ReportSummary summary =
                    reportService.getSummary(
                            from,
                            to,
                            reportType
                    );


            List<FinancialActivity> activities =
                    reportService.getAllActivity(
                            from,
                            to,
                            reportType
                    );


            if (excel) {

                file =
                        ensureExtension(
                                file,
                                ".xlsx"
                        );


                new ExcelReportExporter()
                        .export(
                                file,
                                reportType,
                                from,
                                to,
                                summary,
                                activities
                        );

            } else {

                file =
                        ensureExtension(
                                file,
                                ".pdf"
                        );


                new PdfReportExporter()
                        .export(
                                file,
                                reportType,
                                from,
                                to,
                                summary,
                                activities
                        );
            }


            showInformation(
                    arabic
                            ? "تم تصدير التقرير"
                            : "Report Exported",

                    arabic
                            ? "تم حفظ التقرير بنجاح:\n"
                                    + file.getAbsolutePath()
                            : "The report was saved successfully:\n"
                                    + file.getAbsolutePath()
            );


        } catch (Exception exception) {

            exception.printStackTrace();

            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تصدير التقرير"
                            : "Unable to export report",

                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // FILE NAME
    // =========================================================

    private String createReportFileName(
            ReportType type,
            LocalDate from,
            LocalDate to
    ) {

        String reportName =
                switch (type) {

                    case FINANCIAL_SUMMARY ->
                            "financial-summary";

                    case REVENUE ->
                            "revenue";

                    case EXPENSES ->
                            "expenses";

                    case MEMBERSHIPS ->
                            "memberships";
                };


        return "GymFlow-"
                + reportName
                + "-"
                + from
                + "-to-"
                + to;
    }


    // =========================================================
    // EXTENSION
    // =========================================================

    private File ensureExtension(
            File file,
            String extension
    ) {

        if (file.getName()
                .toLowerCase(Locale.ROOT)
                .endsWith(
                        extension
                )) {

            return file;
        }


        File parent =
                file.getParentFile();


        return new File(
                parent,
                file.getName()
                        + extension
        );
    }


    // =========================================================
    // VIEW ALL
    // =========================================================

    private void updateViewAllButton() {

        if (viewAllButton == null) {
            return;
        }


        if (LanguageManager.isArabic()) {

            viewAllButton.setText(
                    showAllActivity
                            ? "عرض الأحدث"
                            : "عرض الكل"
            );

        } else {

            viewAllButton.setText(
                    showAllActivity
                            ? "Show Recent"
                            : "View All"
            );
        }
    }


    // =========================================================
    // TRANSLATE TYPE
    // =========================================================

    private String translateType(
            String value
    ) {

        if (value == null) {
            return "-";
        }


        return switch (
                value.toUpperCase(Locale.ROOT)
        ) {

            case "REVENUE" ->
                    LanguageManager.isArabic()
                            ? "إيراد"
                            : "Revenue";

            case "EXPENSE" ->
                    LanguageManager.isArabic()
                            ? "مصروف"
                            : "Expense";

            default ->
                    value;
        };
    }


    // =========================================================
    // PAYMENT METHOD
    // =========================================================

    private String translatePaymentMethod(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "-";
        }


        return switch (
                value.toUpperCase(Locale.ROOT)
        ) {

            case "CASH" ->
                    LanguageManager.isArabic()
                            ? "نقدي"
                            : "Cash";

            case "CARD" ->
                    LanguageManager.isArabic()
                            ? "بطاقة"
                            : "Card";

            case "BANK_TRANSFER" ->
                    LanguageManager.isArabic()
                            ? "تحويل بنكي"
                            : "Bank Transfer";

            case "ONLINE" ->
                    LanguageManager.isArabic()
                            ? "دفع إلكتروني"
                            : "Online";

            default ->
                    value;
        };
    }


    // =========================================================
    // STATUS
    // =========================================================

    private String translateStatus(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "-";
        }


        return switch (
                value.toUpperCase(Locale.ROOT)
        ) {

            case "PAID" ->
                    LanguageManager.isArabic()
                            ? "مدفوع"
                            : "Paid";

            case "PENDING" ->
                    LanguageManager.isArabic()
                            ? "معلق"
                            : "Pending";

            case "CANCELLED" ->
                    LanguageManager.isArabic()
                            ? "ملغي"
                            : "Cancelled";

            default ->
                    value;
        };
    }


    // =========================================================
    // MONEY
    // =========================================================

    private String formatMoney(
            BigDecimal value
    ) {

        BigDecimal safeValue =
                value == null
                        ? BigDecimal.ZERO
                        : value;


        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        Locale.US
                );


        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);


        return formatter.format(
                safeValue
        ) + (
                LanguageManager.isArabic()
                        ? " ج.م"
                        : " EGP"
        );
    }


    // =========================================================
    // VALIDATE
    // =========================================================

    private void validateDateRange(
            LocalDate from,
            LocalDate to
    ) {

        if (from == null
                || to == null) {

            throw new IllegalArgumentException(
                    LanguageManager.isArabic()
                            ? "يجب تحديد تاريخ البداية والنهاية."
                            : "From and To dates are required."
            );
        }


        if (to.isBefore(from)) {

            throw new IllegalArgumentException(
                    LanguageManager.isArabic()
                            ? "تاريخ النهاية لا يمكن أن يكون قبل تاريخ البداية."
                            : "End date cannot be before start date."
            );
        }
    }


    // =========================================================
    // SET TEXT
    // =========================================================

    private void setText(
            javafx.scene.control.Labeled control,
            String text
    ) {

        if (control != null) {

            control.setText(
                    text
            );
        }
    }


    // =========================================================
    // SAFE
    // =========================================================

    private String safe(
            String value
    ) {

        return value == null
                || value.isBlank()
                        ? "-"
                        : value.trim();
    }


    // =========================================================
    // OWNER WINDOW
    // =========================================================

    private Window getOwnerWindow() {

        if (exportButton != null
                && exportButton.getScene() != null) {

            return exportButton
                    .getScene()
                    .getWindow();
        }

        if (financialActivityTable != null
                && financialActivityTable.getScene() != null) {

            return financialActivityTable
                    .getScene()
                    .getWindow();
        }

        return null;
    }


    // =========================================================
    // ERROR
    // =========================================================

    private void showError(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );


        alert.setTitle(title);
        alert.setHeaderText(title);

        alert.setContentText(
                message == null
                || message.isBlank()
                        ? "-"
                        : message
        );

        DialogStyler.style(alert);

        WindowManager.showResponsiveDialogAndWait(
                alert,
                getOwnerWindow(),
                520.0,
                320.0
        );
    }


    // =========================================================
    // INFORMATION
    // =========================================================

    private void showInformation(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );


        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);

        DialogStyler.style(alert);

        WindowManager.showResponsiveDialogAndWait(
                alert,
                getOwnerWindow(),
                520.0,
                320.0
        );
    }
}
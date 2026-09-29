package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.ui.dialog.DialogStyler;
import com.mycompany.lazar_app.ui.WindowManager;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.Payment;
import com.mycompany.lazar_app.repository.PaymentRepository;
import com.mycompany.lazar_app.repository.PaymentRepository.MembershipFinancialSummary;
import com.mycompany.lazar_app.repository.PaymentRepository.PaymentInstallment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public class PaymentsController {

    // =========================================================
    // KPI
    // =========================================================

    @FXML
    private Label totalPaymentsLabel;

    @FXML
    private Label monthPaymentsLabel;

    @FXML
    private Label paidPaymentsLabel;

    @FXML
    private Label pendingPaymentsLabel;

    @FXML
    private Label transactionsCountLabel;


    // =========================================================
    // SEARCH / FILTERS
    // =========================================================

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> paymentMethodFilter;

    @FXML
    private ComboBox<String> statusFilter;


    // =========================================================
    // TABLE
    // =========================================================

    @FXML
    private TableView<Payment> paymentsTable;

    @FXML
    private TableColumn<Payment, Number> idColumn;

    @FXML
    private TableColumn<Payment, String> memberColumn;

    @FXML
    private TableColumn<Payment, String> membershipColumn;

    @FXML
    private TableColumn<Payment, BigDecimal> subscriptionAmountColumn;

    @FXML
    private TableColumn<Payment, BigDecimal> amountColumn;

    @FXML
    private TableColumn<Payment, BigDecimal> paidAmountColumn;

    @FXML
    private TableColumn<Payment, BigDecimal> remainingAmountColumn;

    @FXML
    private TableColumn<Payment, String> paymentMethodColumn;

    @FXML
    private TableColumn<Payment, LocalDateTime> dateColumn;

    @FXML
    private TableColumn<Payment, String> statusColumn;

    @FXML
    private TableColumn<Payment, Void> actionsColumn;


    // =========================================================
    // REPOSITORY
    // =========================================================

    private final PaymentRepository paymentRepository =
            new PaymentRepository();


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<Payment> payments =
            FXCollections.observableArrayList();


    // =========================================================
    // DISPLAY CACHE
    // =========================================================

    private final Map<Long, String> memberNames =
            new HashMap<>();

    private final Map<Long, String> membershipNames =
            new HashMap<>();

    private final Map<Long, MembershipFinancialSummary> membershipFinancials =
            new HashMap<>();


    // =========================================================
    // SEARCH DELAY
    // =========================================================

    private final PauseTransition searchDelay =
            new PauseTransition(
                    Duration.millis(350)
            );


    // =========================================================
    // DATE FORMAT
    // =========================================================

    private final DateTimeFormatter tableDateFormatter =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm"
            );


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        configureFilters();

        configureTable();

        configureSmartSearch();

        loadPayments();
    }


    // =========================================================
    // FILTERS
    // =========================================================

    private void configureFilters() {

        paymentMethodFilter
                .getItems()
                .clear();


        statusFilter
                .getItems()
                .clear();


        if (LanguageManager.isArabic()) {

            paymentMethodFilter
                    .getItems()
                    .addAll(
                            "جميع الطرق",
                            "نقدي",
                            "بطاقة",
                            "تحويل بنكي"
                    );


            statusFilter
                    .getItems()
                    .addAll(
                            "جميع الحالات",
                            "مدفوع",
                            "معلق",
                            "ملغي"
                    );

        } else {

            paymentMethodFilter
                    .getItems()
                    .addAll(
                            "All Methods",
                            "Cash",
                            "Card",
                            "Bank Transfer"
                    );


            statusFilter
                    .getItems()
                    .addAll(
                            "All Status",
                            "Paid",
                            "Pending",
                            "Cancelled"
                    );
        }


        paymentMethodFilter
                .getSelectionModel()
                .selectFirst();


        statusFilter
                .getSelectionModel()
                .selectFirst();
    }


    // =========================================================
    // SMART LIVE SEARCH
    // =========================================================

    private void configureSmartSearch() {

        searchDelay.setOnFinished(
                event -> searchPayments()
        );


        // -----------------------------------------------------
        // TEXT SEARCH
        // -----------------------------------------------------

        if (searchField != null) {

            searchField
                    .textProperty()
                    .addListener(
                            (
                                    observable,
                                    oldValue,
                                    newValue
                            ) -> {

                                searchDelay.stop();

                                searchDelay.playFromStart();
                            }
                    );


            searchField.setOnAction(
                    event -> {

                        searchDelay.stop();

                        searchPayments();
                    }
            );
        }


        // -----------------------------------------------------
        // PAYMENT METHOD FILTER
        // -----------------------------------------------------

        if (paymentMethodFilter != null) {

            paymentMethodFilter
                    .valueProperty()
                    .addListener(
                            (
                                    observable,
                                    oldValue,
                                    newValue
                            ) -> {

                                searchDelay.stop();

                                searchPayments();
                            }
                    );
        }


        // -----------------------------------------------------
        // STATUS FILTER
        // -----------------------------------------------------

        if (statusFilter != null) {

            statusFilter
                    .valueProperty()
                    .addListener(
                            (
                                    observable,
                                    oldValue,
                                    newValue
                            ) -> {

                                searchDelay.stop();

                                searchPayments();
                            }
                    );
        }
    }


    // =========================================================
    // TABLE
    // =========================================================

    private void configureTable() {

        /*
         * We now have financial detail columns, so allow horizontal
         * scrolling instead of squeezing all values into unreadable widths.
         */
        paymentsTable.setColumnResizePolicy(
                TableView.UNCONSTRAINED_RESIZE_POLICY
        );

        idColumn.setCellValueFactory(
                data -> new SimpleLongProperty(
                        data.getValue().getId()
                )
        );

        memberColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        getMemberName(
                                data.getValue().getMemberId()
                        )
                )
        );

        membershipColumn.setCellValueFactory(
                data -> {
                    Long membershipId = data.getValue().getMembershipId();

                    return new SimpleStringProperty(
                            membershipId == null
                                    ? "-"
                                    : getMembershipName(membershipId)
                    );
                }
        );

        subscriptionAmountColumn.setCellValueFactory(
                data -> new SimpleObjectProperty<>(
                        getFinancialAmount(
                                data.getValue(),
                                FinancialValue.SUBSCRIPTION
                        )
                )
        );

        amountColumn.setCellValueFactory(
                data -> new SimpleObjectProperty<>(
                        data.getValue().getAmount()
                )
        );

        paidAmountColumn.setCellValueFactory(
                data -> new SimpleObjectProperty<>(
                        getFinancialAmount(
                                data.getValue(),
                                FinancialValue.PAID
                        )
                )
        );

        remainingAmountColumn.setCellValueFactory(
                data -> new SimpleObjectProperty<>(
                        getFinancialAmount(
                                data.getValue(),
                                FinancialValue.REMAINING
                        )
                )
        );

        configureMoneyColumn(subscriptionAmountColumn);
        configureMoneyColumn(amountColumn);
        configureMoneyColumn(paidAmountColumn);
        configureMoneyColumn(remainingAmountColumn);

        paymentMethodColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        displayPaymentMethod(
                                data.getValue().getPaymentMethod()
                        )
                )
        );

        dateColumn.setCellValueFactory(
                data -> new SimpleObjectProperty<>(
                        data.getValue().getPaymentDate()
                )
        );

        dateColumn.setCellFactory(
                column -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            LocalDateTime date,
                            boolean empty
                    ) {
                        super.updateItem(date, empty);

                        setText(
                                empty || date == null
                                        ? null
                                        : date.format(tableDateFormatter)
                        );
                    }
                }
        );

        statusColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        displayStatus(
                                data.getValue().getStatus()
                        )
                )
        );

        boolean ar = LanguageManager.isArabic();

        subscriptionAmountColumn.setText(
                ar ? "قيمة الاشتراك" : "Subscription"
        );
        amountColumn.setText(
                ar ? "الدفعة الحالية" : "Current Payment"
        );
        paidAmountColumn.setText(
                ar ? "إجمالي المدفوع" : "Total Paid"
        );
        remainingAmountColumn.setText(
                ar ? "المتبقي" : "Remaining"
        );

        configureActionsColumn();
        paymentsTable.setItems(payments);
    }


    private enum FinancialValue {
        SUBSCRIPTION,
        PAID,
        REMAINING
    }


    private BigDecimal getFinancialAmount(
            Payment payment,
            FinancialValue value
    ) {

        if (payment == null || payment.getMembershipId() == null) {
            return BigDecimal.ZERO;
        }

        MembershipFinancialSummary summary =
                membershipFinancials.get(payment.getMembershipId());

        if (summary == null) {
            return BigDecimal.ZERO;
        }

        return switch (value) {
            case SUBSCRIPTION -> summary.getMembershipAmount();
            case PAID -> summary.getPaidAmount();
            case REMAINING -> summary.getBalanceDue();
        };
    }


    private void configureMoneyColumn(
            TableColumn<Payment, BigDecimal> column
    ) {

        column.setCellFactory(
                ignored -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            BigDecimal amount,
                            boolean empty
                    ) {
                        super.updateItem(amount, empty);
                        setText(
                                empty
                                        ? null
                                        : formatMoney(amount)
                        );
                    }
                }
        );
    }


    // =========================================================
    // ACTIONS COLUMN
    // =========================================================

    private void configureActionsColumn() {

        actionsColumn.setCellFactory(
                column -> new TableCell<>() {

                    private final Button detailsButton =
                            new Button(
                                    LanguageManager.isArabic()
                                            ? "التفاصيل"
                                            : "Details"
                            );

                    private final Button editButton =
                            new Button(
                                    LanguageManager.isArabic()
                                            ? "تعديل"
                                            : "Edit"
                            );

                    private final Button deleteButton =
                            new Button(
                                    LanguageManager.isArabic()
                                            ? "حذف"
                                            : "Delete"
                            );

                    private final HBox box =
                            new HBox(
                                    6,
                                    detailsButton,
                                    editButton,
                                    deleteButton
                            );

                    {
                        box.setAlignment(Pos.CENTER);

                        detailsButton.setStyle(
                                "-fx-background-color:#0F766E;"
                                + "-fx-text-fill:white;"
                                + "-fx-background-radius:6;"
                                + "-fx-cursor:hand;"
                        );

                        editButton.setStyle(
                                "-fx-background-color:#2563EB;"
                                + "-fx-text-fill:white;"
                                + "-fx-background-radius:6;"
                                + "-fx-cursor:hand;"
                        );

                        deleteButton.setStyle(
                                "-fx-background-color:#DC2626;"
                                + "-fx-text-fill:white;"
                                + "-fx-background-radius:6;"
                                + "-fx-cursor:hand;"
                        );

                        detailsButton.setOnAction(event -> {
                            Payment payment = getCurrentPayment();
                            if (payment != null) {
                                openPaymentDetails(payment);
                            }
                        });

                        editButton.setOnAction(event -> {
                            Payment payment = getCurrentPayment();
                            if (payment != null) {
                                openEditPayment(payment);
                            }
                        });

                        deleteButton.setOnAction(event -> {
                            Payment payment = getCurrentPayment();
                            if (payment != null) {
                                deletePayment(payment);
                            }
                        });
                    }

                    private Payment getCurrentPayment() {
                        if (getIndex() < 0
                                || getIndex() >= getTableView().getItems().size()) {
                            return null;
                        }

                        return getTableView()
                                .getItems()
                                .get(getIndex());
                    }

                    @Override
                    protected void updateItem(
                            Void item,
                            boolean empty
                    ) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : box);
                    }
                }
        );
    }


    // =========================================================
    // LOAD PAYMENTS
    // =========================================================

    private void loadPayments() {

        try {

            payments.setAll(
                    paymentRepository.findAll()
            );


            refreshPaymentView();


        } catch (Exception exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تحميل المدفوعات."
                            : "Unable to load payments.",
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // REFRESH VIEW
    // =========================================================

    private void refreshPaymentView() {

        rebuildDisplayCaches();

        paymentsTable.refresh();

        updateStatistics();
    }


    // =========================================================
    // DISPLAY CACHE
    // =========================================================

    private void rebuildDisplayCaches() {

        memberNames.clear();

        membershipNames.clear();

        membershipFinancials.clear();


        for (Payment payment : payments) {

            long memberId =
                    payment.getMemberId();


            if (!memberNames.containsKey(
                    memberId
            )) {

                try {

                    memberNames.put(
                            memberId,
                            paymentRepository
                                    .findMemberDisplayName(
                                            memberId
                                    )
                    );

                } catch (Exception exception) {

                    memberNames.put(
                            memberId,
                            "#" + memberId
                    );
                }
            }


            Long membershipId =
                    payment.getMembershipId();


            if (membershipId != null
                    && !membershipNames
                            .containsKey(
                                    membershipId
                            )) {

                try {

                    membershipNames.put(
                            membershipId,
                            paymentRepository
                                    .findMembershipDisplayName(
                                            membershipId,
                                            LanguageManager
                                                    .isArabic()
                                    )
                    );

                } catch (Exception exception) {

                    membershipNames.put(
                            membershipId,
                            "#" + membershipId
                    );
                }

                if (!membershipFinancials.containsKey(membershipId)) {
                    try {
                        membershipFinancials.put(
                                membershipId,
                                paymentRepository
                                        .findMembershipFinancialSummary(
                                                membershipId
                                        )
                        );
                    } catch (Exception exception) {
                        membershipFinancials.put(
                                membershipId,
                                new MembershipFinancialSummary(
                                        membershipId,
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO
                                )
                        );
                    }
                }
            }
        }
    }


    // =========================================================
    // MEMBER NAME
    // =========================================================

    private String getMemberName(
            long id
    ) {

        return memberNames.getOrDefault(
                id,
                "#" + id
        );
    }


    // =========================================================
    // MEMBERSHIP NAME
    // =========================================================

    private String getMembershipName(
            long id
    ) {

        return membershipNames.getOrDefault(
                id,
                "#" + id
        );
    }


    // =========================================================
    // NEW PAYMENT
    // =========================================================

    @FXML
    private void openNewPayment() {

        try {

            FXMLLoader loader =
                    createPaymentLoader();


            Parent root =
                    loader.load();


            AddPaymentController controller =
                    loader.getController();


            Stage stage =
                    createPaymentStage(
                            root,
                            LanguageManager.isArabic()
                                    ? "إضافة دفعة جديدة"
                                    : "Add New Payment"
                    );


            showMaximizedModalAndWait(stage);


            Payment payment =
                    controller.getPayment();


            if (payment == null) {

                return;
            }


            paymentRepository.save(
                    payment
            );

            synchronizeScheduleAfterSave(
                    controller,
                    payment
            );

            refreshAfterDataChange();


        } catch (Exception exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر إضافة الدفعة."
                            : "Unable to add payment.",
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // EDIT PAYMENT
    // =========================================================

    private void openEditPayment(
            Payment payment
    ) {

        if (payment == null) {

            return;
        }

        Long originalMembershipId = payment.getMembershipId();


        try {

            FXMLLoader loader =
                    createPaymentLoader();


            Parent root =
                    loader.load();


            AddPaymentController controller =
                    loader.getController();


            controller.setPaymentForEdit(
                    payment
            );


            Stage stage =
                    createPaymentStage(
                            root,
                            LanguageManager.isArabic()
                                    ? "تعديل الدفعة"
                                    : "Edit Payment"
                    );


            showMaximizedModalAndWait(stage);


            Payment edited =
                    controller.getPayment();


            if (edited == null) {

                return;
            }


            paymentRepository.update(
                    edited
            );

            synchronizeScheduleAfterSave(
                    controller,
                    edited
            );

            if (originalMembershipId != null
                    && !originalMembershipId.equals(
                            edited.getMembershipId()
                    )) {
                reconcileExistingSchedule(originalMembershipId);
            }

            refreshAfterDataChange();


        } catch (Exception exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تعديل الدفعة."
                            : "Unable to update payment.",
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // INSTALLMENT SCHEDULE SYNC
    // =========================================================

    private void synchronizeScheduleAfterSave(
            AddPaymentController controller,
            Payment payment
    ) throws Exception {

        if (payment == null || payment.getMembershipId() == null) {
            return;
        }

        long membershipId = payment.getMembershipId();

        MembershipFinancialSummary summary =
                paymentRepository.findMembershipFinancialSummary(
                        membershipId
                );

        if (summary.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
            paymentRepository.replaceOutstandingSchedule(
                    membershipId,
                    BigDecimal.ZERO,
                    0,
                    null
            );
            return;
        }

        if (controller.shouldCreateInstallmentSchedule()) {
            paymentRepository.replaceOutstandingSchedule(
                    membershipId,
                    summary.getBalanceDue(),
                    controller.getScheduleInstallmentCount(),
                    controller.getScheduleFirstDueDate()
            );
            return;
        }

        /*
         * For pending/cancelled edits keep an existing schedule aligned
         * with the latest outstanding balance without silently inventing
         * a new plan when the user never chose installments.
         */
        reconcileExistingSchedule(membershipId);
    }


    private void reconcileExistingSchedule(
            long membershipId
    ) throws Exception {

        MembershipFinancialSummary summary =
                paymentRepository.findMembershipFinancialSummary(
                        membershipId
                );

        List<PaymentInstallment> existing =
                paymentRepository.findPaymentScheduleByMembershipId(
                        membershipId
                );

        if (summary.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
            paymentRepository.replaceOutstandingSchedule(
                    membershipId,
                    BigDecimal.ZERO,
                    0,
                    null
            );
            return;
        }

        if (existing.isEmpty()) {
            return;
        }

        LocalDate firstDue = existing.get(0).getDueDate();

        if (firstDue == null) {
            firstDue = LocalDate.now().plusMonths(1);
        }

        paymentRepository.replaceOutstandingSchedule(
                membershipId,
                summary.getBalanceDue(),
                existing.size(),
                firstDue
        );
    }


    // =========================================================
    // PAYMENT DETAILS
    // =========================================================

    private void openPaymentDetails(
            Payment selectedPayment
    ) {

        if (selectedPayment == null
                || selectedPayment.getMembershipId() == null) {

            showError(
                    LanguageManager.isArabic()
                            ? "لا توجد تفاصيل اشتراك مرتبطة بهذه الدفعة."
                            : "This payment is not linked to a membership.",
                    ""
            );
            return;
        }

        try {
            long membershipId = selectedPayment.getMembershipId();

            MembershipFinancialSummary summary =
                    paymentRepository.findMembershipFinancialSummary(
                            membershipId
                    );

            List<Payment> history =
                    paymentRepository.findPaymentsByMembershipId(
                            membershipId
                    );

            List<PaymentInstallment> schedule =
                    paymentRepository.findPaymentScheduleByMembershipId(
                            membershipId
                    );

            BorderPane root = new BorderPane();
            root.setPrefSize(1050, 720);
            root.setStyle("-fx-background-color:#F5F7FA;");
            root.setNodeOrientation(
                    LanguageManager.isArabic()
                            ? NodeOrientation.RIGHT_TO_LEFT
                            : NodeOrientation.LEFT_TO_RIGHT
            );

            VBox header = new VBox(6);
            header.setPadding(new Insets(22, 24, 14, 24));
            header.setStyle("-fx-background-color:white;");

            Label title = new Label(
                    LanguageManager.isArabic()
                            ? "تفاصيل دفعات الاشتراك"
                            : "Membership Payment Details"
            );
            title.setStyle(
                    "-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#111827;"
            );

            Label subtitle = new Label(
                    getMemberName(selectedPayment.getMemberId())
                    + "  •  "
                    + getMembershipName(membershipId)
            );
            subtitle.setStyle("-fx-font-size:13px;-fx-text-fill:#6B7280;");

            header.getChildren().addAll(title, subtitle);
            root.setTop(header);

            VBox content = new VBox(16);
            content.setPadding(new Insets(18, 24, 18, 24));

            HBox cards = new HBox(12);
            cards.getChildren().addAll(
                    createFinancialCard(
                            LanguageManager.isArabic()
                                    ? "قيمة الاشتراك"
                                    : "Subscription Amount",
                            formatMoney(summary.getMembershipAmount())
                    ),
                    createFinancialCard(
                            LanguageManager.isArabic()
                                    ? "المدفوع فعلياً"
                                    : "Actually Paid",
                            formatMoney(summary.getPaidAmount())
                    ),
                    createFinancialCard(
                            LanguageManager.isArabic()
                                    ? "المتبقي"
                                    : "Remaining",
                            formatMoney(summary.getBalanceDue())
                    )
            );

            for (javafx.scene.Node node : cards.getChildren()) {
                HBox.setHgrow(node, Priority.ALWAYS);
            }

            // -----------------------------------------------------
            // DETAILS VIEW SWITCHER
            // -----------------------------------------------------
            //
            // We intentionally use normal buttons instead of TabPane tabs.
            // On some Linux/JavaFX combinations the TabPane header can be
            // partially covered by the TableView when the window is maximized.
            // Keeping the selector in its own HBox guarantees that both
            // buttons always remain fully visible above the table.

            TableView<Payment> paymentHistoryTable =
                    createPaymentHistoryTable(history);

            TableView<PaymentInstallment> installmentScheduleTable =
                    createInstallmentScheduleTable(schedule);

            paymentHistoryTable.setMaxSize(
                    Double.MAX_VALUE,
                    Double.MAX_VALUE
            );

            installmentScheduleTable.setMaxSize(
                    Double.MAX_VALUE,
                    Double.MAX_VALUE
            );

            installmentScheduleTable.setVisible(false);
            installmentScheduleTable.setManaged(false);

            StackPane detailsTableContainer = new StackPane(
                    paymentHistoryTable,
                    installmentScheduleTable
            );
            detailsTableContainer.setMinHeight(260.0);
            detailsTableContainer.setMaxSize(
                    Double.MAX_VALUE,
                    Double.MAX_VALUE
            );
            VBox.setVgrow(
                    detailsTableContainer,
                    Priority.ALWAYS
            );

            Button actualPaymentsButton = new Button(
                    LanguageManager.isArabic()
                            ? "الدفعات الفعلية"
                            : "Actual Payments"
            );

            Button remainingScheduleButton = new Button(
                    LanguageManager.isArabic()
                            ? "جدول المتبقي"
                            : "Remaining Schedule"
            );

            // Give Arabic text enough vertical room on Linux/JavaFX.
            // Avoid tight fixed padding/height combinations that can clip
            // the lower part of Arabic glyphs.
            actualPaymentsButton.setMinWidth(170.0);
            actualPaymentsButton.setPrefWidth(170.0);
            actualPaymentsButton.setMinHeight(48.0);
            actualPaymentsButton.setPrefHeight(48.0);
            actualPaymentsButton.setAlignment(Pos.CENTER);
            actualPaymentsButton.setContentDisplay(ContentDisplay.CENTER);
            actualPaymentsButton.setMnemonicParsing(false);

            remainingScheduleButton.setMinWidth(170.0);
            remainingScheduleButton.setPrefWidth(170.0);
            remainingScheduleButton.setMinHeight(48.0);
            remainingScheduleButton.setPrefHeight(48.0);
            remainingScheduleButton.setAlignment(Pos.CENTER);
            remainingScheduleButton.setContentDisplay(ContentDisplay.CENTER);
            remainingScheduleButton.setMnemonicParsing(false);

            styleDetailsViewButton(
                    actualPaymentsButton,
                    true
            );

            styleDetailsViewButton(
                    remainingScheduleButton,
                    false
            );

            HBox detailsViewBar;

            if (LanguageManager.isArabic()) {
                // LTR container + reversed children keeps the selected
                // "Actual Payments" button physically on the far right.
                detailsViewBar = new HBox(
                        8.0,
                        remainingScheduleButton,
                        actualPaymentsButton
                );
            } else {
                detailsViewBar = new HBox(
                        8.0,
                        actualPaymentsButton,
                        remainingScheduleButton
                );
            }
            // Use physical right/left alignment explicitly.  The surrounding
            // Arabic scene is RTL, so an RTL HBox can mirror CENTER_RIGHT.
            // Keeping this bar LTR prevents that mirroring while each button
            // still renders its Arabic text correctly.
            detailsViewBar.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            detailsViewBar.setAlignment(
                    LanguageManager.isArabic()
                            ? Pos.CENTER_RIGHT
                            : Pos.CENTER_LEFT
            );
            actualPaymentsButton.setNodeOrientation(
                    LanguageManager.isArabic()
                            ? NodeOrientation.RIGHT_TO_LEFT
                            : NodeOrientation.LEFT_TO_RIGHT
            );
            remainingScheduleButton.setNodeOrientation(
                    LanguageManager.isArabic()
                            ? NodeOrientation.RIGHT_TO_LEFT
                            : NodeOrientation.LEFT_TO_RIGHT
            );
            detailsViewBar.setMinHeight(60.0);
            detailsViewBar.setPrefHeight(60.0);
            detailsViewBar.setPadding(
                    new Insets(4.0, 0.0, 8.0, 0.0)
            );

            actualPaymentsButton.setOnAction(event -> {

                paymentHistoryTable.setManaged(true);
                paymentHistoryTable.setVisible(true);

                installmentScheduleTable.setManaged(false);
                installmentScheduleTable.setVisible(false);

                styleDetailsViewButton(
                        actualPaymentsButton,
                        true
                );

                styleDetailsViewButton(
                        remainingScheduleButton,
                        false
                );
            });

            remainingScheduleButton.setOnAction(event -> {

                paymentHistoryTable.setManaged(false);
                paymentHistoryTable.setVisible(false);

                installmentScheduleTable.setManaged(true);
                installmentScheduleTable.setVisible(true);

                styleDetailsViewButton(
                        actualPaymentsButton,
                        false
                );

                styleDetailsViewButton(
                        remainingScheduleButton,
                        true
                );
            });

            content.getChildren().addAll(
                    cards,
                    detailsViewBar,
                    detailsTableContainer
            );
            root.setCenter(content);

            Button closeButton = new Button(
                    LanguageManager.isArabic() ? "إغلاق" : "Close"
            );
            closeButton.setPrefWidth(110);
            closeButton.setPrefHeight(38);
            closeButton.setStyle(
                    "-fx-background-color:#111827;"
                    + "-fx-text-fill:white;"
                    + "-fx-background-radius:7;"
                    + "-fx-cursor:hand;"
            );

            HBox footer = new HBox(closeButton);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setPadding(new Insets(12, 24, 18, 24));
            footer.setStyle("-fx-background-color:white;");
            root.setBottom(footer);

            Stage stage = WindowManager.createModalStage(
                    root,
                    LanguageManager.isArabic()
                            ? "تفاصيل الدفعات والمتبقي"
                            : "Payments and Remaining Balance",
                    getOwnerWindow()
            );

            configureBlockingModalStage(stage);

            closeButton.setOnAction(event -> stage.close());

            showMaximizedModalAndWait(stage);

        } catch (Exception exception) {
            exception.printStackTrace();
            showError(
                    LanguageManager.isArabic()
                            ? "تعذر فتح تفاصيل الدفعات."
                            : "Unable to open payment details.",
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // DETAILS VIEW BUTTON STYLE
    // =========================================================

    private void styleDetailsViewButton(
            Button button,
            boolean active
    ) {

        if (button == null) {
            return;
        }

        if (active) {

            button.setStyle(
                    "-fx-background-color:#2563EB;"
                    + "-fx-text-fill:white;"
                    + "-fx-font-weight:bold;"
                    + "-fx-font-size:14px;"
                    + "-fx-background-radius:7;"
                    + "-fx-border-radius:7;"
                    + "-fx-padding:0 20 2 20;"
                    + "-fx-cursor:hand;"
            );

        } else {

            button.setStyle(
                    "-fx-background-color:white;"
                    + "-fx-text-fill:#374151;"
                    + "-fx-font-weight:bold;"
                    + "-fx-font-size:14px;"
                    + "-fx-background-radius:7;"
                    + "-fx-border-radius:7;"
                    + "-fx-border-color:#D1D5DB;"
                    + "-fx-border-width:1;"
                    + "-fx-padding:0 20 2 20;"
                    + "-fx-cursor:hand;"
            );
        }
    }


    private VBox createFinancialCard(
            String caption,
            String value
    ) {

        Label captionLabel = new Label(caption);
        captionLabel.setStyle(
                "-fx-font-size:12px;-fx-text-fill:#6B7280;"
        );

        Label valueLabel = new Label(value);
        valueLabel.setStyle(
                "-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#111827;"
        );

        VBox card = new VBox(6, captionLabel, valueLabel);
        card.setPadding(new Insets(14));
        card.setMinWidth(190);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color:white;"
                + "-fx-background-radius:10;"
                + "-fx-border-color:#E5E7EB;"
                + "-fx-border-radius:10;"
        );

        return card;
    }


    private TableView<Payment> createPaymentHistoryTable(
            List<Payment> history
    ) {

        TableView<Payment> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(history));
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        TableColumn<Payment, Number> numberColumn = new TableColumn<>("#");
        numberColumn.setCellValueFactory(
                data -> new SimpleLongProperty(data.getValue().getId())
        );

        TableColumn<Payment, String> date = new TableColumn<>(
                LanguageManager.isArabic() ? "التاريخ" : "Date"
        );
        date.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getPaymentDate() == null
                                ? "-"
                                : data.getValue()
                                        .getPaymentDate()
                                        .format(tableDateFormatter)
                )
        );

        TableColumn<Payment, String> amount = new TableColumn<>(
                LanguageManager.isArabic() ? "المبلغ" : "Amount"
        );
        amount.setCellValueFactory(
                data -> new SimpleStringProperty(
                        formatMoney(data.getValue().getAmount())
                )
        );

        TableColumn<Payment, String> method = new TableColumn<>(
                LanguageManager.isArabic() ? "طريقة الدفع" : "Method"
        );
        method.setCellValueFactory(
                data -> new SimpleStringProperty(
                        displayPaymentMethod(
                                data.getValue().getPaymentMethod()
                        )
                )
        );

        TableColumn<Payment, String> status = new TableColumn<>(
                LanguageManager.isArabic() ? "الحالة" : "Status"
        );
        status.setCellValueFactory(
                data -> new SimpleStringProperty(
                        displayStatus(data.getValue().getStatus())
                )
        );

        TableColumn<Payment, String> reference = new TableColumn<>(
                LanguageManager.isArabic() ? "المرجع" : "Reference"
        );
        reference.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getReferenceNumber() == null
                                ? "-"
                                : data.getValue().getReferenceNumber()
                )
        );

        table.getColumns().addAll(
                numberColumn,
                date,
                amount,
                method,
                status,
                reference
        );

        return table;
    }


    private TableView<PaymentInstallment> createInstallmentScheduleTable(
            List<PaymentInstallment> schedule
    ) {

        TableView<PaymentInstallment> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(schedule));
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        TableColumn<PaymentInstallment, Number> no = new TableColumn<>(
                LanguageManager.isArabic() ? "القسط" : "Installment"
        );
        no.setCellValueFactory(
                data -> new SimpleLongProperty(
                        data.getValue().getInstallmentNo()
                )
        );

        TableColumn<PaymentInstallment, String> due = new TableColumn<>(
                LanguageManager.isArabic()
                        ? "تاريخ الاستحقاق"
                        : "Due Date"
        );
        due.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getDueDate() == null
                                ? "-"
                                : data.getValue().getDueDate().toString()
                )
        );

        TableColumn<PaymentInstallment, String> amount = new TableColumn<>(
                LanguageManager.isArabic() ? "المبلغ" : "Amount"
        );
        amount.setCellValueFactory(
                data -> new SimpleStringProperty(
                        formatMoney(data.getValue().getAmount())
                )
        );

        TableColumn<PaymentInstallment, String> status = new TableColumn<>(
                LanguageManager.isArabic() ? "الحالة" : "Status"
        );
        status.setCellValueFactory(
                data -> new SimpleStringProperty(
                        displayInstallmentStatus(data.getValue())
                )
        );

        table.getColumns().addAll(no, due, amount, status);
        return table;
    }


    private String displayInstallmentStatus(
            PaymentInstallment installment
    ) {

        if (installment == null) {
            return "";
        }

        LocalDate dueDate = installment.getDueDate();

        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            return LanguageManager.isArabic()
                    ? "متأخر"
                    : "Overdue";
        }

        return LanguageManager.isArabic()
                ? "مجدول"
                : "Scheduled";
    }


    // =========================================================
    // REFRESH AFTER SAVE / EDIT / DELETE
    // =========================================================

    private void refreshAfterDataChange() {

        if (hasActiveSearchOrFilter()) {

            searchPayments();

        } else {

            loadPayments();
        }
    }


    // =========================================================
    // ACTIVE SEARCH?
    // =========================================================

    private boolean hasActiveSearchOrFilter() {

        boolean hasKeyword =
                searchField != null
                && searchField.getText() != null
                && !searchField
                        .getText()
                        .isBlank();


        String method =
                paymentMethodFilter == null
                        ? null
                        : normalizePaymentMethodFilter(
                                paymentMethodFilter
                                        .getValue()
                        );


        String status =
                statusFilter == null
                        ? null
                        : normalizeStatusFilter(
                                statusFilter
                                        .getValue()
                        );


        return hasKeyword
                || method != null
                || status != null;
    }


    // =========================================================
    // PAYMENT LOADER
    // =========================================================

    private FXMLLoader createPaymentLoader() {

        return new FXMLLoader(
                getClass().getResource(
                        "/fxml/add-payment.fxml"
                ),
                LanguageManager.getBundle()
        );
    }


    // =========================================================
    // PAYMENT WINDOW
    // =========================================================

    private Stage createPaymentStage(
            Parent root,
            String title
    ) {

        Stage stage = WindowManager.createModalStage(
                root,
                title,
                getOwnerWindow()
        );

        configureBlockingModalStage(stage);

        return stage;
    }


    // =========================================================
    // MAXIMIZED BLOCKING MODAL WINDOWS
    // =========================================================

    /**
     * Makes a child window fully blocking and resizable.
     * APPLICATION_MODAL prevents the user from returning to any
     * previous application window until this window is closed.
     */
    private void configureBlockingModalStage(Stage stage) {

        if (stage == null) {
            return;
        }

        /*
         * WindowManager normally creates a modal stage already.
         * This guard makes the behavior explicit and safe if its
         * implementation changes later. Modality can only be set
         * before the window is shown.
         */
        if (stage.getModality() == Modality.NONE) {
            stage.initModality(Modality.APPLICATION_MODAL);
        }

        stage.setResizable(true);
    }


    /**
     * Shows the window maximized and waits until it is closed.
     * showAndWait() is intentional: code after this call will not
     * continue while the child window is still open.
     */
    private void showMaximizedModalAndWait(Stage stage) {

        if (stage == null) {
            return;
        }

        configureBlockingModalStage(stage);

        WindowManager.showMaximizedAndWait(
                stage
        );
    }


    // =========================================================
    // OWNER WINDOW
    // =========================================================

    private Window getOwnerWindow() {

        if (paymentsTable == null
                || paymentsTable.getScene() == null) {

            return null;
        }

        return paymentsTable
                .getScene()
                .getWindow();
    }


    // =========================================================
    // DELETE PAYMENT
    // =========================================================

    private void deletePayment(
            Payment payment
    ) {

        if (payment == null) {

            return;
        }


        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        confirmation.setTitle(
                LanguageManager.isArabic()
                        ? "تأكيد الحذف"
                        : "Confirm Delete"
        );


        confirmation.setHeaderText(
                LanguageManager.isArabic()
                        ? "حذف عملية الدفع رقم "
                            + payment.getId()
                        : "Delete payment #"
                            + payment.getId()
        );


        confirmation.setContentText(
                LanguageManager.isArabic()
                        ? "هل أنت متأكد؟ لا يمكن التراجع عن الحذف."
                        : "Are you sure? This action cannot be undone."
        );


        DialogStyler.style(confirmation);

        Optional<ButtonType> result =
                WindowManager.showResponsiveDialogAndWait(
                        confirmation,
                        getOwnerWindow(),
                        580.0,
                        340.0
                );


        if (result.isEmpty()
                || result.get()
                != ButtonType.OK) {

            return;
        }


        try {

            paymentRepository.deleteById(
                    payment.getId()
            );

            if (payment.getMembershipId() != null) {
                reconcileExistingSchedule(
                        payment.getMembershipId()
                );
            }

            refreshAfterDataChange();


        } catch (Exception exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر حذف الدفعة."
                            : "Unable to delete payment.",
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // SMART SEARCH
    // =========================================================

    @FXML
    private void searchPayments() {

        try {

            String keyword =
                    searchField == null
                            || searchField.getText() == null
                            ? ""
                            : searchField
                                    .getText()
                                    .trim();


            String method =
                    paymentMethodFilter == null
                            ? null
                            : normalizePaymentMethodFilter(
                                    paymentMethodFilter
                                            .getValue()
                            );


            String status =
                    statusFilter == null
                            ? null
                            : normalizeStatusFilter(
                                    statusFilter
                                            .getValue()
                            );


            payments.setAll(
                    paymentRepository.search(
                            keyword,
                            method,
                            status
                    )
            );


            refreshPaymentView();


        } catch (Exception exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تنفيذ البحث."
                            : "Unable to search payments.",
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // REFRESH
    // =========================================================

    @FXML
    private void refreshPayments() {

        searchDelay.stop();


        if (searchField != null) {

            searchField.clear();
        }


        if (paymentMethodFilter != null) {

            paymentMethodFilter
                    .getSelectionModel()
                    .selectFirst();
        }


        if (statusFilter != null) {

            statusFilter
                    .getSelectionModel()
                    .selectFirst();
        }


        loadPayments();
    }


    // =========================================================
    // STATISTICS
    // =========================================================

    private void updateStatistics() {

        BigDecimal total =
                payments.stream()
                        .map(
                                Payment::getAmount
                        )
                        .filter(
                                amount ->
                                        amount != null
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        LocalDate today =
                LocalDate.now();


        BigDecimal month =
                payments.stream()

                        .filter(
                                payment ->
                                        payment
                                                .getPaymentDate()
                                                != null
                        )

                        .filter(
                                payment -> {

                                    LocalDate date =
                                            payment
                                                    .getPaymentDate()
                                                    .toLocalDate();


                                    return date.getYear()
                                                    == today.getYear()
                                            && date.getMonth()
                                                    == today.getMonth();
                                }
                        )

                        .map(
                                Payment::getAmount
                        )

                        .filter(
                                amount ->
                                        amount != null
                        )

                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        BigDecimal paid =
                payments.stream()

                        .filter(
                                payment ->
                                        isPaid(
                                                payment.getStatus()
                                        )
                        )

                        .map(
                                Payment::getAmount
                        )

                        .filter(
                                amount ->
                                        amount != null
                        )

                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        BigDecimal pending =
                payments.stream()

                        .filter(
                                payment ->
                                        isPending(
                                                payment.getStatus()
                                        )
                        )

                        .map(
                                Payment::getAmount
                        )

                        .filter(
                                amount ->
                                        amount != null
                        )

                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        totalPaymentsLabel.setText(
                formatMoney(total)
        );


        monthPaymentsLabel.setText(
                formatMoney(month)
        );


        paidPaymentsLabel.setText(
                formatMoney(paid)
        );


        pendingPaymentsLabel.setText(
                formatMoney(pending)
        );


        transactionsCountLabel.setText(
                LanguageManager.isArabic()
                        ? payments.size()
                            + " عملية"
                        : payments.size()
                            + " Transactions"
        );
    }


    // =========================================================
    // STATUS HELPERS
    // =========================================================

    private boolean isPaid(
            String status
    ) {

        return status != null
                && status.equalsIgnoreCase(
                        "PAID"
                );
    }


    private boolean isPending(
            String status
    ) {

        return status != null
                && status.equalsIgnoreCase(
                        "PENDING"
                );
    }


    // =========================================================
    // PAYMENT METHOD FILTER
    // =========================================================

    private String normalizePaymentMethodFilter(
            String value
    ) {

        if (value == null
                || value.equals(
                        "جميع الطرق"
                )
                || value.equals(
                        "All Methods"
                )) {

            return null;
        }


        return switch (value) {

            case "نقدي",
                 "Cash" ->
                    "CASH";

            case "بطاقة",
                 "Card" ->
                    "CARD";

            case "تحويل بنكي",
                 "Bank Transfer" ->
                    "BANK_TRANSFER";

            default ->
                    value;
        };
    }


    // =========================================================
    // STATUS FILTER
    // =========================================================

    private String normalizeStatusFilter(
            String value
    ) {

        if (value == null
                || value.equals(
                        "جميع الحالات"
                )
                || value.equals(
                        "All Status"
                )) {

            return null;
        }


        return switch (value) {

            case "مدفوع",
                 "Paid" ->
                    "PAID";

            case "معلق",
                 "Pending" ->
                    "PENDING";

            case "ملغي",
                 "Cancelled" ->
                    "CANCELLED";

            default ->
                    value;
        };
    }


    // =========================================================
    // DISPLAY PAYMENT METHOD
    // =========================================================

    private String displayPaymentMethod(
            String method
    ) {

        if (method == null) {

            return "";
        }


        if (LanguageManager.isArabic()) {

            return switch (
                    method.toUpperCase()
            ) {

                case "CASH" ->
                        "نقدي";

                case "CARD" ->
                        "بطاقة";

                case "BANK_TRANSFER" ->
                        "تحويل بنكي";

                default ->
                        method;
            };
        }


        return switch (
                method.toUpperCase()
        ) {

            case "CASH" ->
                    "Cash";

            case "CARD" ->
                    "Card";

            case "BANK_TRANSFER" ->
                    "Bank Transfer";

            default ->
                    method;
        };
    }


    // =========================================================
    // DISPLAY STATUS
    // =========================================================

    private String displayStatus(
            String status
    ) {

        if (status == null) {

            return "";
        }


        if (LanguageManager.isArabic()) {

            return switch (
                    status.toUpperCase()
            ) {

                case "PAID" ->
                        "مدفوع";

                case "PENDING" ->
                        "معلق";

                case "CANCELLED" ->
                        "ملغي";

                default ->
                        status;
            };
        }


        return switch (
                status.toUpperCase()
        ) {

            case "PAID" ->
                    "Paid";

            case "PENDING" ->
                    "Pending";

            case "CANCELLED" ->
                    "Cancelled";

            default ->
                    status;
        };
    }


    // =========================================================
    // MONEY FORMAT
    // =========================================================

    private String formatMoney(
            BigDecimal amount
    ) {

        if (amount == null) {

            amount =
                    BigDecimal.ZERO;
        }


        return amount
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .toPlainString()
                + " EGP";
    }


    // =========================================================
    // ERROR
    // =========================================================

    private void showError(
            String header,
            String message
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
                header
        );


        alert.setContentText(
                message == null
                        ? ""
                        : message
        );


        DialogStyler.style(alert);


        WindowManager.showResponsiveDialogAndWait(
                alert,
                getOwnerWindow(),
                600.0,
                360.0
        );
    }
}
package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.ui.dialog.DialogStyler;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.Payment;
import com.mycompany.lazar_app.repository.PaymentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
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
    private TableColumn<Payment, BigDecimal> amountColumn;

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

        idColumn.setCellValueFactory(
                data ->
                        new SimpleLongProperty(
                                data.getValue()
                                        .getId()
                        )
        );


        memberColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                getMemberName(
                                        data.getValue()
                                                .getMemberId()
                                )
                        )
        );


        membershipColumn.setCellValueFactory(
                data -> {

                    Long membershipId =
                            data.getValue()
                                    .getMembershipId();


                    return new SimpleStringProperty(
                            membershipId == null
                                    ? "-"
                                    : getMembershipName(
                                            membershipId
                                    )
                    );
                }
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
                                    boolean empty
                            ) {

                                super.updateItem(
                                        amount,
                                        empty
                                );


                                setText(
                                        empty
                                                || amount == null
                                                ? null
                                                : formatMoney(
                                                        amount
                                                )
                                );
                            }
                        }
        );


        paymentMethodColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                displayPaymentMethod(
                                        data.getValue()
                                                .getPaymentMethod()
                                )
                        )
        );


        dateColumn.setCellValueFactory(
                data ->
                        new SimpleObjectProperty<>(
                                data.getValue()
                                        .getPaymentDate()
                        )
        );


        dateColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    LocalDateTime date,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        date,
                                        empty
                                );


                                setText(
                                        empty
                                                || date == null
                                                ? null
                                                : date.format(
                                                        tableDateFormatter
                                                )
                                );
                            }
                        }
        );


        statusColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                displayStatus(
                                        data.getValue()
                                                .getStatus()
                                )
                        )
        );


        configureActionsColumn();


        paymentsTable.setItems(
                payments
        );
    }


    // =========================================================
    // ACTIONS COLUMN
    // =========================================================

    private void configureActionsColumn() {

        actionsColumn.setCellFactory(
                column ->
                        new TableCell<>() {

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
                                            editButton,
                                            deleteButton
                                    );


                            {

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


                                editButton.setOnAction(
                                        event -> {

                                            if (!isValidRow()) {
                                                return;
                                            }


                                            Payment payment =
                                                    getTableView()
                                                            .getItems()
                                                            .get(
                                                                    getIndex()
                                                            );


                                            openEditPayment(
                                                    payment
                                            );
                                        }
                                );


                                deleteButton.setOnAction(
                                        event -> {

                                            if (!isValidRow()) {
                                                return;
                                            }


                                            Payment payment =
                                                    getTableView()
                                                            .getItems()
                                                            .get(
                                                                    getIndex()
                                                            );


                                            deletePayment(
                                                    payment
                                            );
                                        }
                                );
                            }


                            private boolean isValidRow() {

                                return getIndex() >= 0
                                        && getIndex()
                                        < getTableView()
                                                .getItems()
                                                .size();
                            }


                            @Override
                            protected void updateItem(
                                    Void item,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        item,
                                        empty
                                );


                                setGraphic(
                                        empty
                                                ? null
                                                : box
                                );
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


            stage.showAndWait();


            Payment payment =
                    controller.getPayment();


            if (payment == null) {

                return;
            }


            paymentRepository.save(
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


            stage.showAndWait();


            Payment edited =
                    controller.getPayment();


            if (edited == null) {

                return;
            }


            paymentRepository.update(
                    edited
            );


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

        root.setNodeOrientation(
                LanguageManager.isArabic()
                        ? NodeOrientation.RIGHT_TO_LEFT
                        : NodeOrientation.LEFT_TO_RIGHT
        );


        Stage stage =
                new Stage();


        stage.initModality(
                Modality.APPLICATION_MODAL
        );


        if (paymentsTable != null
                && paymentsTable.getScene() != null
                && paymentsTable
                        .getScene()
                        .getWindow() != null) {

            stage.initOwner(
                    paymentsTable
                            .getScene()
                            .getWindow()
            );
        }


        stage.setTitle(
                title
        );


        Scene scene =
                new Scene(root);


        var css =
                getClass().getResource(
                        "/css/app.css"
                );


        if (css != null) {

            scene.getStylesheets()
                    .add(
                            css.toExternalForm()
                    );
        }


        stage.setScene(
                scene
        );


        // Calculate FXML size first

        stage.sizeToScene();


        // Available desktop size

        var visualBounds =
                Screen.getPrimary()
                        .getVisualBounds();


        double maxWidth =
                Math.max(
                        650,
                        visualBounds.getWidth() - 80
                );


        double maxHeight =
                Math.max(
                        650,
                        visualBounds.getHeight() - 80
                );


        double desiredWidth =
                Math.max(
                        stage.getWidth(),
                        720
                );


        double desiredHeight =
                Math.max(
                        stage.getHeight(),
                        760
                );


        desiredWidth =
                Math.min(
                        desiredWidth,
                        maxWidth
                );


        desiredHeight =
                Math.min(
                        desiredHeight,
                        maxHeight
                );


        stage.setWidth(
                desiredWidth
        );


        stage.setHeight(
                desiredHeight
        );


        stage.setMinWidth(
                Math.min(
                        650,
                        maxWidth
                )
        );


        stage.setMinHeight(
                Math.min(
                        650,
                        maxHeight
                )
        );


        stage.setResizable(
                true
        );


        stage.centerOnScreen();


        return stage;
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
                confirmation.showAndWait();


        if (result.isEmpty()
                || result.get()
                != ButtonType.OK) {

            return;
        }


        try {

            paymentRepository.deleteById(
                    payment.getId()
            );


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


        alert.showAndWait();
    }
}
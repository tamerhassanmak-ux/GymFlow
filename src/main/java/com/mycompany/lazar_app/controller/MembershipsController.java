package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.ui.dialog.DialogStyler;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.MemberMembership;
import com.mycompany.lazar_app.model.MembershipPlan;
import com.mycompany.lazar_app.repository.MemberMembershipRepository;
import com.mycompany.lazar_app.repository.MembershipPlanRepository;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.geometry.NodeOrientation;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class MembershipsController {

    // =========================================================
    // REPOSITORIES
    // =========================================================

    private final MemberMembershipRepository membershipRepository =
            new MemberMembershipRepository();

    private final MembershipPlanRepository planRepository =
            new MembershipPlanRepository();


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<MemberMembership> memberships =
            FXCollections.observableArrayList();

    private FilteredList<MemberMembership> filteredMemberships;


    // =========================================================
    // DATE FORMAT
    // =========================================================

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");


    // =========================================================
    // STATISTICS
    // =========================================================

    @FXML
    private Label activeMembershipsLabel;

    @FXML
    private Label expiringMembershipsLabel;

    @FXML
    private Label expiredMembershipsLabel;

    @FXML
    private Label monthlyRevenueLabel;


    // =========================================================
    // FILTERS
    // =========================================================

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> planFilter;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private Label membershipsCountLabel;


    // =========================================================
    // TABLE
    // =========================================================

    @FXML
    private TableView<MemberMembership> membershipsTable;

    @FXML
    private TableColumn<MemberMembership, Number> idColumn;

    @FXML
    private TableColumn<MemberMembership, String> memberColumn;

    @FXML
    private TableColumn<MemberMembership, String> planColumn;

    @FXML
    private TableColumn<MemberMembership, String> startDateColumn;

    @FXML
    private TableColumn<MemberMembership, String> endDateColumn;

    @FXML
    private TableColumn<MemberMembership, Number> amountColumn;

    @FXML
    private TableColumn<MemberMembership, String> statusColumn;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        System.out.println(
                "MembershipsController loaded successfully."
        );

        configureTable();

        configureFilters();

        configureFilterListeners();

        loadData();
    }


    // =========================================================
    // CONFIGURE TABLE
    // =========================================================

    private void configureTable() {

        idColumn.setCellValueFactory(
                cellData ->
                        new ReadOnlyObjectWrapper<>(
                                cellData.getValue().getId()
                        )
        );


        memberColumn.setCellValueFactory(
                cellData -> {

                    MemberMembership membership =
                            cellData.getValue();

                    String memberName =
                            safe(
                                    membership.getMemberName()
                            );

                    String memberCode =
                            safe(
                                    membership.getMemberCode()
                            );

                    String display;

                    if (memberCode.isBlank()) {

                        display = memberName;

                    } else {

                        display =
                                memberName
                                + " ("
                                + memberCode
                                + ")";
                    }

                    return new ReadOnlyStringWrapper(
                            display
                    );
                }
        );


        planColumn.setCellValueFactory(
                cellData -> {

                    MemberMembership membership =
                            cellData.getValue();

                    String planName;

                    if (LanguageManager.isArabic()) {

                        planName =
                                membership.getPlanNameAr();

                    } else {

                        planName =
                                membership.getPlanNameEn();
                    }

                    return new ReadOnlyStringWrapper(
                            safe(planName)
                    );
                }
        );


        startDateColumn.setCellValueFactory(
                cellData ->
                        new ReadOnlyStringWrapper(
                                formatDate(
                                        cellData
                                                .getValue()
                                                .getStartDate()
                                )
                        )
        );


        endDateColumn.setCellValueFactory(
                cellData ->
                        new ReadOnlyStringWrapper(
                                formatDate(
                                        cellData
                                                .getValue()
                                                .getEndDate()
                                )
                        )
        );


        amountColumn.setCellValueFactory(
                cellData ->
                        new ReadOnlyObjectWrapper<>(
                                cellData
                                        .getValue()
                                        .getFinalPrice()
                        )
        );


        statusColumn.setCellValueFactory(
                cellData ->
                        new ReadOnlyStringWrapper(
                                getDisplayStatus(
                                        cellData.getValue()
                                )
                        )
        );


        membershipsTable.setPlaceholder(
                new Label(
                        LanguageManager.isArabic()
                                ? "لا توجد اشتراكات حتى الآن"
                                : "No memberships found"
                )
        );
    }


    // =========================================================
    // CONFIGURE FILTERS
    // =========================================================

    private void configureFilters() {

        planFilter.getItems().clear();

        statusFilter.getItems().clear();


        if (LanguageManager.isArabic()) {

            planFilter.getItems().add(
                    "جميع الخطط"
            );

            statusFilter.getItems().addAll(
                    "جميع الحالات",
                    "نشط",
                    "منتهي",
                    "موقوف",
                    "ملغي"
            );

        } else {

            planFilter.getItems().add(
                    "All Plans"
            );

            statusFilter.getItems().addAll(
                    "All Status",
                    "Active",
                    "Expired",
                    "Suspended",
                    "Cancelled"
            );
        }


        try {

            List<MembershipPlan> plans =
                    planRepository.findActive();

            for (MembershipPlan plan : plans) {

                String planName =
                        LanguageManager.isArabic()
                                ? plan.getNameAr()
                                : plan.getNameEn();

                planFilter.getItems().add(
                        planName
                );
            }

        } catch (SQLException exception) {

            exception.printStackTrace();

            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تحميل خطط العضوية."
                            : "Could not load membership plans.",
                    exception
            );
        }


        if (!planFilter.getItems().isEmpty()) {

            planFilter
                    .getSelectionModel()
                    .selectFirst();
        }


        if (!statusFilter.getItems().isEmpty()) {

            statusFilter
                    .getSelectionModel()
                    .selectFirst();
        }
    }


    // =========================================================
    // CONFIGURE FILTER LISTENERS
    // =========================================================

    private void configureFilterListeners() {

        searchField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                applyFilters()
                );


        planFilter
                .valueProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                applyFilters()
                );


        statusFilter
                .valueProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                applyFilters()
                );
    }


    // =========================================================
    // LOAD DATA
    // =========================================================

    private void loadData() {

        try {

            memberships.setAll(
                    membershipRepository.findLatestPerMember()
            );


            filteredMemberships =
                    new FilteredList<>(
                            memberships,
                            membership -> true
                    );


            membershipsTable.setItems(
                    filteredMemberships
            );


            loadStatistics();

            applyFilters();


            System.out.println(
                    "Memberships loaded: "
                    + memberships.size()
            );

        } catch (SQLException exception) {

            exception.printStackTrace();

            showError(
                    LanguageManager.isArabic()
                            ? "حدث خطأ أثناء تحميل الاشتراكات."
                            : "An error occurred while loading memberships.",
                    exception
            );
        }
    }


    // =========================================================
    // LOAD STATISTICS
    // =========================================================

    private void loadStatistics()
            throws SQLException {

        int active =
                membershipRepository.countActive();

        int expiring =
                membershipRepository
                        .countExpiringSoon(7);

        int expired =
                membershipRepository.countExpired();

        double monthlyRevenue =
                membershipRepository
                        .getMonthlyRevenue();


        activeMembershipsLabel.setText(
                String.valueOf(active)
        );

        expiringMembershipsLabel.setText(
                String.valueOf(expiring)
        );

        expiredMembershipsLabel.setText(
                String.valueOf(expired)
        );

        monthlyRevenueLabel.setText(
                formatCurrency(
                        monthlyRevenue
                )
        );
    }


    // =========================================================
    // APPLY FILTERS
    // =========================================================

    private void applyFilters() {

        if (filteredMemberships == null) {
            return;
        }


        String searchText =
                safe(
                        searchField.getText()
                ).toLowerCase();


        String selectedPlan =
                planFilter.getValue();


        String selectedStatus =
                statusFilter.getValue();


        filteredMemberships.setPredicate(
                membership -> {

                    if (membership == null) {
                        return false;
                    }


                    // =============================================
                    // SEARCH
                    // =============================================

                    boolean matchesSearch =
                            searchText.isBlank()

                            || containsIgnoreCase(
                                    membership.getMemberName(),
                                    searchText
                            )

                            || containsIgnoreCase(
                                    membership.getMemberCode(),
                                    searchText
                            )

                            || containsIgnoreCase(
                                    membership.getPlanNameAr(),
                                    searchText
                            )

                            || containsIgnoreCase(
                                    membership.getPlanNameEn(),
                                    searchText
                            );


                    if (!matchesSearch) {
                        return false;
                    }


                    // =============================================
                    // PLAN FILTER
                    // =============================================

                    if (!isAllPlans(
                            selectedPlan
                    )) {

                        String membershipPlan =
                                LanguageManager.isArabic()
                                        ? membership.getPlanNameAr()
                                        : membership.getPlanNameEn();

                        if (!safe(membershipPlan)
                                .equalsIgnoreCase(
                                        safe(selectedPlan)
                                )) {

                            return false;
                        }
                    }


                    // =============================================
                    // STATUS FILTER
                    // =============================================

                    if (!isAllStatuses(
                            selectedStatus
                    )) {

                        String displayStatus =
                                getDisplayStatus(
                                        membership
                                );

                        if (!displayStatus
                                .equalsIgnoreCase(
                                        safe(selectedStatus)
                                )) {

                            return false;
                        }
                    }


                    return true;
                }
        );


        updateCountLabel();
    }


    // =========================================================
    // UPDATE COUNT
    // =========================================================

    private void updateCountLabel() {

        int count =
                filteredMemberships == null
                        ? 0
                        : filteredMemberships.size();


        if (LanguageManager.isArabic()) {

            membershipsCountLabel.setText(
                    count + " اشتراك"
            );

        } else {

            membershipsCountLabel.setText(
                    count + " Memberships"
            );
        }
    }


    // =========================================================
    // NEW MEMBERSHIP
    // =========================================================
@FXML
private void openNewMembership() {

    try {

        // =====================================================
        // RESOURCE BUNDLE
        // =====================================================

        ResourceBundle bundle =
                ResourceBundle.getBundle(
                        "i18n.messages",
                        LanguageManager.isArabic()
                                ? new Locale("ar")
                                : new Locale("en")
                );


        // =====================================================
        // LOAD FXML
        // =====================================================

        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/fxml/add-membership.fxml"
                        ),
                        bundle
                );

        Parent root =
                loader.load();


        // =====================================================
        // LANGUAGE DIRECTION
        // =====================================================

        if (LanguageManager.isArabic()) {

            root.setNodeOrientation(
                    NodeOrientation.RIGHT_TO_LEFT
            );

        } else {

            root.setNodeOrientation(
                    NodeOrientation.LEFT_TO_RIGHT
            );
        }


        // =====================================================
        // CREATE SCENE
        // =====================================================

        Scene scene =
                new Scene(root);


        // =====================================================
        // LOAD APPLICATION CSS
        // =====================================================

        var css =
                getClass().getResource(
                        "/css/app.css"
                );

        if (css != null) {

            scene.getStylesheets().add(
                    css.toExternalForm()
            );
        }


        // =====================================================
        // CREATE STAGE
        // =====================================================

        Stage stage =
                new Stage();


        // =====================================================
        // WINDOW TITLE
        // =====================================================

        stage.setTitle(
                LanguageManager.isArabic()
                        ? "اشتراك جديد"
                        : "New Membership"
        );


        // =====================================================
        // MODAL WINDOW
        // =====================================================

        stage.initModality(
                Modality.APPLICATION_MODAL
        );


        // =====================================================
        // SET OWNER
        // =====================================================

        if (membershipsTable != null
                && membershipsTable.getScene() != null
                && membershipsTable
                        .getScene()
                        .getWindow() != null) {

            stage.initOwner(
                    membershipsTable
                            .getScene()
                            .getWindow()
            );
        }


        // =====================================================
        // STAGE SETTINGS
        // =====================================================

        stage.setScene(scene);

        stage.setResizable(true);


        // =====================================================
        // ALWAYS OPEN MAXIMIZED
        // =====================================================

        stage.setMaximized(true);


        // =====================================================
        // SAFETY:
        // ENSURE MAXIMIZED AFTER WINDOW IS SHOWN
        // =====================================================

        stage.setOnShown(event -> {

            if (!stage.isMaximized()) {

                stage.setMaximized(true);
            }
        });


        // =====================================================
        // SHOW WINDOW
        // =====================================================

        stage.showAndWait();


        // =====================================================
        // REFRESH MEMBERSHIPS AFTER CLOSE
        // =====================================================

        refresh();


    } catch (IOException exception) {

        exception.printStackTrace();

        showError(
                LanguageManager.isArabic()
                        ? "تعذر فتح شاشة الاشتراك الجديد."
                        : "Could not open the new membership window.",
                exception
        );


    } catch (Exception exception) {

        exception.printStackTrace();

        showError(
                LanguageManager.isArabic()
                        ? "حدث خطأ أثناء فتح شاشة الاشتراك الجديد."
                        : "An error occurred while opening the new membership window.",
                exception
        );
    }
}


    // =========================================================
    // REFRESH
    // =========================================================

    public void refresh() {

        loadData();
    }


    // =========================================================
    // DISPLAY STATUS
    // =========================================================

    private String getDisplayStatus(
            MemberMembership membership
    ) {

        if (membership == null) {
            return "";
        }


        String databaseStatus =
                safe(
                        membership.getStatus()
                );


        // =============================================
        // CANCELLED
        // =============================================

        if ("Cancelled".equalsIgnoreCase(
                databaseStatus
        )) {

            return LanguageManager.isArabic()
                    ? "ملغي"
                    : "Cancelled";
        }


        // =============================================
        // SUSPENDED
        // =============================================

        if ("Suspended".equalsIgnoreCase(
                databaseStatus
        )) {

            return LanguageManager.isArabic()
                    ? "موقوف"
                    : "Suspended";
        }


        // =============================================
        // EXPIRED BY DATE
        // =============================================

        if (membership.isExpired()) {

            return LanguageManager.isArabic()
                    ? "منتهي"
                    : "Expired";
        }


        // =============================================
        // ACTIVE
        // =============================================

        if ("Active".equalsIgnoreCase(
                databaseStatus
        )) {

            return LanguageManager.isArabic()
                    ? "نشط"
                    : "Active";
        }


        return databaseStatus;
    }


    // =========================================================
    // FORMAT DATE
    // =========================================================

    private String formatDate(
            LocalDate date
    ) {

        if (date == null) {
            return "-";
        }

        return date.format(
                dateFormatter
        );
    }


    // =========================================================
    // FORMAT CURRENCY
    // =========================================================

    private String formatCurrency(
            double amount
    ) {

        NumberFormat numberFormat =
                NumberFormat.getNumberInstance(
                        Locale.US
                );


        numberFormat.setMinimumFractionDigits(
                2
        );


        numberFormat.setMaximumFractionDigits(
                2
        );


        String value =
                numberFormat.format(
                        amount
                );


        return LanguageManager.isArabic()
                ? value + " ج.م"
                : value + " EGP";
    }


    // =========================================================
    // ALL PLANS
    // =========================================================

    private boolean isAllPlans(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return true;
        }


        return value.equals(
                "جميع الخطط"
        )
                || value.equalsIgnoreCase(
                        "All Plans"
                );
    }


    // =========================================================
    // ALL STATUS
    // =========================================================

    private boolean isAllStatuses(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return true;
        }


        return value.equals(
                "جميع الحالات"
        )
                || value.equalsIgnoreCase(
                        "All Status"
                );
    }


    // =========================================================
    // CONTAINS IGNORE CASE
    // =========================================================

    private boolean containsIgnoreCase(
            String value,
            String search
    ) {

        if (value == null) {
            return false;
        }


        return value
                .toLowerCase()
                .contains(
                        search
                );
    }


    // =========================================================
    // SAFE
    // =========================================================

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }


    // =========================================================
    // ERROR
    // =========================================================

    private void showError(
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


        String details =
                exception == null
                        ? ""
                        : exception.getMessage();


        alert.setContentText(
                details == null
                        ? ""
                        : details
        );


        DialogStyler.style(alert);


        alert.showAndWait();
    }
}
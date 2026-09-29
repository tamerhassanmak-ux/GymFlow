package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.ui.dialog.DialogStyler;
import com.mycompany.lazar_app.ui.WindowManager;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.MembershipPlan;
import com.mycompany.lazar_app.repository.MembershipPlanRepository;

import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.geometry.NodeOrientation;

import javafx.scene.Parent;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

import java.sql.SQLException;

import java.text.NumberFormat;

import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;


public class MembershipPlansController {

    // =========================================================
    // REPOSITORY
    // =========================================================

    private final MembershipPlanRepository repository =
            new MembershipPlanRepository();


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<MembershipPlan> masterData =
            FXCollections.observableArrayList();

    private FilteredList<MembershipPlan> filteredData;


    // =========================================================
    // HEADER
    // =========================================================

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Button addPlanButton;


    // =========================================================
    // KPI
    // =========================================================

    @FXML
    private Label totalPlansLabel;

    @FXML
    private Label activePlansLabel;

    @FXML
    private Label inactivePlansLabel;


    // =========================================================
    // FILTERS
    // =========================================================

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private Label plansCountLabel;


    // =========================================================
    // TABLE
    // =========================================================

    @FXML
    private TableView<MembershipPlan> plansTable;

    @FXML
    private TableColumn<MembershipPlan, Number> idColumn;

    @FXML
    private TableColumn<MembershipPlan, String> codeColumn;

    @FXML
    private TableColumn<MembershipPlan, String> nameArColumn;

    @FXML
    private TableColumn<MembershipPlan, String> nameEnColumn;

    @FXML
    private TableColumn<MembershipPlan, Number> durationColumn;

    @FXML
    private TableColumn<MembershipPlan, Number> maxSessionsColumn;

    @FXML
    private TableColumn<MembershipPlan, Number> priceColumn;

    @FXML
    private TableColumn<MembershipPlan, String> statusColumn;

    @FXML
    private TableColumn<MembershipPlan, Void> actionsColumn;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        configureLanguage();

        configureTable();

        configureFilters();

        loadData();
    }


    // =========================================================
    // LANGUAGE
    // =========================================================

    private void configureLanguage() {

        boolean arabic =
                LanguageManager.isArabic();


        if (arabic) {

            titleLabel.setText(
                    "إدارة خطط الاشتراك"
            );

            subtitleLabel.setText(
                    "إنشاء وتعديل وإدارة أسعار ومدد خطط الاشتراكات"
            );

            addPlanButton.setText(
                    "+ إضافة خطة"
            );

            searchField.setPromptText(
                    "بحث بالكود أو اسم الخطة..."
            );

            idColumn.setText(
                    "#"
            );

            codeColumn.setText(
                    "الكود"
            );

            nameArColumn.setText(
                    "الاسم العربي"
            );

            nameEnColumn.setText(
                    "الاسم الإنجليزي"
            );

            durationColumn.setText(
                    "المدة"
            );

            maxSessionsColumn.setText(
                    "الجلسات"
            );

            priceColumn.setText(
                    "السعر"
            );

            statusColumn.setText(
                    "الحالة"
            );

            actionsColumn.setText(
                    "الإجراءات"
            );

            plansTable.setNodeOrientation(
                    NodeOrientation.RIGHT_TO_LEFT
            );

        } else {

            titleLabel.setText(
                    "Membership Plans"
            );

            subtitleLabel.setText(
                    "Create and manage membership plans, prices and durations"
            );

            addPlanButton.setText(
                    "+ Add Plan"
            );

            searchField.setPromptText(
                    "Search by code or plan name..."
            );

            idColumn.setText(
                    "#"
            );

            codeColumn.setText(
                    "Code"
            );

            nameArColumn.setText(
                    "Arabic Name"
            );

            nameEnColumn.setText(
                    "English Name"
            );

            durationColumn.setText(
                    "Duration"
            );

            maxSessionsColumn.setText(
                    "Sessions"
            );

            priceColumn.setText(
                    "Price"
            );

            statusColumn.setText(
                    "Status"
            );

            actionsColumn.setText(
                    "Actions"
            );
        }
    }


    // =========================================================
    // TABLE
    // =========================================================

    private void configureTable() {

        plansTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );


        idColumn.setCellValueFactory(
                data ->
                        new ReadOnlyIntegerWrapper(
                                data.getValue().getId()
                        )
        );


        codeColumn.setCellValueFactory(
                data ->
                        new ReadOnlyStringWrapper(
                                safe(
                                        data.getValue().getCode()
                                )
                        )
        );


        nameArColumn.setCellValueFactory(
                data ->
                        new ReadOnlyStringWrapper(
                                safe(
                                        data.getValue().getNameAr()
                                )
                        )
        );


        nameEnColumn.setCellValueFactory(
                data ->
                        new ReadOnlyStringWrapper(
                                safe(
                                        data.getValue().getNameEn()
                                )
                        )
        );


        durationColumn.setCellValueFactory(
                data ->
                        new ReadOnlyIntegerWrapper(
                                data.getValue()
                                        .getDurationMonths()
                        )
        );


        durationColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    Number months,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        months,
                                        empty
                                );


                                if (empty || months == null) {

                                    setText(null);

                                    return;
                                }


                                int value =
                                        months.intValue();


                                setText(
                                        LanguageManager.isArabic()
                                                ? value + " شهر"
                                                : value + " Month"
                                                + (value == 1 ? "" : "s")
                                );
                            }
                        }
        );


        maxSessionsColumn.setCellValueFactory(
                data ->
                        new ReadOnlyIntegerWrapper(
                                data.getValue()
                                        .getMaxSessions()
                        )
        );


        maxSessionsColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    Number sessions,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        sessions,
                                        empty
                                );


                                if (empty || sessions == null) {

                                    setText(null);

                                    return;
                                }


                                int value =
                                        sessions.intValue();


                                setText(
                                        LanguageManager.isArabic()
                                                ? value + " جلسة"
                                                : value + " Session"
                                                + (value == 1 ? "" : "s")
                                );
                            }
                        }
        );


        priceColumn.setCellValueFactory(
                data ->
                        new ReadOnlyDoubleWrapper(
                                data.getValue().getPrice()
                        )
        );


        priceColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    Number price,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        price,
                                        empty
                                );


                                if (empty || price == null) {

                                    setText(null);

                                    return;
                                }


                                setText(
                                        formatMoney(
                                                price.doubleValue()
                                        )
                                );
                            }
                        }
        );


        statusColumn.setCellValueFactory(
                data ->
                        new ReadOnlyStringWrapper(
                                safe(
                                        data.getValue().getStatus()
                                )
                        )
        );


        statusColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    String status,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        status,
                                        empty
                                );


                                if (empty || status == null) {

                                    setText(null);

                                    return;
                                }


                                boolean active =
                                        "Active".equalsIgnoreCase(
                                                status
                                        );


                                setText(
                                        LanguageManager.isArabic()
                                                ? active
                                                    ? "نشطة"
                                                    : "غير نشطة"
                                                : active
                                                    ? "Active"
                                                    : "Inactive"
                                );
                            }
                        }
        );


        configureActionsColumn();


        plansTable.setRowFactory(
                table -> {

                    var row =
                            new javafx.scene.control.TableRow<MembershipPlan>();


                    row.setOnMouseClicked(
                            event -> {

                                if (event.getClickCount() == 2
                                        && !row.isEmpty()) {

                                    editPlan(
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
    // ACTION BUTTONS
    // =========================================================

    private void configureActionsColumn() {

        actionsColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            private final Button editButton =
                                    new Button();

                            private final Button statusButton =
                                    new Button();

                            private final Button deleteButton =
                                    new Button();

                            private final javafx.scene.layout.HBox container =
                                    new javafx.scene.layout.HBox(
                                            6,
                                            editButton,
                                            statusButton,
                                            deleteButton
                                    );


                            {

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


                                editButton.setOnAction(
                                        event -> {

                                            MembershipPlan plan =
                                                    getCurrentPlan();

                                            if (plan != null) {

                                                editPlan(
                                                        plan
                                                );
                                            }
                                        }
                                );


                                statusButton.setOnAction(
                                        event -> {

                                            MembershipPlan plan =
                                                    getCurrentPlan();

                                            if (plan != null) {

                                                toggleStatus(
                                                        plan
                                                );
                                            }
                                        }
                                );


                                deleteButton.setOnAction(
                                        event -> {

                                            MembershipPlan plan =
                                                    getCurrentPlan();

                                            if (plan != null) {

                                                deletePlan(
                                                        plan
                                                );
                                            }
                                        }
                                );
                            }


                            private MembershipPlan getCurrentPlan() {

                                int index =
                                        getIndex();


                                if (index < 0
                                        || index >= getTableView()
                                                .getItems()
                                                .size()) {

                                    return null;
                                }


                                return getTableView()
                                        .getItems()
                                        .get(index);
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


                                if (empty) {

                                    setGraphic(null);

                                    return;
                                }


                                MembershipPlan plan =
                                        getCurrentPlan();


                                if (plan != null) {

                                    statusButton.setText(
                                            plan.isActive()
                                                    ? LanguageManager.isArabic()
                                                        ? "إيقاف"
                                                        : "Disable"
                                                    : LanguageManager.isArabic()
                                                        ? "تفعيل"
                                                        : "Enable"
                                    );
                                }


                                setGraphic(
                                        container
                                );
                            }
                        }
        );
    }


    // =========================================================
    // FILTERS
    // =========================================================

    private void configureFilters() {

        statusFilter.getItems().clear();


        if (LanguageManager.isArabic()) {

            statusFilter.getItems().addAll(
                    "الكل",
                    "نشطة",
                    "غير نشطة"
            );

        } else {

            statusFilter.getItems().addAll(
                    "All",
                    "Active",
                    "Inactive"
            );
        }


        statusFilter.getSelectionModel()
                .selectFirst();


        searchField.textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );


        statusFilter.valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );
    }


    // =========================================================
    // LOAD DATA
    // =========================================================

    private void loadData() {

        try {

            masterData.setAll(
                    repository.findAll()
            );


            filteredData =
                    new FilteredList<>(
                            masterData,
                            plan -> true
                    );


            plansTable.setItems(
                    filteredData
            );


            updateStatistics();

            applyFilters();


        } catch (SQLException exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تحميل خطط الاشتراك."
                            : "Could not load membership plans.",
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
    // APPLY FILTERS
    // =========================================================

    private void applyFilters() {

        if (filteredData == null) {

            return;
        }


        String query =
                normalize(
                        searchField.getText()
                );


        String selectedStatus =
                statusFilter.getValue();


        filteredData.setPredicate(
                plan -> {

                    boolean matchesSearch =
                            query.isBlank()
                            || normalize(
                                    plan.getCode()
                            ).contains(query)
                            || normalize(
                                    plan.getNameAr()
                            ).contains(query)
                            || normalize(
                                    plan.getNameEn()
                            ).contains(query)
                            || normalize(
                                    plan.getDescription()
                            ).contains(query);


                    if (!matchesSearch) {

                        return false;
                    }


                    if (selectedStatus == null
                            || selectedStatus.equals("الكل")
                            || selectedStatus.equals("All")) {

                        return true;
                    }


                    if (selectedStatus.equals("نشطة")
                            || selectedStatus.equals("Active")) {

                        return plan.isActive();
                    }


                    return !plan.isActive();
                }
        );


        updateVisibleCount();
    }


    // =========================================================
    // STATISTICS
    // =========================================================

    private void updateStatistics() {

        long active =
                masterData.stream()
                        .filter(
                                MembershipPlan::isActive
                        )
                        .count();


        long inactive =
                masterData.size()
                - active;


        totalPlansLabel.setText(
                String.valueOf(
                        masterData.size()
                )
        );


        activePlansLabel.setText(
                String.valueOf(
                        active
                )
        );


        inactivePlansLabel.setText(
                String.valueOf(
                        inactive
                )
        );
    }


    private void updateVisibleCount() {

        int count =
                filteredData == null
                        ? 0
                        : filteredData.size();


        plansCountLabel.setText(
                LanguageManager.isArabic()
                        ? "عدد النتائج: " + count
                        : "Results: " + count
        );
    }


    // =========================================================
    // ADD
    // =========================================================

    @FXML
    private void addPlan() {

        openPlanForm(
                null
        );
    }


    // =========================================================
    // EDIT
    // =========================================================

    private void editPlan(
            MembershipPlan plan
    ) {

        openPlanForm(
                plan
        );
    }


    // =========================================================
    // OPEN FORM
    // =========================================================

    private void openPlanForm(
            MembershipPlan plan
    ) {

        try {

            Locale locale =
                    LanguageManager.isArabic()
                            ? new Locale("ar")
                            : Locale.ENGLISH;


            ResourceBundle bundle =
                    ResourceBundle.getBundle(
                            "i18n.messages",
                            locale
                    );


            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/membership-plan-form.fxml"
                            ),
                            bundle
                    );


            Parent root =
                    loader.load();


            MembershipPlanFormController controller =
                    loader.getController();


            controller.setPlan(
                    plan
            );


            String windowTitle =
                    LanguageManager.isArabic()
                            ? plan == null
                                ? "إضافة خطة اشتراك"
                                : "تعديل خطة الاشتراك"
                            : plan == null
                                ? "Add Membership Plan"
                                : "Edit Membership Plan";


            Stage stage =
                    WindowManager.createModalStage(
                            root,
                            windowTitle,
                            getOwnerWindow()
                    );


            /*
             * Centralized window policy:
             * WindowManager applies the usable-screen bounds, minimum size,
             * owner relationship and Linux/Ubuntu maximize retry.
             */
            WindowManager.showMaximizedAndWait(
                    stage,
                    820.0,
                    650.0
            );


            if (controller.isSaved()) {

                refresh();
            }


        } catch (IOException exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر فتح نافذة خطة الاشتراك."
                            : "Could not open membership plan form.",
                    exception
            );
        }
    }


    // =========================================================
    // ENABLE / DISABLE
    // =========================================================

    private void toggleStatus(
            MembershipPlan plan
    ) {

        if (plan == null) {

            return;
        }


        String oldStatus =
                plan.getStatus();


        String newStatus =
                plan.isActive()
                        ? "Inactive"
                        : "Active";


        try {

            plan.setStatus(
                    newStatus
            );


            repository.update(
                    plan
            );


            refresh();


        } catch (Exception exception) {

            plan.setStatus(
                    oldStatus
            );


            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تغيير حالة الخطة."
                            : "Could not change plan status.",
                    exception
            );
        }
    }


    // =========================================================
    // DELETE
    // =========================================================

    private void deletePlan(
            MembershipPlan plan
    ) {

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
                        ? "هل تريد حذف خطة الاشتراك؟"
                        : "Delete membership plan?"
        );


        confirmation.setContentText(
                LanguageManager.isArabic()
                        ? "الخطة: "
                            + safe(plan.getNameAr())
                            + "\n\nإذا كانت الخطة مستخدمة في اشتراكات سابقة فلن يتم حذفها حفاظًا على التاريخ."
                        : "Plan: "
                            + safe(plan.getNameEn())
                            + "\n\nIf this plan is used by memberships it will not be deleted to preserve history."
        );


        DialogStyler.style(confirmation);

        Optional<ButtonType> result =
                WindowManager.showResponsiveDialogAndWait(
                        confirmation,
                        getOwnerWindow(),
                        590.0,
                        360.0
                );


        if (result.isEmpty()
                || result.get()
                        != ButtonType.OK) {

            return;
        }


        try {

            boolean deleted =
                    repository.delete(
                            plan.getId()
                    );


            if (deleted) {

                refresh();
            }


        } catch (SQLException exception) {

            exception.printStackTrace();


            showWarning(
                    LanguageManager.isArabic()
                            ? "لا يمكن حذف هذه الخطة لأنها مستخدمة في اشتراكات سابقة.\n\nيمكنك إيقاف الخطة بدلًا من حذفها."
                            : "This plan cannot be deleted because it is used by existing memberships.\n\nDisable the plan instead."
            );
        }
    }


    // =========================================================
    // NORMALIZE SEARCH
    // =========================================================

    private String normalize(
            String value
    ) {

        if (value == null) {

            return "";
        }


        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ى", "ي")
                .replace("ة", "ه")
                .replaceAll(
                        "[\\u064B-\\u065F\\u0670]",
                        ""
                );
    }


    // =========================================================
    // MONEY
    // =========================================================

    private String formatMoney(
            double value
    ) {

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        Locale.US
                );


        formatter.setMinimumFractionDigits(
                2
        );


        formatter.setMaximumFractionDigits(
                2
        );


        return formatter.format(
                value
        )
                + (
                    LanguageManager.isArabic()
                            ? " ج.م"
                            : " EGP"
                );
    }


    // =========================================================
    // OWNER WINDOW
    // =========================================================

    private Window getOwnerWindow() {

        if (plansTable == null
                || plansTable.getScene() == null) {

            return null;
        }

        return plansTable
                .getScene()
                .getWindow();
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
    // WARNING
    // =========================================================

    private void showWarning(
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


        WindowManager.showResponsiveDialogAndWait(
                alert,
                getOwnerWindow(),
                560.0,
                320.0
        );
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


        alert.setContentText(
                exception == null
                        || exception.getMessage() == null
                        ? ""
                        : exception.getMessage()
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
package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.ui.dialog.DialogStyler;
import com.mycompany.lazar_app.ui.WindowManager;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.DashboardContentController;

import com.mycompany.lazar_app.model.CheckIn;
import com.mycompany.lazar_app.model.Member;
import com.mycompany.lazar_app.model.MemberMembership;

import com.mycompany.lazar_app.repository.CheckInRepository;
import com.mycompany.lazar_app.repository.MemberRepository;
import com.mycompany.lazar_app.repository.MemberMembershipRepository;

import java.sql.SQLException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.application.Platform;

import javafx.beans.property.SimpleStringProperty;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import javafx.fxml.FXML;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.stage.Window;


public class CheckinController {

    // =========================================================
    // REPOSITORIES
    // =========================================================

    private final CheckInRepository checkInRepository =
            new CheckInRepository();

    private final MemberRepository memberRepository =
            new MemberRepository();

    private final MemberMembershipRepository membershipRepository =
            new MemberMembershipRepository();


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<CheckIn> masterData =
            FXCollections.observableArrayList();

    private FilteredList<CheckIn> filteredData;


    // =========================================================
    // STATISTICS
    // =========================================================

    @FXML
    private Label todayCheckinsLabel;

    @FXML
    private Label currentlyInsideLabel;

    @FXML
    private Label checkedOutLabel;

    @FXML
    private Label activeMembersLabel;

    @FXML
    private Label attendanceCountLabel;


    // =========================================================
    // SEARCH
    // =========================================================

    @FXML
    private TextField searchField;


    // =========================================================
    // TABLE
    // =========================================================

    @FXML
    private TableView<CheckIn> checkinTable;

    @FXML
    private TableColumn<CheckIn, String> idColumn;

    @FXML
    private TableColumn<CheckIn, String> memberColumn;

    @FXML
    private TableColumn<CheckIn, String> phoneColumn;

    @FXML
    private TableColumn<CheckIn, String> membershipColumn;

    @FXML
    private TableColumn<CheckIn, String> checkinTimeColumn;

    @FXML
    private TableColumn<CheckIn, String> checkoutTimeColumn;

    @FXML
    private TableColumn<CheckIn, String> statusColumn;

    @FXML
    private TableColumn<CheckIn, Void> actionsColumn;


    // =========================================================
    // FORMAT
    // =========================================================

    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm"
            );


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        configureTable();

        configureSearch();

        loadData();
    }


    // =========================================================
    // CONFIGURE TABLE
    // =========================================================

    private void configureTable() {

        checkinTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );


        idColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                String.valueOf(
                                        data.getValue().getId()
                                )
                        )
        );


        memberColumn.setCellValueFactory(
                data -> {

                    CheckIn record =
                            data.getValue();

                    return new SimpleStringProperty(
                            safe(record.getMemberName())
                                    + "\n"
                                    + safe(record.getMemberCode())
                    );
                }
        );


        phoneColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                safe(
                                        data.getValue().getPhone()
                                )
                        )
        );


        membershipColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                data.getValue()
                                        .getMembershipDisplayName(
                                                LanguageManager.isArabic()
                                        )
                        )
        );


        checkinTimeColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                formatDateTime(
                                        data.getValue()
                                                .getCheckInTime()
                                )
                        )
        );


        checkoutTimeColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                formatDateTime(
                                        data.getValue()
                                                .getCheckOutTime()
                                )
                        )
        );


        statusColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                getStatusText(
                                        data.getValue()
                                )
                        )
        );


        configureActionsColumn();
    }


    // =========================================================
    // ACTIONS COLUMN
    // =========================================================

    private void configureActionsColumn() {

        actionsColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            private final Button checkoutButton =
                                    new Button();

                            private final Button notesButton =
                                    new Button();

                            private final Button deleteButton =
                                    new Button();

                            private final HBox box =
                                    new HBox(
                                            5,
                                            checkoutButton,
                                            notesButton,
                                            deleteButton
                                    );


                            {

                                // =================================================
                                // TEXT
                                // =================================================

                                checkoutButton.setText(
                                        LanguageManager.isArabic()
                                                ? "خروج"
                                                : "Out"
                                );


                                notesButton.setText(
                                        LanguageManager.isArabic()
                                                ? "ملاحظة"
                                                : "Note"
                                );


                                deleteButton.setText(
                                        LanguageManager.isArabic()
                                                ? "حذف"
                                                : "Delete"
                                );


                                // =================================================
                                // STYLE
                                // =================================================

                                checkoutButton.setStyle(
                                        "-fx-background-color:#16A34A;"
                                                + "-fx-text-fill:white;"
                                                + "-fx-background-radius:6;"
                                                + "-fx-cursor:hand;"
                                );


                                notesButton.setStyle(
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


                                // =================================================
                                // CHECK OUT
                                // =================================================

                                checkoutButton.setOnAction(
                                        event -> {

                                            CheckIn record =
                                                    getCurrentRecord();

                                            if (record != null) {

                                                checkout(record);
                                            }
                                        }
                                );


                                // =================================================
                                // NOTES
                                // =================================================

                                notesButton.setOnAction(
                                        event -> {

                                            CheckIn record =
                                                    getCurrentRecord();

                                            if (record != null) {

                                                editNotes(record);
                                            }
                                        }
                                );


                                // =================================================
                                // DELETE
                                // =================================================

                                deleteButton.setOnAction(
                                        event -> {

                                            CheckIn record =
                                                    getCurrentRecord();

                                            if (record != null) {

                                                deleteRecord(record);
                                            }
                                        }
                                );
                            }


                            private CheckIn getCurrentRecord() {

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


                                CheckIn record =
                                        getCurrentRecord();


                                if (record == null) {

                                    setGraphic(null);

                                    return;
                                }


                                checkoutButton.setDisable(
                                        !record.isInside()
                                );


                                setGraphic(box);
                            }
                        }
        );
    }


    // =========================================================
    // CONFIGURE SEARCH
    // =========================================================

    private void configureSearch() {

        filteredData =
                new FilteredList<>(
                        masterData,
                        value -> true
                );


        checkinTable.setItems(
                filteredData
        );


        searchField.textProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                applySearch(
                                        newValue
                                )
                );
    }


    // =========================================================
    // SEARCH BUTTON
    // =========================================================

    @FXML
    private void search() {

        applySearch(
                searchField.getText()
        );
    }


    // =========================================================
    // APPLY SEARCH
    // =========================================================

    private void applySearch(
            String value
    ) {

        if (filteredData == null) {

            return;
        }


        String search =
                normalize(
                        value
                );


        filteredData.setPredicate(
                record -> {

                    if (search.isBlank()) {

                        return true;
                    }


                    return normalize(
                            record.getMemberName()
                    ).contains(search)

                            || normalize(
                                    record.getMemberCode()
                            ).contains(search)

                            || normalize(
                                    record.getPhone()
                            ).contains(search)

                            || normalize(
                                    record.getMembershipDisplayName(
                                            LanguageManager.isArabic()
                                    )
                            ).contains(search)

                            || normalize(
                                    record.getStatus()
                            ).contains(search);
                }
        );


        updateAttendanceCount();
    }


    // =========================================================
    // LOAD DATA
    // =========================================================

    private void loadData() {

        try {

            masterData.setAll(
                    checkInRepository.findToday()
            );


            updateStatistics();

            updateAttendanceCount();


        } catch (SQLException exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تحميل بيانات الحضور."
                            : "Could not load attendance data."
            );
        }
    }


    // =========================================================
    // REFRESH
    // =========================================================

    @FXML
    private void refresh() {

        loadData();

        searchField.clear();
    }


    // =========================================================
    // OPEN NEW CHECK-IN
    // =========================================================

    @FXML
    private void openCheckinDialog() {

        Dialog<Void> dialog =
                new Dialog<>();


        // =====================================================
        // TITLE
        // =====================================================

        dialog.setTitle(
                LanguageManager.isArabic()
                        ? "تسجيل حضور جديد"
                        : "New Check-in"
        );


        dialog.setHeaderText(
                LanguageManager.isArabic()
                        ? "امسح باركود العضو"
                        : "Scan Member Barcode"
        );


        // =====================================================
        // BARCODE FIELD
        // =====================================================

        TextField barcodeField =
                new TextField();


        barcodeField.setPrefWidth(
                430
        );


        barcodeField.setPrefHeight(
                50
        );


        barcodeField.setPromptText(
                LanguageManager.isArabic()
                        ? "امسح الباركود أو أدخل كود العضو"
                        : "Scan barcode or enter member code"
        );


        barcodeField.setStyle(
                "-fx-font-size:17px;"
                        + "-fx-font-weight:bold;"
                        + "-fx-padding:10 14;"
                        + "-fx-background-color:white;"
                        + "-fx-background-radius:8;"
                        + "-fx-border-radius:8;"
                        + "-fx-border-color:#D1D5DB;"
                        + "-fx-border-width:1.5;"
        );


        // =====================================================
        // HELP LABEL
        // =====================================================

        Label helpLabel =
                new Label(
                        LanguageManager.isArabic()
                                ? "ضع المؤشر هنا ثم امسح بطاقة العضو — مثال: LZR-000001"
                                : "Scan the member card — Example: LZR-000001"
                );


        helpLabel.setStyle(
                "-fx-text-fill:#6B7280;"
                        + "-fx-font-size:12px;"
        );


        // =====================================================
        // STATUS LABEL
        // =====================================================

        Label statusLabel =
                new Label(
                        LanguageManager.isArabic()
                                ? "جاهز للمسح"
                                : "Ready to scan"
                );


        statusLabel.setStyle(
                "-fx-font-size:13px;"
                        + "-fx-font-weight:bold;"
                        + "-fx-text-fill:#16A34A;"
        );


        // =====================================================
        // CONTENT
        // =====================================================

        VBox content =
                new VBox(
                        12,
                        barcodeField,
                        helpLabel,
                        statusLabel
                );


        content.setPrefWidth(
                450
        );


        dialog.getDialogPane()
                .setContent(
                        content
                );


        // =====================================================
        // CANCEL BUTTON
        // =====================================================

        ButtonType cancelButton =
                new ButtonType(
                        LanguageManager.isArabic()
                                ? "إلغاء"
                                : "Cancel",

                        ButtonBar.ButtonData.CANCEL_CLOSE
                );


        dialog.getDialogPane()
                .getButtonTypes()
                .add(
                        cancelButton
                );


        // =====================================================
        // AUTO FOCUS WHEN DIALOG OPENS
        // =====================================================

        dialog.setOnShown(
                event ->
                        Platform.runLater(
                                () -> {

                                    barcodeField.clear();

                                    barcodeField.requestFocus();

                                    barcodeField.positionCaret(
                                            0
                                    );
                                }
                        )
        );


        // =====================================================
        // SCAN BARCODE + ENTER
        // =====================================================

        barcodeField.setOnAction(
                event -> {

                    String code =
                            safe(
                                    barcodeField.getText()
                            );


                    // -----------------------------------------
                    // Empty
                    // -----------------------------------------

                    if (code.isBlank()) {

                        barcodeField.requestFocus();

                        return;
                    }


                    // -----------------------------------------
                    // Show processing
                    // -----------------------------------------

                    statusLabel.setText(
                            LanguageManager.isArabic()
                                    ? "جارٍ التحقق..."
                                    : "Checking..."
                    );


                    // -----------------------------------------
                    // Register
                    // -----------------------------------------

                    boolean saved =
                            tryQuickCheckInAndReturnResult(
                                    code
                            );


                    // =========================================
                    // SUCCESS
                    // =========================================

                    if (saved) {

                        // Clear barcode
                        barcodeField.clear();


                        // Close dialog immediately
                        dialog.close();


                        // Refresh table + KPIs
                        loadData();


                        // Return focus to main screen
                        Platform.runLater(
                                () -> {

                                    searchField.clear();

                                    searchField.requestFocus();
                                }
                        );

                    } else {

                        // =====================================
                        // FAILED
                        // Keep New Check-in open
                        // =====================================

                        statusLabel.setText(
                                LanguageManager.isArabic()
                                        ? "لم يتم التسجيل — حاول مرة أخرى"
                                        : "Not registered — try again"
                        );


                        statusLabel.setStyle(
                                "-fx-font-size:13px;"
                                        + "-fx-font-weight:bold;"
                                        + "-fx-text-fill:#DC2626;"
                        );


                        barcodeField.clear();


                        Platform.runLater(
                                barcodeField::requestFocus
                        );
                    }
                }
        );


        // =====================================================
        // SHOW DIALOG
        // =====================================================

        DialogStyler.style(dialog);

        WindowManager.showResponsiveDialogAndWait(
                dialog,
                getOwnerWindow(),
                620.0,
                400.0
        );


        // =====================================================
        // AFTER DIALOG CLOSES
        // =====================================================

        Platform.runLater(
                () -> {

                    searchField.clear();

                    searchField.requestFocus();
                }
        );
    }


    // =========================================================
    // QUICK CHECK-IN WITH RESULT
    // =========================================================

    private boolean tryQuickCheckInAndReturnResult(
            String searchValue
    ) {

        if (searchValue == null
                || searchValue.isBlank()) {

            return false;
        }


        try {

            List<Member> members =
                    memberRepository.findAll();


            String search =
                    normalize(
                            searchValue
                    );


            // =================================================
            // 1. EXACT MEMBER CODE
            // Barcode scanner uses this first.
            // =================================================

            for (Member member : members) {

                if (normalize(
                        member.getMemberCode()
                ).equals(search)) {

                    return registerCheckInAndReturnResult(
                            member
                    );
                }
            }


            // =================================================
            // 2. EXACT PHONE
            // =================================================

            for (Member member : members) {

                if (normalize(
                        member.getPhone()
                ).equals(search)) {

                    return registerCheckInAndReturnResult(
                            member
                    );
                }
            }


            // =================================================
            // 3. MEMBER NAME
            // =================================================

            List<Member> matches =
                    new ArrayList<>();


            for (Member member : members) {

                if (normalize(
                        member.getName()
                ).contains(search)) {

                    matches.add(
                            member
                    );
                }
            }


            // =================================================
            // NOT FOUND
            // =================================================

            if (matches.isEmpty()) {

                showWarning(
                        LanguageManager.isArabic()
                                ? "لم يتم العثور على عضو بهذا الكود أو الاسم أو الهاتف."
                                : "Member was not found."
                );

                return false;
            }


            // =================================================
            // MULTIPLE MEMBERS
            // =================================================

            if (matches.size() > 1) {

                showWarning(
                        LanguageManager.isArabic()
                                ? "يوجد أكثر من عضو مطابق. استخدم كود العضو أو رقم الهاتف."
                                : "More than one member matched. Use member code or phone."
                );

                return false;
            }


            // =================================================
            // ONE MEMBER
            // =================================================

            return registerCheckInAndReturnResult(
                    matches.get(0)
            );


        } catch (SQLException exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "حدث خطأ أثناء البحث عن العضو."
                            : "Error searching for member."
            );


            return false;
        }
    }


    // =========================================================
    // REGISTER CHECK-IN
    // =========================================================

    private boolean registerCheckInAndReturnResult(
            Member member
    ) {

        if (member == null) {

            return false;
        }


        try {

            // =================================================
            // ALREADY INSIDE?
            // =================================================

            Optional<CheckIn> openCheckIn =
                    checkInRepository
                            .findOpenByMemberId(
                                    member.getId()
                            );


            if (openCheckIn.isPresent()) {

                showWarning(
                        LanguageManager.isArabic()
                                ? "العضو "
                                        + member.getName()
                                        + " مسجل داخل الصالة بالفعل."
                                : member.getName()
                                        + " is already checked in."
                );


                return false;
            }


            // =================================================
            // GET CURRENT MEMBERSHIP
            // =================================================

            Optional<MemberMembership> currentMembership =
                    membershipRepository
                            .findCurrentMembershipByMemberId(
                                    member.getId()
                            );


            if (currentMembership.isEmpty()) {

                showWarning(
                        LanguageManager.isArabic()
                                ? "لا يوجد اشتراك فعال للعضو "
                                        + member.getName()
                                        + "."
                                : member.getName()
                                        + " does not have an active membership."
                );


                return false;
            }


            MemberMembership membership =
                    currentMembership.get();


            // =================================================
            // MEMBERSHIP VALIDATION
            // =================================================

            if (!membership.isActive()) {

                showWarning(
                        LanguageManager.isArabic()
                                ? "اشتراك العضو "
                                        + member.getName()
                                        + " منتهي أو غير فعال."
                                : member.getName()
                                        + "'s membership is expired or inactive."
                );


                return false;
            }


            // =================================================
            // SESSION LIMIT VALIDATION
            // Both membership date and remaining sessions are
            // mandatory for check-in.
            // =================================================

            int allowedSessions =
                    membership.getAllowedSessions();


            if (allowedSessions <= 0) {

                showWarning(
                        LanguageManager.isArabic()
                                ? "عدد الجلسات غير مضبوط لهذا الاشتراك. راجع الإدارة قبل تسجيل الحضور."
                                : "The session limit is not configured for this membership. Please contact administration."
                );


                return false;
            }


            int usedSessions =
                    checkInRepository.countByMembershipId(
                            membership.getId()
                    );


            if (usedSessions >= allowedSessions) {

                showWarning(
                        LanguageManager.isArabic()
                                ? "تم استهلاك جميع جلسات الاشتراك للعضو "
                                        + member.getName()
                                        + ".\n\n"
                                        + "الجلسات المستخدمة: "
                                        + usedSessions
                                        + " من "
                                        + allowedSessions
                                        + ".\n"
                                        + "يلزم تجديد الاشتراك للسماح بالدخول."
                                : "All membership sessions have been used for "
                                        + member.getName()
                                        + ".\n\n"
                                        + "Used sessions: "
                                        + usedSessions
                                        + " of "
                                        + allowedSessions
                                        + ".\n"
                                        + "Membership renewal is required before check-in."
                );


                return false;
            }


            // =================================================
            // CREATE CHECK-IN
            // =================================================

            CheckIn checkIn =
                    new CheckIn();


            checkIn.setMemberId(
                    member.getId()
            );


            // Store membership used at time of attendance
            checkIn.setMembershipId(
                    membership.getId()
            );


            checkIn.setCheckInTime(
                    LocalDateTime.now()
            );


            checkIn.setCheckOutTime(
                    null
            );


            checkIn.setStatus(
                    "IN"
            );


            checkIn.setNotes(
                    ""
            );


            // =================================================
            // SAVE
            // =================================================

            checkInRepository.insert(
                    checkIn
            );


            // =================================================
            // REFRESH DASHBOARD KPI IMMEDIATELY
            // =================================================

            /*
             * The insert has completed successfully at this point, so ask
             * the dashboard that is currently on screen to read the summary
             * again from the database. If the dashboard is not loaded, the
             * call safely does nothing and its next load will show the new
             * value.
             */
            DashboardContentController.refreshCurrentSummary();


            /*
             * IMPORTANT:
             *
             * No success popup.
             *
             * Returning TRUE tells New Check-in dialog:
             *
             * 1. Registration succeeded.
             * 2. Clear barcode.
             * 3. Close dialog.
             * 4. Refresh table.
             */


            return true;


        } catch (SQLException exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تسجيل دخول العضو."
                            : "Could not register check-in."
            );


            return false;
        }
    }


    // =========================================================
    // CHECK OUT
    // =========================================================

    private void checkout(
            CheckIn record
    ) {

        if (record == null
                || !record.isInside()) {

            return;
        }


        ButtonType result =
                showConfirmation(
                        LanguageManager.isArabic()
                                ? "هل تريد تسجيل خروج العضو "
                                        + record.getMemberName()
                                        + "؟"
                                : "Check out "
                                        + record.getMemberName()
                                        + "?"
                );


        if (result != ButtonType.OK) {

            return;
        }


        try {

            boolean updated =
                    checkInRepository.checkOut(
                            record.getId()
                    );


            if (updated) {

                loadData();
            }


        } catch (SQLException exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تسجيل الخروج."
                            : "Could not register check-out."
            );
        }
    }


    // =========================================================
    // EDIT NOTES
    // =========================================================

    private void editNotes(
            CheckIn record
    ) {

        if (record == null) {

            return;
        }


        Dialog<ButtonType> dialog =
                new Dialog<>();


        dialog.setTitle(
                LanguageManager.isArabic()
                        ? "ملاحظات الحضور"
                        : "Attendance Notes"
        );


        dialog.setHeaderText(
                LanguageManager.isArabic()
                        ? "ملاحظات العضو: "
                                + record.getMemberName()
                        : "Notes for: "
                                + record.getMemberName()
        );


        TextField notesField =
                new TextField(
                        safe(
                                record.getNotes()
                        )
                );


        notesField.setPrefWidth(
                400
        );


        notesField.setPromptText(
                LanguageManager.isArabic()
                        ? "أدخل الملاحظات"
                        : "Enter notes"
        );


        dialog.getDialogPane()
                .setContent(
                        notesField
                );


        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        ButtonType.OK,
                        ButtonType.CANCEL
                );


        Platform.runLater(
                notesField::requestFocus
        );


        DialogStyler.style(dialog);

        Optional<ButtonType> result =
                WindowManager.showResponsiveDialogAndWait(
                        dialog,
                        getOwnerWindow(),
                        600.0,
                        340.0
                );


        if (result.isEmpty()
                || result.get() != ButtonType.OK) {

            return;
        }


        try {

            record.setNotes(
                    notesField.getText()
            );


            checkInRepository.update(
                    record
            );


            loadData();


        } catch (SQLException exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تعديل الملاحظات."
                            : "Could not update notes."
            );
        }
    }


    // =========================================================
    // DELETE RECORD
    // =========================================================

    private void deleteRecord(
            CheckIn record
    ) {

        if (record == null) {

            return;
        }


        ButtonType result =
                showConfirmation(
                        LanguageManager.isArabic()
                                ? "هل تريد حذف سجل حضور "
                                        + record.getMemberName()
                                        + "؟"
                                : "Delete attendance record for "
                                        + record.getMemberName()
                                        + "?"
                );


        if (result != ButtonType.OK) {

            return;
        }


        try {

            boolean deleted =
                    checkInRepository.delete(
                            record.getId()
                    );


            if (deleted) {

                loadData();

                // A deleted attendance record changes today's dashboard count.
                DashboardContentController.refreshCurrentSummary();
            }


        } catch (SQLException exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر حذف سجل الحضور."
                            : "Could not delete attendance record."
            );
        }
    }


    // =========================================================
    // UPDATE STATISTICS
    // =========================================================

    private void updateStatistics()
            throws SQLException {

        int today =
                checkInRepository.countToday();


        int inside =
                checkInRepository.countCurrentlyInside();


        int checkedOut =
                checkInRepository.countCheckedOutToday();


        int activeMembers =
                membershipRepository.countActive();


        if (todayCheckinsLabel != null) {

            todayCheckinsLabel.setText(
                    String.valueOf(
                            today
                    )
            );
        }


        if (currentlyInsideLabel != null) {

            currentlyInsideLabel.setText(
                    String.valueOf(
                            inside
                    )
            );
        }


        if (checkedOutLabel != null) {

            checkedOutLabel.setText(
                    String.valueOf(
                            checkedOut
                    )
            );
        }


        if (activeMembersLabel != null) {

            activeMembersLabel.setText(
                    String.valueOf(
                            activeMembers
                    )
            );
        }
    }


    // =========================================================
    // ATTENDANCE COUNT
    // =========================================================

    private void updateAttendanceCount() {

        if (attendanceCountLabel == null
                || filteredData == null) {

            return;
        }


        int count =
                filteredData.size();


        attendanceCountLabel.setText(
                LanguageManager.isArabic()
                        ? count + " حضور"
                        : count + " Check-ins"
        );
    }


    // =========================================================
    // FORMAT DATE TIME
    // =========================================================

    private String formatDateTime(
            LocalDateTime value
    ) {

        if (value == null) {

            return "-";
        }


        return value.format(
                dateTimeFormatter
        );
    }


    // =========================================================
    // STATUS TEXT
    // =========================================================

    private String getStatusText(
            CheckIn record
    ) {

        if (record == null) {

            return "";
        }


        if (record.isInside()) {

            return LanguageManager.isArabic()
                    ? "داخل الصالة"
                    : "Inside";
        }


        return LanguageManager.isArabic()
                ? "غادر"
                : "Checked Out";
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
    // OWNER WINDOW
    // =========================================================

    private Window getOwnerWindow() {

        if (checkinTable == null
                || checkinTable.getScene() == null) {

            return null;
        }

        return checkinTable
                .getScene()
                .getWindow();
    }


    // =========================================================
    // SAFE STRING
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


        /*
         * IMPORTANT:
         *
         * Do NOT redirect focus to main searchField here.
         *
         * If warning occurred while New Check-in dialog
         * is open, the dialog must keep control of focus.
         */

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
                340.0
        );
    }


    // =========================================================
    // CONFIRMATION
    // =========================================================

    private ButtonType showConfirmation(
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        alert.setTitle(
                LanguageManager.isArabic()
                        ? "تأكيد"
                        : "Confirmation"
        );


        alert.setHeaderText(
                null
        );


        alert.setContentText(
                message
        );


        DialogStyler.style(alert);

        return WindowManager.showResponsiveDialogAndWait(
                        alert,
                        getOwnerWindow(),
                        560.0,
                        330.0
                )
                .orElse(
                        ButtonType.CANCEL
                );
    }
}
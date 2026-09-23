package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.ui.dialog.DialogStyler;

import com.mycompany.lazar_app.LanguageManager;

import com.mycompany.lazar_app.model.Member;
import com.mycompany.lazar_app.model.MemberMembership;
import com.mycompany.lazar_app.model.MembershipPlan;

import com.mycompany.lazar_app.repository.MemberRepository;
import com.mycompany.lazar_app.repository.MemberMembershipRepository;
import com.mycompany.lazar_app.repository.MembershipPlanRepository;

import javafx.fxml.FXML;

import javafx.geometry.NodeOrientation;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import javafx.scene.layout.HBox;

import javafx.stage.Stage;

import java.sql.SQLException;

import java.text.NumberFormat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import java.util.function.UnaryOperator;


public class AddMembershipController {


    // =========================================================
    // REPOSITORIES
    // =========================================================

    private final MemberRepository memberRepository =
            new MemberRepository();

    private final MembershipPlanRepository planRepository =
            new MembershipPlanRepository();

    private final MemberMembershipRepository membershipRepository =
            new MemberMembershipRepository();


    // =========================================================
    // DATA CACHE
    // =========================================================

    private final List<Member> allMembers =
            new ArrayList<>();


    // =========================================================
    // DATE FORMAT
    // =========================================================

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy"
            );


    // =========================================================
    // HEADER
    // =========================================================

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;


    // =========================================================
    // LABELS
    // =========================================================

    @FXML
    private Label memberLabel;

    @FXML
    private Label planLabel;

    @FXML
    private Label startDateLabel;

    @FXML
    private Label endDateLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private Label discountLabel;

    @FXML
    private Label finalPriceTitleLabel;

    @FXML
    private Label notesLabel;


    // =========================================================
    // MEMBER SEARCH
    // =========================================================

    @FXML
    private TextField memberSearchField;


    // =========================================================
    // FIELDS
    // =========================================================

    @FXML
    private ComboBox<Member> memberComboBox;

    @FXML
    private ComboBox<MembershipPlan> planComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField priceField;

    @FXML
    private TextField discountField;

    @FXML
    private Label finalPriceLabel;

    @FXML
    private TextArea notesArea;


    // =========================================================
    // BUTTONS
    // =========================================================

    @FXML
    private HBox buttonsContainer;

    @FXML
    private Button cancelButton;

    @FXML
    private Button saveButton;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        System.out.println(
                "AddMembershipController loaded successfully."
        );


        configureDefaults();

        configureLanguage();

        configureMemberComboBox();

        configurePlanComboBox();

        configureDiscountField();

        configureListeners();

        loadMembers();

        loadMembershipPlans();

        updateMembershipDetails();


        // Focus search automatically.
        if (memberSearchField != null) {

            memberSearchField.requestFocus();
        }
    }


    // =========================================================
    // DEFAULTS
    // =========================================================

    private void configureDefaults() {

        startDatePicker.setValue(
                LocalDate.now()
        );


        endDatePicker.setValue(
                null
        );


        endDatePicker.setEditable(
                false
        );


        priceField.setText(
                "0.00"
        );


        priceField.setEditable(
                false
        );


        discountField.setText(
                "0"
        );
    }


    // =========================================================
    // LANGUAGE
    // =========================================================

    private void configureLanguage() {

        if (LanguageManager.isArabic()) {

            configureArabic();

        } else {

            configureEnglish();
        }
    }


    // =========================================================
    // ARABIC
    // =========================================================

    private void configureArabic() {

        titleLabel.setText(
                "اشتراك جديد"
        );


        subtitleLabel.setText(
                "تسجيل اشتراك جديد أو تجديد اشتراك منتهي"
        );


        memberLabel.setText(
                "العضو"
        );


        planLabel.setText(
                "خطة الاشتراك"
        );


        startDateLabel.setText(
                "تاريخ البداية"
        );


        endDateLabel.setText(
                "تاريخ الانتهاء"
        );


        priceLabel.setText(
                "سعر الاشتراك"
        );


        discountLabel.setText(
                "الخصم"
        );


        finalPriceTitleLabel.setText(
                "السعر النهائي"
        );


        notesLabel.setText(
                "ملاحظات"
        );


        if (memberSearchField != null) {

            memberSearchField.setPromptText(
                    "ابحث بالاسم أو امسح باركود العضو"
            );
        }


        memberComboBox.setPromptText(
                "اختر العضو"
        );


        planComboBox.setPromptText(
                "اختر خطة الاشتراك"
        );


        notesArea.setPromptText(
                "ملاحظات اختيارية"
        );


        cancelButton.setText(
                "إلغاء"
        );


        saveButton.setText(
                "حفظ الاشتراك"
        );


        finalPriceLabel.setText(
                "0.00 ج.م"
        );


        buttonsContainer.setNodeOrientation(
                NodeOrientation.RIGHT_TO_LEFT
        );
    }


    // =========================================================
    // ENGLISH
    // =========================================================

    private void configureEnglish() {

        titleLabel.setText(
                "New Membership"
        );


        subtitleLabel.setText(
                "Create a new membership or renew an expired membership"
        );


        memberLabel.setText(
                "Member"
        );


        planLabel.setText(
                "Membership Plan"
        );


        startDateLabel.setText(
                "Start Date"
        );


        endDateLabel.setText(
                "End Date"
        );


        priceLabel.setText(
                "Plan Price"
        );


        discountLabel.setText(
                "Discount"
        );


        finalPriceTitleLabel.setText(
                "Final Price"
        );


        notesLabel.setText(
                "Notes"
        );


        if (memberSearchField != null) {

            memberSearchField.setPromptText(
                    "Search by name or scan member barcode"
            );
        }


        memberComboBox.setPromptText(
                "Select member"
        );


        planComboBox.setPromptText(
                "Select membership plan"
        );


        notesArea.setPromptText(
                "Optional notes"
        );


        cancelButton.setText(
                "Cancel"
        );


        saveButton.setText(
                "Save Membership"
        );


        finalPriceLabel.setText(
                "0.00 EGP"
        );


        buttonsContainer.setNodeOrientation(
                NodeOrientation.LEFT_TO_RIGHT
        );
    }


    // =========================================================
    // MEMBER COMBO BOX
    // =========================================================

    private void configureMemberComboBox() {

        memberComboBox.setCellFactory(
                listView ->
                        new ListCell<>() {

                            @Override
                            protected void updateItem(
                                    Member member,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        member,
                                        empty
                                );


                                if (empty || member == null) {

                                    setText(null);

                                } else {

                                    setText(
                                            getMemberDisplayText(
                                                    member
                                            )
                                    );
                                }
                            }
                        }
        );


        memberComboBox.setButtonCell(
                new ListCell<>() {

                    @Override
                    protected void updateItem(
                            Member member,
                            boolean empty
                    ) {

                        super.updateItem(
                                member,
                                empty
                        );


                        if (empty || member == null) {

                            setText(null);

                        } else {

                            setText(
                                    getMemberDisplayText(
                                            member
                                    )
                            );
                        }
                    }
                }
        );
    }


    // =========================================================
    // MEMBER DISPLAY
    // =========================================================

    private String getMemberDisplayText(
            Member member
    ) {

        if (member == null) {

            return "";
        }


        String name =
                safe(
                        member.getName()
                );


        String code =
                safe(
                        member.getMemberCode()
                );


        if (code.isBlank()) {

            return name;
        }


        return name
                + " ("
                + code
                + ")";
    }


    // =========================================================
    // PLAN COMBO BOX
    // =========================================================

    private void configurePlanComboBox() {

        planComboBox.setCellFactory(
                listView ->
                        new ListCell<>() {

                            @Override
                            protected void updateItem(
                                    MembershipPlan plan,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        plan,
                                        empty
                                );


                                if (empty || plan == null) {

                                    setText(null);

                                } else {

                                    setText(
                                            getPlanDisplayText(
                                                    plan
                                            )
                                    );
                                }
                            }
                        }
        );


        planComboBox.setButtonCell(
                new ListCell<>() {

                    @Override
                    protected void updateItem(
                            MembershipPlan plan,
                            boolean empty
                    ) {

                        super.updateItem(
                                plan,
                                empty
                        );


                        if (empty || plan == null) {

                            setText(null);

                        } else {

                            setText(
                                    getPlanDisplayText(
                                            plan
                                    )
                            );
                        }
                    }
                }
        );
    }


    // =========================================================
    // PLAN DISPLAY
    // =========================================================

    private String getPlanDisplayText(
            MembershipPlan plan
    ) {

        if (plan == null) {

            return "";
        }


        String name =
                LanguageManager.isArabic()
                        ? safe(plan.getNameAr())
                        : safe(plan.getNameEn());


        String sessionsText =
                LanguageManager.isArabic()
                        ? plan.getMaxSessions() + " جلسة"
                        : plan.getMaxSessions() + " sessions";


        return name
                + " - "
                + sessionsText
                + " - "
                + formatMoney(
                        plan.getPrice()
                );
    }


    // =========================================================
    // LOAD MEMBERS
    // =========================================================

    private void loadMembers() {

        try {

            List<Member> members =
                    memberRepository.findAll();


            allMembers.clear();

            allMembers.addAll(
                    members
            );


            memberComboBox
                    .getItems()
                    .setAll(
                            allMembers
                    );


            System.out.println(
                    "Members loaded: "
                    + allMembers.size()
            );


        } catch (SQLException exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تحميل بيانات الأعضاء."
                            : "Could not load members.",
                    exception
            );
        }
    }


    // =========================================================
    // LOAD PLANS
    // =========================================================

    private void loadMembershipPlans() {

        try {

            List<MembershipPlan> plans =
                    planRepository.findActive();


            planComboBox
                    .getItems()
                    .setAll(
                            plans
                    );


            System.out.println(
                    "Membership plans loaded: "
                    + plans.size()
            );


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
    // LISTENERS
    // =========================================================

    private void configureListeners() {

        // =====================================================
        // SMART MEMBER SEARCH
        // =====================================================

        if (memberSearchField != null) {

            memberSearchField
                    .textProperty()
                    .addListener(
                            (
                                    observable,
                                    oldValue,
                                    newValue
                            ) -> {

                                searchMembers(
                                        newValue
                                );
                            }
                    );


            /*
             * Most USB barcode scanners send ENTER
             * after scanning.
             */
            memberSearchField.setOnAction(
                    event ->
                            selectMemberFromBarcode()
            );
        }


        // =====================================================
        // MEMBER SELECTION
        // =====================================================

        memberComboBox
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldMember,
                                newMember
                        ) -> {

                            if (newMember != null) {

                                checkSelectedMemberMembership(
                                        newMember
                                );
                            }
                        }
                );


        // =====================================================
        // PLAN
        // =====================================================

        planComboBox
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldPlan,
                                newPlan
                        ) -> {

                            updateMembershipDetails();
                        }
                );


        // =====================================================
        // START DATE
        // =====================================================

        startDatePicker
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldDate,
                                newDate
                        ) -> {

                            updateEndDate();
                        }
                );


        // =====================================================
        // DISCOUNT
        // =====================================================

        discountField
                .textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            updateFinalPrice();
                        }
                );
    }


    // =========================================================
    // SMART MEMBER SEARCH
    // =========================================================

    private void searchMembers(
            String searchText
    ) {

        String query =
                normalizeSearchText(
                        searchText
                );


        if (query.isBlank()) {

            memberComboBox
                    .getItems()
                    .setAll(
                            allMembers
                    );

            return;
        }


        List<Member> results =
                allMembers
                        .stream()
                        .filter(
                                member ->
                                        memberMatchesSearch(
                                                member,
                                                query
                                        )
                        )
                        .toList();


        memberComboBox
                .getItems()
                .setAll(
                        results
                );


        // =====================================================
        // EXACT BARCODE / MEMBER CODE
        // =====================================================

        Optional<Member> exactCodeMatch =
                results
                        .stream()
                        .filter(
                                member ->
                                        normalizeSearchText(
                                                member.getMemberCode()
                                        ).equals(query)
                        )
                        .findFirst();


        if (exactCodeMatch.isPresent()) {

            selectMember(
                    exactCodeMatch.get()
            );

            return;
        }


        // =====================================================
        // ONLY ONE RESULT
        // =====================================================

        if (results.size() == 1) {

            memberComboBox.setValue(
                    results.get(0)
            );

        } else {

            memberComboBox.setValue(
                    null
            );
        }


        // =====================================================
        // OPEN RESULTS AUTOMATICALLY
        // =====================================================

        if (!results.isEmpty()
                && memberSearchField != null
                && memberSearchField.isFocused()) {

            memberComboBox.show();
        }
    }


    // =========================================================
    // MEMBER MATCH
    // =========================================================

    private boolean memberMatchesSearch(
            Member member,
            String query
    ) {

        if (member == null) {

            return false;
        }


        String name =
                normalizeSearchText(
                        member.getName()
                );


        String code =
                normalizeSearchText(
                        member.getMemberCode()
                );


        return name.contains(query)
                || code.contains(query);
    }


    // =========================================================
    // BARCODE ENTER
    // =========================================================

    private void selectMemberFromBarcode() {

        if (memberSearchField == null) {

            return;
        }


        String barcode =
                normalizeSearchText(
                        memberSearchField.getText()
                );


        if (barcode.isBlank()) {

            return;
        }


        Optional<Member> exactMatch =
                allMembers
                        .stream()
                        .filter(
                                member ->
                                        normalizeSearchText(
                                                member.getMemberCode()
                                        ).equals(barcode)
                        )
                        .findFirst();


        if (exactMatch.isPresent()) {

            selectMember(
                    exactMatch.get()
            );

            return;
        }


        /*
         * If it wasn't an exact barcode but only one
         * member matches the entered name, select them.
         */

        List<Member> matches =
                allMembers
                        .stream()
                        .filter(
                                member ->
                                        memberMatchesSearch(
                                                member,
                                                barcode
                                        )
                        )
                        .toList();


        if (matches.size() == 1) {

            selectMember(
                    matches.get(0)
            );

            return;
        }


        showWarning(
                LanguageManager.isArabic()
                        ? "لم يتم العثور على عضو مطابق للاسم أو الباركود المدخل."
                        : "No member matches the entered name or barcode."
        );


        memberSearchField.requestFocus();

        memberSearchField.selectAll();
    }


    // =========================================================
    // SELECT MEMBER
    // =========================================================

    private void selectMember(
            Member member
    ) {

        if (member == null) {

            return;
        }


        /*
         * Restore full list so the selected member remains
         * available inside the ComboBox.
         */

        memberComboBox
                .getItems()
                .setAll(
                        allMembers
                );


        memberComboBox.setValue(
                member
        );


        if (memberSearchField != null) {

            /*
             * Do not replace the search field with the full
             * display text because it would fire the listener
             * again and make barcode behavior confusing.
             */

            memberSearchField.setText(
                    safe(
                            member.getMemberCode()
                    )
            );
        }


        planComboBox.requestFocus();
    }


    // =========================================================
    // CHECK SELECTED MEMBER
    // =========================================================

    private void checkSelectedMemberMembership(
            Member member
    ) {

        if (member == null) {

            return;
        }


        try {

            Optional<MemberMembership> currentMembership =
                    membershipRepository
                            .findCurrentMembershipByMemberId(
                                    member.getId()
                            );


            if (currentMembership.isPresent()) {

                showActiveMembershipWarning(
                        member,
                        currentMembership.get()
                );
            }


        } catch (SQLException exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر التحقق من اشتراك العضو."
                            : "Could not verify the member's membership.",
                    exception
            );
        }
    }


    // =========================================================
    // NORMALIZE SEARCH
    // =========================================================

    private String normalizeSearchText(
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

                // Arabic normalization
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ى", "ي")
                .replace("ة", "ه")

                // Remove Arabic diacritics
                .replaceAll(
                        "[\\u064B-\\u065F\\u0670]",
                        ""
                );
    }


    // =========================================================
    // DISCOUNT VALIDATION
    // =========================================================

    private void configureDiscountField() {

        UnaryOperator<TextFormatter.Change> filter =
                change -> {

                    String newText =
                            change.getControlNewText();


                    if (newText.matches(
                            "\\d*(\\.\\d{0,2})?"
                    )) {

                        return change;
                    }


                    return null;
                };


        discountField.setTextFormatter(
                new TextFormatter<>(
                        filter
                )
        );
    }


    // =========================================================
    // UPDATE MEMBERSHIP DETAILS
    // =========================================================

    private void updateMembershipDetails() {

        MembershipPlan selectedPlan =
                planComboBox.getValue();


        if (selectedPlan == null) {

            priceField.setText(
                    "0.00"
            );


            endDatePicker.setValue(
                    null
            );


            updateFinalPrice();

            return;
        }


        priceField.setText(
                formatNumber(
                        selectedPlan.getPrice()
                )
        );


        updateEndDate();

        updateFinalPrice();
    }


    // =========================================================
    // CALCULATE END DATE
    // =========================================================

    private void updateEndDate() {

        MembershipPlan selectedPlan =
                planComboBox.getValue();

        LocalDate startDate =
                startDatePicker.getValue();


        if (selectedPlan == null
                || startDate == null) {

            endDatePicker.setValue(
                    null
            );

            return;
        }


        int durationMonths =
                selectedPlan.getDurationMonths();


        if (durationMonths <= 0) {

            endDatePicker.setValue(
                    startDate
            );

            return;
        }


        LocalDate endDate =
                startDate
                        .plusMonths(
                                durationMonths
                        )
                        .minusDays(1);


        endDatePicker.setValue(
                endDate
        );
    }


    // =========================================================
    // FINAL PRICE
    // =========================================================

    private void updateFinalPrice() {

        MembershipPlan selectedPlan =
                planComboBox.getValue();


        double price =
                selectedPlan == null
                        ? 0
                        : selectedPlan.getPrice();


        double discount =
                parseDouble(
                        discountField.getText()
                );


        double finalPrice =
                Math.max(
                        price - discount,
                        0
                );


        finalPriceLabel.setText(
                formatMoney(
                        finalPrice
                )
        );
    }


    // =========================================================
    // SAVE MEMBERSHIP
    // =========================================================

    @FXML
    private void saveMembership() {

        Member selectedMember =
                memberComboBox.getValue();

        MembershipPlan selectedPlan =
                planComboBox.getValue();

        LocalDate startDate =
                startDatePicker.getValue();

        LocalDate endDate =
                endDatePicker.getValue();


        // =====================================================
        // MEMBER
        // =====================================================

        if (selectedMember == null) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "يرجى البحث عن العضو واختياره أولاً."
                            : "Please search for and select a member."
            );


            if (memberSearchField != null) {

                memberSearchField.requestFocus();

            } else {

                memberComboBox.requestFocus();
            }


            return;
        }


        // =====================================================
        // PLAN
        // =====================================================

        if (selectedPlan == null) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "يرجى اختيار خطة الاشتراك."
                            : "Please select a membership plan."
            );


            planComboBox.requestFocus();

            return;
        }


        // =====================================================
        // PLAN SESSION LIMIT
        // =====================================================

        if (selectedPlan.getMaxSessions() <= 0) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "عدد مرات التمرين غير مضبوط لهذه الخطة. يرجى تعديل خطة الاشتراك أولاً."
                            : "The session limit is not configured for this plan. Please update the membership plan first."
            );

            planComboBox.requestFocus();

            return;
        }


        // =====================================================
        // START DATE
        // =====================================================

        if (startDate == null) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "يرجى تحديد تاريخ بداية الاشتراك."
                            : "Please select the membership start date."
            );


            startDatePicker.requestFocus();

            return;
        }


        // =====================================================
        // END DATE
        // =====================================================

        if (endDate == null) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "تعذر حساب تاريخ انتهاء الاشتراك."
                            : "Could not calculate membership end date."
            );

            return;
        }


        // =====================================================
        // ACTIVE MEMBERSHIP CHECK
        // =====================================================

        try {

            Optional<MemberMembership> currentMembership =
                    membershipRepository
                            .findCurrentMembershipByMemberId(
                                    selectedMember.getId()
                            );


            if (currentMembership.isPresent()) {

                showActiveMembershipWarning(
                        selectedMember,
                        currentMembership.get()
                );

                return;
            }


            // =================================================
            // PERIOD OVERLAP CHECK
            // =================================================

            Optional<MemberMembership> overlapping =
                    membershipRepository
                            .findOverlappingMembership(
                                    selectedMember.getId(),
                                    startDate,
                                    endDate
                            );


            if (overlapping.isPresent()) {

                showOverlappingMembershipWarning(
                        selectedMember,
                        overlapping.get()
                );

                return;
            }


        } catch (SQLException exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر التحقق من حالة اشتراك العضو."
                            : "Could not verify the member's membership status.",
                    exception
            );


            return;
        }


        // =====================================================
        // PRICE / DISCOUNT
        // =====================================================

        double price =
                selectedPlan.getPrice();


        double discount =
                parseDouble(
                        discountField.getText()
                );


        if (discount < 0) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "قيمة الخصم لا يمكن أن تكون سالبة."
                            : "Discount cannot be negative."
            );


            discountField.requestFocus();

            return;
        }


        if (discount > price) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "قيمة الخصم لا يمكن أن تكون أكبر من سعر الاشتراك."
                            : "Discount cannot be greater than the membership price."
            );


            discountField.requestFocus();

            return;
        }


        double finalPrice =
                Math.max(
                        price - discount,
                        0
                );


        // =====================================================
        // CREATE MEMBERSHIP
        // =====================================================

        MemberMembership membership =
                new MemberMembership();


        membership.setMemberId(
                selectedMember.getId()
        );


        membership.setMembershipPlanId(
                selectedPlan.getId()
        );


        membership.setStartDate(
                startDate
        );


        membership.setEndDate(
                endDate
        );


        /*
         * Snapshot the plan session limit into this membership.
         * Future changes to the plan will not change existing
         * memberships.
         */
        membership.setAllowedSessions(
                selectedPlan.getMaxSessions()
        );


        membership.setPrice(
                price
        );


        membership.setDiscount(
                discount
        );


        membership.setFinalPrice(
                finalPrice
        );


        membership.setStatus(
                "Active"
        );


        membership.setNotes(
                safe(
                        notesArea.getText()
                )
        );


        // =====================================================
        // SAVE
        // =====================================================

        try {

            saveButton.setDisable(
                    true
            );


            membershipRepository.insert(
                    membership
            );


            showSuccess(
                    LanguageManager.isArabic()
                            ? "تم حفظ اشتراك العضو بنجاح."
                            : "Membership saved successfully."
            );


            closeWindow();


        } catch (SQLException exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر حفظ الاشتراك. تحقق من عدم وجود اشتراك آخر في نفس الفترة."
                            : "Could not save membership. Check for another membership in the same period.",
                    exception
            );


            saveButton.setDisable(
                    false
            );


        } catch (Exception exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "حدث خطأ غير متوقع أثناء حفظ الاشتراك."
                            : "An unexpected error occurred while saving the membership.",
                    exception
            );


            saveButton.setDisable(
                    false
            );
        }
    }


    // =========================================================
    // ACTIVE MEMBERSHIP WARNING
    // =========================================================

    private void showActiveMembershipWarning(
            Member member,
            MemberMembership membership
    ) {

        String memberName =
                safe(
                        member.getName()
                );


        String memberCode =
                safe(
                        member.getMemberCode()
                );


        String planName =
                membership.getPlanDisplayName(
                        LanguageManager.isArabic()
                );


        String startDate =
                formatDate(
                        membership.getStartDate()
                );


        String endDate =
                formatDate(
                        membership.getEndDate()
                );


        long remainingDays =
                membership.getRemainingDays();


        String message;


        if (LanguageManager.isArabic()) {

            message =
                    "العضو لديه اشتراك فعال حاليًا."
                    + "\n\n"
                    + "العضو: "
                    + memberName
                    + "\n"
                    + "الكود / الباركود: "
                    + memberCode
                    + "\n"
                    + "الخطة: "
                    + planName
                    + "\n"
                    + "تاريخ البداية: "
                    + startDate
                    + "\n"
                    + "تاريخ الانتهاء: "
                    + endDate
                    + "\n"
                    + "الأيام المتبقية: "
                    + remainingDays
                    + "\n\n"
                    + "لا يمكن إنشاء اشتراك جديد قبل انتهاء الاشتراك الحالي.";

        } else {

            message =
                    "This member currently has an active membership."
                    + "\n\n"
                    + "Member: "
                    + memberName
                    + "\n"
                    + "Code / Barcode: "
                    + memberCode
                    + "\n"
                    + "Plan: "
                    + planName
                    + "\n"
                    + "Start date: "
                    + startDate
                    + "\n"
                    + "End date: "
                    + endDate
                    + "\n"
                    + "Remaining days: "
                    + remainingDays
                    + "\n\n"
                    + "A new membership cannot be created until the current membership expires.";
        }


        showWarning(
                message
        );
    }


    // =========================================================
    // OVERLAP WARNING
    // =========================================================

    private void showOverlappingMembershipWarning(
            Member member,
            MemberMembership membership
    ) {

        String message;


        if (LanguageManager.isArabic()) {

            message =
                    "الفترة المحددة تتداخل مع اشتراك سابق للعضو."
                    + "\n\n"
                    + "العضو: "
                    + safe(member.getName())
                    + "\n"
                    + "من: "
                    + formatDate(
                            membership.getStartDate()
                    )
                    + "\n"
                    + "إلى: "
                    + formatDate(
                            membership.getEndDate()
                    )
                    + "\n\n"
                    + "اختر تاريخ بداية بعد انتهاء الاشتراك السابق.";

        } else {

            message =
                    "The selected period overlaps an existing membership."
                    + "\n\n"
                    + "Member: "
                    + safe(member.getName())
                    + "\n"
                    + "From: "
                    + formatDate(
                            membership.getStartDate()
                    )
                    + "\n"
                    + "To: "
                    + formatDate(
                            membership.getEndDate()
                    )
                    + "\n\n"
                    + "Choose a start date after the previous membership ends.";
        }


        showWarning(
                message
        );
    }


    // =========================================================
    // CANCEL
    // =========================================================

    @FXML
    private void cancel() {

        closeWindow();
    }


    // =========================================================
    // CLOSE WINDOW
    // =========================================================

    private void closeWindow() {

        if (saveButton == null
                || saveButton.getScene() == null
                || saveButton
                        .getScene()
                        .getWindow() == null) {

            return;
        }


        Stage stage =
                (Stage) saveButton
                        .getScene()
                        .getWindow();


        stage.close();
    }


    // =========================================================
    // PARSE DOUBLE
    // =========================================================

    private double parseDouble(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return 0;
        }


        try {

            return Double.parseDouble(
                    value.trim()
            );


        } catch (NumberFormatException exception) {

            return 0;
        }
    }


    // =========================================================
    // FORMAT NUMBER
    // =========================================================

    private String formatNumber(
            double value
    ) {

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }


    // =========================================================
    // FORMAT MONEY
    // =========================================================

    private String formatMoney(
            double value
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


        String formatted =
                numberFormat.format(
                        value
                );


        return LanguageManager.isArabic()
                ? formatted + " ج.م"
                : formatted + " EGP";
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
                DISPLAY_DATE_FORMAT
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
                LanguageManager.isArabic()
                        ? "تنبيه الاشتراك"
                        : "Membership Warning"
        );


        alert.setContentText(
                message
        );


        DialogStyler.style(alert);


        alert.showAndWait();
    }


    // =========================================================
    // SUCCESS
    // =========================================================

    private void showSuccess(
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
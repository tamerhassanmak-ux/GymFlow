package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.ui.dialog.DialogStyler;
import com.mycompany.lazar_app.ui.WindowManager;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.Payment;
import com.mycompany.lazar_app.repository.PaymentRepository;
import com.mycompany.lazar_app.repository.PaymentRepository.MemberOption;
import com.mycompany.lazar_app.repository.PaymentRepository.MembershipOption;
import com.mycompany.lazar_app.repository.PaymentRepository.PaymentInstallment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class AddPaymentController {

    // =========================================================
    // FXML CONTROLS
    // =========================================================

    @FXML
    private ScrollPane paymentFormScrollPane;

    @FXML
    private ComboBox<MemberOption> memberComboBox;

    @FXML
    private ComboBox<MembershipOption> membershipComboBox;

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<String> paymentMethodComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private DatePicker paymentDatePicker;

    @FXML
    private TextField referenceField;

    @FXML
    private TextArea notesArea;

    @FXML
    private VBox financialSummaryBox;

    @FXML
    private Label financialSummaryTitleLabel;

    @FXML
    private Label membershipAmountCaptionLabel;

    @FXML
    private Label membershipAmountValueLabel;

    @FXML
    private Label paidAmountCaptionLabel;

    @FXML
    private Label paidAmountValueLabel;

    @FXML
    private Label balanceCaptionLabel;

    @FXML
    private Label balanceValueLabel;

    @FXML
    private Label remainingAfterPaymentLabel;

    @FXML
    private VBox scheduleBox;

    @FXML
    private Label scheduleTitleLabel;

    @FXML
    private Label scheduleHintLabel;

    @FXML
    private Label installmentsCaptionLabel;

    @FXML
    private ComboBox<Integer> installmentsComboBox;

    @FXML
    private Label firstDueDateCaptionLabel;

    @FXML
    private DatePicker firstInstallmentDatePicker;

    @FXML
    private Label installmentAmountCaptionLabel;

    @FXML
    private Label installmentAmountValueLabel;


    // =========================================================
    // REPOSITORY
    // =========================================================

    private final PaymentRepository paymentRepository =
            new PaymentRepository();


    // =========================================================
    // MEMBER DATA
    // =========================================================

    private final ObservableList<MemberOption> allMembers =
            FXCollections.observableArrayList();


    private final ObservableList<MemberOption> filteredMembers =
            FXCollections.observableArrayList();


    // =========================================================
    // SMART SEARCH
    // =========================================================

    private final PauseTransition memberSearchDelay =
            new PauseTransition(
                    Duration.millis(200)
            );


    private boolean internalMemberChange = false;


    // =========================================================
    // PAYMENT STATE
    // =========================================================

    private Payment payment;

    private Payment editingPayment;

    private boolean saved;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        configurePaymentMethods();

        configureStatuses();

        configureInstallmentControls();

        configureFinancialPreviewLabels();

        configureMemberComboBox();

        configureMembershipComboBox();

        paymentDatePicker.setValue(
                LocalDate.now()
        );


        membershipComboBox.setDisable(
                true
        );


        loadMembers();

        configureFinancialPreviewListeners();

        updateFinancialPreview();


        /*
         * Always open Add/Edit Payment from the top.
         * This prevents JavaFX ScrollPane from jumping to a lower
         * focused control when the window is first shown.
         */
        Platform.runLater(
                () -> {

                    if (paymentFormScrollPane != null) {
                        paymentFormScrollPane.setVvalue(0.0);
                    }

                    if (memberComboBox != null) {
                        memberComboBox.requestFocus();
                    }
                }
        );
    }


    // =========================================================
    // CONFIGURE SMART MEMBER COMBO BOX
    // =========================================================

    private void configureMemberComboBox() {

        // Allow typing inside ComboBox
        memberComboBox.setEditable(true);


        // -----------------------------------------------------
        // DISPLAY MEMBER IN LIST
        // -----------------------------------------------------

        memberComboBox.setCellFactory(
                listView ->
                        new ListCell<>() {

                            @Override
                            protected void updateItem(
                                    MemberOption member,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        member,
                                        empty
                                );


                                if (empty
                                        || member == null) {

                                    setText(null);

                                } else {

                                    setText(
                                            buildMemberDisplay(
                                                    member
                                            )
                                    );
                                }
                            }
                        }
        );


        // -----------------------------------------------------
        // DISPLAY SELECTED MEMBER
        // -----------------------------------------------------

        memberComboBox.setConverter(
                new StringConverter<>() {

                    @Override
                    public String toString(
                            MemberOption member
                    ) {

                        if (member == null) {

                            return "";
                        }


                        return buildMemberDisplay(
                                member
                        );
                    }


                    @Override
                    public MemberOption fromString(
                            String text
                    ) {

                        if (text == null
                                || text.isBlank()) {

                            return null;
                        }


                        String normalized =
                                normalizeText(text);


                        for (MemberOption member : allMembers) {

                            if (normalizeText(
                                    buildMemberDisplay(member)
                            ).equals(normalized)) {

                                return member;
                            }
                        }


                        return null;
                    }
                }
        );


        // -----------------------------------------------------
        // SEARCH DELAY
        // -----------------------------------------------------

        memberSearchDelay.setOnFinished(
                event -> {

                    if (internalMemberChange) {

                        return;
                    }


                    String text =
                            memberComboBox
                                    .getEditor()
                                    .getText();


                    filterMembers(
                            text
                    );
                }
        );


        // -----------------------------------------------------
        // LIVE SEARCH
        // -----------------------------------------------------

        memberComboBox
                .getEditor()
                .textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (internalMemberChange) {

                                return;
                            }


                            memberSearchDelay.stop();

                            memberSearchDelay.playFromStart();
                        }
                );


        // -----------------------------------------------------
        // MEMBER SELECTION
        // -----------------------------------------------------

        memberComboBox
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldMember,
                                newMember
                        ) -> {

                            if (internalMemberChange) {

                                return;
                            }


                            if (newMember != null) {

                                memberSearchDelay.stop();


                                internalMemberChange = true;

                                try {

                                    memberComboBox
                                            .getEditor()
                                            .setText(
                                                    buildMemberDisplay(
                                                            newMember
                                                    )
                                            );

                                } finally {

                                    internalMemberChange =
                                            false;
                                }


                                loadMembershipsForSelectedMember(
                                        null
                                );

                            } else {

                                membershipComboBox
                                        .getItems()
                                        .clear();


                                membershipComboBox
                                        .setDisable(
                                                true
                                        );
                            }
                        }
                );


        // -----------------------------------------------------
        // ENTER KEY
        // -----------------------------------------------------

        memberComboBox
                .getEditor()
                .setOnAction(
                        event -> {

                            memberSearchDelay.stop();


                            MemberOption selected =
                                    findBestMemberMatch(
                                            memberComboBox
                                                    .getEditor()
                                                    .getText()
                                    );


                            if (selected != null) {

                                selectMember(
                                        selected
                                );
                            }
                        }
                );
    }


    // =========================================================
    // CONFIGURE MEMBERSHIP COMBO BOX
    // =========================================================

    private void configureMembershipComboBox() {

        membershipComboBox.setCellFactory(
                listView ->
                        new ListCell<>() {

                            @Override
                            protected void updateItem(
                                    MembershipOption membership,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        membership,
                                        empty
                                );


                                if (empty
                                        || membership == null) {

                                    setText(null);

                                } else {

                                    setText(
                                            membership
                                                    .getDisplayName(
                                                            LanguageManager
                                                                    .isArabic()
                                                    )
                                    );
                                }
                            }
                        }
        );


        membershipComboBox.setButtonCell(
                new ListCell<>() {

                    @Override
                    protected void updateItem(
                            MembershipOption membership,
                            boolean empty
                    ) {

                        super.updateItem(
                                membership,
                                empty
                        );


                        if (empty
                                || membership == null) {

                            setText(null);

                        } else {

                            setText(
                                    membership
                                            .getDisplayName(
                                                    LanguageManager
                                                            .isArabic()
                                            )
                            );
                        }
                    }
                }
        );


        /*
         * New payments are linked to a concrete member membership.
         * Prefill the outstanding balance of that membership rather
         * than the current MembershipPlan catalogue price.
         */
        membershipComboBox
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldMembership,
                                newMembership
                        ) -> {
                            applyMembershipBalanceToAmount(
                                    newMembership
                            );

                            loadScheduleDefaults(
                                    newMembership
                            );

                            updateFinancialPreview();
                        }
                );
    }


    // =========================================================
    // LOAD MEMBERS
    // =========================================================

    private void loadMembers() {

        try {

            List<MemberOption> members =
                    paymentRepository
                            .findMemberOptions();


            allMembers.setAll(
                    members
            );


            filteredMembers.setAll(
                    members
            );


            memberComboBox.setItems(
                    filteredMembers
            );


        } catch (Exception exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تحميل الأعضاء."
                            : "Unable to load members.",
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // SMART MEMBER FILTER
    // =========================================================

    private void filterMembers(
            String searchText
    ) {

        String query =
                normalizeText(
                        searchText
                );


        // -----------------------------------------------------
        // EMPTY SEARCH
        // -----------------------------------------------------

        if (query.isBlank()) {

            filteredMembers.setAll(
                    allMembers
            );


            if (!memberComboBox.isShowing()) {

                memberComboBox.show();
            }


            return;
        }


        // -----------------------------------------------------
        // MULTI-WORD SEARCH
        // -----------------------------------------------------

        String[] words =
                query.split("\\s+");


        List<MemberOption> matches =
                new ArrayList<>();


        for (MemberOption member : allMembers) {

            String searchable =
                    normalizeText(
                            buildMemberSearchText(
                                    member
                            )
                    );


            boolean matchesAll =
                    true;


            for (String word : words) {

                if (word.isBlank()) {

                    continue;
                }


                if (!searchable.contains(word)) {

                    matchesAll =
                            false;

                    break;
                }
            }


            if (matchesAll) {

                matches.add(
                        member
                );
            }
        }


        filteredMembers.setAll(
                matches
        );


        /*
         * Preserve what the user typed because changing
         * ComboBox items can otherwise affect the editor.
         */

        internalMemberChange =
                true;


        try {

            memberComboBox
                    .getEditor()
                    .setText(
                            searchText
                    );


            memberComboBox
                    .getEditor()
                    .positionCaret(
                            searchText == null
                                    ? 0
                                    : searchText.length()
                    );

        } finally {

            internalMemberChange =
                    false;
        }


        // -----------------------------------------------------
        // SHOW RESULTS
        // -----------------------------------------------------

        if (!matches.isEmpty()) {

            if (!memberComboBox.isShowing()) {

                memberComboBox.show();
            }

        } else {

            memberComboBox.hide();
        }
    }


    // =========================================================
    // MEMBER SEARCH TEXT
    // =========================================================

    private String buildMemberSearchText(
            MemberOption member
    ) {

        if (member == null) {

            return "";
        }


        return safe(
                member.getCode()
        )
                + " "
                + safe(
                        member.getName()
                )
                + " "
                + member.getId();
    }


    // =========================================================
    // MEMBER DISPLAY
    // =========================================================

    private String buildMemberDisplay(
            MemberOption member
    ) {

        if (member == null) {

            return "";
        }


        String code =
                safe(
                        member.getCode()
                );


        String name =
                safe(
                        member.getName()
                );


        if (code.isBlank()) {

            return name;
        }


        if (name.isBlank()) {

            return code;
        }


        return code
                + " - "
                + name;
    }


    // =========================================================
    // FIND BEST MEMBER MATCH
    // =========================================================

    private MemberOption findBestMemberMatch(
            String text
    ) {

        if (text == null
                || text.isBlank()) {

            return null;
        }


        String query =
                normalizeText(
                        text
                );


        // -----------------------------------------------------
        // EXACT MATCH
        // -----------------------------------------------------

        for (MemberOption member : allMembers) {

            String code =
                    normalizeText(
                            member.getCode()
                    );


            String name =
                    normalizeText(
                            member.getName()
                    );


            String display =
                    normalizeText(
                            buildMemberDisplay(
                                    member
                            )
                    );


            if (query.equals(code)
                    || query.equals(name)
                    || query.equals(display)) {

                return member;
            }
        }


        // -----------------------------------------------------
        // IF ONLY ONE FILTERED RESULT
        // -----------------------------------------------------

        if (filteredMembers.size() == 1) {

            return filteredMembers.get(0);
        }


        return null;
    }


    // =========================================================
    // SELECT MEMBER
    // =========================================================

    private void selectMember(
            MemberOption member
    ) {

        if (member == null) {

            return;
        }


        memberSearchDelay.stop();


        internalMemberChange =
                true;


        try {

            /*
             * Restore complete list so selected item
             * always belongs to ComboBox items.
             */

            filteredMembers.setAll(
                    allMembers
            );


            memberComboBox.setValue(
                    member
            );


            memberComboBox
                    .getEditor()
                    .setText(
                            buildMemberDisplay(
                                    member
                            )
                    );


            memberComboBox.hide();


        } finally {

            internalMemberChange =
                    false;
        }


        loadMembershipsForSelectedMember(
                null
        );
    }


    // =========================================================
    // APPLY OUTSTANDING MEMBERSHIP BALANCE
    // =========================================================

    private void applyMembershipBalanceToAmount(
            MembershipOption membership
    ) {

        if (membership == null) {
            amountField.clear();
            return;
        }

        /*
         * In edit mode the existing payment amount remains authoritative.
         * For a new payment we prefill the currently available balance, but
         * the user can enter any smaller positive value to make a partial
         * payment.
         */
        if (editingPayment != null) {
            return;
        }

        BigDecimal available = getAvailableBalanceForForm(membership);

        amountField.setText(
                moneyNumber(available)
        );
    }


    // =========================================================
    // PARTIAL PAYMENT / INSTALLMENT CONFIGURATION
    // =========================================================

    private void configureInstallmentControls() {

        installmentsComboBox.getItems().clear();

        for (int i = 1; i <= 12; i++) {
            installmentsComboBox.getItems().add(i);
        }

        installmentsComboBox.setValue(2);
        firstInstallmentDatePicker.setValue(LocalDate.now().plusMonths(1));
    }


    private void configureFinancialPreviewLabels() {

        boolean ar = LanguageManager.isArabic();

        financialSummaryTitleLabel.setText(
                ar ? "ملخص الاشتراك المالي" : "Membership financial summary"
        );

        membershipAmountCaptionLabel.setText(
                ar ? "قيمة الاشتراك" : "Subscription amount"
        );

        paidAmountCaptionLabel.setText(
                ar ? "المدفوع فعلياً" : "Actually paid"
        );

        balanceCaptionLabel.setText(
                ar ? "المتبقي قبل الدفعة" : "Remaining before payment"
        );

        scheduleTitleLabel.setText(
                ar ? "جدولة باقي الاشتراك" : "Remaining balance schedule"
        );

        scheduleHintLabel.setText(
                ar
                        ? "سيتم تقسيم المتبقي بعد هذه الدفعة إلى أقساط شهرية متساوية تقريباً."
                        : "The remaining balance after this payment will be split into approximately equal monthly installments."
        );

        installmentsCaptionLabel.setText(
                ar ? "عدد الأقساط" : "Number of installments"
        );

        firstDueDateCaptionLabel.setText(
                ar ? "تاريخ أول استحقاق" : "First due date"
        );

        installmentAmountCaptionLabel.setText(
                ar ? "قيمة القسط التقريبية" : "Approx. installment"
        );
    }


    private void configureFinancialPreviewListeners() {

        amountField.textProperty().addListener(
                (observable, oldValue, newValue) -> updateFinancialPreview()
        );

        statusComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> updateFinancialPreview()
        );

        paymentDatePicker.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null
                            && firstInstallmentDatePicker.getValue() == null) {
                        firstInstallmentDatePicker.setValue(
                                newValue.plusMonths(1)
                        );
                    }
                    updateFinancialPreview();
                }
        );

        installmentsComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> updateFinancialPreview()
        );
    }


    private void loadScheduleDefaults(
            MembershipOption membership
    ) {

        if (membership == null) {
            return;
        }

        try {
            List<PaymentInstallment> existing =
                    paymentRepository.findPaymentScheduleByMembershipId(
                            membership.getId()
                    );

            if (!existing.isEmpty()) {
                int count = Math.min(12, Math.max(1, existing.size()));
                installmentsComboBox.setValue(count);

                LocalDate firstDue = existing.get(0).getDueDate();

                if (firstDue != null) {
                    firstInstallmentDatePicker.setValue(firstDue);
                }

            } else {
                installmentsComboBox.setValue(2);

                LocalDate paymentDate = paymentDatePicker.getValue();
                firstInstallmentDatePicker.setValue(
                        (paymentDate == null ? LocalDate.now() : paymentDate)
                                .plusMonths(1)
                );
            }

        } catch (Exception exception) {
            /*
             * The schedule table is self-created by PaymentRepository.
             * A schedule loading problem must not block entering a payment;
             * the save flow will still report any persistence error.
             */
            installmentsComboBox.setValue(2);
        }
    }


    private void updateFinancialPreview() {

        MembershipOption membership = membershipComboBox.getValue();

        boolean hasMembership = membership != null;

        financialSummaryBox.setVisible(hasMembership);
        financialSummaryBox.setManaged(hasMembership);

        if (!hasMembership) {
            scheduleBox.setVisible(false);
            scheduleBox.setManaged(false);
            remainingAfterPaymentLabel.setText("");
            return;
        }

        BigDecimal membershipAmount = membership.getMembershipAmount();
        BigDecimal paidBefore = getPaidBeforeCurrentFormPayment(membership);
        BigDecimal availableBalance = getAvailableBalanceForForm(membership);
        BigDecimal enteredAmount = parseAmountSilently();

        boolean paymentCountsAsPaid = isSelectedStatusPaid();

        BigDecimal paidAfter = paidBefore;
        BigDecimal remainingAfter = availableBalance;

        if (paymentCountsAsPaid
                && enteredAmount != null
                && enteredAmount.compareTo(BigDecimal.ZERO) > 0) {

            paidAfter = paidBefore.add(enteredAmount);
            remainingAfter = availableBalance
                    .subtract(enteredAmount)
                    .max(BigDecimal.ZERO);
        }

        membershipAmountValueLabel.setText(formatMoney(membershipAmount));
        paidAmountValueLabel.setText(formatMoney(paidBefore));
        balanceValueLabel.setText(formatMoney(availableBalance));

        boolean ar = LanguageManager.isArabic();

        remainingAfterPaymentLabel.setText(
                ar
                        ? "بعد حفظ هذه الدفعة: المدفوع = "
                            + formatMoney(paidAfter)
                            + " | المتبقي = "
                            + formatMoney(remainingAfter)
                        : "After this payment: paid = "
                            + formatMoney(paidAfter)
                            + " | remaining = "
                            + formatMoney(remainingAfter)
        );

        boolean partialPaidPayment =
                paymentCountsAsPaid
                && enteredAmount != null
                && enteredAmount.compareTo(BigDecimal.ZERO) > 0
                && enteredAmount.compareTo(availableBalance) < 0
                && remainingAfter.compareTo(BigDecimal.ZERO) > 0;

        scheduleBox.setVisible(partialPaidPayment);
        scheduleBox.setManaged(partialPaidPayment);

        if (partialPaidPayment) {
            Integer count = installmentsComboBox.getValue();

            BigDecimal installmentAmount =
                    count == null || count <= 0
                            ? remainingAfter
                            : remainingAfter.divide(
                                    BigDecimal.valueOf(count),
                                    2,
                                    RoundingMode.HALF_UP
                            );

            installmentAmountValueLabel.setText(
                    formatMoney(installmentAmount)
            );
        } else {
            installmentAmountValueLabel.setText(formatMoney(BigDecimal.ZERO));
        }
    }


    private BigDecimal getAvailableBalanceForForm(
            MembershipOption membership
    ) {

        if (membership == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal available = membership.getBalanceDue();

        if (editingPayment != null
                && editingPayment.getMembershipId() != null
                && editingPayment.getMembershipId() == membership.getId()
                && isPaidStatus(editingPayment.getStatus())
                && editingPayment.getAmount() != null) {

            available = available.add(editingPayment.getAmount());
        }

        return available.max(BigDecimal.ZERO);
    }


    private BigDecimal getPaidBeforeCurrentFormPayment(
            MembershipOption membership
    ) {

        if (membership == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal paid = membership.getPaidAmount();

        if (editingPayment != null
                && editingPayment.getMembershipId() != null
                && editingPayment.getMembershipId() == membership.getId()
                && isPaidStatus(editingPayment.getStatus())
                && editingPayment.getAmount() != null) {

            paid = paid.subtract(editingPayment.getAmount());
        }

        return paid.max(BigDecimal.ZERO);
    }


    private BigDecimal parseAmountSilently() {

        if (amountField.getText() == null
                || amountField.getText().isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(amountField.getText().trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }


    private boolean isSelectedStatusPaid() {
        return "PAID".equalsIgnoreCase(
                normalizeStatus(statusComboBox.getValue())
        );
    }


    private boolean isPaidStatus(
            String status
    ) {
        return status != null && status.equalsIgnoreCase("PAID");
    }


    private String formatMoney(
            BigDecimal amount
    ) {
        return moneyNumber(amount) + " EGP";
    }


    private String moneyNumber(
            BigDecimal amount
    ) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;

        return safeAmount
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }


    public boolean shouldCreateInstallmentSchedule() {

        MembershipOption membership = membershipComboBox.getValue();
        BigDecimal amount = parseAmountSilently();

        if (membership == null
                || amount == null
                || !isSelectedStatusPaid()) {
            return false;
        }

        return amount.compareTo(BigDecimal.ZERO) > 0
                && amount.compareTo(getAvailableBalanceForForm(membership)) < 0;
    }


    public int getScheduleInstallmentCount() {
        Integer value = installmentsComboBox.getValue();
        return value == null ? 1 : Math.max(1, value);
    }


    public LocalDate getScheduleFirstDueDate() {
        return firstInstallmentDatePicker.getValue();
    }


    // =========================================================
    // LOAD MEMBERSHIPS
    // =========================================================

    private void loadMembershipsForSelectedMember(
            Long preferredMembershipId
    ) {

        membershipComboBox
                .getItems()
                .clear();


        MemberOption member =
                memberComboBox.getValue();


        if (member == null) {

            membershipComboBox.setDisable(
                    true
            );

            return;
        }


        try {

            List<MembershipOption> memberships =
                    paymentRepository
                            .findMembershipOptionsByMemberId(
                                    member.getId()
                            );


            membershipComboBox
                    .getItems()
                    .setAll(
                            memberships
                    );


            membershipComboBox.setDisable(
                    memberships.isEmpty()
            );


            // -------------------------------------------------
            // EDIT MODE - RESTORE MEMBERSHIP
            // -------------------------------------------------

            if (preferredMembershipId != null) {

                memberships
                        .stream()

                        .filter(
                                item ->
                                        item.getId()
                                                == preferredMembershipId
                        )

                        .findFirst()

                        .ifPresent(
                                membershipComboBox::setValue
                        );


                return;
            }


            // -------------------------------------------------
            // NEW PAYMENT - PREFER ACTIVE MEMBERSHIP
            // -------------------------------------------------

            MembershipOption activeMembership =
                    memberships
                            .stream()

                            .filter(
                                    this::isActiveMembership
                            )

                            .findFirst()

                            .orElse(null);


            if (activeMembership != null) {

                membershipComboBox.setValue(
                        activeMembership
                );

            } else if (!memberships.isEmpty()) {

                membershipComboBox
                        .getSelectionModel()
                        .selectFirst();
            }


        } catch (Exception exception) {

            exception.printStackTrace();


            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تحميل اشتراكات العضو."
                            : "Unable to load member memberships.",
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // ACTIVE MEMBERSHIP
    // =========================================================

    private boolean isActiveMembership(
            MembershipOption membership
    ) {

        if (membership == null
                || membership.getStatus() == null) {

            return false;
        }


        String status =
                membership
                        .getStatus()
                        .trim();


        return status.equalsIgnoreCase(
                "ACTIVE"
        )
                || status.equalsIgnoreCase(
                        "Active"
                )
                || status.equals(
                        "نشط"
                );
    }


    // =========================================================
    // EDIT PAYMENT
    // =========================================================

    public void setPaymentForEdit(
            Payment payment
    ) {

        if (payment == null) {

            return;
        }


        this.editingPayment =
                payment;


        // -----------------------------------------------------
        // MEMBER
        // -----------------------------------------------------

        MemberOption selectedMember =
                allMembers
                        .stream()

                        .filter(
                                item ->
                                        item.getId()
                                                == payment.getMemberId()
                        )

                        .findFirst()

                        .orElse(null);


        if (selectedMember != null) {

            selectMember(
                    selectedMember
            );


            loadMembershipsForSelectedMember(
                    payment.getMembershipId()
            );
        }


        // -----------------------------------------------------
        // AMOUNT
        // -----------------------------------------------------

        amountField.setText(
                payment.getAmount() == null
                        ? ""
                        : payment
                                .getAmount()
                                .toPlainString()
        );


        // -----------------------------------------------------
        // PAYMENT METHOD
        // -----------------------------------------------------

        paymentMethodComboBox.setValue(
                displayPaymentMethod(
                        payment.getPaymentMethod()
                )
        );


        // -----------------------------------------------------
        // STATUS
        // -----------------------------------------------------

        statusComboBox.setValue(
                displayStatus(
                        payment.getStatus()
                )
        );


        // -----------------------------------------------------
        // DATE
        // -----------------------------------------------------

        if (payment.getPaymentDate()
                != null) {

            paymentDatePicker.setValue(
                    payment
                            .getPaymentDate()
                            .toLocalDate()
            );
        }


        // -----------------------------------------------------
        // REFERENCE
        // -----------------------------------------------------

        referenceField.setText(
                payment.getReferenceNumber() == null
                        ? ""
                        : payment
                                .getReferenceNumber()
        );


        // -----------------------------------------------------
        // NOTES
        // -----------------------------------------------------

        notesArea.setText(
                payment.getNotes() == null
                        ? ""
                        : payment.getNotes()
        );

        updateFinancialPreview();
    }


    // =========================================================
    // PAYMENT METHODS
    // =========================================================

    private void configurePaymentMethods() {

        paymentMethodComboBox
                .getItems()
                .clear();


        if (LanguageManager.isArabic()) {

            paymentMethodComboBox
                    .getItems()
                    .addAll(
                            "نقدي",
                            "بطاقة",
                            "تحويل بنكي"
                    );

        } else {

            paymentMethodComboBox
                    .getItems()
                    .addAll(
                            "Cash",
                            "Card",
                            "Bank Transfer"
                    );
        }


        paymentMethodComboBox
                .getSelectionModel()
                .selectFirst();
    }


    // =========================================================
    // STATUSES
    // =========================================================

    private void configureStatuses() {

        statusComboBox
                .getItems()
                .clear();


        if (LanguageManager.isArabic()) {

            statusComboBox
                    .getItems()
                    .addAll(
                            "مدفوع",
                            "معلق",
                            "ملغي"
                    );

        } else {

            statusComboBox
                    .getItems()
                    .addAll(
                            "Paid",
                            "Pending",
                            "Cancelled"
                    );
        }


        statusComboBox
                .getSelectionModel()
                .selectFirst();
    }


    // =========================================================
    // SAVE PAYMENT
    // =========================================================

    @FXML
    private void savePayment() {

        /*
         * Important:
         * Editable ComboBox may contain typed text
         * without an actual selected MemberOption.
         */

        if (memberComboBox.getValue() == null) {

            MemberOption bestMatch =
                    findBestMemberMatch(
                            memberComboBox
                                    .getEditor()
                                    .getText()
                    );


            if (bestMatch != null) {

                selectMember(
                        bestMatch
                );
            }
        }


        if (!validateForm()) {

            return;
        }


        MemberOption selectedMember =
                memberComboBox.getValue();


        MembershipOption selectedMembership =
                membershipComboBox.getValue();


        BigDecimal amount =
                new BigDecimal(
                        amountField
                                .getText()
                                .trim()
                );


        LocalDate selectedDate =
                paymentDatePicker
                        .getValue();


        LocalTime time =
                editingPayment != null
                        && editingPayment
                                .getPaymentDate()
                                != null

                        ? editingPayment
                                .getPaymentDate()
                                .toLocalTime()

                        : LocalTime.now();


        LocalDateTime paymentDate =
                LocalDateTime.of(
                        selectedDate,
                        time
                );


        Payment candidate =
                editingPayment == null
                        ? new Payment()
                        : editingPayment;


        candidate.setMemberId(
                selectedMember.getId()
        );


        candidate.setMembershipId(
                selectedMembership == null
                        ? null
                        : selectedMembership
                                .getId()
        );


        candidate.setAmount(
                amount
        );


        candidate.setPaymentMethod(
                normalizePaymentMethod(
                        paymentMethodComboBox
                                .getValue()
                )
        );


        candidate.setStatus(
                normalizeStatus(
                        statusComboBox
                                .getValue()
                )
        );


        candidate.setPaymentDate(
                paymentDate
        );


        candidate.setReferenceNumber(
                clean(
                        referenceField
                                .getText()
                )
        );


        candidate.setNotes(
                clean(
                        notesArea
                                .getText()
                )
        );


        this.payment =
                candidate;


        this.saved =
                true;


        closeWindow();
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private boolean validateForm() {

        if (memberComboBox
                .getValue() == null) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "يرجى البحث عن عضو واختياره من القائمة."
                            : "Please search for and select a member."
            );


            memberComboBox.requestFocus();

            return false;
        }


        if (amountField.getText() == null
                || amountField
                        .getText()
                        .isBlank()) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "يرجى إدخال المبلغ."
                            : "Please enter the payment amount."
            );


            amountField.requestFocus();

            return false;
        }


        try {

            BigDecimal amount =
                    new BigDecimal(
                            amountField
                                    .getText()
                                    .trim()
                    );


            if (amount.compareTo(
                    BigDecimal.ZERO
            ) <= 0) {

                showWarning(
                        LanguageManager.isArabic()
                                ? "يجب أن يكون المبلغ أكبر من صفر."
                                : "Amount must be greater than zero."
                );


                amountField.requestFocus();

                return false;
            }


            /*
             * Prevent over-payment for both new and edited payments.
             * When editing a previously PAID transaction, its old amount
             * is temporarily added back to the available balance.
             */
            MembershipOption selectedMembership =
                    membershipComboBox.getValue();

            if (selectedMembership != null) {

                BigDecimal balanceAvailable =
                        getAvailableBalanceForForm(selectedMembership);

                if (balanceAvailable.compareTo(BigDecimal.ZERO) <= 0) {

                    showWarning(
                            LanguageManager.isArabic()
                                    ? "هذا الاشتراك مسدد بالكامل بالفعل."
                                    : "This membership is already fully paid."
                    );

                    return false;
                }

                if (amount.compareTo(balanceAvailable) > 0) {

                    showWarning(
                            LanguageManager.isArabic()
                                    ? "المبلغ أكبر من الرصيد المتاح على الاشتراك: "
                                        + moneyNumber(balanceAvailable)
                                    : "Amount exceeds the available membership balance: "
                                        + moneyNumber(balanceAvailable)
                    );

                    amountField.requestFocus();
                    return false;
                }
            }


        } catch (NumberFormatException exception) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "يرجى إدخال مبلغ صحيح."
                            : "Please enter a valid amount."
            );


            amountField.requestFocus();

            return false;
        }


        if (paymentMethodComboBox
                .getValue() == null) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "يرجى اختيار طريقة الدفع."
                            : "Please select a payment method."
            );


            return false;
        }


        if (statusComboBox
                .getValue() == null) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "يرجى اختيار حالة الدفع."
                            : "Please select payment status."
            );


            return false;
        }


        if (paymentDatePicker
                .getValue() == null) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "يرجى اختيار تاريخ الدفع."
                            : "Please select payment date."
            );

            return false;
        }

        if (shouldCreateInstallmentSchedule()) {

            if (installmentsComboBox.getValue() == null
                    || installmentsComboBox.getValue() <= 0) {

                showWarning(
                        LanguageManager.isArabic()
                                ? "يرجى اختيار عدد أقساط صحيح لباقي الاشتراك."
                                : "Please select a valid installment count for the remaining balance."
                );

                return false;
            }

            if (firstInstallmentDatePicker.getValue() == null) {

                showWarning(
                        LanguageManager.isArabic()
                                ? "يرجى اختيار تاريخ أول استحقاق."
                                : "Please select the first installment due date."
                );

                return false;
            }
        }


        return true;
    }


    // =========================================================
    // NORMALIZE PAYMENT METHOD
    // =========================================================

    private String normalizePaymentMethod(
            String value
    ) {

        if (value == null) {

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
                    value
                            .toUpperCase()
                            .replace(
                                    " ",
                                    "_"
                            );
        };
    }


    // =========================================================
    // NORMALIZE STATUS
    // =========================================================

    private String normalizeStatus(
            String value
    ) {

        if (value == null) {

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
                    value.toUpperCase();
        };
    }


    // =========================================================
    // DISPLAY PAYMENT METHOD
    // =========================================================

    private String displayPaymentMethod(
            String value
    ) {

        if (value == null) {

            return null;
        }


        if (LanguageManager.isArabic()) {

            return switch (
                    value.toUpperCase()
            ) {

                case "CASH" ->
                        "نقدي";


                case "CARD" ->
                        "بطاقة";


                case "BANK_TRANSFER" ->
                        "تحويل بنكي";


                default ->
                        value;
            };
        }


        return switch (
                value.toUpperCase()
        ) {

            case "CASH" ->
                    "Cash";


            case "CARD" ->
                    "Card";


            case "BANK_TRANSFER" ->
                    "Bank Transfer";


            default ->
                    value;
        };
    }


    // =========================================================
    // DISPLAY STATUS
    // =========================================================

    private String displayStatus(
            String value
    ) {

        if (value == null) {

            return null;
        }


        if (LanguageManager.isArabic()) {

            return switch (
                    value.toUpperCase()
            ) {

                case "PAID" ->
                        "مدفوع";


                case "PENDING" ->
                        "معلق";


                case "CANCELLED" ->
                        "ملغي";


                default ->
                        value;
            };
        }


        return switch (
                value.toUpperCase()
        ) {

            case "PAID" ->
                    "Paid";


            case "PENDING" ->
                    "Pending";


            case "CANCELLED" ->
                    "Cancelled";


            default ->
                    value;
        };
    }


    // =========================================================
    // NORMALIZE TEXT
    // =========================================================

    private String normalizeText(
            String value
    ) {

        if (value == null) {

            return "";
        }


        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
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
    // CLEAN
    // =========================================================

    private String clean(
            String value
    ) {

        if (value == null) {

            return null;
        }


        String result =
                value.trim();


        return result.isBlank()
                ? null
                : result;
    }


    // =========================================================
    // CANCEL
    // =========================================================

    @FXML
    private void cancel() {

        payment =
                null;


        saved =
                false;


        closeWindow();
    }


    // =========================================================
    // CLOSE
    // =========================================================

    private void closeWindow() {

        Stage stage =
                (Stage) amountField
                        .getScene()
                        .getWindow();


        stage.close();
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
                        ? "بيانات غير مكتملة"
                        : "Incomplete Data"
        );


        alert.setContentText(
                message
        );


        DialogStyler.style(alert);


        WindowManager.showResponsiveDialogAndWait(
                alert,
                getOwnerWindow(),
                560.0,
                330.0
        );
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


    // =========================================================
    // OWNER WINDOW
    // =========================================================

    private Window getOwnerWindow() {

        if (amountField == null
                || amountField.getScene() == null) {

            return null;
        }

        return amountField
                .getScene()
                .getWindow();
    }


    // =========================================================
    // RESULT
    // =========================================================

    public Payment getPayment() {

        return saved
                ? payment
                : null;
    }


    public boolean isSaved() {

        return saved;
    }
}
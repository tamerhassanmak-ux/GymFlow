package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.Member;
import com.mycompany.lazar_app.repository.MemberRepository;
import com.mycompany.lazar_app.util.QrCodeGenerator;
import com.mycompany.lazar_app.util.MemberCardPrinter;
import com.mycompany.lazar_app.ui.dialog.AppDialog;

import java.io.File;
import java.time.LocalDate;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;



public class AddMemberController {

    private final MemberRepository memberRepository = new MemberRepository();

    // =====================================================
    // BASIC DATA
    // =====================================================

    @FXML
    private TextField memberCodeField;

    @FXML
    private Button applyMemberCodeButton;

    @FXML
    private Label memberCodeHintLabel;

    @FXML
    private TextField nameField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField whatsappField;

    @FXML
    private TextField emailField;


    // =====================================================
    // PERSONAL DATA
    // =====================================================

    @FXML
    private ComboBox<String> genderComboBox;

    @FXML
    private DatePicker birthDatePicker;


    // =====================================================
    // MEMBER STATUS
    // =====================================================

    /*
     * Member master/account status only.
     * Subscription status belongs to member_memberships.
     */
    @FXML
    private ComboBox<String> statusComboBox;


    // =====================================================
    // NOTES
    // =====================================================

    @FXML
    private TextArea notesField;


    // =====================================================
    // MEMBER PHOTO
    // =====================================================

    @FXML
    private ImageView memberImageView;

    @FXML
    private ImageView cardMemberImageView;

    @FXML
    private Label photoPlaceholderLabel;

    @FXML
    private Label cardPhotoPlaceholder;


    // =====================================================
    // QR CODE
    // =====================================================

    @FXML
    private ImageView qrCodeImageView;

    @FXML
    private ImageView cardQrImageView;

    @FXML
    private Label qrPlaceholderLabel;

    @FXML
    private Label cardQrPlaceholder;

    @FXML
    private Label qrCodeLabel;


    // =====================================================
    // MEMBERSHIP CARD PREVIEW
    // =====================================================

    @FXML
    private Label cardMemberNameLabel;

    @FXML
    private Label cardMemberCodeLabel;

    @FXML
    private Label printHintLabel;


    // =====================================================
    // INTERNAL DATA
    // =====================================================

    private ResourceBundle bundle;

    private Member newMember;

    private static final String NEW_MEMBER_CODE = "LZR-AUTO";

    private String photoPath = "";


    // =====================================================
    // EDIT MODE
    // =====================================================

    private Member editingMember;

    private boolean editMode = false;

    /*
     * Used while loading existing member data.
     *
     * Prevents listeners from treating programmatic field loading
     * as user edits while opening Edit mode.
     */
    private boolean loadingMemberData = false;

    /*
     * Printing is restricted to persisted data only.
     * If an existing member is edited, the card must not be printed
     * until the changes are saved.
     */
    private boolean formDirty = false;


    // =====================================================
    // INITIALIZE
    // =====================================================

    @FXML
    public void initialize() {

        bundle = LanguageManager.getBundle();

        initializeGender();

        initializeStatuses();

        initializeDefaults();

        initializeMemberCodeControls();

        initializeListeners();

        initializeDirtyTracking();

        updateCardPreview();

        updatePrintHint();
    }


    // =====================================================
    // INITIALIZE GENDER
    // =====================================================

    private void initializeGender() {

        genderComboBox.setItems(
                FXCollections.observableArrayList(
                        bundle.getString("gender.male"),
                        bundle.getString("gender.female")
                )
        );
    }


    // =====================================================
    // INITIALIZE STATUS
    // =====================================================

    private void initializeStatuses() {

        statusComboBox.setItems(
                FXCollections.observableArrayList(
                        bundle.getString("status.active"),
                        bundle.getString("status.inactive"),
                        bundle.getString("status.suspended")
                )
        );
    }


    // =====================================================
    // DEFAULT VALUES
    // =====================================================

    private void initializeDefaults() {

        statusComboBox.setValue(
                bundle.getString("status.active")
        );
    }


    // =====================================================
    // MEMBER CODE / QR MANUAL ENTRY
    // =====================================================

    private void initializeMemberCodeControls() {

        memberCodeField.setEditable(true);
        memberCodeField.setFocusTraversable(true);

        memberCodeField.setPromptText(
                LanguageManager.isArabic()
                        ? "اختياري — اتركه فارغًا للتوليد التلقائي"
                        : "Optional — leave blank for automatic code"
        );

        if (applyMemberCodeButton != null) {
            applyMemberCodeButton.setText(
                    LanguageManager.isArabic()
                            ? "تطبيق وإنشاء QR"
                            : "Apply & Generate QR"
            );
        }

        if (memberCodeHintLabel != null) {
            memberCodeHintLabel.setText(
                    LanguageManager.isArabic()
                            ? "يمكن كتابة الكود يدويًا أو مسحه بقارئ Barcode/QR. اترك الحقل فارغًا لاستخدام الكود التلقائي."
                            : "Type the code manually or scan it with a Barcode/QR reader. Leave blank to use the automatic code."
            );
        }
    }


    // =====================================================
    // LIVE LISTENERS
    // =====================================================

    private void initializeListeners() {

        /*
         * Update member name on membership card.
         */
        nameField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        updateCardPreview()
        );


        /*
         * Member code is editable so the user can type/paste a code
         * or scan it using a keyboard-wedge Barcode/QR reader.
         */
        memberCodeField.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    updateCardPreview();

                    if (newValue == null || newValue.isBlank()) {
                        clearQrCodePreview();
                    }
                }
        );


        /*
         * Generate QR when the user leaves the member-code field.
         * This avoids regenerating a large QR image on every keystroke.
         */
        memberCodeField.focusedProperty().addListener(
                (observable, oldValue, focused) -> {

                    if (!focused
                            && !loadingMemberData
                            && !safeText(memberCodeField).isBlank()) {

                        generateQrCode();
                    }
                }
        );


        /*
         * Automatically copy phone to WhatsApp.
         *
         * If WhatsApp was manually changed,
         * don't overwrite it.
         */
        phoneField.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    if (loadingMemberData) {
                        return;
                    }

                    String whatsapp =
                            whatsappField.getText();

                    if (whatsapp == null
                            || whatsapp.isBlank()
                            || whatsapp.equals(oldValue)) {

                        whatsappField.setText(
                                newValue
                        );
                    }
                }
        );
    }


    // =====================================================
    // DIRTY TRACKING
    // =====================================================

    private void initializeDirtyTracking() {

        memberCodeField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        markFormDirty()
        );

        nameField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        markFormDirty()
        );

        phoneField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        markFormDirty()
        );

        whatsappField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        markFormDirty()
        );

        emailField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        markFormDirty()
        );

        notesField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        markFormDirty()
        );

        genderComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                        markFormDirty()
        );

        birthDatePicker.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                        markFormDirty()
        );

        statusComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                        markFormDirty()
        );
    }


    private void markFormDirty() {

        if (loadingMemberData) {
            return;
        }

        formDirty = true;

        updatePrintHint();
    }


    private void updatePrintHint() {

        if (printHintLabel == null) {
            return;
        }

        if (!editMode
                || editingMember == null
                || editingMember.getId() <= 0) {

            printHintLabel.setText(
                    LanguageManager.isArabic()
                            ? "احفظ العضو أولاً. بعد الحفظ يمكنك طباعة الكارت أو QR من جدول الأعضاء أو شاشة التفاصيل."
                            : "Save the member first. After saving, you can print the card or QR from Members or Member Details."
            );

            printHintLabel.setStyle(
                    "-fx-text-fill: #b45309;"
                            + "-fx-font-size: 11px;"
                            + "-fx-font-weight: bold;"
            );

            return;
        }

        if (formDirty) {

            printHintLabel.setText(
                    LanguageManager.isArabic()
                            ? "يوجد تعديلات غير محفوظة. احفظ التعديلات أولاً لطباعة أحدث بيانات."
                            : "There are unsaved changes. Save them before printing the latest member data."
            );

            printHintLabel.setStyle(
                    "-fx-text-fill: #b45309;"
                            + "-fx-font-size: 11px;"
                            + "-fx-font-weight: bold;"
            );

            return;
        }

        printHintLabel.setText(
                LanguageManager.isArabic()
                        ? "العضو محفوظ — الطباعة متاحة."
                        : "Saved member — printing is available."
        );

        printHintLabel.setStyle(
                "-fx-text-fill: #15803d;"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
        );
    }


    // =====================================================
    // PREPARE ADD MODE
    // =====================================================

    public void prepareForAdd() {

        loadingMemberData = true;

        editMode = false;
        editingMember = null;

        /*
         * Blank = automatic code after INSERT.
         * Any nonblank value = manual code / scanned QR payload.
         */
        memberCodeField.clear();
        memberCodeField.setEditable(true);

        clearQrCodePreview();

        loadingMemberData = false;
        formDirty = false;

        updateCardPreview();

        updatePrintHint();
    }


    // =====================================================
    // LOAD MEMBER FOR EDIT
    // =====================================================

    public void setMemberForEdit(Member member) {

        if (member == null) {
            return;
        }


        loadingMemberData = true;

        editMode = true;

        editingMember = member;


        // =================================================
        // MEMBER CODE
        // =================================================

        String memberCode =
                safeString(
                        member.getMemberCode()
                );


        /*
         * Safety for old members created before
         * memberCode was introduced.
         */
        if (memberCode.isBlank()) {

            memberCode =
                    generateMemberCode(
                            member.getId()
                    );
        }


        memberCodeField.setText(
                memberCode
        );

        memberCodeField.setEditable(true);


        // =================================================
        // BASIC DATA
        // =================================================

        nameField.setText(
                safeString(
                        member.getName()
                )
        );


        phoneField.setText(
                safeString(
                        member.getPhone()
                )
        );


        whatsappField.setText(
                safeString(
                        member.getWhatsapp()
                )
        );


        emailField.setText(
                safeString(
                        member.getEmail()
                )
        );


        // =================================================
        // PERSONAL DATA
        // =================================================

        setGenderDisplayValue(
                member.getGender()
        );


        birthDatePicker.setValue(
                member.getBirthDate()
        );


        // =================================================
        // MEMBER STATUS
        // =================================================

        /*
         * This is the member master/account status only.
         * Plan, subscription dates and subscription status are loaded
         * exclusively from member_memberships elsewhere in the system.
         */
        setStatusDisplayValue(
                member.getStatus()
        );


        // =================================================
        // NOTES
        // =================================================

        notesField.setText(
                safeString(
                        member.getNotes()
                )
        );


        // =================================================
        // PHOTO
        // =================================================

        photoPath =
                safeString(
                        member.getPhotoPath()
                );


        loadExistingPhoto(
                photoPath
        );


        /*
         * Finish loading before allowing listeners
         * to perform automatic calculations again.
         */
        loadingMemberData = false;


        // =================================================
        // QR + CARD
        // =================================================

        qrCodeLabel.setText(
                memberCode
        );


        cardMemberCodeLabel.setText(
                memberCode
        );


        generateQrCode();

        updateCardPreview();

        formDirty = false;

        updatePrintHint();
    }


    // =====================================================
    // GENERATE MEMBER CODE
    // =====================================================

    private String generateMemberCode(int id) {

        return String.format(
                "LZR-%06d",
                id
        );
    }


    // =====================================================
    // APPLY MANUAL MEMBER CODE / QR VALUE
    // =====================================================

    @FXML
    private void applyManualMemberCode() {

        String memberCode =
                safeText(memberCodeField);

        if (memberCode.isBlank()) {

            clearQrCodePreview();
            updateCardPreview();

            showInformation(
                    LanguageManager.isArabic()
                            ? "تم ترك الكود فارغًا. سيتم إنشاء كود العضو تلقائيًا بعد الحفظ."
                            : "The code is blank. A member code will be generated automatically after saving."
            );

            return;
        }

        if (!validateMemberCode(memberCode)) {
            return;
        }

        /*
         * Store the trimmed value back in the field.
         * This value can be typed, pasted or supplied by a scanner.
         */
        if (!memberCode.equals(memberCodeField.getText())) {
            memberCodeField.setText(memberCode);
        }

        generateQrCode();
        updateCardPreview();
    }


    private boolean validateMemberCode(String memberCode) {

        if (memberCode == null || memberCode.isBlank()) {
            return true;
        }

        /*
         * members.member_code is VARCHAR(30) in the current schema.
         */
        if (memberCode.length() > 30) {

            showValidation(
                    LanguageManager.isArabic()
                            ? "كود العضو يجب ألا يزيد عن 30 حرفًا."
                            : "Member code must not exceed 30 characters."
            );

            memberCodeField.requestFocus();
            memberCodeField.selectAll();

            return false;
        }

        return true;
    }


    private void clearQrCodePreview() {

        qrCodeImageView.setImage(null);
        cardQrImageView.setImage(null);

        qrPlaceholderLabel.setVisible(true);
        qrPlaceholderLabel.setManaged(true);

        cardQrPlaceholder.setVisible(true);
        cardQrPlaceholder.setManaged(true);
    }


    // =====================================================
    // GENERATE QR CODE
    // =====================================================

    private void generateQrCode() {

        String memberCode =
                safeText(memberCodeField);


        if (memberCode.isBlank()) {

            clearQrCodePreview();

            return;
        }

        if (!validateMemberCode(memberCode)) {
            return;
        }


        try {

            // Main QR

            Image largeQr =
                    QrCodeGenerator.generate(
                            memberCode,
                            300,
                            300
                    );


            // Membership card QR

            Image cardQr =
                    QrCodeGenerator.generate(
                            memberCode,
                            180,
                            180
                    );


            qrCodeImageView.setImage(
                    largeQr
            );


            cardQrImageView.setImage(
                    cardQr
            );


            // Hide placeholders

            qrPlaceholderLabel.setVisible(
                    false
            );

            qrPlaceholderLabel.setManaged(
                    false
            );


            cardQrPlaceholder.setVisible(
                    false
            );

            cardQrPlaceholder.setManaged(
                    false
            );


            System.out.println(
                    "QR generated successfully: "
                            + memberCode
            );


        } catch (Exception e) {

            System.err.println(
                    "QR generation failed: "
                            + memberCode
            );

            e.printStackTrace();


            showInformation(
                    LanguageManager.isArabic()
                            ? "تعذر إنشاء رمز QR للعضو."
                            : "Unable to generate member QR code."
            );
        }
    }


    // =====================================================
    // UPLOAD MEMBER PHOTO
    // =====================================================

    @FXML
    private void uploadPhoto() {

        FileChooser chooser =
                new FileChooser();


        chooser.setTitle(
                LanguageManager.isArabic()
                        ? "اختيار صورة العضو"
                        : "Select Member Photo"
        );


        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image Files",
                        "*.png",
                        "*.jpg",
                        "*.jpeg",
                        "*.webp"
                )
        );


        Stage stage =
                getStage();


        File file =
                chooser.showOpenDialog(
                        stage
                );


        if (file == null) {
            return;
        }


        photoPath =
                file.getAbsolutePath();


        try {

            Image image =
                    new Image(
                            file.toURI().toString(),
                            true
                    );


            memberImageView.setImage(
                    image
            );


            cardMemberImageView.setImage(
                    image
            );


            photoPlaceholderLabel.setVisible(
                    false
            );

            photoPlaceholderLabel.setManaged(
                    false
            );


            cardPhotoPlaceholder.setVisible(
                    false
            );

            cardPhotoPlaceholder.setManaged(
                    false
            );

            markFormDirty();


        } catch (Exception e) {

            System.err.println(
                    "Unable to load selected member photo."
            );

            e.printStackTrace();


            showInformation(
                    LanguageManager.isArabic()
                            ? "تعذر تحميل صورة العضو."
                            : "Unable to load member photo."
            );
        }
    }


    // =====================================================
    // LOAD EXISTING PHOTO
    // =====================================================

    private void loadExistingPhoto(String path) {

        if (path == null
                || path.isBlank()) {

            clearPhotoPreview();

            return;
        }


        try {

            File file =
                    new File(
                            path
                    );


            if (!file.exists()) {

                clearPhotoPreview();

                return;
            }


            Image image =
                    new Image(
                            file.toURI().toString(),
                            true
                    );


            memberImageView.setImage(
                    image
            );


            cardMemberImageView.setImage(
                    image
            );


            photoPlaceholderLabel.setVisible(
                    false
            );

            photoPlaceholderLabel.setManaged(
                    false
            );


            cardPhotoPlaceholder.setVisible(
                    false
            );

            cardPhotoPlaceholder.setManaged(
                    false
            );


        } catch (Exception e) {

            clearPhotoPreview();


            System.err.println(
                    "Unable to load member photo: "
                            + path
            );

            e.printStackTrace();
        }
    }


    // =====================================================
    // CLEAR PHOTO PREVIEW
    // =====================================================

    private void clearPhotoPreview() {

        memberImageView.setImage(
                null
        );


        cardMemberImageView.setImage(
                null
        );


        photoPlaceholderLabel.setVisible(
                true
        );

        photoPlaceholderLabel.setManaged(
                true
        );


        cardPhotoPlaceholder.setVisible(
                true
        );

        cardPhotoPlaceholder.setManaged(
                true
        );
    }


    // =====================================================
    // UPDATE MEMBERSHIP CARD PREVIEW
    // =====================================================

    private void updateCardPreview() {

        if (cardMemberNameLabel == null) {
            return;
        }


        // =================================================
        // MEMBER NAME
        // =================================================

        String name =
                nameField.getText();


        if (name == null
                || name.isBlank()) {

            cardMemberNameLabel.setText(
                    LanguageManager.isArabic()
                            ? "اسم العضو"
                            : "Member Name"
            );

        } else {

            cardMemberNameLabel.setText(
                    name.trim()
            );
        }


        // =================================================
        // MEMBER CODE
        // =================================================

        String memberCode =
                memberCodeField.getText();


        if (memberCode == null
                || memberCode.isBlank()) {

            String automaticLabel =
                    LanguageManager.isArabic()
                            ? "تلقائي بعد الحفظ"
                            : "AUTO AFTER SAVE";

            cardMemberCodeLabel.setText(
                    automaticLabel
            );


            qrCodeLabel.setText(
                    automaticLabel
            );

        } else {

            cardMemberCodeLabel.setText(
                    memberCode
            );


            qrCodeLabel.setText(
                    memberCode
            );
        }
    }


    // =====================================================
    // SAVE MEMBER
    // =====================================================

  
   @FXML
private void saveMember() {

    // =====================================================
    // READ FORM DATA
    // =====================================================

    String name =
            safeText(nameField);

    String phone =
            safeText(phoneField);

    String whatsapp =
            safeText(whatsappField);

    String email =
            safeText(emailField);

    String requestedMemberCode =
            safeText(memberCodeField);

    String notes =
            notesField.getText() == null
                    ? ""
                    : notesField.getText().trim();


    // =====================================================
    // VALIDATION - NAME
    // =====================================================

    if (name.isEmpty()) {

        showValidation(
                bundle.getString(
                        "validation.nameRequired"
                )
        );

        nameField.requestFocus();

        return;
    }


    // =====================================================
    // VALIDATION - PHONE
    // =====================================================

    if (phone.isEmpty()) {

        showValidation(
                bundle.getString(
                        "validation.phoneRequired"
                )
        );

        phoneField.requestFocus();

        return;
    }


    // =====================================================
    // VALIDATION - UNIQUE PHONE
    // =====================================================

    try {

        Optional<Member> duplicateMember;

        if (editMode && editingMember != null) {

            duplicateMember =
                    memberRepository.findAnotherByPhone(
                            phone,
                            editingMember.getId()
                    );

        } else {

            duplicateMember =
                    memberRepository.findByPhone(
                            phone
                    );
        }

        if (duplicateMember.isPresent()) {

            showDuplicatePhoneAlert(
                    duplicateMember.get()
            );

            phoneField.requestFocus();
            phoneField.selectAll();

            return;
        }

    } catch (SQLException exception) {

        System.err.println(
                "Unable to verify member phone."
        );

        exception.printStackTrace();

        showDatabaseValidationError();

        return;
    }


    // =====================================================
    // VALIDATION - MEMBER CODE / QR VALUE
    // =====================================================

    if (!validateMemberCode(requestedMemberCode)) {
        return;
    }

    /*
     * Blank is valid and means: use automatic LZR-xxxxxx code.
     * A manual/scanned code must be unique.
     */
    if (!requestedMemberCode.isBlank()) {

        try {

            Optional<Member> duplicateCode;

            if (editMode && editingMember != null) {

                duplicateCode =
                        memberRepository.findAnotherByMemberCode(
                                requestedMemberCode,
                                editingMember.getId()
                        );

            } else {

                duplicateCode =
                        memberRepository.findByMemberCode(
                                requestedMemberCode
                        );
            }

            if (duplicateCode.isPresent()) {

                AppDialog.warning(
                        getStage(),
                        LanguageManager.isArabic()
                                ? "كود مستخدم بالفعل"
                                : "Code Already In Use",
                        LanguageManager.isArabic()
                                ? "كود العضو/QR مستخدم بالفعل لعضو آخر. يرجى إدخال كود مختلف."
                                : "This member/QR code is already assigned to another member. Enter a different code."
                );

                memberCodeField.requestFocus();
                memberCodeField.selectAll();

                return;
            }

        } catch (SQLException exception) {

            System.err.println(
                    "Unable to verify member code."
            );

            exception.printStackTrace();

            AppDialog.error(
                    getStage(),
                    LanguageManager.isArabic()
                            ? "تعذر التحقق من الكود"
                            : "Unable to Verify Code",
                    LanguageManager.isArabic()
                            ? "تعذر التحقق من كود العضو في قاعدة البيانات. لم يتم حفظ العضو."
                            : "The member code could not be verified in the database. The member was not saved."
            );

            return;
        }
    }


    // =====================================================
    // VALIDATION - MEMBER STATUS
    // =====================================================

    if (statusComboBox.getValue() == null) {

        showValidation(
                bundle.getString(
                        "validation.statusRequired"
                )
        );

        statusComboBox.requestFocus();

        return;
    }


    // =====================================================
    // CONVERT UI VALUES TO SYSTEM VALUES
    // =====================================================

    String status =
            getStatusSystemValue();

    String gender =
            getGenderSystemValue();


    // =====================================================
    // MEMBER ID + CODE
    // =====================================================

    int memberId;
    String memberCode;

    if (editMode && editingMember != null) {

        memberId = editingMember.getId();
        memberCode = requestedMemberCode;

        /*
         * Clearing the field while editing restores the automatic
         * code derived from the existing database ID.
         */
        if (memberCode.isBlank()) {
            memberCode = generateMemberCode(memberId);
        }

    } else {

        /*
         * Nonblank = manual/scanned code.
         * Blank = MemberRepository generates LZR-xxxxxx after INSERT.
         */
        memberId = 0;
        memberCode = requestedMemberCode;
    }


    // =====================================================
    // BIRTH DATE
    // =====================================================

    LocalDate birthDate =
            birthDatePicker.getValue();


    // =====================================================
    // CREATE RESULT MEMBER
    // =====================================================

    /*
     * IMPORTANT:
     *
     * Member constructor order:
     *
     * 1  id
     * 2  memberCode
     * 3  name
     * 4  phone
     * 5  whatsapp
     * 6  email
     * 7  gender
     * 8  birthDate
     * 9  legacy membership      -> intentionally blank
     * 10 legacy startDate      -> intentionally null
     * 11 legacy endDate        -> intentionally null
     * 12 member account status
     * 13 notes
     * 14 photoPath
     *
     * Subscription data is created only through member_memberships.
     */

    newMember =
            new Member(
                    memberId,
                    memberCode,
                    name,
                    phone,
                    whatsapp,
                    email,
                    gender,
                    birthDate,
                    "",
                    null,
                    null,
                    status,
                    notes,
                    photoPath
            );


    // =====================================================
    // CLOSE WINDOW
    // =====================================================

    closeWindow();
}


    // =====================================================
    // MEMBER STATUS SYSTEM VALUE
    // =====================================================

    private String getStatusSystemValue() {

        String selected =
                statusComboBox.getValue();


        if (selected == null) {
            return "";
        }


        if (selected.equals(
                bundle.getString(
                        "status.active"
                ))) {

            return "Active";
        }


        if (selected.equals(
                bundle.getString(
                        "status.inactive"
                ))) {

            return "Inactive";
        }


        if (selected.equals(
                bundle.getString(
                        "status.suspended"
                ))) {

            return "Suspended";
        }


        return selected;
    }


    // =====================================================
    // GENDER SYSTEM VALUE
    // =====================================================

    private String getGenderSystemValue() {

        String selected =
                genderComboBox.getValue();


        if (selected == null) {
            return "";
        }


        if (selected.equals(
                bundle.getString(
                        "gender.male"
                ))) {

            return "Male";
        }


        if (selected.equals(
                bundle.getString(
                        "gender.female"
                ))) {

            return "Female";
        }


        return selected;
    }


    // =====================================================
    // SET GENDER DISPLAY VALUE
    // =====================================================

    private void setGenderDisplayValue(
            String gender
    ) {

        if (gender == null
                || gender.isBlank()) {

            genderComboBox.setValue(
                    null
            );

            return;
        }


        switch (gender) {

            case "Male" ->
                    genderComboBox.setValue(
                            bundle.getString(
                                    "gender.male"
                            )
                    );

            case "Female" ->
                    genderComboBox.setValue(
                            bundle.getString(
                                    "gender.female"
                            )
                    );

            default ->
                    genderComboBox.setValue(
                            gender
                    );
        }
    }


    // =====================================================
    // SET MEMBER STATUS DISPLAY VALUE
    // =====================================================

    private void setStatusDisplayValue(
            String status
    ) {

        if (status == null
                || status.isBlank()) {

            statusComboBox.setValue(
                    null
            );

            return;
        }


        switch (status) {

            case "Active" ->
                    statusComboBox.setValue(
                            bundle.getString(
                                    "status.active"
                            )
                    );

            case "Inactive" ->
                    statusComboBox.setValue(
                            bundle.getString(
                                    "status.inactive"
                            )
                    );

            case "Suspended" ->
                    statusComboBox.setValue(
                            bundle.getString(
                                    "status.suspended"
                            )
                    );

            default ->
                    statusComboBox.setValue(
                            status
                    );
        }
    }


    // =====================================================
    // SAVED-ONLY PRINTING
    // =====================================================

    @FXML
    private void printMemberCard() {

        Member printableMember =
                getSavedMemberForPrinting();

        if (printableMember == null) {
            return;
        }

        MemberCardPrinter.PrintResult result =
                MemberCardPrinter.printCard(
                        getStage(),
                        printableMember,
                        LanguageManager.isArabic()
                );

        handlePrintResult(
                result,
                LanguageManager.isArabic()
                        ? "كارت العضو"
                        : "Member Card"
        );
    }


    @FXML
    private void printQrCode() {

        Member printableMember =
                getSavedMemberForPrinting();

        if (printableMember == null) {
            return;
        }

        MemberCardPrinter.PrintResult result =
                MemberCardPrinter.printQrLabel(
                        getStage(),
                        printableMember,
                        LanguageManager.isArabic()
                );

        handlePrintResult(
                result,
                LanguageManager.isArabic()
                        ? "QR العضو"
                        : "Member QR"
        );
    }


    private Member getSavedMemberForPrinting() {

        if (!editMode
                || editingMember == null
                || !MemberCardPrinter.isSavedMember(
                        editingMember
                )) {

            AppDialog.warning(
                    getStage(),
                    LanguageManager.isArabic()
                            ? "الحفظ مطلوب قبل الطباعة"
                            : "Save Required Before Printing",
                    LanguageManager.isArabic()
                            ? "احفظ العضو أولاً. بعد الحفظ يمكنك طباعة الكارت من جدول الأعضاء أو من شاشة تفاصيل العضو."
                            : "Save the member first. After saving, print the card from the Members table or Member Details."
            );

            return null;
        }

        if (formDirty) {

            AppDialog.warning(
                    getStage(),
                    LanguageManager.isArabic()
                            ? "تعديلات غير محفوظة"
                            : "Unsaved Changes",
                    LanguageManager.isArabic()
                            ? "تم تعديل بيانات العضو ولم يتم حفظها بعد. احفظ التعديلات أولاً ثم اطبع الكارت."
                            : "Member data has changed but is not saved yet. Save the changes before printing the card."
            );

            return null;
        }

        return editingMember;
    }


    private void handlePrintResult(
            MemberCardPrinter.PrintResult result,
            String itemName
    ) {

        if (result == null
                || result == MemberCardPrinter.PrintResult.CANCELLED) {

            return;
        }

        switch (result) {

            case PRINTED ->
                    AppDialog.info(
                            getStage(),
                            LanguageManager.isArabic()
                                    ? "تم إرسال الطباعة"
                                    : "Print Sent",
                            LanguageManager.isArabic()
                                    ? "تم إرسال " + itemName + " إلى الطابعة بنجاح."
                                    : itemName + " was sent to the printer successfully."
                    );

            case NO_PRINTER ->
                    AppDialog.warning(
                            getStage(),
                            LanguageManager.isArabic()
                                    ? "لا توجد طابعة"
                                    : "No Printer",
                            LanguageManager.isArabic()
                                    ? "لم يتم العثور على طابعة متاحة على النظام."
                                    : "No available printer was found on this system."
                    );

            case FAILED ->
                    AppDialog.error(
                            getStage(),
                            LanguageManager.isArabic()
                                    ? "فشل الطباعة"
                                    : "Print Failed",
                            LanguageManager.isArabic()
                                    ? "تعذر إتمام عملية الطباعة. راجع إعدادات الطابعة وحاول مرة أخرى."
                                    : "The print operation could not be completed. Check the printer settings and try again."
                    );

            default -> {
            }
        }
    }


    // =====================================================
    // SAFE TEXT FIELD
    // =====================================================

    private String safeText(
            TextField field
    ) {

        if (field == null
                || field.getText() == null) {

            return "";
        }


        return field
                .getText()
                .trim();
    }


    // =====================================================
    // SAFE STRING
    // =====================================================

    private String safeString(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }


    // =====================================================
    // PROFESSIONAL DIALOGS
    // =====================================================

    private void showInformation(String message) {
        AppDialog.info(getStage(), LanguageManager.isArabic() ? "معلومة" : "Information", message);
    }

    private void showValidation(String message) {
        AppDialog.warning(getStage(), LanguageManager.isArabic() ? "يرجى مراجعة البيانات" : "Check Your Data", message);
    }

    private void showDuplicatePhoneAlert(Member existingMember) {
        AppDialog.duplicatePhone(getStage(), existingMember);
    }

    private void showDatabaseValidationError() {
        AppDialog.error(
                getStage(),
                LanguageManager.isArabic() ? "تعذر التحقق من رقم الهاتف" : "Unable to Verify Phone",
                LanguageManager.isArabic()
                        ? "تعذر التحقق من رقم الهاتف في قاعدة البيانات. لم يتم حفظ العضو."
                        : "The phone number could not be verified in the database. The member was not saved."
        );
    }


    // =====================================================
    // CANCEL
    // =====================================================

    @FXML
    private void cancel() {

        /*
         * Important:
         * MembersController will understand null as:
         * user cancelled, don't add/update anything.
         */

        newMember =
                null;


        closeWindow();
    }


    // =====================================================
    // GET CREATED / EDITED MEMBER
    // =====================================================

    public Member getNewMember() {

        return newMember;
    }


    // =====================================================
    // CHECK EDIT MODE
    // =====================================================

    public boolean isEditMode() {

        return editMode;
    }


    // =====================================================
    // GET CURRENT STAGE
    // =====================================================

    private Stage getStage() {

        return (Stage) nameField
                .getScene()
                .getWindow();
    }


    // =====================================================
    // CLOSE WINDOW
    // =====================================================

    private void closeWindow() {

        getStage().close();
    }
}
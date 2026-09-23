package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.Member;
import com.mycompany.lazar_app.repository.MemberRepository;
import com.mycompany.lazar_app.util.QrCodeGenerator;
import com.mycompany.lazar_app.ui.dialog.AppDialog;

import java.io.File;
import java.time.LocalDate;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
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
    // MEMBERSHIP
    // =====================================================

    @FXML
    private ComboBox<String> membershipComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

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
    private Label cardMembershipLabel;


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
     * Without this flag, setting membership/start date while
     * opening Edit mode could trigger listeners and overwrite
     * the stored end date.
     */
    private boolean loadingMemberData = false;


    // =====================================================
    // INITIALIZE
    // =====================================================

    @FXML
    public void initialize() {

        bundle = LanguageManager.getBundle();

        initializeGender();

        initializeMemberships();

        initializeStatuses();

        initializeDefaults();

        initializeListeners();

        updateCardPreview();
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
    // INITIALIZE MEMBERSHIPS
    // =====================================================

    private void initializeMemberships() {

        membershipComboBox.setItems(
                FXCollections.observableArrayList(
                        bundle.getString("membership.monthly"),
                        bundle.getString("membership.threeMonths"),
                        bundle.getString("membership.halfYear"),
                        bundle.getString("membership.yearly")
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

        startDatePicker.setValue(
                LocalDate.now()
        );

        statusComboBox.setValue(
                bundle.getString("status.active")
        );
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
         * Membership changed:
         * recalculate end date only when user changes it.
         */
        membershipComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> {

                    if (!loadingMemberData) {

                        calculateEndDate();
                    }

                    updateCardPreview();
                }
        );


        /*
         * Start date changed:
         * recalculate end date only outside member loading.
         */
        startDatePicker.valueProperty().addListener(
                (observable, oldValue, newValue) -> {

                    if (!loadingMemberData) {

                        calculateEndDate();
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
    // PREPARE ADD MODE
    // =====================================================

    public void prepareForAdd() {

        editMode = false;
        editingMember = null;

        memberCodeField.setText(NEW_MEMBER_CODE);
        memberCodeField.setEditable(false);

        qrCodeLabel.setText(NEW_MEMBER_CODE);
        cardMemberCodeLabel.setText(NEW_MEMBER_CODE);

        qrCodeImageView.setImage(null);
        cardQrImageView.setImage(null);

        qrPlaceholderLabel.setVisible(true);
        qrPlaceholderLabel.setManaged(true);
        cardQrPlaceholder.setVisible(true);
        cardQrPlaceholder.setManaged(true);

        updateCardPreview();
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
        // MEMBERSHIP
        // =================================================

        setMembershipDisplayValue(
                member.getMembership()
        );


        startDatePicker.setValue(
                member.getStartDate()
        );


        endDatePicker.setValue(
                member.getEndDate()
        );


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
    // GENERATE QR CODE
    // =====================================================

    private void generateQrCode() {

        String memberCode =
                memberCodeField.getText();


        if (memberCode == null
                || memberCode.isBlank()) {

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
    // CALCULATE MEMBERSHIP END DATE
    // =====================================================

    private void calculateEndDate() {

        LocalDate startDate =
                startDatePicker.getValue();


        String selected =
                membershipComboBox.getValue();


        if (startDate == null
                || selected == null) {

            endDatePicker.setValue(
                    null
            );

            return;
        }


        LocalDate endDate;


        if (selected.equals(
                bundle.getString(
                        "membership.monthly"
                ))) {

            endDate =
                    startDate.plusMonths(1);


        } else if (selected.equals(
                bundle.getString(
                        "membership.threeMonths"
                ))) {

            endDate =
                    startDate.plusMonths(3);


        } else if (selected.equals(
                bundle.getString(
                        "membership.halfYear"
                ))) {

            endDate =
                    startDate.plusMonths(6);


        } else if (selected.equals(
                bundle.getString(
                        "membership.yearly"
                ))) {

            endDate =
                    startDate.plusYears(1);


        } else {

            endDate =
                    null;
        }


        endDatePicker.setValue(
                endDate
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
        // MEMBERSHIP
        // =================================================

        String membership =
                membershipComboBox.getValue();


        if (membership == null
                || membership.isBlank()) {

            cardMembershipLabel.setText(
                    LanguageManager.isArabic()
                            ? "خطة الاشتراك"
                            : "Membership Plan"
            );

        } else {

            cardMembershipLabel.setText(
                    membership
            );
        }


        // =================================================
        // MEMBER CODE
        // =================================================

        String memberCode =
                memberCodeField.getText();


        if (memberCode == null
                || memberCode.isBlank()) {

            cardMemberCodeLabel.setText(
                    "LZR-000000"
            );


            qrCodeLabel.setText(
                    "LZR-000000"
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
    // VALIDATION - MEMBERSHIP
    // =====================================================

    if (membershipComboBox.getValue() == null) {

        showValidation(
                bundle.getString(
                        "validation.membershipRequired"
                )
        );

        membershipComboBox.requestFocus();

        return;
    }


    // =====================================================
    // VALIDATION - START DATE
    // =====================================================

    if (startDatePicker.getValue() == null) {

        showValidation(
                LanguageManager.isArabic()
                        ? "يرجى تحديد تاريخ بداية الاشتراك."
                        : "Please select the membership start date."
        );

        startDatePicker.requestFocus();

        return;
    }


    // =====================================================
    // VALIDATION - STATUS
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

    String membership =
            getMembershipSystemValue();

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
        memberCode = safeString(editingMember.getMemberCode());

        if (memberCode.isBlank()) {
            memberCode = generateMemberCode(memberId);
        }

    } else {

        // Database will generate the real ID and permanent member code.
        memberId = 0;
        memberCode = "";
    }


    // =====================================================
    // MEMBERSHIP END DATE
    // =====================================================

    LocalDate startDate =
            startDatePicker.getValue();

    LocalDate endDate =
            endDatePicker.getValue();


    /*
     * Safety:
     * if end date wasn't generated for any reason,
     * calculate it before saving.
     */

    if (endDate == null) {

        calculateEndDate();

        endDate =
                endDatePicker.getValue();
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
     * 9  membership
     * 10 startDate
     * 11 endDate
     * 12 status
     * 13 notes
     * 14 photoPath
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
                    membership,
                    startDate,
                    endDate,
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
    // MEMBERSHIP SYSTEM VALUE
    // =====================================================

    private String getMembershipSystemValue() {

        String selected =
                membershipComboBox.getValue();


        if (selected == null) {
            return "";
        }


        if (selected.equals(
                bundle.getString(
                        "membership.monthly"
                ))) {

            return "Monthly";
        }


        if (selected.equals(
                bundle.getString(
                        "membership.threeMonths"
                ))) {

            return "3 Months";
        }


        if (selected.equals(
                bundle.getString(
                        "membership.halfYear"
                ))) {

            return "6 Months";
        }


        if (selected.equals(
                bundle.getString(
                        "membership.yearly"
                ))) {

            return "Yearly";
        }


        return selected;
    }


    // =====================================================
    // STATUS SYSTEM VALUE
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
    // SET MEMBERSHIP DISPLAY VALUE
    // =====================================================

    private void setMembershipDisplayValue(
            String membership
    ) {

        if (membership == null
                || membership.isBlank()) {

            membershipComboBox.setValue(
                    null
            );

            return;
        }


        switch (membership) {

            case "Monthly" ->
                    membershipComboBox.setValue(
                            bundle.getString(
                                    "membership.monthly"
                            )
                    );

            case "3 Months" ->
                    membershipComboBox.setValue(
                            bundle.getString(
                                    "membership.threeMonths"
                            )
                    );

            case "6 Months" ->
                    membershipComboBox.setValue(
                            bundle.getString(
                                    "membership.halfYear"
                            )
                    );

            case "Yearly" ->
                    membershipComboBox.setValue(
                            bundle.getString(
                                    "membership.yearly"
                            )
                    );

            default ->
                    membershipComboBox.setValue(
                            membership
                    );
        }
    }


    // =====================================================
    // SET STATUS DISPLAY VALUE
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
    // PRINT MEMBERSHIP CARD
    // =====================================================

    @FXML
    private void printMemberCard() {

        if (qrCodeImageView.getImage() == null) {

            showInformation(
                    LanguageManager.isArabic()
                            ? "لم يتم إنشاء QR Code بعد."
                            : "QR Code has not been generated yet."
            );

            return;
        }


        showInformation(
                LanguageManager.isArabic()
                        ? "بطاقة العضوية جاهزة للطباعة."
                        : "Membership card is ready for printing."
        );
    }


    // =====================================================
    // PRINT QR LABEL
    // =====================================================

    @FXML
    private void printQrCode() {

        if (qrCodeImageView.getImage() == null) {

            showInformation(
                    LanguageManager.isArabic()
                            ? "لم يتم إنشاء QR Code بعد."
                            : "QR Code has not been generated yet."
            );

            return;
        }


        showInformation(
                LanguageManager.isArabic()
                        ? "ملصق QR جاهز للطباعة."
                        : "QR label is ready for printing."
        );
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
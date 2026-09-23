package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.Member;
import com.mycompany.lazar_app.model.MemberMembership;
import com.mycompany.lazar_app.repository.CheckInRepository;
import com.mycompany.lazar_app.repository.MemberMembershipRepository;
import com.mycompany.lazar_app.util.QrCodeGenerator;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.geometry.NodeOrientation;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.stage.Modality;
import javafx.stage.Stage;


public class MemberDetailsController {

    // =========================================================
    // PROFILE
    // =========================================================

    @FXML
    private ImageView memberImageView;

    @FXML
    private Label photoPlaceholderLabel;

    @FXML
    private Label memberNameLabel;

    @FXML
    private Label memberCodeLabel;

    @FXML
    private Label membershipBadgeLabel;

    @FXML
    private Label statusBadgeLabel;


    // =========================================================
    // QR
    // =========================================================

    @FXML
    private ImageView qrCodeImageView;

    @FXML
    private Label qrPlaceholderLabel;


    // =========================================================
    // KPI
    // =========================================================

    @FXML
    private Label kpiStatusLabel;

    @FXML
    private Label kpiStatusDescriptionLabel;

    @FXML
    private Label remainingDaysLabel;

    @FXML
    private Label daysTextLabel;

    @FXML
    private Label remainingDescriptionLabel;

    @FXML
    private Label kpiStartDateLabel;

    @FXML
    private Label kpiEndDateLabel;

    @FXML
    private Label endDateDescriptionLabel;

    @FXML
    private Label sessionsUsedLabel;

    @FXML
    private Label sessionsRemainingLabel;

    @FXML
    private Label sessionsDescriptionLabel;

    @FXML
    private ProgressBar sessionsProgressBar;


    // =========================================================
    // PERSONAL INFORMATION
    // =========================================================

    @FXML
    private Label phoneLabel;

    @FXML
    private Label whatsappLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private Label genderLabel;

    @FXML
    private Label birthDateLabel;


    // =========================================================
    // MEMBERSHIP INFORMATION
    // =========================================================

    @FXML
    private Label membershipLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label startDateLabel;

    @FXML
    private Label endDateLabel;

    @FXML
    private Label membershipRemainingLabel;


    // =========================================================
    // NOTES
    // =========================================================

    @FXML
    private Label notesLabel;


    // =========================================================
    // INTERNAL DATA
    // =========================================================

    private Member member;

    private ResourceBundle bundle;

    private boolean memberEdited = false;

    private final MemberMembershipRepository memberMembershipRepository =
            new MemberMembershipRepository();

    private final CheckInRepository checkInRepository =
            new CheckInRepository();


    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy"
            );


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        bundle =
                LanguageManager.getBundle();


        if (daysTextLabel != null) {

            daysTextLabel.setText(
                    LanguageManager.isArabic()
                            ? "يوم"
                            : "Days"
            );
        }
    }


    // =========================================================
    // SET MEMBER
    // =========================================================

    public void setMember(
            Member member
    ) {

        if (member == null) {
            return;
        }


        this.member =
                member;


        populateMemberData();
    }


    // =========================================================
    // POPULATE SCREEN
    // =========================================================

    private void populateMemberData() {

        if (member == null) {
            return;
        }


        // =====================================================
        // PROFILE
        // =====================================================

        memberNameLabel.setText(
                displayValue(
                        member.getName()
                )
        );


        memberCodeLabel.setText(
                displayValue(
                        member.getMemberCode()
                )
        );


        String translatedMembership =
                translateMembership(
                        member.getMembership()
                );


        String translatedStatus =
                translateStatus(
                        member.getStatus()
                );


        membershipBadgeLabel.setText(
                translatedMembership
        );


        statusBadgeLabel.setText(
                translatedStatus
        );


        updateStatusStyle(
                statusBadgeLabel,
                member.getStatus()
        );


        // =====================================================
        // PHOTO
        // =====================================================

        loadMemberPhoto();


        // =====================================================
        // QR
        // =====================================================

        generateQrCode();


        // =====================================================
        // KPI
        // =====================================================

        populateKpis(
                translatedStatus
        );

        populateTrainingSessions();


        // =====================================================
        // PERSONAL DATA
        // =====================================================

        phoneLabel.setText(
                displayValue(
                        member.getPhone()
                )
        );


        whatsappLabel.setText(
                displayValue(
                        member.getWhatsapp()
                )
        );


        emailLabel.setText(
                displayValue(
                        member.getEmail()
                )
        );


        genderLabel.setText(
                translateGender(
                        member.getGender()
                )
        );


        birthDateLabel.setText(
                formatDate(
                        member.getBirthDate()
                )
        );


        // =====================================================
        // MEMBERSHIP DATA
        // =====================================================

        membershipLabel.setText(
                translatedMembership
        );


        statusLabel.setText(
                translatedStatus
        );


        startDateLabel.setText(
                formatDate(
                        member.getStartDate()
                )
        );


        endDateLabel.setText(
                formatDate(
                        member.getEndDate()
                )
        );


        membershipRemainingLabel.setText(
                formatRemainingDays()
        );


        // =====================================================
        // NOTES
        // =====================================================

        notesLabel.setText(
                displayValue(
                        member.getNotes()
                )
        );
    }


    // =========================================================
    // KPI
    // =========================================================

    private void populateKpis(
            String translatedStatus
    ) {

        kpiStatusLabel.setText(
                translatedStatus
        );


        updateStatusStyle(
                kpiStatusLabel,
                member.getStatus()
        );


        long remainingDays =
                member.getRemainingDays();


        remainingDaysLabel.setText(
                String.valueOf(
                        remainingDays
                )
        );


        kpiStartDateLabel.setText(
                formatDate(
                        member.getStartDate()
                )
        );


        kpiEndDateLabel.setText(
                formatDate(
                        member.getEndDate()
                )
        );


        if (member.isExpired()) {

            kpiStatusDescriptionLabel.setText(
                    LanguageManager.isArabic()
                            ? "انتهى الاشتراك"
                            : "Membership expired"
            );


            remainingDescriptionLabel.setText(
                    LanguageManager.isArabic()
                            ? "يحتاج إلى تجديد"
                            : "Renewal required"
            );


            endDateDescriptionLabel.setText(
                    LanguageManager.isArabic()
                            ? "تاريخ انتهاء الاشتراك"
                            : "Membership expiry"
            );

        } else {

            kpiStatusDescriptionLabel.setText(
                    LanguageManager.isArabic()
                            ? "العضوية الحالية"
                            : "Current membership"
            );


            remainingDescriptionLabel.setText(
                    LanguageManager.isArabic()
                            ? "حتى انتهاء الاشتراك"
                            : "Until membership expiry"
            );


            endDateDescriptionLabel.setText(
                    LanguageManager.isArabic()
                            ? "تاريخ انتهاء الاشتراك"
                            : "Membership expiry"
            );
        }
    }


    // =========================================================
    // TRAINING SESSIONS KPI
    // =========================================================

    private void populateTrainingSessions() {

        if (sessionsUsedLabel == null
                || sessionsRemainingLabel == null
                || sessionsDescriptionLabel == null
                || sessionsProgressBar == null
                || member == null) {
            return;
        }

        sessionsUsedLabel.setText("-");
        sessionsRemainingLabel.setText("-");
        sessionsDescriptionLabel.setText(
                LanguageManager.isArabic()
                        ? "لا يوجد اشتراك حالي"
                        : "No current membership"
        );
        sessionsProgressBar.setProgress(0.0);

        try {
            var currentMembership =
                    memberMembershipRepository.findCurrentMembershipByMemberId(
                            member.getId()
                    );

            if (currentMembership.isEmpty()) {
                return;
            }

            MemberMembership membership = currentMembership.get();

            int allowedSessions =
                    Math.max(membership.getAllowedSessions(), 0);

            int usedSessions =
                    checkInRepository.countByMembershipId(
                            membership.getId()
                    );

            int remainingSessions =
                    Math.max(allowedSessions - usedSessions, 0);

            sessionsUsedLabel.setText(
                    usedSessions + " / " + allowedSessions
            );

            sessionsRemainingLabel.setText(
                    LanguageManager.isArabic()
                            ? "متبقي " + remainingSessions + " جلسة"
                            : remainingSessions + " sessions remaining"
            );

            if (allowedSessions > 0) {
                double progress =
                        Math.min(
                                (double) usedSessions / allowedSessions,
                                1.0
                        );

                sessionsProgressBar.setProgress(progress);

                int percentage =
                        (int) Math.round(progress * 100.0);

                sessionsDescriptionLabel.setText(
                        LanguageManager.isArabic()
                                ? "تم استخدام " + percentage + "% من جلسات الخطة"
                                : percentage + "% of plan sessions used"
                );
            } else {
                sessionsProgressBar.setProgress(0.0);
                sessionsDescriptionLabel.setText(
                        LanguageManager.isArabic()
                                ? "عدد الجلسات غير مضبوط للاشتراك"
                                : "Session limit is not configured"
                );
            }

        } catch (Exception exception) {
            sessionsUsedLabel.setText("-");
            sessionsRemainingLabel.setText("-");
            sessionsDescriptionLabel.setText(
                    LanguageManager.isArabic()
                            ? "تعذر تحميل بيانات التمارين"
                            : "Unable to load training sessions"
            );
            sessionsProgressBar.setProgress(0.0);
            exception.printStackTrace();
        }
    }


    // =========================================================
    // PHOTO
    // =========================================================

    private void loadMemberPhoto() {

        String path =
                member.getPhotoPath();


        if (path == null
                || path.isBlank()) {

            showPhotoPlaceholder();

            return;
        }


        try {

            File file =
                    new File(
                            path
                    );


            if (!file.exists()) {

                showPhotoPlaceholder();

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


            photoPlaceholderLabel.setVisible(
                    false
            );


            photoPlaceholderLabel.setManaged(
                    false
            );


        } catch (Exception exception) {

            showPhotoPlaceholder();

            exception.printStackTrace();
        }
    }


    private void showPhotoPlaceholder() {

        memberImageView.setImage(
                null
        );


        photoPlaceholderLabel.setVisible(
                true
        );


        photoPlaceholderLabel.setManaged(
                true
        );
    }


    // =========================================================
    // QR
    // =========================================================

    private void generateQrCode() {

        String code =
                member.getMemberCode();


        if (code == null
                || code.isBlank()) {

            showQrPlaceholder();

            return;
        }


        try {

            Image qr =
                    QrCodeGenerator.generate(
                            code,
                            220,
                            220
                    );


            qrCodeImageView.setImage(
                    qr
            );


            qrPlaceholderLabel.setVisible(
                    false
            );


            qrPlaceholderLabel.setManaged(
                    false
            );


        } catch (Exception exception) {

            showQrPlaceholder();

            exception.printStackTrace();
        }
    }


    private void showQrPlaceholder() {

        qrCodeImageView.setImage(
                null
        );


        qrPlaceholderLabel.setVisible(
                true
        );


        qrPlaceholderLabel.setManaged(
                true
        );
    }


    // =========================================================
    // STATUS STYLE
    // =========================================================

    private void updateStatusStyle(
            Label label,
            String status
    ) {

        label.getStyleClass().removeAll(
                "member-status-active",
                "member-status-expired",
                "member-status-inactive",
                "member-status-suspended"
        );


        if (status == null) {
            return;
        }


        switch (status) {

            case "Active" ->
                    label.getStyleClass()
                            .add(
                                    "member-status-active"
                            );


            case "Expired" ->
                    label.getStyleClass()
                            .add(
                                    "member-status-expired"
                            );


            case "Suspended" ->
                    label.getStyleClass()
                            .add(
                                    "member-status-suspended"
                            );


            default ->
                    label.getStyleClass()
                            .add(
                                    "member-status-inactive"
                            );
        }
    }


    // =========================================================
    // TRANSLATIONS
    // =========================================================

    private String translateMembership(
            String membership
    ) {

        if (membership == null
                || membership.isBlank()) {

            return "-";
        }


        return switch (membership) {

            case "Monthly" ->
                    bundle.getString(
                            "membership.monthly"
                    );

            case "3 Months" ->
                    bundle.getString(
                            "membership.threeMonths"
                    );

            case "6 Months" ->
                    bundle.getString(
                            "membership.halfYear"
                    );

            case "Yearly" ->
                    bundle.getString(
                            "membership.yearly"
                    );

            default ->
                    membership;
        };
    }


    private String translateStatus(
            String status
    ) {

        if (status == null
                || status.isBlank()) {

            return "-";
        }


        return switch (status) {

            case "Active" ->
                    bundle.getString(
                            "status.active"
                    );

            case "Expired" ->
                    bundle.getString(
                            "status.expired"
                    );

            case "Inactive" ->
                    bundle.getString(
                            "status.inactive"
                    );

            case "Suspended" ->
                    bundle.getString(
                            "status.suspended"
                    );

            default ->
                    status;
        };
    }


    private String translateGender(
            String gender
    ) {

        if (gender == null
                || gender.isBlank()) {

            return "-";
        }


        return switch (gender) {

            case "Male" ->
                    bundle.getString(
                            "gender.male"
                    );

            case "Female" ->
                    bundle.getString(
                            "gender.female"
                    );

            default ->
                    gender;
        };
    }


    // =========================================================
    // FORMATTERS
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


    private String formatRemainingDays() {

        long days =
                member.getRemainingDays();


        if (LanguageManager.isArabic()) {

            return days
                    + " يوم";
        }


        return days
                + (days == 1
                ? " Day"
                : " Days");
    }


    private String displayValue(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "-";
        }


        return value.trim();
    }


    // =========================================================
    // EDIT MEMBER
    // =========================================================

    @FXML
    private void editMember() {

        if (member == null) {
            return;
        }


        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass()
                                    .getResource(
                                            "/fxml/add-member.fxml"
                                    ),
                            LanguageManager.getBundle()
                    );


            Parent root =
                    loader.load();


            AddMemberController controller =
                    loader.getController();


            controller.setMemberForEdit(
                    member
            );


            if (LanguageManager.isArabic()) {

                root.setNodeOrientation(
                        NodeOrientation.RIGHT_TO_LEFT
                );

                root.getStyleClass()
                        .add(
                                "arabic"
                        );

            } else {

                root.setNodeOrientation(
                        NodeOrientation.LEFT_TO_RIGHT
                );

                root.getStyleClass()
                        .add(
                                "english"
                        );
            }


            Scene scene =
                    new Scene(
                            root
                    );


            var css =
                    getClass()
                            .getResource(
                                    "/css/app.css"
                            );


            if (css != null) {

                scene.getStylesheets()
                        .add(
                                css.toExternalForm()
                        );
            }


            Stage editStage =
                    new Stage();


            editStage.setTitle(
                    bundle.getString(
                            "members.editWindowTitle"
                    )
            );


            editStage.setScene(
                    scene
            );


            editStage.initModality(
                    Modality.APPLICATION_MODAL
            );


            Stage currentStage =
                    getStage();


            editStage.initOwner(
                    currentStage
            );


            editStage.setResizable(true);
            editStage.setMaximized(true);

            editStage.showAndWait();


            Member editedMember =
                    controller.getNewMember();


            if (editedMember != null) {

                /*
                 * Keep the edited result.
                 *
                 * MembersController will retrieve this after
                 * the details window closes and replace the
                 * original member in the list.
                 */

                this.member =
                        editedMember;


                memberEdited =
                        true;


                populateMemberData();
            }


        } catch (IOException exception) {

            exception.printStackTrace();
        }
    }


    // =========================================================
    // RESULT
    // =========================================================

    public boolean isMemberEdited() {

        return memberEdited;
    }


    public Member getMember() {

        return member;
    }


    // =========================================================
    // CLOSE
    // =========================================================

    @FXML
    private void closeWindow() {

        getStage().close();
    }


    private Stage getStage() {

        return (Stage) memberNameLabel
                .getScene()
                .getWindow();
    }
}
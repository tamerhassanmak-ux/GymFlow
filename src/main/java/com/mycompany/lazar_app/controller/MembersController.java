package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.Member;
import com.mycompany.lazar_app.repository.MemberRepository;
import com.mycompany.lazar_app.ui.WindowManager;
import com.mycompany.lazar_app.util.MemberCardPrinter;
import com.mycompany.lazar_app.ui.dialog.AppDialog;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;


public class MembersController {

    private final MemberRepository memberRepository = new MemberRepository();

    // =========================================================
    // TABLE
    // =========================================================

    @FXML
    private TableView<Member> membersTable;

    @FXML
    private TableColumn<Member, Integer> idColumn;

    @FXML
    private TableColumn<Member, String> nameColumn;

    @FXML
    private TableColumn<Member, String> phoneColumn;

    @FXML
    private TableColumn<Member, String> membershipColumn;

    @FXML
    private TableColumn<Member, String> statusColumn;

    @FXML
    private TableColumn<Member, Void> actionsColumn;


    // =========================================================
    // SEARCH / COUNTERS
    // =========================================================

    @FXML
    private TextField searchField;

    @FXML
    private Label membersCountLabel;

    @FXML
    private Label showingLabel;


    // =========================================================
    // MEMBERS LIST
    // =========================================================

    private final ObservableList<Member> members =
            FXCollections.observableArrayList();

    private final ObservableList<Member> filteredMembers =
            FXCollections.observableArrayList();


    // =========================================================
    // LANGUAGE
    // =========================================================

    private ResourceBundle bundle;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        bundle = LanguageManager.getBundle();

        configureTable();

        configureActionsColumn();

        loadMembersFromDatabase();

        configureSearch();

        refreshMembersTable();
    }


    // =========================================================
    // CONFIGURE TABLE
    // =========================================================

    private void configureTable() {

        membersTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );

        phoneColumn.setCellValueFactory(
                new PropertyValueFactory<>("phone")
        );

        membershipColumn.setCellValueFactory(
                new PropertyValueFactory<>("membership")
        );

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );

        configureMembershipColumn();

        configureStatusColumn();
    }


    // =========================================================
    // MEMBERSHIP COLUMN
    // =========================================================

    private void configureMembershipColumn() {

        membershipColumn.setCellFactory(
                column ->
                        new TableCell<Member, String>() {

                            @Override
                            protected void updateItem(
                                    String membership,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        membership,
                                        empty
                                );

                                if (empty || membership == null) {

                                    setText(null);
                                    setGraphic(null);

                                    return;
                                }

                                setText(
                                        translateMembership(
                                                membership
                                        )
                                );
                            }
                        }
        );
    }


    // =========================================================
    // STATUS COLUMN
    // =========================================================

    private void configureStatusColumn() {

        statusColumn.setCellFactory(
                column ->
                        new TableCell<Member, String>() {

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
                                    setGraphic(null);

                                    return;
                                }

                                setText(
                                        translateStatus(
                                                status
                                        )
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
                column ->
                        new TableCell<Member, Void>() {

                            private final Button viewButton =
                                    new Button(
                                            bundle.getString(
                                                    "members.action.view"
                                            )
                                    );

                            private final Button editButton =
                                    new Button(
                                            bundle.getString(
                                                    "members.action.edit"
                                            )
                                    );

                            private final Button previewCardButton =
                                    new Button(
                                            LanguageManager.isArabic()
                                                    ? "معاينة"
                                                    : "Preview"
                                    );

                            private final Button printCardButton =
                                    new Button(
                                            LanguageManager.isArabic()
                                                    ? "طباعة كارت"
                                                    : "Print Card"
                                    );

                            private final Button deleteButton =
                                    new Button(
                                            bundle.getString(
                                                    "members.action.delete"
                                            )
                                    );

                            private final HBox actionsBox =
                                    new HBox(
                                            6,
                                            viewButton,
                                            editButton,
                                            previewCardButton,
                                            printCardButton,
                                            deleteButton
                                    );


                            {
                                configureActionButtons();
                                configureActionEvents();
                            }


                            private void configureActionButtons() {

                                viewButton
                                        .getStyleClass()
                                        .add("member-view-button");

                                editButton
                                        .getStyleClass()
                                        .add("member-edit-button");

                                previewCardButton
                                        .getStyleClass()
                                        .add("member-preview-button");

                                previewCardButton.setStyle(
                                        "-fx-background-color: #e0f2fe;"
                                                + "-fx-text-fill: #0369a1;"
                                                + "-fx-font-weight: bold;"
                                                + "-fx-background-radius: 7;"
                                                + "-fx-padding: 7 10;"
                                                + "-fx-cursor: hand;"
                                );

                                previewCardButton.setTooltip(
                                        new Tooltip(
                                                LanguageManager.isArabic()
                                                        ? "معاينة كارت العضو قبل الطباعة"
                                                        : "Preview the membership card before printing"
                                        )
                                );

                                printCardButton
                                        .getStyleClass()
                                        .add("member-print-button");

                                printCardButton.setStyle(
                                        "-fx-background-color: #f97316;"
                                                + "-fx-text-fill: white;"
                                                + "-fx-font-weight: bold;"
                                                + "-fx-background-radius: 7;"
                                                + "-fx-padding: 7 10;"
                                                + "-fx-cursor: hand;"
                                );

                                printCardButton.setTooltip(
                                        new Tooltip(
                                                LanguageManager.isArabic()
                                                        ? "طباعة كارت العضوية المحفوظ"
                                                        : "Print the saved membership card"
                                        )
                                );

                                deleteButton
                                        .getStyleClass()
                                        .add("member-delete-button");

                                actionsBox
                                        .getStyleClass()
                                        .add("member-actions");

                                actionsBox.setAlignment(
                                        Pos.CENTER
                                );
                            }


                            private void configureActionEvents() {

                                viewButton.setOnAction(
                                        event -> {

                                            Member member =
                                                    getCurrentMember();

                                            if (member != null) {

                                                openMemberDetails(
                                                        member
                                                );
                                            }
                                        }
                                );


                                editButton.setOnAction(
                                        event -> {

                                            Member member =
                                                    getCurrentMember();

                                            if (member != null) {

                                                openEditMember(
                                                        member
                                                );
                                            }
                                        }
                                );


                                previewCardButton.setOnAction(
                                        event -> {

                                            Member member =
                                                    getCurrentMember();

                                            if (member != null) {

                                                previewMemberCard(
                                                        member
                                                );
                                            }
                                        }
                                );


                                printCardButton.setOnAction(
                                        event -> {

                                            Member member =
                                                    getCurrentMember();

                                            if (member != null) {

                                                printMemberCard(
                                                        member
                                                );
                                            }
                                        }
                                );


                                deleteButton.setOnAction(
                                        event -> {

                                            Member member =
                                                    getCurrentMember();

                                            if (member != null) {

                                                deleteMember(
                                                        member
                                                );
                                            }
                                        }
                                );
                            }


                            private Member getCurrentMember() {

                                int index = getIndex();

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

                                } else {

                                    setGraphic(
                                            actionsBox
                                    );
                                }
                            }
                        }
        );
    }


    // =========================================================
    // PREVIEW SAVED MEMBER CARD
    // =========================================================

    private void previewMemberCard(
            Member member
    ) {

        Window owner =
                membersTable != null
                        && membersTable.getScene() != null
                        ? membersTable.getScene().getWindow()
                        : null;


        if (!MemberCardPrinter.isSavedMember(
                member
        )) {

            AppDialog.warning(
                    owner,
                    LanguageManager.isArabic()
                            ? "لا يمكن عرض المعاينة"
                            : "Preview Unavailable",
                    LanguageManager.isArabic()
                            ? "يجب حفظ العضو أولاً والحصول على كود عضوية قبل معاينة الكارت."
                            : "Save the member and generate a membership code before previewing the card."
            );

            return;
        }


        boolean arabic =
                LanguageManager.isArabic();


        Stage previewStage =
                new Stage();

        previewStage.setTitle(
                arabic
                        ? "معاينة كارت العضو - " + member.getName()
                        : "Member Card Preview - " + member.getName()
        );

        previewStage.initModality(
                Modality.WINDOW_MODAL
        );

        if (owner != null) {

            previewStage.initOwner(
                    owner
            );
        }


        Label titleLabel =
                new Label(
                        arabic
                                ? "معاينة كارت العضوية"
                                : "Membership Card Preview"
                );

        titleLabel.setStyle(
                "-fx-font-size: 22px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #111827;"
        );


        Label descriptionLabel =
                new Label(
                        arabic
                                ? "هذه هي النسخة التي سيتم إرسالها إلى الطابعة."
                                : "This is the card that will be sent to the printer."
                );

        descriptionLabel.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-text-fill: #64748b;"
        );


        VBox heading =
                new VBox(
                        5,
                        titleLabel,
                        descriptionLabel
                );

        heading.setAlignment(
                arabic
                        ? Pos.CENTER_RIGHT
                        : Pos.CENTER_LEFT
        );


        Parent card =
                MemberCardPrinter.createCard(
                        member,
                        arabic
                );


        Button printButton =
                new Button(
                        arabic
                                ? "طباعة الكارت"
                                : "Print Card"
                );

        printButton.setStyle(
                "-fx-background-color: #f97316;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 10 18;"
                        + "-fx-cursor: hand;"
        );

        printButton.setOnAction(
                event ->
                        printMemberCard(
                                member
                        )
        );


        Button closeButton =
                new Button(
                        arabic
                                ? "إغلاق"
                                : "Close"
                );

        closeButton.setStyle(
                "-fx-background-color: #e5e7eb;"
                        + "-fx-text-fill: #111827;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 10 18;"
                        + "-fx-cursor: hand;"
        );

        closeButton.setOnAction(
                event ->
                        previewStage.close()
        );


        HBox buttons =
                new HBox(
                        10,
                        printButton,
                        closeButton
                );

        buttons.setAlignment(
                arabic
                        ? Pos.CENTER_LEFT
                        : Pos.CENTER_RIGHT
        );


        VBox root =
                new VBox(
                        22,
                        heading,
                        card,
                        buttons
                );

        root.setPadding(
                new Insets(
                        28
                )
        );

        root.setAlignment(
                Pos.CENTER
        );

        root.setStyle(
                "-fx-background-color: #f8fafc;"
        );

        root.setNodeOrientation(
                arabic
                        ? NodeOrientation.RIGHT_TO_LEFT
                        : NodeOrientation.LEFT_TO_RIGHT
        );


        Scene scene =
                new Scene(
                        root,
                        650,
                        535
                );

        var css =
                getClass().getResource(
                        "/css/app.css"
                );

        if (css != null) {

            scene.getStylesheets().add(
                    css.toExternalForm()
            );
        }


        previewStage.setScene(
                scene
        );

        WindowManager.showResponsiveAndWait(
                previewStage,
                760.0,
                620.0
        );
    }


    // =========================================================
    // PRINT SAVED MEMBER CARD
    // =========================================================

    private void printMemberCard(
            Member member
    ) {

        if (!MemberCardPrinter.isSavedMember(
                member
        )) {

            AppDialog.warning(
                    membersTable == null
                            || membersTable.getScene() == null
                            ? null
                            : membersTable.getScene().getWindow(),
                    LanguageManager.isArabic()
                            ? "لا يمكن الطباعة"
                            : "Unable to Print",
                    LanguageManager.isArabic()
                            ? "يجب حفظ العضو والحصول على رقم عضوية قبل طباعة الكارت."
                            : "The member must be saved and have a membership code before the card can be printed."
            );

            return;
        }

        Window owner =
                membersTable != null
                        && membersTable.getScene() != null
                        ? membersTable.getScene().getWindow()
                        : null;

        MemberCardPrinter.PrintResult result =
                MemberCardPrinter.printCard(
                        owner,
                        member,
                        LanguageManager.isArabic()
                );

        if (result == null
                || result == MemberCardPrinter.PrintResult.CANCELLED) {

            return;
        }

        switch (result) {

            case PRINTED ->
                    AppDialog.info(
                            owner,
                            LanguageManager.isArabic()
                                    ? "تم إرسال الطباعة"
                                    : "Print Sent",
                            LanguageManager.isArabic()
                                    ? "تم إرسال كارت العضو إلى الطابعة بنجاح."
                                    : "The member card was sent to the printer successfully."
                    );

            case NO_PRINTER ->
                    AppDialog.warning(
                            owner,
                            LanguageManager.isArabic()
                                    ? "لا توجد طابعة"
                                    : "No Printer",
                            LanguageManager.isArabic()
                                    ? "لم يتم العثور على طابعة متاحة على النظام."
                                    : "No available printer was found on this system."
                    );

            case FAILED ->
                    AppDialog.error(
                            owner,
                            LanguageManager.isArabic()
                                    ? "فشل الطباعة"
                                    : "Print Failed",
                            LanguageManager.isArabic()
                                    ? "تعذر إتمام طباعة كارت العضو. راجع إعدادات الطابعة."
                                    : "The member card could not be printed. Check the printer settings."
                    );

            default -> {
            }
        }
    }


    // =========================================================
    // SEARCH
    // =========================================================

    private void configureSearch() {

        if (searchField == null) {
            return;
        }

        searchField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                filterMembers(
                                        newValue
                                )
                );
    }


    // =========================================================
    // FILTER MEMBERS
    // =========================================================

    private void filterMembers(
            String searchText
    ) {

        if (searchText == null
                || searchText.isBlank()) {

            refreshMembersTable();
            return;
        }

        String search =
                searchText
                        .trim()
                        .toLowerCase();

        filteredMembers.clear();

        for (Member member : members) {

            if (memberMatchesSearch(
                    member,
                    search
            )) {

                filteredMembers.add(
                        member
                );
            }
        }

        membersTable.setItems(
                filteredMembers
        );

        membersTable.refresh();

        updateCounters();
    }


    // =========================================================
    // MEMBER SEARCH MATCHER
    // =========================================================

    private boolean memberMatchesSearch(
            Member member,
            String search
    ) {

        if (member == null) {
            return false;
        }

        if (String.valueOf(
                member.getId()
        ).contains(search)) {

            return true;
        }

        if (containsIgnoreCase(
                member.getMemberCode(),
                search
        )) {

            return true;
        }

        if (containsIgnoreCase(
                member.getName(),
                search
        )) {

            return true;
        }

        if (containsIgnoreCase(
                member.getPhone(),
                search
        )) {

            return true;
        }

        if (containsIgnoreCase(
                member.getWhatsapp(),
                search
        )) {

            return true;
        }

        return containsIgnoreCase(
                member.getEmail(),
                search
        );
    }


    // =========================================================
    // CASE INSENSITIVE SEARCH
    // =========================================================

    private boolean containsIgnoreCase(
            String value,
            String search
    ) {

        if (value == null || search == null) {
            return false;
        }

        return value
                .toLowerCase()
                .contains(search);
    }


    // =========================================================
    // REFRESH TABLE
    // =========================================================

    private void refreshMembersTable() {

        membersTable.setItems(
                members
        );

        membersTable.refresh();

        updateCounters();
    }


    // =========================================================
    // COUNTERS
    // =========================================================

    private void updateCounters() {

        if (membersCountLabel != null) {

            membersCountLabel.setText(
                    String.valueOf(
                            members.size()
                    )
            );
        }

        if (showingLabel != null) {

            int visible =
                    membersTable.getItems() == null
                            ? 0
                            : membersTable
                            .getItems()
                            .size();

            showingLabel.setText(
                    visible
                            + " / "
                            + members.size()
            );
        }
    }


    // =========================================================
    // LOAD MEMBERS FROM DATABASE
    // =========================================================

    private void loadMembersFromDatabase() {

        try {
            members.setAll(memberRepository.findAll());
            refreshAfterChange();
        } catch (SQLException exception) {
            System.err.println("Unable to load members from database.");
            exception.printStackTrace();
            showDatabaseError(
                    LanguageManager.isArabic()
                            ? "تعذر تحميل بيانات الأعضاء من قاعدة البيانات."
                            : "Unable to load members from database."
            );
        }
    }


    // =========================================================
    // TRANSLATE MEMBERSHIP
    // =========================================================

    private String translateMembership(
            String membership
    ) {

        if (membership == null
                || membership.isBlank()) {

            return "";
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


    // =========================================================
    // TRANSLATE STATUS
    // =========================================================

    private String translateStatus(
            String status
    ) {

        if (status == null
                || status.isBlank()) {

            return "";
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


    // =========================================================
    // OPEN ADD MEMBER
    // =========================================================

    @FXML
    private void openAddMember() {

        try {

            FXMLLoader loader =
                    createLoader(
                            "/fxml/add-member.fxml"
                    );

            Parent root =
                    loader.load();

            AddMemberController controller =
                    loader.getController();

            controller.prepareForAdd();

            Stage stage =
                    createModalStage(
                            root,
                            bundle.getString(
                                    "members.addWindowTitle"
                            )
                    );

            configureMemberFormStage(
                    stage
            );

            WindowManager.showMaximizedAndWait(
                    stage
            );


            Member newMember =
                    controller.getNewMember();

            if (newMember == null) {
                return;
            }

            try {

                /*
                 * If Print Card / Print QR was pressed while creating
                 * the member, AddMemberController has already inserted
                 * the member in order to obtain its permanent code.
                 *
                 * Only insert here when the member is still unsaved.
                 */
                if (newMember.getId() <= 0) {

                    memberRepository.insert(
                            newMember
                    );
                }

                loadMembersFromDatabase();

            } catch (SQLException exception) {
                System.err.println("Unable to save member.");
                exception.printStackTrace();

                if (isDuplicatePhoneException(exception)) {
                    showDatabaseError(
                            LanguageManager.isArabic()
                                    ? "رقم الهاتف مستخدم بالفعل لعضو آخر."
                                    : "This phone number is already used by another member."
                    );
                } else {
                    showDatabaseError(
                            LanguageManager.isArabic()
                                    ? "تعذر حفظ العضو في قاعدة البيانات."
                                    : "Unable to save member to database."
                    );
                }
            }


        } catch (IOException e) {

            showLoadError(
                    "Add Member",
                    e
            );
        }
    }


    // =========================================================
    // EDIT MEMBER
    // =========================================================

    private void openEditMember(
            Member member
    ) {

        if (member == null) {
            return;
        }

        try {

            FXMLLoader loader =
                    createLoader(
                            "/fxml/add-member.fxml"
                    );

            Parent root =
                    loader.load();

            AddMemberController controller =
                    loader.getController();

            controller.setMemberForEdit(
                    member
            );

            Stage stage =
                    createModalStage(
                            root,
                            bundle.getString(
                                    "members.editWindowTitle"
                            )
                    );

            configureMemberFormStage(
                    stage
            );

            WindowManager.showMaximizedAndWait(
                    stage
            );


            Member editedMember =
                    controller.getNewMember();

            if (editedMember == null) {
                return;
            }

            try {
                boolean updated = memberRepository.update(editedMember);
                if (!updated) {
                    showDatabaseError(
                            LanguageManager.isArabic()
                                    ? "لم يتم العثور على العضو لتحديثه."
                                    : "Member was not found for update."
                    );
                    return;
                }
                loadMembersFromDatabase();
            } catch (SQLException exception) {
                System.err.println("Unable to update member.");
                exception.printStackTrace();

                if (isDuplicatePhoneException(exception)) {
                    showDatabaseError(
                            LanguageManager.isArabic()
                                    ? "رقم الهاتف مستخدم بالفعل لعضو آخر."
                                    : "This phone number is already used by another member."
                    );
                } else {
                    showDatabaseError(
                            LanguageManager.isArabic()
                                    ? "تعذر تحديث بيانات العضو."
                                    : "Unable to update member."
                    );
                }
            }


        } catch (IOException e) {

            showLoadError(
                    "Edit Member",
                    e
            );
        }
    }

    // =========================================================
    // CONFIGURE ADD / EDIT MEMBER WINDOW
    // =========================================================

    private void configureMemberFormStage(Stage stage) {

        if (stage == null) {
            return;
        }

        /*
         * Open Add/Edit Member at the maximum usable screen size.
         * setMaximized(true) lets the operating system use the full
         * visual desktop while respecting the taskbar / desktop panel.
         */
        stage.setResizable(true);
        stage.setMinWidth(850.0);
        stage.setMinHeight(600.0);
        stage.setMaximized(true);
    }
    // =========================================================
    // CENTER STAGE ON OWNER
    // =========================================================

    private void centerStageOnOwner(
            Stage stage
    ) {

        if (stage == null) {
            return;
        }


        Window owner =
                stage.getOwner();


        if (owner != null
                && owner.isShowing()) {

            double x =
                    owner.getX()
                            + (
                            owner.getWidth()
                                    - stage.getWidth()
                    ) / 2.0;


            double y =
                    owner.getY()
                            + (
                            owner.getHeight()
                                    - stage.getHeight()
                    ) / 2.0;


            stage.setX(
                    x
            );

            stage.setY(
                    y
            );

        } else {

            stage.centerOnScreen();
        }
    }


    // =========================================================
    // DELETE MEMBER
    // =========================================================

    private void deleteMember(
            Member member
    ) {

        if (member == null) {
            return;
        }

        Window owner =
                membersTable != null
                        && membersTable.getScene() != null
                        ? membersTable.getScene().getWindow()
                        : null;

        /*
         * IMPORTANT:
         * Never create a raw Alert here.
         *
         * AppDialog uses the central WindowManager policy, so the confirmation
         * opens at one stable size and is centered over the owner window.
         */
        boolean confirmed =
                AppDialog.confirmDelete(
                        owner,
                        member
                );

        if (!confirmed) {
            return;
        }

        try {

            boolean deleted =
                    memberRepository.delete(
                            member.getId()
                    );

            if (!deleted) {

                showDatabaseError(
                        LanguageManager.isArabic()
                                ? "لم يتم العثور على العضو لحذفه."
                                : "Member was not found for deletion."
                );

                return;
            }

            loadMembersFromDatabase();

            AppDialog.success(
                    owner,
                    LanguageManager.isArabic()
                            ? "تم الحذف"
                            : "Deleted",
                    LanguageManager.isArabic()
                            ? "تم حذف العضو بنجاح."
                            : "The member was deleted successfully."
            );

        } catch (SQLException exception) {

            System.err.println(
                    "Unable to delete member."
            );

            exception.printStackTrace();

            showDatabaseError(
                    LanguageManager.isArabic()
                            ? "تعذر حذف العضو من قاعدة البيانات."
                            : "Unable to delete member."
            );
        }
    }


    // =========================================================
    // VIEW MEMBER DETAILS
    // =========================================================

    private void openMemberDetails(Member member) {

        if (member == null) {
            return;
        }

        try {

            FXMLLoader loader =
                    createLoader("/fxml/member-details.fxml");

            Parent root =
                    loader.load();

            MemberDetailsController controller =
                    loader.getController();

            controller.setMember(member);

            Stage stage =
                    createModalStage(
                            root,
                            bundle.getString(
                                    "members.detailsWindowTitle"
                            )
                    );

            configureMemberDetailsStage(stage);

            /*
             * showAndWait() must be called only once.
             * The window dimensions and position are already calculated
             * before the stage becomes visible.
             */
            WindowManager.showMaximizedAndWait(
                    stage
            );

            if (controller.isMemberEdited()) {

                /*
                 * MemberDetailsController persists edits immediately.
                 * The list only needs to reload the latest stored data.
                 */
                loadMembersFromDatabase();
            }

        } catch (IOException exception) {

            showLoadError(
                    "Member Details",
                    exception
            );
        }
    }


    // =========================================================
    // CONFIGURE MEMBER DETAILS WINDOW
    // =========================================================

    private void configureMemberDetailsStage(Stage stage) {

        if (stage == null) {
            return;
        }

        /*
         * Always open Member Details maximized.
         * This removes the inconsistent fixed-size behaviour and gives
         * the responsive FXML the complete usable screen area.
         */
        stage.setResizable(true);
        stage.setMinWidth(760.0);
        stage.setMinHeight(560.0);
        stage.setMaximized(true);
    }



    // =========================================================
    // TRANSLATE GENDER
    // =========================================================

    private String translateGender(
            String gender
    ) {

        if (gender == null
                || gender.isBlank()) {

            return "";
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
    // SAFE DISPLAY VALUE
    // =========================================================

    private String safeValue(
            Object value
    ) {

        if (value == null) {
            return "-";
        }

        String text =
                value
                        .toString()
                        .trim();

        return text.isEmpty()
                ? "-"
                : text;
    }


    // =========================================================
    // CREATE FXML LOADER
    // =========================================================

    private FXMLLoader createLoader(
            String resource
    ) throws IOException {

        var url =
                getClass()
                        .getResource(
                                resource
                        );


        if (url == null) {

            throw new IOException(
                    "FXML resource not found: "
                            + resource
            );
        }


        return new FXMLLoader(
                url,
                LanguageManager.getBundle()
        );
    }


    // =========================================================
    // CREATE MODAL STAGE
    // =========================================================

    private Stage createModalStage(
            Parent root,
            String title
    ) {

        // =====================================================
        // RTL / LTR
        // =====================================================

        if (LanguageManager.isArabic()) {

            root.setNodeOrientation(
                    NodeOrientation.RIGHT_TO_LEFT
            );

            if (!root
                    .getStyleClass()
                    .contains("arabic")) {

                root
                        .getStyleClass()
                        .add("arabic");
            }

        } else {

            root.setNodeOrientation(
                    NodeOrientation.LEFT_TO_RIGHT
            );

            if (!root
                    .getStyleClass()
                    .contains("english")) {

                root
                        .getStyleClass()
                        .add("english");
            }
        }


        // =====================================================
        // SCENE
        // =====================================================

        Scene scene =
                new Scene(
                        root
                );


        // =====================================================
        // CSS
        // =====================================================

        var css =
                getClass()
                        .getResource(
                                "/css/app.css"
                        );

        if (css != null) {

            scene
                    .getStylesheets()
                    .add(
                            css.toExternalForm()
                    );
        }


        // =====================================================
        // STAGE
        // =====================================================

        Stage stage =
                new Stage();

        stage.setTitle(
                title
        );

        stage.setScene(
                scene
        );

        stage.initModality(
                Modality.APPLICATION_MODAL
        );


        // =====================================================
        // OWNER
        // =====================================================

        if (membersTable != null
                && membersTable.getScene() != null
                && membersTable
                .getScene()
                .getWindow() != null) {

            stage.initOwner(
                    membersTable
                            .getScene()
                            .getWindow()
            );
        }


        return stage;
    }


    // =========================================================
    // REFRESH AFTER CHANGE
    // =========================================================

    private void refreshAfterChange() {

        String search =
                searchField == null
                        ? ""
                        : searchField.getText();


        if (search == null
                || search.isBlank()) {

            refreshMembersTable();

        } else {

            filterMembers(
                    search
            );
        }
    }


    // =========================================================
    // SHOW LOAD ERROR
    // =========================================================

    private void showLoadError(
            String screen,
            Exception exception
    ) {

        System.err.println(
                "Error loading "
                        + screen
                        + " screen:"
        );

        if (exception != null) {
            exception.printStackTrace();
        }

        Window owner =
                membersTable != null
                        && membersTable.getScene() != null
                        ? membersTable.getScene().getWindow()
                        : null;

        AppDialog.error(
                owner,
                LanguageManager.isArabic()
                        ? "تعذر فتح الشاشة"
                        : "Unable to open screen",
                LanguageManager.isArabic()
                        ? "حدث خطأ أثناء فتح الشاشة. راجع سجل التشغيل للتفاصيل."
                        : "An error occurred while opening the screen. Check the application log for details."
        );
    }


    // =========================================================
    // DUPLICATE PHONE DATABASE ERROR
    // =========================================================

    private boolean isDuplicatePhoneException(
            SQLException exception
    ) {

        if (exception == null) {
            return false;
        }

        String message =
                exception.getMessage();

        if (message == null) {
            return false;
        }

        String normalized =
                message.toLowerCase();

        return normalized.contains("duplicate member phone")
                || normalized.contains("unique constraint failed: members.phone")
                || normalized.contains("idx_members_phone_unique");
    }


    // =========================================================
    // DATABASE ERROR
    // =========================================================

    private void showDatabaseError(
            String message
    ) {

        Window owner =
                membersTable != null
                        && membersTable.getScene() != null
                        ? membersTable.getScene().getWindow()
                        : null;

        AppDialog.error(
                owner,
                LanguageManager.isArabic()
                        ? "خطأ في قاعدة البيانات"
                        : "Database Error",
                message
        );
    }

}
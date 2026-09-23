package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.Member;
import com.mycompany.lazar_app.repository.MemberRepository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.layout.HBox;

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

            stage.showAndWait();


            Member newMember =
                    controller.getNewMember();

            if (newMember == null) {
                return;
            }

            try {
                memberRepository.insert(newMember);
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

            stage.showAndWait();


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


        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        confirmation.setTitle(
                bundle.getString(
                        "members.delete.title"
                )
        );


        confirmation.setHeaderText(
                bundle.getString(
                        "members.delete.header"
                )
        );


        confirmation.setContentText(
                bundle.getString(
                        "members.delete.message"
                )
                        + "\n\n"
                        + member.getName()
                        + "\n"
                        + member.getMemberCode()
        );


        if (membersTable.getScene() != null
                && membersTable
                .getScene()
                .getWindow() != null) {

            confirmation.initOwner(
                    membersTable
                            .getScene()
                            .getWindow()
            );
        }


        Optional<ButtonType> result =
                confirmation.showAndWait();


        if (result.isPresent()
                && result.get()
                == ButtonType.OK) {

            try {
                boolean deleted = memberRepository.delete(member.getId());
                if (!deleted) {
                    showDatabaseError(
                            LanguageManager.isArabic()
                                    ? "لم يتم العثور على العضو لحذفه."
                                    : "Member was not found for deletion."
                    );
                    return;
                }
                loadMembersFromDatabase();
            } catch (SQLException exception) {
                System.err.println("Unable to delete member.");
                exception.printStackTrace();
                showDatabaseError(
                        LanguageManager.isArabic()
                                ? "تعذر حذف العضو من قاعدة البيانات."
                                : "Unable to delete member."
                );
            }
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
            stage.showAndWait();

            if (controller.isMemberEdited()) {

                Member editedMember =
                        controller.getMember();

                int index =
                        members.indexOf(member);

                if (index >= 0
                        && editedMember != null) {

                    try {
                        memberRepository.update(editedMember);
                        loadMembersFromDatabase();
                    } catch (SQLException exception) {
                        System.err.println("Unable to update member from details screen.");
                        exception.printStackTrace();
                        showDatabaseError(
                                LanguageManager.isArabic()
                                        ? "تعذر تحديث بيانات العضو."
                                        : "Unable to update member."
                        );
                    }
                }
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

        exception.printStackTrace();


        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );


        alert.setTitle(
                "Lazar Gym"
        );


        alert.setHeaderText(
                LanguageManager.isArabic()
                        ? "تعذر فتح الشاشة"
                        : "Unable to open screen"
        );


        alert.setContentText(
                LanguageManager.isArabic()
                        ? "حدث خطأ أثناء فتح الشاشة. راجع سجل التشغيل للتفاصيل."
                        : "An error occurred while opening the screen. Check the application log for details."
        );


        if (membersTable != null
                && membersTable.getScene() != null
                && membersTable
                .getScene()
                .getWindow() != null) {

            alert.initOwner(
                    membersTable
                            .getScene()
                            .getWindow()
            );
        }


        alert.showAndWait();
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

    private void showDatabaseError(String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lazar Gym");
        alert.setHeaderText(
                LanguageManager.isArabic()
                        ? "خطأ في قاعدة البيانات"
                        : "Database Error"
        );
        alert.setContentText(message);

        if (membersTable != null
                && membersTable.getScene() != null
                && membersTable.getScene().getWindow() != null) {
            alert.initOwner(membersTable.getScene().getWindow());
        }

        alert.showAndWait();
    }

}
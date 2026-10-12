package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.LanguageManager;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class DashboardController {

    // ==========================================
    // CONTENT AREA
    // ==========================================

    @FXML
    private StackPane contentArea;

    @FXML
    private ImageView gymLogoImageView;

    @FXML
    private ImageView systemLogoImageView;


    // ==========================================
    // SIDEBAR BUTTONS
    // ==========================================

    @FXML
    private Button dashboardButton;

    @FXML
    private Button membersButton;

    @FXML
    private Button membershipsButton;

    // NEW
    @FXML
    private Button membershipPlansButton;

    @FXML
    private Button checkInButton;

    @FXML
    private Button attendanceHistoryButton;

    @FXML
    private Button paymentsButton;

    @FXML
    private Button revenuesButton;

    @FXML
    private Button expensesButton;

    @FXML
    private Button reportsButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Button languageButton;


    // ==========================================
    // INITIALIZE
    // ==========================================

    @FXML
    public void initialize() {

        loadLogos();

        openDashboard();
    }


    // ==========================================
    // LOGOS
    // ==========================================

    private void loadLogos() {

        loadImage(
                gymLogoImageView,
                "/com/mycompany/lazar_app/images/logo.png"
        );

        loadImage(
                systemLogoImageView,
                "/com/mycompany/lazar_app/images/systemlogo.png"
        );
    }


    private void loadImage(
            ImageView imageView,
            String resourcePath
    ) {

        if (imageView == null) {
            return;
        }

        var resource =
                getClass().getResource(
                        resourcePath
                );

        if (resource == null) {

            System.err.println(
                    "Image resource not found: "
                            + resourcePath
            );

            return;
        }

        imageView.setImage(
                new Image(
                        resource.toExternalForm(),
                        true
                )
        );
    }


    // ==========================================
    // GENERAL VIEW LOADER
    // ==========================================

    private boolean loadView(String fxmlFile) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/" + fxmlFile
                            ),
                            LanguageManager.getBundle()
                    );


            Node view =
                    loader.load();


            // ==================================
            // PAGE DIRECTION
            // ==================================

            view.setNodeOrientation(
                    LanguageManager.isArabic()
                            ? NodeOrientation.RIGHT_TO_LEFT
                            : NodeOrientation.LEFT_TO_RIGHT
            );


            // ==================================
            // DISPLAY VIEW
            // ==================================

            contentArea
                    .getChildren()
                    .setAll(view);


            return true;


        } catch (IOException exception) {

            System.err.println(
                    "Error loading view: "
                    + fxmlFile
            );


            exception.printStackTrace();


            return false;
        }
    }


    // ==========================================
    // ACTIVE NAVIGATION
    // ==========================================

    private void setActiveButton(
            Button activeButton
    ) {

        clearActiveButton(
                dashboardButton
        );

        clearActiveButton(
                membersButton
        );

        clearActiveButton(
                membershipsButton
        );

        clearActiveButton(
                membershipPlansButton
        );

        clearActiveButton(
                checkInButton
        );

        clearActiveButton(attendanceHistoryButton);

        clearActiveButton(
                paymentsButton
        );

        clearActiveButton(
                revenuesButton
        );

        clearActiveButton(
                expensesButton
        );

        clearActiveButton(
                reportsButton
        );

        clearActiveButton(
                settingsButton
        );


        if (activeButton != null) {

            if (!activeButton
                    .getStyleClass()
                    .contains(
                            "sidebar-button-active"
                    )) {

                activeButton
                        .getStyleClass()
                        .add(
                                "sidebar-button-active"
                        );
            }
        }
    }


    // ==========================================
    // REMOVE ACTIVE CLASS
    // ==========================================

    private void clearActiveButton(
            Button button
    ) {

        if (button == null) {

            return;
        }


        button
                .getStyleClass()
                .remove(
                        "sidebar-button-active"
                );
    }


    // ==========================================
    // LANGUAGE SWITCH
    // ==========================================

    @FXML
    private void switchLanguage() {

        LanguageManager.toggleLanguage();

        reloadApplication();
    }


    // ==========================================
    // RELOAD MAIN WINDOW
    // ==========================================

    private void reloadApplication() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/dashboard.fxml"
                            ),
                            LanguageManager.getBundle()
                    );


            Parent root =
                    loader.load();


            root.setNodeOrientation(
                    LanguageManager.isArabic()
                            ? NodeOrientation.RIGHT_TO_LEFT
                            : NodeOrientation.LEFT_TO_RIGHT
            );


            Scene scene =
                    contentArea.getScene();


            if (scene != null) {

                scene.setRoot(
                        root
                );
            }


        } catch (IOException exception) {

            System.err.println(
                    "Error changing application language."
            );


            exception.printStackTrace();
        }
    }


    // ==========================================
    // DASHBOARD
    // ==========================================

    @FXML
    private void openDashboard() {

        if (loadView(
                "dashboard-content.fxml"
        )) {

            setActiveButton(
                    dashboardButton
            );
        }
    }


    // ==========================================
    // MEMBERS
    // ==========================================

    @FXML
    private void openMembers() {

        if (loadView(
                "members.fxml"
        )) {

            setActiveButton(
                    membersButton
            );
        }
    }


    // ==========================================
    // MEMBERSHIPS
    // ==========================================

    @FXML
    private void openMemberships() {

        if (loadView(
                "memberships.fxml"
        )) {

            setActiveButton(
                    membershipsButton
            );
        }
    }


    // ==========================================
    // MEMBERSHIP PLANS
    // ==========================================

    @FXML
    private void openMembershipPlans() {

        if (loadView(
                "membership-plans.fxml"
        )) {

            setActiveButton(
                    membershipPlansButton
            );
        }
    }


    // ==========================================
    // CHECK-IN
    // ==========================================

    @FXML
    private void openCheckIn() {

        if (loadView(
                "checkin.fxml"
        )) {

            setActiveButton(
                    checkInButton
            );
        }
    }


    @FXML
    private void openAttendanceHistory() {
        if (loadView("attendance-history.fxml")) {
            setActiveButton(attendanceHistoryButton);
        }
    }

    // ==========================================
    // PAYMENTS
    // ==========================================

    @FXML
    private void openPayments() {

        if (loadView(
                "payments.fxml"
        )) {

            setActiveButton(
                    paymentsButton
            );
        }
    }


    // ==========================================
    // REVENUES
    // ==========================================

    @FXML
    private void openRevenues() {

        if (loadView(
                "revenues.fxml"
        )) {

            setActiveButton(
                    revenuesButton
            );
        }
    }


    // ==========================================
    // EXPENSES
    // ==========================================

    @FXML
    private void openExpenses() {

        if (loadView(
                "expenses.fxml"
        )) {

            setActiveButton(
                    expensesButton
            );
        }
    }


    // ==========================================
    // REPORTS
    // ==========================================

    @FXML
    private void openReports() {

        if (loadView(
                "reports.fxml"
        )) {

            setActiveButton(
                    reportsButton
            );
        }
    }


    // ==========================================
    // SETTINGS
    // ==========================================

    @FXML
    private void openSettings() {

        if (loadView(
                "settings.fxml"
        )) {

            setActiveButton(
                    settingsButton
            );
        }
    }
}
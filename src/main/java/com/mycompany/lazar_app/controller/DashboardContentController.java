package com.mycompany.lazar_app;

import com.mycompany.lazar_app.model.dashboard.DashboardExpiration;
import com.mycompany.lazar_app.model.dashboard.DashboardMember;
import com.mycompany.lazar_app.model.dashboard.DashboardMembershipData;
import com.mycompany.lazar_app.model.dashboard.DashboardMonthlyData;
import com.mycompany.lazar_app.model.dashboard.DashboardPayment;
import com.mycompany.lazar_app.model.dashboard.DashboardSummary;
import com.mycompany.lazar_app.service.DashboardService;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;


public class DashboardContentController {

    // =========================================================
    // LIVE DASHBOARD INSTANCE
    // =========================================================

    /**
     * Weak reference to the dashboard controller that is currently
     * attached to the JavaFX scene. Using a WeakReference avoids
     * keeping an old dashboard view alive after navigation.
     */
    private static WeakReference<DashboardContentController> currentInstance =
            new WeakReference<>(null);


    // =========================================================
    // SERVICES
    // =========================================================

    private final DashboardService dashboardService =
            new DashboardService();

    private ResourceBundle bundle;


    // =========================================================
    // HEADER
    // =========================================================

    @FXML
    private Label currentDateLabel;


    // =========================================================
    // KPI CARDS
    // =========================================================

    @FXML
    private Label activeMembersValueLabel;

    @FXML
    private Label memberGrowthPercentageLabel;

    @FXML
    private Label todayCheckInsValueLabel;

    @FXML
    private Label expiringMembershipsValueLabel;

    @FXML
    private Label monthlyRevenueValueLabel;

    @FXML
    private Label revenueChangePercentageLabel;

    @FXML
    private Label activeMembersChartValueLabel;


    // =========================================================
    // CHARTS
    // =========================================================

    @FXML
    private BarChart<String, Number> revenueChart;

    @FXML
    private LineChart<String, Number> memberGrowthChart;

    @FXML
    private PieChart membershipChart;


    // =========================================================
    // DYNAMIC CONTAINERS
    // =========================================================

    @FXML
    private VBox recentMembersContainer;

    @FXML
    private VBox expirationsContainer;

    @FXML
    private VBox recentPaymentsContainer;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        // Register the controller that belongs to the currently visible
        // dashboard view. Check-in operations can then refresh this exact
        // instance instead of creating a detached controller.
        currentInstance =
                new WeakReference<>(this);

        bundle =
                LanguageManager.getBundle();

        refreshDashboard();
    }


    // =========================================================
    // LIVE KPI REFRESH
    // =========================================================

    /**
     * Refreshes the KPI cards of the dashboard that is currently loaded.
     *
     * This method is intentionally static so controllers such as
     * CheckinController can notify the visible dashboard after a successful
     * database change without creating a new DashboardContentController.
     *
     * If the dashboard is not currently loaded, there is nothing to update;
     * the next dashboard load will read the latest values from the database.
     */
    public static void refreshCurrentSummary() {

        DashboardContentController controller =
                currentInstance.get();

        if (controller == null) {
            return;
        }

        Runnable refreshTask =
                controller::refreshSummary;

        if (Platform.isFxApplicationThread()) {

            refreshTask.run();

        } else {

            Platform.runLater(
                    refreshTask
            );
        }
    }


    // =========================================================
    // LOAD / REFRESH DASHBOARD
    // =========================================================

    /**
     * Loads every dashboard section from the database.
     * Kept public because DashboardController can call it whenever
     * the dashboard view becomes active again.
     */
    public void loadDashboard() {

        updateCurrentDate();

        loadSummary();

        loadCharts();

        loadRecentMembers();

        loadUpcomingExpirations();

        loadRecentPayments();
    }


    /**
     * Safe full refresh.  A database error will not escape to the
     * JavaFX event thread and leave the view in a half-rendered state.
     */
    public void refreshDashboard() {

        bundle =
                LanguageManager.getBundle();

        try {

            loadDashboard();

        } catch (Exception exception) {

            exception.printStackTrace();

            showDashboardError();
        }
    }


    /**
     * Lightweight KPI refresh.
     * This can be called after a successful check-in/check-out so the
     * "Today Check-ins" card immediately reads the database again
     * without rebuilding all charts and lists.
     */
    public void refreshSummary() {

        try {

            loadSummary();

        } catch (Exception exception) {

            exception.printStackTrace();

            if (todayCheckInsValueLabel != null) {
                todayCheckInsValueLabel.setText("-");
            }
        }
    }


    // =========================================================
    // SUMMARY
    // =========================================================

    private void loadSummary() {

        DashboardSummary summary =
                dashboardService.getSummary();


        activeMembersValueLabel.setText(
                String.valueOf(
                        summary.getActiveMembers()
                )
        );


        activeMembersChartValueLabel.setText(
                String.valueOf(
                        summary.getActiveMembers()
                )
        );


        todayCheckInsValueLabel.setText(
                String.valueOf(
                        summary.getTodayCheckIns()
                )
        );


        expiringMembershipsValueLabel.setText(
                String.valueOf(
                        summary.getExpiringMemberships()
                )
        );


        monthlyRevenueValueLabel.setText(
                formatMoney(
                        summary.getMonthlyRevenue()
                )
        );


        updatePercentageLabel(
                revenueChangePercentageLabel,
                summary.getRevenueChangePercentage()
        );


        updatePercentageLabel(
                memberGrowthPercentageLabel,
                summary.getMemberChangePercentage()
        );
    }


    // =========================================================
    // PERCENTAGE LABEL
    // =========================================================

    private void updatePercentageLabel(
            Label label,
            double percentage
    ) {

        String prefix =
                percentage > 0
                        ? "+"
                        : "";


        label.setText(
                prefix
                + String.format(
                        Locale.US,
                        "%.1f%%",
                        percentage
                )
        );


        if (percentage > 0) {

            label.setStyle(
                    "-fx-font-size: 11px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #16A34A;"
            );

        } else if (percentage < 0) {

            label.setStyle(
                    "-fx-font-size: 11px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #DC2626;"
            );

        } else {

            label.setStyle(
                    "-fx-font-size: 11px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #64748B;"
            );
        }
    }


    // =========================================================
    // CHARTS
    // =========================================================

    private void loadCharts() {

        List<DashboardMonthlyData> monthlyData =
                dashboardService.getMonthlyData();


        loadRevenueChart(
                monthlyData
        );


        loadMemberGrowthChart(
                monthlyData
        );


        loadMembershipChart();


        /*
         * Chart nodes are created after the CSS/layout pass.
         */
        Platform.runLater(() -> {

            addBarChartLabels();

            addMemberGrowthLabels();

            addPieChartTooltips();
        });
    }


    // =========================================================
    // REVENUE / EXPENSES CHART
    // =========================================================

    private void loadRevenueChart(
            List<DashboardMonthlyData> monthlyData
    ) {

        revenueChart
                .getData()
                .clear();


        XYChart.Series<String, Number> revenue =
                new XYChart.Series<>();


        revenue.setName(
                getText(
                        "chart.revenue",
                        LanguageManager.isArabic()
                                ? "الإيرادات"
                                : "Revenue"
                )
        );


        XYChart.Series<String, Number> expenses =
                new XYChart.Series<>();


        expenses.setName(
                getText(
                        "chart.expenses",
                        LanguageManager.isArabic()
                                ? "المصروفات"
                                : "Expenses"
                )
        );


        for (DashboardMonthlyData item
                : monthlyData) {

            String month =
                    formatMonth(
                            item.getMonth()
                    );


            revenue.getData().add(
                    new XYChart.Data<>(
                            month,
                            item.getRevenue()
                    )
            );


            expenses.getData().add(
                    new XYChart.Data<>(
                            month,
                            item.getExpenses()
                    )
            );
        }


        revenueChart
                .getData()
                .addAll(
                        revenue,
                        expenses
                );
    }


    // =========================================================
    // MEMBER GROWTH CHART
    // =========================================================

    private void loadMemberGrowthChart(
            List<DashboardMonthlyData> monthlyData
    ) {

        memberGrowthChart
                .getData()
                .clear();


        XYChart.Series<String, Number> members =
                new XYChart.Series<>();


        members.setName(
                getText(
                        "chart.members",
                        LanguageManager.isArabic()
                                ? "الأعضاء"
                                : "Members"
                )
        );


        for (DashboardMonthlyData item
                : monthlyData) {

            members.getData().add(
                    new XYChart.Data<>(
                            formatMonth(
                                    item.getMonth()
                            ),
                            item.getNewMembers()
                    )
            );
        }


        memberGrowthChart
                .getData()
                .add(members);
    }


    // =========================================================
    // MEMBERSHIP DISTRIBUTION
    // =========================================================

    private void loadMembershipChart() {

        membershipChart
                .getData()
                .clear();


        List<DashboardMembershipData> data =
                dashboardService
                        .getMembershipDistribution();


        boolean arabic =
                LanguageManager.isArabic();


        for (DashboardMembershipData item
                : data) {

            if (item.getMembersCount() <= 0) {
                continue;
            }


            String name =
                    item.getDisplayName(
                            arabic
                    );


            PieChart.Data pieData =
                    new PieChart.Data(
                            name,
                            item.getMembersCount()
                    );


            membershipChart
                    .getData()
                    .add(pieData);
        }


        updatePieLabels();
    }


    // =========================================================
    // PIE LABELS
    // =========================================================

    private void updatePieLabels() {

        double total =
                membershipChart
                        .getData()
                        .stream()
                        .mapToDouble(
                                PieChart.Data::getPieValue
                        )
                        .sum();


        if (total <= 0) {
            return;
        }


        for (PieChart.Data data
                : membershipChart.getData()) {

            double value =
                    data.getPieValue();


            double percentage =
                    value
                    / total
                    * 100.0;


            String originalName =
                    data.getName();


            data.setName(
                    originalName
                    + " "
                    + (long) value
                    + " ("
                    + String.format(
                            Locale.US,
                            "%.0f",
                            percentage
                    )
                    + "%)"
            );
        }
    }


    // =========================================================
    // RECENT MEMBERS
    // =========================================================

    private void loadRecentMembers() {

        recentMembersContainer
                .getChildren()
                .clear();


        List<DashboardMember> members =
                dashboardService
                        .getRecentMembers();


        if (members.isEmpty()) {

            recentMembersContainer
                    .getChildren()
                    .add(
                            createEmptyLabel()
                    );

            return;
        }


        for (int i = 0;
                i < members.size();
                i++) {

            DashboardMember member =
                    members.get(i);


            recentMembersContainer
                    .getChildren()
                    .add(
                            createMemberRow(
                                    member
                            )
                    );


            if (i < members.size() - 1) {

                recentMembersContainer
                        .getChildren()
                        .add(
                                createSeparator()
                        );
            }
        }
    }


    // =========================================================
    // CREATE MEMBER ROW
    // =========================================================

    private HBox createMemberRow(
            DashboardMember member
    ) {

        HBox row =
                new HBox(10);


        row.setAlignment(
                Pos.CENTER_LEFT
        );


        StackPane avatar =
                createAvatar(
                        createInitials(
                                member.getName()
                        ),
                        "#EFF6FF",
                        "#2563EB"
                );


        VBox information =
                new VBox(2);


        Label name =
                new Label(
                        safe(
                                member.getName()
                        )
                );


        name.setStyle(
                "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #111827;"
        );


        String membershipName =
                safe(
                        member.getMembership()
                );


        Label membership =
                new Label(
                        membershipName
                );


        membership.setStyle(
                "-fx-font-size: 10px;"
                + "-fx-text-fill: #64748B;"
        );


        information
                .getChildren()
                .addAll(
                        name,
                        membership
                );


        Region spacer =
                new Region();


        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );


        Label status =
                new Label(
                        translateStatus(
                                member.getStatus()
                        )
                );


        styleStatusLabel(
                status,
                member.getStatus()
        );


        row.getChildren().addAll(
                avatar,
                information,
                spacer,
                status
        );


        return row;
    }


    // =========================================================
    // UPCOMING EXPIRATIONS
    // =========================================================

    private void loadUpcomingExpirations() {

        expirationsContainer
                .getChildren()
                .clear();


        List<DashboardExpiration> expirations =
                dashboardService
                        .getUpcomingExpirations();


        if (expirations.isEmpty()) {

            expirationsContainer
                    .getChildren()
                    .add(
                            createEmptyLabel()
                    );

            return;
        }


        for (int i = 0;
                i < expirations.size();
                i++) {

            DashboardExpiration expiration =
                    expirations.get(i);


            expirationsContainer
                    .getChildren()
                    .add(
                            createExpirationRow(
                                    expiration
                            )
                    );


            if (i < expirations.size() - 1) {

                expirationsContainer
                        .getChildren()
                        .add(
                                createSeparator()
                        );
            }
        }
    }


    // =========================================================
    // CREATE EXPIRATION ROW
    // =========================================================

    private HBox createExpirationRow(
            DashboardExpiration expiration
    ) {

        HBox row =
                new HBox(10);


        row.setAlignment(
                Pos.CENTER_LEFT
        );


        VBox information =
                new VBox(2);


        Label memberName =
                new Label(
                        safe(
                                expiration.getMemberName()
                        )
                );


        memberName.setStyle(
                "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #111827;"
        );


        String membershipName;


        if (LanguageManager.isArabic()) {

            membershipName =
                    firstNotBlank(
                            expiration.getMembershipNameAr(),
                            expiration.getMembershipNameEn()
                    );

        } else {

            membershipName =
                    firstNotBlank(
                            expiration.getMembershipNameEn(),
                            expiration.getMembershipNameAr()
                    );
        }


        Label membership =
                new Label(
                        membershipName
                );


        membership.setStyle(
                "-fx-font-size: 10px;"
                + "-fx-text-fill: #64748B;"
        );


        information
                .getChildren()
                .addAll(
                        memberName,
                        membership
                );


        Region spacer =
                new Region();


        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );


        long days =
                expiration
                        .getDaysRemaining();


        HBox badge =
                createDaysBadge(
                        days
                );


        row.getChildren().addAll(
                information,
                spacer,
                badge
        );


        return row;
    }


    // =========================================================
    // DAYS BADGE
    // =========================================================

    private HBox createDaysBadge(
            long days
    ) {

        HBox badge =
                new HBox(3);


        badge.setAlignment(
                Pos.CENTER
        );


        String background;
        String foreground;


        if (days <= 2) {

            background = "#FEE2E2";
            foreground = "#DC2626";

        } else if (days <= 5) {

            background = "#FFEDD5";
            foreground = "#EA580C";

        } else {

            background = "#FEF3C7";
            foreground = "#D97706";
        }


        badge.setStyle(
                "-fx-background-color: "
                + background
                + ";"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 4 9;"
        );


        Label number =
                new Label(
                        String.valueOf(
                                Math.max(
                                        0,
                                        days
                                )
                        )
                );


        number.setStyle(
                "-fx-font-size: 10px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: "
                + foreground
                + ";"
        );


        Label daysLabel =
                new Label(
                        getText(
                                "common.days",
                                LanguageManager.isArabic()
                                        ? "يوم"
                                        : "days"
                        )
                );


        daysLabel.setStyle(
                "-fx-font-size: 9px;"
                + "-fx-text-fill: "
                + foreground
                + ";"
        );


        badge.getChildren().addAll(
                number,
                daysLabel
        );


        return badge;
    }


    // =========================================================
    // RECENT PAYMENTS
    // =========================================================

    private void loadRecentPayments() {

        recentPaymentsContainer
                .getChildren()
                .clear();


        List<DashboardPayment> payments =
                dashboardService
                        .getRecentPayments();


        if (payments.isEmpty()) {

            recentPaymentsContainer
                    .getChildren()
                    .add(
                            createEmptyLabel()
                    );

            return;
        }


        for (int i = 0;
                i < payments.size();
                i++) {

            DashboardPayment payment =
                    payments.get(i);


            recentPaymentsContainer
                    .getChildren()
                    .add(
                            createPaymentRow(
                                    payment
                            )
                    );


            if (i < payments.size() - 1) {

                recentPaymentsContainer
                        .getChildren()
                        .add(
                                createSeparator()
                        );
            }
        }
    }


    // =========================================================
    // CREATE PAYMENT ROW
    // =========================================================

    private HBox createPaymentRow(
            DashboardPayment payment
    ) {

        HBox row =
                new HBox(10);


        row.setAlignment(
                Pos.CENTER_LEFT
        );


        StackPane icon =
                createAvatar(
                        "EGP",
                        "#ECFDF5",
                        "#059669"
                );


        VBox information =
                new VBox(2);


        Label member =
                new Label(
                        safe(
                                payment.getMemberName()
                        )
                );


        member.setStyle(
                "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #111827;"
        );


        Label method =
                new Label(
                        translatePaymentMethod(
                                payment.getPaymentMethod()
                        )
                );


        method.setStyle(
                "-fx-font-size: 10px;"
                + "-fx-text-fill: #64748B;"
        );


        information
                .getChildren()
                .addAll(
                        member,
                        method
                );


        Region spacer =
                new Region();


        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );


        VBox amountBox =
                new VBox(2);


        amountBox.setAlignment(
                Pos.CENTER_RIGHT
        );


        Label amount =
                new Label(
                        formatMoney(
                                payment.getAmount()
                        )
                );


        amount.setStyle(
                "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #111827;"
        );


        Label status =
                new Label(
                        translatePaymentStatus(
                                payment.getStatus()
                        )
                );


        status.setStyle(
                "-fx-font-size: 9px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: "
                + paymentStatusColor(
                        payment.getStatus()
                )
                + ";"
        );


        amountBox
                .getChildren()
                .addAll(
                        amount,
                        status
                );


        row.getChildren().addAll(
                icon,
                information,
                spacer,
                amountBox
        );


        return row;
    }


    // =========================================================
    // BAR CHART LABELS
    // =========================================================

    private void addBarChartLabels() {

        for (XYChart.Series<String, Number> series
                : revenueChart.getData()) {

            for (XYChart.Data<String, Number> data
                    : series.getData()) {

                Node node =
                        data.getNode();


                if (node == null) {
                    continue;
                }


                Tooltip tooltip =
                        new Tooltip(
                                series.getName()
                                + "\n"
                                + data.getXValue()
                                + "\n"
                                + formatMoney(
                                        BigDecimal.valueOf(
                                                data.getYValue()
                                                        .doubleValue()
                                        )
                                )
                        );


                Tooltip.install(
                        node,
                        tooltip
                );
            }
        }
    }


    // =========================================================
    // MEMBER GROWTH LABELS
    // =========================================================

    private void addMemberGrowthLabels() {

        for (XYChart.Series<String, Number> series
                : memberGrowthChart.getData()) {

            for (XYChart.Data<String, Number> data
                    : series.getData()) {

                Node node =
                        data.getNode();


                if (node == null) {
                    continue;
                }


                Tooltip tooltip =
                        new Tooltip(
                                data.getXValue()
                                + ": "
                                + data.getYValue()
                                        .longValue()
                        );


                Tooltip.install(
                        node,
                        tooltip
                );
            }
        }
    }


    // =========================================================
    // PIE TOOLTIPS
    // =========================================================

    private void addPieChartTooltips() {

        double total =
                membershipChart
                        .getData()
                        .stream()
                        .mapToDouble(
                                PieChart.Data::getPieValue
                        )
                        .sum();


        if (total <= 0) {
            return;
        }


        for (PieChart.Data data
                : membershipChart.getData()) {

            Node node =
                    data.getNode();


            if (node == null) {
                continue;
            }


            double percentage =
                    data.getPieValue()
                    / total
                    * 100.0;


            Tooltip tooltip =
                    new Tooltip(
                            data.getName()
                            + "\n"
                            + String.format(
                                    Locale.US,
                                    "%.1f%%",
                                    percentage
                            )
                    );


            Tooltip.install(
                    node,
                    tooltip
            );
        }
    }


    // =========================================================
    // CURRENT DATE
    // =========================================================

    private void updateCurrentDate() {

        Locale locale =
                LanguageManager.isArabic()
                        ? Locale.forLanguageTag("ar")
                        : Locale.ENGLISH;


        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "EEEE, d MMMM yyyy",
                        locale
                );


        currentDateLabel.setText(
                LocalDate.now()
                        .format(formatter)
        );
    }


    // =========================================================
    // MONTH FORMAT
    // =========================================================

    private String formatMonth(
            YearMonth month
    ) {

        if (month == null) {
            return "-";
        }


        Locale locale =
                LanguageManager.isArabic()
                        ? Locale.forLanguageTag("ar")
                        : Locale.ENGLISH;


        return month
                .getMonth()
                .getDisplayName(
                        TextStyle.SHORT,
                        locale
                );
    }


    // =========================================================
    // MONEY FORMAT
    // =========================================================

    private String formatMoney(
            BigDecimal amount
    ) {

        BigDecimal safeAmount =
                amount == null
                        ? BigDecimal.ZERO
                        : amount;


        String currency =
                LanguageManager.isArabic()
                        ? "ج.م"
                        : "EGP";


        return String.format(
                Locale.US,
                "%,.2f %s",
                safeAmount,
                currency
        );
    }


    // =========================================================
    // AVATAR
    // =========================================================

    private StackPane createAvatar(
            String text,
            String background,
            String foreground
    ) {

        StackPane avatar =
                new StackPane();


        avatar.setPrefSize(
                38,
                38
        );


        avatar.setMinSize(
                38,
                38
        );


        avatar.setMaxSize(
                38,
                38
        );


        avatar.setStyle(
                "-fx-background-color: "
                + background
                + ";"
                + "-fx-background-radius: 19;"
        );


        Label label =
                new Label(
                        text
                );


        label.setStyle(
                "-fx-font-size: 10px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: "
                + foreground
                + ";"
        );


        avatar
                .getChildren()
                .add(label);


        return avatar;
    }


    // =========================================================
    // INITIALS
    // =========================================================

    private String createInitials(
            String name
    ) {

        if (name == null
                || name.isBlank()) {

            return "M";
        }


        String[] words =
                name.trim()
                        .split("\\s+");


        if (words.length == 1) {

            return words[0]
                    .substring(
                            0,
                            1
                    )
                    .toUpperCase();
        }


        return (
                words[0].substring(0, 1)
                + words[words.length - 1]
                        .substring(0, 1)
        ).toUpperCase();
    }


    // =========================================================
    // STATUS
    // =========================================================

    private void styleStatusLabel(
            Label label,
            String status
    ) {

        boolean active =
                status != null
                && status.equalsIgnoreCase(
                        "ACTIVE"
                );


        if (active) {

            label.setStyle(
                    "-fx-background-color: #DCFCE7;"
                    + "-fx-background-radius: 10;"
                    + "-fx-padding: 4 8;"
                    + "-fx-font-size: 9px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #15803D;"
            );

        } else {

            label.setStyle(
                    "-fx-background-color: #F1F5F9;"
                    + "-fx-background-radius: 10;"
                    + "-fx-padding: 4 8;"
                    + "-fx-font-size: 9px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #64748B;"
            );
        }
    }


    private String translateStatus(
            String status
    ) {

        if (status == null) {
            return "-";
        }


        if (status.equalsIgnoreCase("ACTIVE")) {

            return LanguageManager.isArabic()
                    ? "نشط"
                    : "Active";
        }


        if (status.equalsIgnoreCase("INACTIVE")) {

            return LanguageManager.isArabic()
                    ? "غير نشط"
                    : "Inactive";
        }


        if (status.equalsIgnoreCase("EXPIRED")) {

            return LanguageManager.isArabic()
                    ? "منتهي"
                    : "Expired";
        }


        return status;
    }


    // =========================================================
    // PAYMENT METHOD
    // =========================================================

    private String translatePaymentMethod(
            String method
    ) {

        if (method == null) {
            return "-";
        }


        String normalized =
                method.trim()
                        .toUpperCase();


        return switch (normalized) {

            case "CASH" ->
                LanguageManager.isArabic()
                        ? "نقدي"
                        : "Cash";

            case "CARD" ->
                LanguageManager.isArabic()
                        ? "بطاقة"
                        : "Card";

            case "BANK_TRANSFER",
                 "BANK TRANSFER" ->
                LanguageManager.isArabic()
                        ? "تحويل بنكي"
                        : "Bank Transfer";

            case "WALLET" ->
                LanguageManager.isArabic()
                        ? "محفظة إلكترونية"
                        : "Wallet";

            default ->
                method;
        };
    }


    // =========================================================
    // PAYMENT STATUS
    // =========================================================

    private String translatePaymentStatus(
            String status
    ) {

        if (status == null) {
            return "-";
        }


        return switch (
                status.trim()
                        .toUpperCase()
        ) {

            case "PAID" ->
                LanguageManager.isArabic()
                        ? "مدفوع"
                        : "Paid";

            case "PENDING" ->
                LanguageManager.isArabic()
                        ? "معلق"
                        : "Pending";

            case "CANCELLED" ->
                LanguageManager.isArabic()
                        ? "ملغي"
                        : "Cancelled";

            default ->
                status;
        };
    }


    private String paymentStatusColor(
            String status
    ) {

        if (status == null) {
            return "#64748B";
        }


        return switch (
                status.trim()
                        .toUpperCase()
        ) {

            case "PAID" ->
                "#16A34A";

            case "PENDING" ->
                "#D97706";

            case "CANCELLED" ->
                "#DC2626";

            default ->
                "#64748B";
        };
    }


    // =========================================================
    // SEPARATOR
    // =========================================================

    private Separator createSeparator() {

        Separator separator =
                new Separator();


        separator.setOpacity(
                0.18
        );


        return separator;
    }


    // =========================================================
    // EMPTY LABEL
    // =========================================================

    private Label createEmptyLabel() {

        Label label =
                new Label(
                        LanguageManager.isArabic()
                                ? "لا توجد بيانات"
                                : "No data available"
                );


        label.setStyle(
                "-fx-font-size: 11px;"
                + "-fx-text-fill: #94A3B8;"
                + "-fx-padding: 12 0;"
        );


        return label;
    }


    // =========================================================
    // ERROR
    // =========================================================

    private void showDashboardError() {

        if (activeMembersValueLabel != null) {
            activeMembersValueLabel.setText("-");
        }

        if (todayCheckInsValueLabel != null) {
            todayCheckInsValueLabel.setText("-");
        }

        if (expiringMembershipsValueLabel != null) {
            expiringMembershipsValueLabel.setText("-");
        }

        if (monthlyRevenueValueLabel != null) {
            monthlyRevenueValueLabel.setText("-");
        }


        if (recentMembersContainer != null) {

            recentMembersContainer
                    .getChildren()
                    .setAll(
                            createErrorLabel()
                    );
        }


        if (expirationsContainer != null) {

            expirationsContainer
                    .getChildren()
                    .setAll(
                            createErrorLabel()
                    );
        }


        if (recentPaymentsContainer != null) {

            recentPaymentsContainer
                    .getChildren()
                    .setAll(
                            createErrorLabel()
                    );
        }
    }


    private Label createErrorLabel() {

        Label label =
                new Label(
                        LanguageManager.isArabic()
                                ? "تعذر تحميل البيانات"
                                : "Unable to load data"
                );


        label.setStyle(
                "-fx-font-size: 11px;"
                + "-fx-text-fill: #DC2626;"
                + "-fx-padding: 12 0;"
        );


        return label;
    }


    // =========================================================
    // RESOURCE BUNDLE SAFE GET
    // =========================================================

    private String getText(
            String key,
            String fallback
    ) {

        try {

            if (bundle != null
                    && bundle.containsKey(key)) {

                return bundle.getString(
                        key
                );
            }

        } catch (Exception ignored) {
        }


        return fallback;
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "-";
        }


        return value.trim();
    }


    private String firstNotBlank(
            String first,
            String second
    ) {

        if (first != null
                && !first.isBlank()) {

            return first.trim();
        }


        return safe(
                second
        );
    }
}
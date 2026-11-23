package com.mycompany.lazar_app.ui.dialog;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.Member;
import com.mycompany.lazar_app.model.CheckIn;
import com.mycompany.lazar_app.repository.MemberRepository;
import com.mycompany.lazar_app.repository.MemberMembershipRepository;
import com.mycompany.lazar_app.model.MemberMembership;
import com.mycompany.lazar_app.repository.PaymentRepository;
import com.mycompany.lazar_app.ui.WindowManager;
import javafx.concurrent.Task;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import java.nio.file.*;
import java.text.NumberFormat;
import java.util.*;

/** Loads financial records outside the JavaFX thread, after attendance is saved. */
public final class CheckinDetailsDialog {
    private CheckinDetailsDialog() {}

    private record Details(Member member, MemberMembership membership,
                           PaymentRepository.MembershipFinancialSummary finance, Integer remainingVisits, String currency) {}

    /** Read the exact subscription on the attendance record, including historical rows. */
    public static void loadAndShow(CheckIn attendance, Window owner, Runnable finished) {
        int memberId = attendance.getMemberId();
        Integer membershipId = attendance.getMembershipId();
        Task<Details> task = new Task<>() {
            @Override protected Details call() throws Exception {
                Member member = new MemberRepository().findById(memberId)
                        .orElseThrow(() -> new IllegalStateException("Member no longer exists"));
                MemberMembership membership = membershipId == null ? null
                        : new MemberMembershipRepository().findById(membershipId).orElse(null);
                if (membership != null && membership.getMemberId() != memberId) {
                    throw new IllegalStateException("Attendance subscription does not belong to this member");
                }
                return details(member, membership);
            }
        };
        task.setOnSucceeded(e -> {
            finished.run();
            Details details = task.getValue();
            if (owner == null || owner.isShowing()) {
                show(details.member(), details.membership(), owner, details.finance(), details.remainingVisits(), details.currency());
            }
        });
        task.setOnFailed(e -> {
            finished.run();
            task.getException().printStackTrace();
            if (owner == null || owner.isShowing()) {
                AppDialog.error(owner, text("checkin.details.viewTitle"), text("checkin.details.loadError"));
            }
        });
        task.setOnCancelled(e -> finished.run());
        start(task);
    }

    public static void loadAndShow(Member member, MemberMembership membership, Window owner) {
        Task<Details> task = new Task<>() {
            @Override protected Details call() throws Exception { return details(member, membership); }
        };
        task.setOnSucceeded(e -> {
            Details d = task.getValue();
            if (owner == null || owner.isShowing()) show(d.member(), d.membership(), owner,
                    d.finance(), d.remainingVisits(), d.currency());
        });
        task.setOnFailed(e -> {
            if (owner == null || owner.isShowing())
                AppDialog.error(owner, text("checkin.details.viewTitle"), text("checkin.details.loadError"));
        });
        start(task);
    }

    private static Details details(Member member, MemberMembership membership) throws java.sql.SQLException {
        Integer remaining = membership == null || membership.getAllowedSessions() <= 0 ? null
                : Math.max(0, membership.getAllowedSessions()
                        - new com.mycompany.lazar_app.repository.CheckInRepository().countByMembershipId(membership.getId()));
        String currency = com.mycompany.lazar_app.settings.AppSettingsManager.load().getCurrency();
        try { Currency.getInstance(currency); } catch (IllegalArgumentException exception) { currency = "EGP"; }
        return new Details(member, membership, loadFinance(membership), remaining, currency);
    }

    private static PaymentRepository.MembershipFinancialSummary loadFinance(MemberMembership membership) {
        if (membership == null) return null;
        try {
            return new PaymentRepository().findMembershipFinancialSummary(membership.getId());
        } catch (java.sql.SQLException error) {
            error.printStackTrace();
            return null; // Show the member even if financial information is unavailable.
        }
    }

    private static void start(Task<?> task) {
        Thread worker = new Thread(task, "checkin-details");
        worker.setDaemon(true);
        worker.start();
    }

    private static String text(String key) {
        return LanguageManager.getBundle().getString(key);
    }

    private static void show(Member member, MemberMembership membership, Window owner,
                             PaymentRepository.MembershipFinancialSummary finance, Integer remainingVisits, String currency) {
        ResourceBundle bundle = LanguageManager.getBundle();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(bundle.getString("checkin.details.viewTitle"));
        VBox content = new VBox(18);
        content.getStyleClass().add("checkin-details");
        content.setNodeOrientation(LanguageManager.isArabic()
                ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        StackPane avatar = createAvatar(member.getPhotoPath());
        Label name = new Label(member.getName());
        name.getStyleClass().add("checkin-member-name");
        name.setId("checkin-member-name");
        name.setWrapText(true);
        name.setMinHeight(Region.USE_PREF_SIZE);
        Label code = new Label(text("checkin.details.memberCode") + ": " + member.getMemberCode());
        Label phone = new Label(text("checkin.details.phone") + ": " + member.getPhone());
        code.getStyleClass().add("checkin-value");
        phone.getStyleClass().add("checkin-value");
        code.setWrapText(true);
        phone.setWrapText(true);
        VBox identity = new VBox(12, name, code, phone);
        identity.setMinWidth(0);
        HBox.setHgrow(identity, Priority.ALWAYS);
        if (membership != null) {
            Label plan = new Label(membership.getPlanDisplayName(LanguageManager.isArabic()));
            plan.setWrapText(true);
            plan.getStyleClass().add("checkin-value");
            Label status = new Label(text(membership.isActive() ? "checkin.details.active" : "checkin.details.inactive"));
            status.setId("checkin-status");
            status.getStyleClass().add(membership.isActive() ? "checkin-status-active" : "checkin-status-inactive");
            identity.getChildren().addAll(plan, status);
        }
        HBox header = new HBox(24, avatar, identity);
        header.getStyleClass().add("checkin-header");
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        content.getChildren().add(header);

        NumberFormat money = NumberFormat.getCurrencyInstance(LanguageManager.getCurrentLocale());
        money.setCurrency(Currency.getInstance(currency));
        HBox financial = new HBox(12);
        financial.getStyleClass().add("checkin-financial-summary");
        VBox amountBox = new VBox();
        VBox paidBox = new VBox();
        VBox balanceBox = new VBox();
        add(amountBox, bundle, "amount", finance == null ? "—" : formatMoney(money, finance.getMembershipAmount()));
        add(paidBox, bundle, "paid", finance == null ? "—" : formatMoney(money, finance.getPaidAmount()));
        Label balance = add(balanceBox, bundle, "balance", finance == null ? "—" : formatMoney(money, finance.getBalanceDue()));
        if (finance != null && finance.getBalanceDue().signum() > 0) balance.getStyleClass().add("checkin-outstanding");
        for (VBox box : List.of(amountBox, paidBox, balanceBox)) {
            box.setMinWidth(0);
            box.setPrefWidth(240);
            HBox.setHgrow(box, Priority.ALWAYS);
            financial.getChildren().add(box);
        }
        content.getChildren().add(financial);
        if (finance == null) {
            Label unavailable = new Label(text(membership == null
                    ? "checkin.details.noMembership" : "checkin.details.financeUnavailable"));
            unavailable.setWrapText(true);
            unavailable.getStyleClass().add("checkin-outstanding");
            content.getChildren().add(unavailable);
        }
        GridPane details = new GridPane();
        details.setHgap(12);
        details.setVgap(12);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50);
            column.setMinWidth(0);
            details.getColumnConstraints().add(column);
        }
        int row = 0;
        if (membership != null) {
            detail(details, bundle, "member", member.getMemberCode() + " / " + membership.getId(), 0, row);
            detail(details, bundle, "plan", membership.getPlanDisplayName(LanguageManager.isArabic()), 1, row++);
            detail(details, bundle, "start", Objects.toString(membership.getStartDate(), "—"), 0, row);
            detail(details, bundle, "end", Objects.toString(membership.getEndDate(), "—"), 1, row++);
            detail(details, bundle, "days", Long.toString(membership.getRemainingDays()), 0, row);
            detail(details, bundle, "sessions", membership.getAllowedSessions() > 0
                    ? Objects.toString(remainingVisits, "—") : text("checkin.details.unlimited"), 1, row++);
        }
        detail(details, bundle, "email", member.getEmail(), 0, row);
        detail(details, bundle, "whatsapp", member.getWhatsapp(), 1, row);
        content.getChildren().add(details);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType(
                LanguageManager.isArabic() ? "إغلاق" : "Close", ButtonBar.ButtonData.CANCEL_CLOSE));
        DialogStyler.style(dialog);
        dialog.getDialogPane().getStyleClass().add("checkin-details-dialog");
        dialog.getDialogPane().setNodeOrientation(content.getNodeOrientation());
        dialog.getDialogPane().getStylesheets().add(CheckinDetailsDialog.class.getResource("/css/checkin-details.css").toExternalForm());
        WindowManager.showResponsiveDialogAndWait(dialog, owner, 640, 720);
    }

    private static String formatMoney(NumberFormat format, java.math.BigDecimal amount) {
        // Layout already supplies direction. Avoid missing-glyph boxes for invisible
        // bidi marks emitted by some JDK currency providers/font combinations.
        return format.format(amount).replace("\u061C", "").replace("\u200E", "").replace("\u200F", "");
    }

    /** Fixed photo bounds keep the image visible even in a narrow dialog. */
    private static StackPane createAvatar(String photoPath) {
        Label placeholder = new Label("👤");
        placeholder.setStyle("-fx-font-size: 42px;");
        ImageView view = new ImageView();
        view.setFitWidth(224);
        view.setFitHeight(224);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        StackPane frame = new StackPane(placeholder, view);
        frame.getStyleClass().add("checkin-avatar");
        frame.setId("checkin-photo");
        frame.setMinSize(240, 240);
        frame.setPrefSize(240, 240);
        frame.setMaxSize(240, 240);
        placeholder.setText(text("checkin.details.noPhoto"));
        placeholder.setWrapText(true);
        placeholder.setStyle("-fx-font-size: 14px;");
        if (photoPath == null || photoPath.isBlank()) return frame;
        try {
            String path = photoPath.trim();
            Path photo = path.startsWith("file:")
                    ? Path.of(java.net.URI.create(path)) : Path.of(path);
            if (!Files.isRegularFile(photo) || !Files.isReadable(photo)) return frame;
            Image image = new Image(photo.toUri().toString(), 896, 896, true, true, true);
            Runnable update = () -> {
                boolean ready = !image.isError() && image.getProgress() >= 1
                        && image.getWidth() > 0;
                view.setImage(ready ? image : null);
                placeholder.setVisible(!ready);
            };
            image.progressProperty().addListener((o, before, after) -> update.run());
            image.errorProperty().addListener((o, before, after) -> update.run());
            update.run();
        } catch (RuntimeException ignored) {
            // Missing, malformed or unsupported photos retain the placeholder.
        }
        return frame;
    }

    private static void detail(GridPane grid, ResourceBundle bundle, String key,
                               String value, int column, int row) {
        VBox box = new VBox();
        box.setMinWidth(0);
        add(box, bundle, key, value);
        grid.add(box, column, row);
    }

    private static Label add(VBox box, ResourceBundle bundle, String key, String value) {
        Label caption = new Label(bundle.getString("checkin.details." + key));
        caption.getStyleClass().add("checkin-caption");
        caption.setWrapText(true);
        caption.setMinHeight(Region.USE_PREF_SIZE);
        Label label = new Label(value == null || value.isBlank() ? "—" : value);
        label.setId("checkin-" + key);
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        label.getStyleClass().add("checkin-value");
        VBox card = new VBox(5, caption, label);
        card.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("checkin-detail-card");
        if (Set.of("amount", "paid", "balance").contains(key)) {
            card.getStyleClass().add("checkin-financial-card");
        }
        box.getChildren().add(card);
        return label;
    }
}

package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.ui.WindowManager;
import com.mycompany.lazar_app.ui.dialog.DialogStyler;
import com.mycompany.lazar_app.model.Revenue;
import com.mycompany.lazar_app.repository.RevenueRepository;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public class RevenuesController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Button newRevenueButton;

    @FXML
    private Label totalTitleLabel;

    @FXML
    private Label totalRevenueLabel;

    @FXML
    private Label monthTitleLabel;

    @FXML
    private Label monthRevenueLabel;

    @FXML
    private Label transactionsTitleLabel;

    @FXML
    private Label transactionsCountLabel;

    @FXML
    private Label typesTitleLabel;

    @FXML
    private Label typesCountLabel;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> typeFilter;

    @FXML
    private Button refreshButton;

    @FXML
    private TableView<Revenue> revenuesTable;

    @FXML
    private TableColumn<Revenue, String> codeColumn;

    @FXML
    private TableColumn<Revenue, String> nameColumn;

    @FXML
    private TableColumn<Revenue, String> typeColumn;

    @FXML
    private TableColumn<Revenue, BigDecimal> amountColumn;

    @FXML
    private TableColumn<Revenue, String> dateColumn;

    @FXML
    private TableColumn<Revenue, String> attachmentColumn;

    @FXML
    private TableColumn<Revenue, Void> actionsColumn;

    private final RevenueRepository revenueRepository =
            new RevenueRepository();

    private final ObservableList<Revenue> revenues =
            FXCollections.observableArrayList();

    private final PauseTransition searchDelay =
            new PauseTransition(Duration.millis(250));

    /*
     * Prevent opening more than one edit/add window at the same time.
     * This also protects against repeated clicks on the action buttons.
     */
    private Stage revenueFormStage;

    private static final NumberFormat MONEY_FORMAT =
            NumberFormat.getNumberInstance(Locale.US);

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {

        configureLanguage();
        configureTable();
        configureFilters();
        configureSearch();
        refreshAll();
    }

    private void configureLanguage() {

        boolean arabic = LanguageManager.isArabic();

        setText(titleLabel, arabic ? "الإيرادات" : "Revenues");

        setText(
                subtitleLabel,
                arabic
                        ? "إدارة الإيرادات الأخرى وإثباتات مصادر الدخل"
                        : "Manage other revenues and source documents"
        );

        setText(
                newRevenueButton,
                arabic ? "+ إضافة إيراد" : "+ New Revenue"
        );

        setText(
                totalTitleLabel,
                arabic
                        ? "إجمالي الإيرادات الأخرى"
                        : "Total Other Revenue"
        );

        setText(
                monthTitleLabel,
                arabic ? "إيرادات هذا الشهر" : "This Month"
        );

        setText(
                transactionsTitleLabel,
                arabic ? "عدد العمليات" : "Transactions"
        );

        setText(
                typesTitleLabel,
                arabic ? "أنواع الإيرادات" : "Revenue Types"
        );

        setText(
                refreshButton,
                arabic ? "تحديث" : "Refresh"
        );

        searchField.setPromptText(
                arabic
                        ? "بحث بالاسم أو النوع أو الكود أو المبلغ..."
                        : "Search name, type, code or amount..."
        );

        codeColumn.setText(arabic ? "الكود" : "Code");
        nameColumn.setText(arabic ? "اسم الإيراد" : "Revenue Name");
        typeColumn.setText(arabic ? "النوع" : "Type");
        amountColumn.setText(arabic ? "المبلغ" : "Amount");
        dateColumn.setText(arabic ? "التاريخ" : "Date");
        attachmentColumn.setText(arabic ? "المرفق" : "Attachment");
        actionsColumn.setText(arabic ? "الإجراءات" : "Actions");

        revenuesTable.setNodeOrientation(
                arabic
                        ? NodeOrientation.RIGHT_TO_LEFT
                        : NodeOrientation.LEFT_TO_RIGHT
        );
    }

    private void configureTable() {

        revenuesTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        codeColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        safe(
                                data.getValue()
                                        .getRevenueCode()
                        )
                )
        );

        nameColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        safe(
                                data.getValue()
                                        .getName()
                        )
                )
        );

        typeColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        safe(
                                data.getValue()
                                        .getRevenueType()
                        )
                )
        );

        amountColumn.setCellValueFactory(
                data -> new SimpleObjectProperty<>(
                        data.getValue()
                                .getAmount()
                )
        );

        amountColumn.setCellFactory(
                column -> new TableCell<>() {

                    @Override
                    protected void updateItem(
                            BigDecimal amount,
                            boolean empty) {

                        super.updateItem(
                                amount,
                                empty
                        );

                        setText(
                                empty || amount == null
                                        ? null
                                        : formatMoney(amount)
                        );
                    }
                }
        );

        dateColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue()
                                        .getRevenueDate()
                                == null
                                ? "-"
                                : data.getValue()
                                        .getRevenueDate()
                                        .format(DATE_FORMAT)
                )
        );

        attachmentColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        hasAttachment(
                                data.getValue()
                        )
                                ? (
                                        LanguageManager.isArabic()
                                                ? "مرفق"
                                                : "Attached"
                                )
                                : "-"
                )
        );

        configureActionsColumn();

        revenuesTable.setItems(
                revenues
        );

        revenuesTable.setPlaceholder(
                new Label(
                        LanguageManager.isArabic()
                                ? "لا توجد إيرادات مسجلة"
                                : "No revenues found"
                )
        );

        /*
         * Deliberately do not open Edit on a row double-click.
         *
         * The table already has an Edit button. Keeping both behaviors can
         * cause the same click sequence to open two modal windows, especially
         * when the user double-clicks an action button. This was a likely
         * cause of freezes / excessive memory usage.
         */
        revenuesTable.setRowFactory(
                table -> new javafx.scene.control.TableRow<>()
        );
    }

    private void configureActionsColumn() {

        actionsColumn.setCellFactory(
                column -> new TableCell<>() {

                    private final Button editButton =
                            new Button(
                                    LanguageManager.isArabic()
                                            ? "تعديل"
                                            : "Edit"
                            );

                    private final Button fileButton =
                            new Button(
                                    LanguageManager.isArabic()
                                            ? "المرفق"
                                            : "File"
                            );

                    private final Button deleteButton =
                            new Button(
                                    LanguageManager.isArabic()
                                            ? "حذف"
                                            : "Delete"
                            );

                    private final HBox box =
                            new HBox(
                                    6,
                                    editButton,
                                    fileButton,
                                    deleteButton
                            );

                    {
                        editButton.setStyle(
                                buttonStyle("#2563EB")
                        );

                        fileButton.setStyle(
                                buttonStyle("#475569")
                        );

                        deleteButton.setStyle(
                                buttonStyle("#DC2626")
                        );

                        editButton.setOnAction(
                                event -> {

                                    Revenue item =
                                            getCurrentRevenue();

                                    if (item != null) {
                                        openRevenueForm(
                                                item
                                        );
                                    }
                                }
                        );

                        fileButton.setOnAction(
                                event -> {

                                    Revenue item =
                                            getCurrentRevenue();

                                    if (item != null) {
                                        openAttachment(
                                                item
                                        );
                                    }
                                }
                        );

                        deleteButton.setOnAction(
                                event -> {

                                    Revenue item =
                                            getCurrentRevenue();

                                    if (item != null) {
                                        deleteRevenue(
                                                item
                                        );
                                    }
                                }
                        );
                    }

                    private Revenue getCurrentRevenue() {

                        int index =
                                getIndex();

                        if (getTableView() == null
                                || getTableView()
                                        .getItems()
                                        == null
                                || index < 0
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
                            boolean empty) {

                        super.updateItem(
                                item,
                                empty
                        );

                        if (empty) {

                            setGraphic(null);

                            return;
                        }

                        Revenue current =
                                getCurrentRevenue();

                        fileButton.setDisable(
                                current == null
                                        || !hasAttachment(
                                                current
                                        )
                        );

                        setGraphic(
                                box
                        );
                    }
                }
        );
    }

    private void configureFilters() {

        typeFilter.valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applySearch()
                );
    }

    private void configureSearch() {

        searchDelay.setOnFinished(
                event -> applySearch()
        );

        searchField.textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            searchDelay.stop();
                            searchDelay.playFromStart();
                        }
                );

        searchField.setOnAction(
                event -> {

                    searchDelay.stop();
                    applySearch();
                }
        );
    }

    @FXML
    private void openNewRevenue() {

        openRevenueForm(
                null
        );
    }

    private void openRevenueForm(
            Revenue revenue) {

        /*
         * If the user clicks Edit repeatedly, reuse the already opened form
         * instead of creating multiple Stage/Scene instances.
         */
        if (revenueFormStage != null
                && revenueFormStage.isShowing()) {

            revenueFormStage.toFront();
            revenueFormStage.requestFocus();

            return;
        }

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass()
                                    .getResource(
                                            "/fxml/revenue-form.fxml"
                                    ),
                            LanguageManager.getBundle()
                    );

            Parent root =
                    loader.load();

            RevenueFormController controller =
                    loader.getController();

            controller.setRevenueRepository(
                    revenueRepository
            );

            controller.setRevenue(
                    revenue
            );

            controller.setOnSaved(
                    this::refreshAll
            );

            root.setNodeOrientation(
                    LanguageManager.isArabic()
                            ? NodeOrientation.RIGHT_TO_LEFT
                            : NodeOrientation.LEFT_TO_RIGHT
            );

            Scene scene =
                    new Scene(
                            root,
                            1100,
                            760
                    );

            Stage stage =
                    new Stage();

            revenueFormStage =
                    stage;

            stage.setScene(
                    scene
            );

            stage.setTitle(
                    revenue == null
                            ? (
                                    LanguageManager.isArabic()
                                            ? "إضافة إيراد"
                                            : "New Revenue"
                            )
                            : (
                                    LanguageManager.isArabic()
                                            ? "تعديل الإيراد"
                                            : "Edit Revenue"
                            )
            );

            stage.initModality(
                    Modality.APPLICATION_MODAL
            );

            Window owner =
                    getOwnerWindow();

            if (owner != null) {

                stage.initOwner(
                        owner
                );
            }

            /*
             * Do not maximize the revenue form.
             *
             * A normal 1100x760 form is lighter and avoids unnecessary
             * full-screen rendering / memory pressure on the JavaFX process.
             */
            stage.setResizable(
                    true
            );

            stage.setMinWidth(
                    900
            );

            stage.setMinHeight(
                    650
            );

            stage.setOnHidden(
                    event -> revenueFormStage = null
            );

            /*
             * show() is enough with APPLICATION_MODAL. It keeps the owner
             * disabled while avoiding a nested showAndWait event loop.
             */
            stage.show();

            Platform.runLater(
                    () -> centerStageOverOwner(
                            stage,
                            owner
                    )
            );

        } catch (Exception exception) {

            revenueFormStage =
                    null;

            exception.printStackTrace();

            showError(
                    LanguageManager.isArabic()
                            ? "تعذر فتح نافذة الإيراد"
                            : "Unable to open revenue window",
                    exception.getMessage()
            );
        }
    }

    private void centerStageOverOwner(
            Stage stage,
            Window owner
    ) {

        if (stage == null) {
            return;
        }

        if (owner == null
                || !owner.isShowing()
                || owner.getWidth() <= 0
                || owner.getHeight() <= 0) {

            stage.centerOnScreen();
            return;
        }

        stage.setX(
                owner.getX()
                        + (owner.getWidth() - stage.getWidth()) / 2.0
        );

        stage.setY(
                owner.getY()
                        + (owner.getHeight() - stage.getHeight()) / 2.0
        );
    }


    @FXML
    private void refreshAll() {

        try {

            loadTypesFilter();

            if (hasActiveFilter()) {

                applySearch();

            } else {

                revenues.setAll(
                        revenueRepository.findAll()
                );
            }

            totalRevenueLabel.setText(
                    formatMoney(
                            revenueRepository
                                    .getTotalRevenue()
                    )
            );

            monthRevenueLabel.setText(
                    formatMoney(
                            revenueRepository
                                    .getCurrentMonthRevenue()
                    )
            );

            transactionsCountLabel.setText(
                    String.valueOf(
                            revenueRepository
                                    .countAll()
                    )
            );

            typesCountLabel.setText(
                    String.valueOf(
                            revenueRepository
                                    .countRevenueTypes()
                    )
            );

        } catch (Exception exception) {

            exception.printStackTrace();

            showError(
                    LanguageManager.isArabic()
                            ? "تعذر تحميل الإيرادات"
                            : "Unable to load revenues",
                    exception.getMessage()
            );
        }
    }

    private void loadTypesFilter()
            throws Exception {

        String selected =
                typeFilter.getValue();

        List<String> types =
                revenueRepository
                        .findRevenueTypes();

        ObservableList<String> items =
                FXCollections.observableArrayList();

        items.add(
                "ALL"
        );

        items.addAll(
                types
        );

        typeFilter.setItems(
                items
        );

        if (selected != null
                && items.contains(
                        selected
                )) {

            typeFilter.setValue(
                    selected
            );

        } else {

            typeFilter.setValue(
                    "ALL"
            );
        }

        typeFilter.setButtonCell(
                new javafx.scene.control.ListCell<>() {

                    @Override
                    protected void updateItem(
                            String item,
                            boolean empty) {

                        super.updateItem(
                                item,
                                empty
                        );

                        setText(
                                empty || item == null
                                        ? null
                                        : displayTypeFilter(
                                                item
                                        )
                        );
                    }
                }
        );

        typeFilter.setCellFactory(
                list -> new javafx.scene.control.ListCell<>() {

                    @Override
                    protected void updateItem(
                            String item,
                            boolean empty) {

                        super.updateItem(
                                item,
                                empty
                        );

                        setText(
                                empty || item == null
                                        ? null
                                        : displayTypeFilter(
                                                item
                                        )
                        );
                    }
                }
        );
    }

    private String displayTypeFilter(
            String value) {

        if ("ALL".equalsIgnoreCase(
                value
        )) {

            return LanguageManager.isArabic()
                    ? "كل الأنواع"
                    : "All Types";
        }

        return value;
    }

    private boolean hasActiveFilter() {

        return (
                searchField.getText() != null
                        && !searchField
                                .getText()
                                .isBlank()
        )
                || (
                        typeFilter.getValue() != null
                                && !"ALL".equalsIgnoreCase(
                                        typeFilter.getValue()
                                )
                );
    }

    private void applySearch() {

        try {

            revenues.setAll(
                    revenueRepository.search(
                            searchField.getText(),
                            typeFilter.getValue()
                    )
            );

        } catch (Exception exception) {

            showError(
                    LanguageManager.isArabic()
                            ? "خطأ في البحث"
                            : "Search Error",
                    exception.getMessage()
            );
        }
    }

    private void openAttachment(
            Revenue revenue) {

        if (!hasAttachment(
                revenue
        )) {

            showWarning(
                    LanguageManager.isArabic()
                            ? "لا يوجد مرفق"
                            : "No Attachment",
                    LanguageManager.isArabic()
                            ? "لا يوجد مستند مصدر دخل لهذا الإيراد."
                            : "This revenue has no source document."
            );

            return;
        }

        try {

            Path file =
                    Path.of(
                            revenue.getAttachmentPath()
                    )
                            .toAbsolutePath()
                            .normalize();

            if (!Files.exists(
                    file
            )) {

                throw new IllegalStateException(
                        LanguageManager.isArabic()
                                ? "الملف المرفق غير موجود على الجهاز:\n"
                                        + file
                                : "The attachment file does not exist:\n"
                                        + file
                );
            }

            if (!Files.isRegularFile(
                    file
            )) {

                throw new IllegalStateException(
                        LanguageManager.isArabic()
                                ? "مسار المرفق لا يشير إلى ملف صالح."
                                : "The attachment path is not a regular file."
                );
            }

            openWithSystemApplication(
                    file
            );

        } catch (Exception exception) {

            exception.printStackTrace();

            showError(
                    LanguageManager.isArabic()
                            ? "تعذر فتح المرفق"
                            : "Unable to open attachment",
                    exception.getMessage()
            );
        }
    }

    /**
     * Open an attachment using the operating system without initializing
     * java.awt.Desktop/AWT inside the JavaFX application.
     *
     * On Ubuntu/Linux this uses xdg-open, which is the native desktop
     * integration expected by the user's environment.
     */
    private void openWithSystemApplication(
            Path file) throws Exception {

        String os =
                System.getProperty(
                        "os.name",
                        ""
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        ProcessBuilder processBuilder;

        if (os.contains(
                "win"
        )) {

            processBuilder =
                    new ProcessBuilder(
                            "cmd",
                            "/c",
                            "start",
                            "",
                            file.toString()
                    );

        } else if (os.contains(
                "mac"
        )) {

            processBuilder =
                    new ProcessBuilder(
                            "open",
                            file.toString()
                    );

        } else {

            processBuilder =
                    new ProcessBuilder(
                            "xdg-open",
                            file.toString()
                    );
        }

        /*
         * Do not waitFor(). The external viewer must never block the JavaFX
         * Application Thread.
         */
        processBuilder
                .redirectOutput(
                        ProcessBuilder.Redirect.DISCARD
                )
                .redirectError(
                        ProcessBuilder.Redirect.DISCARD
                )
                .start();
    }

    private void deleteRevenue(
            Revenue revenue) {

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                LanguageManager.isArabic()
                        ? "حذف الإيراد"
                        : "Delete Revenue"
        );

        confirmation.setHeaderText(
                LanguageManager.isArabic()
                        ? "حذف "
                                + safe(
                                        revenue.getName()
                                )
                                + "؟"
                        : "Delete "
                                + safe(
                                        revenue.getName()
                                )
                                + "?"
        );

        confirmation.setContentText(
                LanguageManager.isArabic()
                        ? "سيتم حذف سجل الإيراد والمرفق المرتبط به نهائيًا."
                        : "The revenue record and its linked attachment will be permanently deleted."
        );

        DialogStyler.style(confirmation);

        Optional<ButtonType> result =
                WindowManager.showResponsiveDialogAndWait(
                        confirmation,
                        getOwnerWindow(),
                        520.0,
                        320.0
                );

        if (result.isEmpty()
                || result.get()
                        != ButtonType.OK) {

            return;
        }

        /*
         * Keep the attachment path before deleting the database row.
         * After the database delete succeeds, remove the file that GymFlow
         * copied into data/revenue_attachments.
         */
        String attachmentPath =
                revenue.getAttachmentPath();

        try {

            revenueRepository.deleteById(
                    revenue.getId()
            );

            try {

                deleteRevenueAttachment(
                        attachmentPath
                );

            } catch (Exception attachmentException) {

                /*
                 * The revenue row has already been removed successfully.
                 * Do not report the whole delete as failed just because the
                 * physical attachment could not be removed.
                 */
                refreshAll();

                showWarning(
                        LanguageManager.isArabic()
                                ? "تم حذف الإيراد"
                                : "Revenue Deleted",
                        LanguageManager.isArabic()
                                ? "تم حذف سجل الإيراد، ولكن تعذر حذف المرفق من الجهاز.\n"
                                        + safe(
                                                attachmentException.getMessage()
                                        )
                                : "The revenue record was deleted, but the attachment file could not be removed.\n"
                                        + safe(
                                                attachmentException.getMessage()
                                        )
                );

                return;
            }

            refreshAll();

        } catch (Exception exception) {

            showError(
                    LanguageManager.isArabic()
                            ? "تعذر حذف الإيراد"
                            : "Unable to delete revenue",
                    exception.getMessage()
            );
        }
    }

    /**
     * Deletes only attachments managed by GymFlow.
     *
     * Revenue attachments are copied by the application to:
     * data/revenue_attachments
     *
     * The directory check protects against accidentally deleting an arbitrary
     * external user file if an old/invalid database path is stored.
     */
    private void deleteRevenueAttachment(
            String attachmentPath) throws Exception {

        if (attachmentPath == null
                || attachmentPath.isBlank()) {

            return;
        }

        Path attachment =
                Path.of(
                        attachmentPath
                )
                        .toAbsolutePath()
                        .normalize();

        Path managedDirectory =
                Path.of(
                        "data",
                        "revenue_attachments"
                )
                        .toAbsolutePath()
                        .normalize();

        if (!attachment.startsWith(
                managedDirectory
        )) {

            throw new IllegalStateException(
                    LanguageManager.isArabic()
                            ? "المرفق خارج مجلد مرفقات GymFlow، لذلك لم يتم حذفه تلقائيًا: "
                                    + attachment
                            : "The attachment is outside the GymFlow attachment directory and was not deleted automatically: "
                                    + attachment
            );
        }

        Files.deleteIfExists(
                attachment
        );
    }

    private Window getOwnerWindow() {

        return revenuesTable == null
                || revenuesTable.getScene() == null
                ? null
                : revenuesTable
                        .getScene()
                        .getWindow();
    }

    private boolean hasAttachment(
            Revenue revenue) {

        return revenue != null
                && revenue.getAttachmentPath()
                        != null
                && !revenue.getAttachmentPath()
                        .isBlank();
    }

    private String formatMoney(
            BigDecimal value) {

        BigDecimal amount =
                value == null
                        ? BigDecimal.ZERO
                        : value;

        return MONEY_FORMAT.format(
                amount
        )
                + " EGP";
    }

    private String buttonStyle(
            String background) {

        return "-fx-background-color:"
                + background
                + ";"
                + "-fx-text-fill:white;"
                + "-fx-background-radius:7;"
                + "-fx-padding:7 11;"
                + "-fx-cursor:hand;";
    }

    private void setText(
            Label label,
            String value) {

        if (label != null) {

            label.setText(
                    value
            );
        }
    }

    private void setText(
            Button button,
            String value) {

        if (button != null) {

            button.setText(
                    value
            );
        }
    }

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }

    private void showWarning(
            String title,
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.WARNING
                );

        alert.setTitle(
                title
        );

        alert.setHeaderText(
                null
        );

        alert.setContentText(
                message
        );

        DialogStyler.style(alert);

        WindowManager.showResponsiveDialogAndWait(
                alert,
                getOwnerWindow(),
                520.0,
                320.0
        );
    }

    private void showError(
            String title,
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle(
                title
        );

        alert.setHeaderText(
                null
        );

        alert.setContentText(
                message == null
                        || message.isBlank()
                        ? (
                                LanguageManager.isArabic()
                                        ? "حدث خطأ غير متوقع."
                                        : "Unexpected error."
                        )
                        : message
        );

        DialogStyler.style(alert);

        WindowManager.showResponsiveDialogAndWait(
                alert,
                getOwnerWindow(),
                520.0,
                320.0
        );
    }
}

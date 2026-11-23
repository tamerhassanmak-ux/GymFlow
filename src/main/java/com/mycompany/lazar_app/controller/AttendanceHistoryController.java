package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.ui.paging.TablePager;
import com.mycompany.lazar_app.repository.CheckInRepository.AttendanceFilter;
import com.mycompany.lazar_app.repository.CheckInRepository.AttendanceField;
import com.mycompany.lazar_app.repository.CheckInRepository.AttendanceOrder;
import javafx.scene.layout.HBox;
import java.util.ArrayList;
import java.util.List;
import com.mycompany.lazar_app.model.CheckIn;
import com.mycompany.lazar_app.repository.CheckInRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/** Searchable attendance archive. Read-only: never changes recorded attendance. */
public final class AttendanceHistoryController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> rangeFilter;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private TableView<CheckIn> historyTable;
    @FXML private TableColumn<CheckIn,String> idColumn, codeColumn, nameColumn,
            dateColumn, inColumn, outColumn, statusColumn, membershipColumn;
    @FXML private Label recordsLabel, insideLabel, outLabel, feedbackLabel;
    @FXML private Button refreshButton, resetButton;

    private final CheckInRepository repository = new CheckInRepository();
    @FXML private HBox paginationHost;
    private TablePager<CheckIn> pager;
    private final PauseTransition typingDelay = new PauseTransition(Duration.millis(250));
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    private ResourceBundle bundle;
    private boolean applyingPreset;

    private String tr(String key, String fallback) {
        try { return bundle != null && bundle.containsKey(key) ? bundle.getString(key) : fallback; }
        catch (Exception ex) { return fallback; }
    }
    private String str(String value) { return value == null ? "" : value; }
    @FXML public void initialize() {
        bundle = LanguageManager.getBundle();
        idColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().getId())));
        codeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(str(c.getValue().getMemberCode())));
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(str(c.getValue().getMemberName())));
        dateColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(date(c.getValue().getCheckInTime())));
        inColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(time(c.getValue().getCheckInTime())));
        outColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(time(c.getValue().getCheckOutTime())));
        membershipColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(str(c.getValue().getMembershipDisplayName(LanguageManager.isArabic()))));
        statusColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().isInside()
                ? tr("history.status.inside", "Inside") : tr("history.status.out", "Checked out")));
        historyTable.setItems(FXCollections.observableArrayList());
        historyTable.setPlaceholder(new Label(tr("history.empty", "No attendance records match the filters")));
        rangeFilter.getItems().setAll(tr("history.range.today","Today"), tr("history.range.yesterday","Yesterday"),
                tr("history.range.week","Last 7 days"), tr("history.range.month","This month"),
                tr("history.range.all","All dates"), tr("history.range.custom","Custom range"));
        rangeFilter.getSelectionModel().select(0);
        rangeFilter.valueProperty().addListener((obs, oldValue, value) -> {
            if (value != null && !applyingPreset) { applyPreset(rangeFilter.getSelectionModel().getSelectedIndex()); filter(); }
        });
        fromDate.valueProperty().addListener((obs, oldValue, value) -> {
            if (applyingPreset) return;
            selectCustomRange();
            filter();
        });
        toDate.valueProperty().addListener((obs, oldValue, value) -> {
            if (applyingPreset) return;
            selectCustomRange();
            filter();
        });
        typingDelay.setOnFinished(event -> filter());
        searchField.textProperty().addListener((obs, before, after) -> {
            if (!applyingPreset) typingDelay.playFromStart();
        });
        applyPreset(0);
        pager = new TablePager<>(historyTable, paginationHost, (page, size) -> {
            AttendanceFilter request = new AttendanceFilter(fromDate.getValue(), toDate.getValue(),
                    searchField.getText(), false, attendanceSort());
            return () -> repository.findPage(request, page, size);
        }, result -> {
            feedbackLabel.setText("");
            recordsLabel.setText(Long.toString(result.total()));
            insideLabel.setText(Long.toString(result.statistics().get("inside")));
            outLabel.setText(Long.toString(result.statistics().get("out")));
        }, error -> {
            error.printStackTrace();
            feedbackLabel.setText(tr("history.error", "Unable to load attendance records"));
            recordsLabel.setText("—"); insideLabel.setText("—"); outLabel.setText("—");
        });
        refresh();
    }
    private String date(LocalDateTime time) { return time == null ? "—" : time.format(dateFormat); }
    private String time(LocalDateTime time) { return time == null ? "—" : time.format(timeFormat); }
    private void applyPreset(int index) {
        LocalDate today = LocalDate.now();
        LocalDate start; LocalDate end;
        switch (index) {
            case 0 -> { start = today; end = today; }
            case 1 -> { start = today.minusDays(1); end = start; }
            case 2 -> { start = today.minusDays(6); end = today; }
            case 3 -> { start = today.withDayOfMonth(1); end = today; }
            case 4 -> { start = null; end = null; }
            default -> { return; }
        }
        applyingPreset = true;
        try {
            fromDate.setValue(start);
            toDate.setValue(end);
        } finally { applyingPreset = false; }
    }
    private void selectCustomRange() {
        applyingPreset = true;
        try { rangeFilter.getSelectionModel().select(5); }
        finally { applyingPreset = false; }
    }
    @FXML private void reset() {
        applyingPreset = true;
        try {
            searchField.clear();
            rangeFilter.getSelectionModel().select(0);
        } finally { applyingPreset = false; }
        applyPreset(0);
        filter();
    }
    @FXML private void refresh() {
        if (pager != null) filter();
    }

    private List<AttendanceOrder> attendanceSort() {
        List<AttendanceOrder> orders = new ArrayList<>();
        for (TableColumn<CheckIn, ?> column : historyTable.getSortOrder()) {
            AttendanceField field = column == idColumn ? AttendanceField.ID
                    : column == codeColumn ? AttendanceField.CODE
                    : column == nameColumn ? AttendanceField.NAME
                    : column == membershipColumn ? (LanguageManager.isArabic() ? AttendanceField.PLAN_AR : AttendanceField.PLAN_EN)
                    : column == outColumn ? AttendanceField.CHECK_OUT
                    : column == statusColumn ? AttendanceField.STATUS : AttendanceField.CHECK_IN;
            orders.add(new AttendanceOrder(field, column.getSortType() == TableColumn.SortType.ASCENDING));
        }
        return orders;
    }

    private void filter() {
        typingDelay.stop();
        if (pager == null || applyingPreset) return;
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from != null && to != null && from.isAfter(to)) {
            pager.clear();
            feedbackLabel.setText(tr("history.invalidDate", "Start date must not be after end date"));
            recordsLabel.setText("0"); insideLabel.setText("0"); outLabel.setText("0");
        } else {
            feedbackLabel.setText("");
            pager.first();
        }
    }
}

package com.mycompany.lazar_app.controller;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.CheckIn;
import com.mycompany.lazar_app.repository.CheckInRepository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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
    private final ObservableList<CheckIn> records = FXCollections.observableArrayList();
    private final FilteredList<CheckIn> visible = new FilteredList<>(records);
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
    private String normalize(String value) {
        return str(value).strip().toLowerCase(Locale.ROOT)
                .replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي')
                .replace('ة','ه').replaceAll("[\\u064B-\\u065F\\u0670]", "");
    }
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
        historyTable.setItems(visible);
        historyTable.setPlaceholder(new Label(tr("history.empty", "No attendance records match the filters")));
        rangeFilter.getItems().setAll(tr("history.range.today","Today"), tr("history.range.yesterday","Yesterday"),
                tr("history.range.week","Last 7 days"), tr("history.range.month","This month"),
                tr("history.range.all","All dates"), tr("history.range.custom","Custom range"));
        rangeFilter.getSelectionModel().select(0);
        rangeFilter.valueProperty().addListener((obs, oldValue, value) -> {
            if (value != null) { applyPreset(rangeFilter.getSelectionModel().getSelectedIndex()); filter(); }
        });
        fromDate.valueProperty().addListener((obs, oldValue, value) -> {
            if (!applyingPreset && rangeFilter.getSelectionModel().getSelectedIndex() != 5) rangeFilter.getSelectionModel().select(5);
            filter();
        });
        toDate.valueProperty().addListener((obs, oldValue, value) -> {
            if (!applyingPreset && rangeFilter.getSelectionModel().getSelectedIndex() != 5) rangeFilter.getSelectionModel().select(5);
            filter();
        });
        typingDelay.setOnFinished(event -> filter());
        searchField.textProperty().addListener((obs, before, after) -> typingDelay.playFromStart());
        applyPreset(0);
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
    @FXML private void reset() {
        searchField.clear();
        rangeFilter.getSelectionModel().select(0);
        applyPreset(0);
        filter();
    }
    @FXML private void refresh() {
        try {
            records.setAll(repository.findAll());
            feedbackLabel.setText("");
            filter();
        } catch (SQLException ex) {
            ex.printStackTrace();
            feedbackLabel.setText(tr("history.error","Unable to load attendance records"));
            new Alert(Alert.AlertType.ERROR, tr("history.error","Unable to load attendance records")).showAndWait();
        }
    }
    private void filter() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        String query = normalize(searchField.getText());
        if (from != null && to != null && from.isAfter(to)) {
            feedbackLabel.setText(tr("history.invalidDate","Start date must not be after end date"));
            visible.setPredicate(item -> false);
        } else {
            feedbackLabel.setText("");
            visible.setPredicate(item -> {
                LocalDateTime checkIn = item.getCheckInTime();
                if (checkIn == null) return false;
                LocalDate day = checkIn.toLocalDate();
                if (from != null && day.isBefore(from)) return false;
                if (to != null && day.isAfter(to)) return false;
                if (query.isEmpty()) return true;
                return normalize(item.getMemberName()).contains(query)
                        || normalize(item.getMemberCode()).contains(query)
                        || normalize(item.getPhone()).contains(query);
            });
        }
        int inside = 0, out = 0;
        for (CheckIn item : visible) { if (item.isInside()) inside++; else out++; }
        recordsLabel.setText(String.valueOf(visible.size()));
        insideLabel.setText(String.valueOf(inside));
        outLabel.setText(String.valueOf(out));
    }
}

package com.mycompany.lazar_app.ui;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.*;
import java.util.*;

/** HBox-compatible row that wraps at the available logical width, retaining FXML IDs and state. */
public class ResponsiveRow extends HBox {
    public ResponsiveRow() { setMinWidth(0); }
    public ResponsiveRow(Node... children) { this(); getChildren().setAll(children); }
    public ResponsiveRow(double spacing, Node... children) { this(children); setSpacing(spacing); }
    @Override public Orientation getContentBias() { return Orientation.HORIZONTAL; }
    private boolean spacer(Node n) { return n.getClass() == Region.class && HBox.getHgrow(n) == Priority.ALWAYS; }
    private double preferred(Node n, double available) {
        return Math.min(available, Math.max(0, Math.max(n.minWidth(-1), n.prefWidth(-1))));
    }
    private List<List<Node>> rows(double available) {
        List<List<Node>> rows = new ArrayList<>();
        List<Node> row = new ArrayList<>(); double used = 0;
        for (Node n : getManagedChildren()) {
            if (spacer(n)) continue;
            double width = preferred(n, available);
            if (!row.isEmpty() && used + getSpacing() + width > available + 0.5) {
                rows.add(row); row = new ArrayList<>(); used = 0;
            }
            used += (row.isEmpty() ? 0 : getSpacing()) + width; row.add(n);
        }
        if (!row.isEmpty()) rows.add(row);
        return rows;
    }
    private double available(double width) { return Math.max(1, width - snappedLeftInset() - snappedRightInset()); }
    private boolean wraps(double width) {
        return width > 0 && super.computePrefWidth(-1) > width + 0.5;
    }
    @Override protected double computeMinWidth(double height) { return snappedLeftInset() + snappedRightInset(); }
    @Override protected double computeMinHeight(double width) { return computePrefHeight(width); }
    @Override protected double computePrefHeight(double width) {
        if (!wraps(width)) return super.computePrefHeight(width);
        double available = available(width), height = snappedTopInset() + snappedBottomInset();
        var rows = rows(available);
        for (var row : rows) height += row.stream().mapToDouble(n -> n.prefHeight(preferred(n, available))).max().orElse(0);
        return height + Math.max(0, rows.size() - 1) * Math.max(8, getSpacing());
    }
    @Override protected void layoutChildren() {
        if (!wraps(getWidth())) { super.layoutChildren(); return; }
        double available = available(getWidth()), y = snappedTopInset();
        for (Node n : getManagedChildren()) if (spacer(n)) n.resizeRelocate(0, 0, 0, 0);
        for (var row : rows(available)) {
            double used = row.stream().mapToDouble(n -> preferred(n, available)).sum() + Math.max(0, row.size() - 1) * getSpacing();
            long growing = row.stream().filter(n -> HBox.getHgrow(n) == Priority.ALWAYS).count();
            double extra = growing == 0 ? 0 : Math.max(0, available - used) / growing;
            double x = snappedLeftInset();
            if (growing == 0) x += switch (getAlignment().getHpos()) {
                case RIGHT -> Math.max(0, available - used);
                case CENTER -> Math.max(0, available - used) / 2;
                default -> 0;
            };
            double rowHeight = 0;
            for (Node n : row) {
                double w = preferred(n, available) + (HBox.getHgrow(n) == Priority.ALWAYS ? extra : 0);
                double h = n.prefHeight(w);
                n.resizeRelocate(snapPositionX(x), snapPositionY(y), snapSizeX(w), snapSizeY(h));
                x += w + getSpacing(); rowHeight = Math.max(rowHeight, h);
            }
            y += rowHeight + Math.max(8, getSpacing());
        }
    }
}

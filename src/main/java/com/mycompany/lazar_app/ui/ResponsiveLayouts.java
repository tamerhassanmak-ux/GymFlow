package com.mycompany.lazar_app.ui;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Layout adaptation only: never recreates controls/controllers or changes business values. */
public final class ResponsiveLayouts {
    private ResponsiveLayouts() {}
    public static Parent prepare(Parent root) {
        if (root.getProperties().putIfAbsent("gymflow.responsive.prepared", true) != null) return root;
        adapt(root);
        if (root instanceof Region region) region.setMinSize(0,0);
        if (root instanceof BorderPane dashboard && root.getStyleClass().contains("dashboard-shell")) navigation(dashboard);
        if (root instanceof BorderPane border) {
            if(border.getTop()!=null && !root.getStyleClass().contains("dashboard-shell")){
                Node header=border.getTop();border.setTop(null);
                ScrollPane headerViewport=header instanceof ScrollPane existing?existing:scroll(header);
                headerViewport.getStyleClass().add("responsive-header");
                headerViewport.prefHeightProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                    ()->Math.min((header instanceof ScrollPane nested&&nested.getContent()!=null?nested.getContent():header).prefHeight(Math.max(1,border.getWidth())),Math.max(0,Math.min(border.getHeight()*.28,border.getHeight()-border.getInsets().getTop()-border.getInsets().getBottom()-(border.getBottom()==null?0:border.getBottom().prefHeight(Math.max(1,border.getWidth()-border.getInsets().getLeft()-border.getInsets().getRight())))-48))),border.widthProperty(),border.heightProperty()));
                headerViewport.setMinHeight(0);border.setTop(headerViewport);
            }
            Node center = border.getCenter();
            if(center instanceof BorderPane nested) prepare(nested);
            else if (center != null && !root.getStyleClass().contains("dashboard-shell") && !(center instanceof ScrollPane) && !(center instanceof Control)) {
                border.setCenter(null); border.setCenter(scroll(center));
            }
            if (border.getBottom() != null) {
                Node footer=border.getBottom();footer.getStyleClass().add("persistent-actions");
                if(footer instanceof Region r){r.setMinHeight(Region.USE_PREF_SIZE);r.setPrefHeight(Region.USE_COMPUTED_SIZE);r.setMaxHeight(Double.MAX_VALUE);}
            }
        }
        root.sceneProperty().addListener((obs, oldScene, scene) -> { if (scene != null) installFocusReveal(scene); });
        if (root.getScene() != null) installFocusReveal(root.getScene());
        return root;
    }
    private static void navigation(BorderPane dashboard) {
        Node navigation=dashboard.getLeft(); if(navigation==null)return;
        ToggleButton menu=new ToggleButton(com.mycompany.lazar_app.LanguageManager.isArabic()?"القائمة":"Menu");
        menu.setId("navigationToggle");menu.setAccessibleText(menu.getText());
        VBox header=new VBox(8);header.setMinWidth(0);
        Runnable update=()->{
            boolean narrow=dashboard.getWidth()>0&&dashboard.getWidth()<900;
            dashboard.setLeft(null);dashboard.setTop(null);header.getChildren().clear();
            if(narrow){
                header.getChildren().add(menu);
                if(menu.isSelected()){
                    if(navigation instanceof Region r){r.setMaxHeight(180);r.setPrefHeight(180);}
                    header.getChildren().add(navigation);
                }
                dashboard.setTop(header);
            }else{
                if(navigation instanceof Region r){r.setMaxHeight(Double.MAX_VALUE);r.setPrefHeight(Region.USE_COMPUTED_SIZE);r.setPrefWidth(250);}
                dashboard.setLeft(navigation);
            }
        };
        dashboard.widthProperty().addListener((o,b,a)->update.run());menu.selectedProperty().addListener((o,b,a)->update.run());update.run();
    }
    public static ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true); scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMinSize(0, 0); scroll.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        scroll.getStyleClass().add("responsive-content");
        if (content instanceof Region region) { region.setMinWidth(0); region.setMinHeight(Region.USE_PREF_SIZE); }
        return scroll;
    }
    public static void adapt(Node node) {
        if (node instanceof Region region && !(node instanceof Control)) {
            region.setMinWidth(0); region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            if(node instanceof VBox || node instanceof AnchorPane) region.setPrefHeight(Region.USE_COMPUTED_SIZE);
        }
        if (node instanceof Label label) { label.setWrapText(true); label.setMinWidth(0); label.setMinHeight(Region.USE_PREF_SIZE); }
        if (node instanceof Button button) { button.setWrapText(true); button.setPrefHeight(Region.USE_COMPUTED_SIZE); button.setMinHeight(Region.USE_PREF_SIZE); }
        if (node instanceof TextArea area) area.setWrapText(true);
        if (node instanceof TableView<?> table) {
            table.setMinSize(0,0);
            javafx.scene.shape.Rectangle clip=new javafx.scene.shape.Rectangle();
            clip.widthProperty().bind(table.widthProperty());clip.heightProperty().bind(table.heightProperty());table.setClip(clip);
        }
        if (node instanceof javafx.scene.chart.Chart) return;
        if (node instanceof ScrollPane preview && preview.getStyleClass().contains("document-preview")) {
            preview.setMinSize(0,0);preview.setFitToWidth(false);preview.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);return;
        }
        if (node instanceof ScrollPane scroll) {
            scroll.setFitToWidth(true); scroll.setFitToHeight(false); scroll.setMinSize(0,0);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            if (scroll.getContent() != null) { adapt(scroll.getContent()); if (scroll.getContent() instanceof Region r) r.setMinHeight(Region.USE_PREF_SIZE); }
        } else if (node instanceof TitledPane titled) { if (titled.getContent() != null) adapt(titled.getContent()); }
        else if (node instanceof SplitPane split) split.getItems().forEach(ResponsiveLayouts::adapt);
        else if (node instanceof Parent parent && !(node instanceof Control)) {
            java.util.List.copyOf(parent.getChildrenUnmodifiable()).forEach(ResponsiveLayouts::adapt);
        }
    }
    public static void installFocusReveal(Scene scene) {
        if (scene.getProperties().putIfAbsent("gymflow.focus.reveal", true) != null) return;
        scene.focusOwnerProperty().addListener((obs,before,after) -> { if(after != null) Platform.runLater(() -> reveal(after)); });
    }
    public static void focusInvalid(Node field) {
        if (field == null) return;
        field.requestFocus(); Platform.runLater(() -> reveal(field));
    }
    public static void reveal(Node field) {
        if (field.getScene() == null) return;
        field.getScene().getRoot().applyCss(); field.getScene().getRoot().layout();
        for (Parent parent=field.getParent(); parent!=null; parent=parent.getParent()) {
            if (!(parent instanceof ScrollPane scroll) || scroll.getContent()==null) continue;
            Node content=scroll.getContent();
            Bounds target=content.sceneToLocal(field.localToScene(field.getBoundsInLocal()));
            double total=content.getBoundsInLocal().getHeight(), viewport=scroll.getViewportBounds().getHeight();
            double overflow=total-viewport;
            if(overflow<=0)continue;
            double top=scroll.getVvalue()*overflow;
            double desired=target.getMinY()<top ? target.getMinY()-12 : target.getMaxY()>top+viewport ? target.getMaxY()-viewport+12 : top;
            scroll.setVvalue(Math.max(0,Math.min(1,desired/overflow)));
        }
    }
}

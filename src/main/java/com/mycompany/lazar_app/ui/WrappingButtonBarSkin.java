package com.mycompany.lazar_app.ui;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.geometry.Pos;

/** Keeps DialogPane's actual buttons, event filters, default/cancel roles and accessible names. */
final class WrappingButtonBarSkin extends SkinBase<ButtonBar> {
    private final ResponsiveRow flow=new ResponsiveRow();
    private final ListChangeListener<Node> listener=change -> update();
    WrappingButtonBarSkin(ButtonBar bar) {
        super(bar);flow.setSpacing(10);flow.setAlignment(Pos.CENTER_RIGHT);flow.setMinWidth(0);
        getChildren().add(flow);bar.getButtons().addListener(listener);update();
    }
    private void update(){flow.getChildren().setAll(getSkinnable().getButtons());getSkinnable().requestLayout();}
    @Override protected double computeMinWidth(double h,double top,double right,double bottom,double left){return left+right;}
    @Override protected double computePrefWidth(double h,double top,double right,double bottom,double left){return flow.prefWidth(h)+left+right;}
    @Override protected double computeMinHeight(double w,double top,double right,double bottom,double left){return computePrefHeight(w,top,right,bottom,left);}
    @Override protected double computePrefHeight(double w,double top,double right,double bottom,double left){return flow.prefHeight(Math.max(1,w-left-right))+top+bottom;}
    @Override protected void layoutChildren(double x,double y,double w,double h){flow.resizeRelocate(x,y,w,h);}
    @Override public void dispose(){getSkinnable().getButtons().removeListener(listener);super.dispose();}
}

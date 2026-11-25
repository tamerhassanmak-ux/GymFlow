package com.mycompany.lazar_app.ui;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

/** Collapses multi-column field groups into a single column without recreating controls. */
public class ResponsiveGrid extends GridPane {
    private record Cell(int row,int column,Integer rows,Integer columns){}
    private final Map<Node,Cell> cells=new LinkedHashMap<>();
    private List<ColumnConstraints> columns;
    private boolean narrow;
    private static int number(Integer value){return value==null?0:value;}
    public ResponsiveGrid(){setMinWidth(0);}
    @Override public Orientation getContentBias(){return Orientation.HORIZONTAL;}
    private void reflow(double width){
        if(width<=0)return;
        if(columns==null)columns=List.copyOf(getColumnConstraints());
        for(Node child:getChildren())cells.computeIfAbsent(child,n->new Cell(number(getRowIndex(n)),number(getColumnIndex(n)),getRowSpan(n),getColumnSpan(n)));
        int count=Math.max(columns.size(),cells.values().stream().mapToInt(c->c.column()+1).max().orElse(1));
        boolean collapse=count>1 && width-getInsets().getLeft()-getInsets().getRight()<Math.min(760,count*260);
        if(collapse==narrow)return;
        narrow=collapse;
        if(narrow){
            boolean columnGroups=cells.entrySet().stream().anyMatch(e->(e.getKey() instanceof TextInputControl||e.getKey() instanceof ComboBoxBase<?>)
                &&cells.entrySet().stream().anyMatch(label->label.getKey() instanceof Label&&label.getValue().column()==e.getValue().column()&&label.getValue().row()<e.getValue().row()));
            var order=new ArrayList<>(cells.keySet());
            order.sort(columnGroups?Comparator.comparingInt((Node n)->cells.get(n).column()).thenComparingInt(n->cells.get(n).row())
                :Comparator.comparingInt((Node n)->cells.get(n).row()).thenComparingInt(n->cells.get(n).column()));
            ColumnConstraints one=new ColumnConstraints();one.setPercentWidth(100);one.setHgrow(Priority.ALWAYS);getColumnConstraints().setAll(one);
            int row=0;for(Node child:order){setRowIndex(child,row++);setColumnIndex(child,0);setRowSpan(child,1);setColumnSpan(child,1);}
        }else{
            getColumnConstraints().setAll(columns);cells.forEach((child,cell)->{setRowIndex(child,cell.row());setColumnIndex(child,cell.column());setRowSpan(child,cell.rows());setColumnSpan(child,cell.columns());});
        }
    }
    @Override protected double computePrefHeight(double width){reflow(width);return super.computePrefHeight(width);}
    @Override protected double computeMinHeight(double width){reflow(width);return super.computeMinHeight(width);}
    @Override protected void layoutChildren(){reflow(getWidth());super.layoutChildren();}
}

package com.mycompany.lazar_app.ui;

import javafx.scene.*;
import javafx.scene.control.ScrollPane;

/** Preserve a page's persistent header/footer; only its content scrolls. */
public final class PageViewport {
    private PageViewport() {}
    public static Parent wrap(Node page) {
        if(page instanceof javafx.scene.layout.BorderPane border)return ResponsiveLayouts.prepare(border);
        if(page instanceof ScrollPane scroll){ResponsiveLayouts.adapt(scroll);return scroll;}
        if(page instanceof Parent parent)ResponsiveLayouts.prepare(parent);
        return ResponsiveLayouts.scroll(page);
    }
}

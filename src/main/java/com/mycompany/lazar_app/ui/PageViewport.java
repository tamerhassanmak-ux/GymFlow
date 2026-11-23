package com.mycompany.lazar_app.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

/** Shared scrolling policy for pages loaded into the main dashboard. */
public final class PageViewport {
    private PageViewport() {}

    public static ScrollPane wrap(Node page) {
        // Reuse pages which already scroll instead of nesting two viewports.
        if (page instanceof ScrollPane scroll) {
            prepareContent(scroll.getContent());
            configureNestedScrolling(scroll.getContent());
            configure(scroll);
            return scroll;
        }
        prepareContent(page);
        if (!page.getStyleClass().contains("bounded-page")) configureNestedScrolling(page);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setNodeOrientation(page.getNodeOrientation());
        configure(scroll);
        return scroll;
    }

    private static void prepareContent(Node content) {
        if (!(content instanceof Region region)) return;
        // Keep the designed width, but let height follow all the child cards.
        // Fixed FXML heights on AnchorPane/VBox otherwise compress the last rows.
        if (region.getStyleClass().contains("bounded-page")) {
            // A table page owns its scrolling and pins its pager in BorderPane.bottom.
            // Fill the available area instead of forcing the whole page's design size.
            region.setMinWidth(0);
            region.setMinHeight(0);
            region.setPrefHeight(Region.USE_COMPUTED_SIZE);
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            return;
        }
        double designWidth = Math.max(region.prefWidth(-1), region.minWidth(-1));
        region.setPrefHeight(Region.USE_COMPUTED_SIZE);
        region.setMinHeight(Region.USE_PREF_SIZE);
        region.setMinWidth(Math.max(0, designWidth));
        region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    private static void configure(ScrollPane scroll) {
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMinSize(0, 0);
        scroll.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        if (!scroll.getStyleClass().contains("app-page-viewport")) {
            scroll.getStyleClass().add("app-page-viewport");
        }
    }

    private static void configureNestedScrolling(Node node) {
        if (node instanceof ScrollPane scroll) {
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            prepareContent(scroll.getContent());
            configureNestedScrolling(scroll.getContent());
        } else if (node instanceof Parent parent && !(node instanceof javafx.scene.control.Control)) {
            // Traverse FXML layouts, leaving control skins and virtualized tables alone.
            for (Node child : parent.getChildrenUnmodifiable()) configureNestedScrolling(child);
        }
    }
}

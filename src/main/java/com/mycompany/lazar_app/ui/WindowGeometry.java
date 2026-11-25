package com.mycompany.lazar_app.ui;

import javafx.geometry.Rectangle2D;
import java.util.List;

/** Pure logical-coordinate geometry; negative monitor origins and removed monitors are supported. */
public final class WindowGeometry {
    private WindowGeometry() {}
    private static double finite(double value,double fallback){return Double.isFinite(value)?value:fallback;}
    public static Rectangle2D screenFor(Rectangle2D window,List<Rectangle2D> screens) {
        if(screens.isEmpty())throw new IllegalArgumentException("A visual screen is required.");
        Rectangle2D best=screens.getFirst();double area=-1;
        for(var screen:screens){
            double overlap=Math.max(0,Math.min(window.getMaxX(),screen.getMaxX())-Math.max(window.getMinX(),screen.getMinX()))
                    *Math.max(0,Math.min(window.getMaxY(),screen.getMaxY())-Math.max(window.getMinY(),screen.getMinY()));
            if(overlap>area){area=overlap;best=screen;}
        }
        return best;
    }
    public static Rectangle2D fit(Rectangle2D requested,Rectangle2D visual) {
        double w=Math.min(visual.getWidth(),Math.max(1,finite(requested.getWidth(),960)));
        double h=Math.min(visual.getHeight(),Math.max(1,finite(requested.getHeight(),680)));
        double x=Math.max(visual.getMinX(),Math.min(finite(requested.getMinX(),visual.getMinX()),visual.getMaxX()-w));
        double y=Math.max(visual.getMinY(),Math.min(finite(requested.getMinY(),visual.getMinY()),visual.getMaxY()-h));
        return new Rectangle2D(x,y,w,h);
    }
}

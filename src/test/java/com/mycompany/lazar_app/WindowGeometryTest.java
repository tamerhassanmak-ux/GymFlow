package com.mycompany.lazar_app;
import com.mycompany.lazar_app.ui.WindowGeometry;
import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class WindowGeometryTest {
 @Test void allResolutionsAndLogicalScalingFit(){
  int[][] resolutions={{1024,768},{1280,720},{1366,768},{1440,900},{1600,900},{1920,1080},{2560,1440},{3840,2160}};
  for(int[] r:resolutions)for(double scale:new double[]{1,1.25,1.5,1.75,2}){
   var screen=new Rectangle2D(0,0,r[0]/scale,Math.max(1,r[1]/scale-40));
   var fitted=WindowGeometry.fit(new Rectangle2D(10000,-500,1400,850),screen);
   assertTrue(screen.contains(fitted),Arrays.toString(r)+" scale="+scale);
  }
 }
 @Test void removedAndNegativeOriginMonitors(){
  var left=new Rectangle2D(-1920,-200,1920,1080);var main=new Rectangle2D(0,0,1366,728);
  var old=new Rectangle2D(-1800,-100,900,680);assertEquals(left,WindowGeometry.screenFor(old,List.of(main,left)));
  assertTrue(main.contains(WindowGeometry.fit(old,WindowGeometry.screenFor(old,List.of(main)))));
 }
 @Test void spanningWindowChoosesLargestIntersection(){
  var a=new Rectangle2D(0,0,1920,1080);var b=new Rectangle2D(1920,0,1920,1080);
  assertEquals(b,WindowGeometry.screenFor(new Rectangle2D(1850,20,1000,680),List.of(a,b)));
 }
 @Test void corruptSizesAndTinyWorkAreasFailSafely(){
  var screen=new Rectangle2D(10,20,240,160);assertTrue(screen.contains(WindowGeometry.fit(new Rectangle2D(Double.NaN,Double.NaN,Double.NaN,Double.NaN),screen)));
  assertThrows(IllegalArgumentException.class,()->WindowGeometry.screenFor(screen,List.of()));
 }
}

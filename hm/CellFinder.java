package hm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import profile.Profiler;

import map.AffineTransform;
import map.Map;
import map.SampleMap;
import map.SampleMap2;
import map.Utils;
import map.properties.ImagePosition;
import map.properties.ImageSetProperties;
import map.properties.StitchStackProperties;
import segment.Segment;
import general.collections.Pair;
import general.numerals.BigRational;
import geometry.AffinePoint;

public class CellFinder {
  
  public static AffinePoint getPoint(double[] coords) {
    final int k = 8;
    double m = 1;
    for (int i = 0; i != k; ++i)
      m *= 2;
    
    for (int i = 0; i != 2; ++i)
      coords[i] = Math.floor(coords[i] * m) / m;
    
    return new AffinePoint(new BigRational(coords[0]), new BigRational(coords[1]));
  }
  
  public static Pair<Double, Double> getPoint(AffinePoint point) {
    return new Pair<Double, Double>(point.getX().doubleValue(), point.getY().doubleValue());
  }
  
  public static ArrayList<Pair<AffinePoint, AffinePoint>> getRectangle(double[][] coords) {
    AffinePoint[] points = new AffinePoint[4];
    for (int i = 0; i != 4; ++i)
      points[i] = getPoint(coords[i]);
    
    ArrayList<Pair<AffinePoint, AffinePoint>> result =
        new ArrayList<Pair<AffinePoint, AffinePoint>>();
    for (int i = 0; i != 4; ++i)
      result.add(new Pair<AffinePoint, AffinePoint>(points[i], points[(i + 1) % 4]));
    
    return result;
  }
  
  public static Collection<Pair<AffinePoint, AffinePoint>>[] computeVoroniCells(
      ImageSetProperties imageSet, int channel, AffineTransform transform, double distance) {
    Map map =
        Utils.getMap(imageSet.getSize(), imageSet.getOpticalProperties(channel),
            imageSet.getPerspectiveProperties(), AffineTransform.ID);
    double[] inwards = Utils.getInwardsBoundary(imageSet.getSize(), map, distance);
    
    double[][] imageCorners = new double[4][2];
    for (int i = 0; i != 4; ++i)
      for (int j = 0; j != 2; ++j)
        imageCorners[i][j] = inwards[((i + 1 - j) % 4) / 2 * 2 + j];
    
    double[] hullBoundary = Utils.getBoundary(imageSet, transform);
    
    double[][] hullCorners = new double[4][2];
    for (int i = 0; i != 4; ++i)
      for (int j = 0; j != 2; ++j)
        hullCorners[i][j] = hullBoundary[((i + 1 - j) % 4) / 2 * 2 + j];
    
    int n = imageSet.getNumImages();
    double[] coords = new double[2];
    
    ArrayList<Pair<AffinePoint, AffinePoint>> hull = getRectangle(hullCorners);
    AffinePoint[] points = new AffinePoint[n];
    
    @SuppressWarnings("unchecked")
    ArrayList<Pair<AffinePoint, AffinePoint>>[] regions =
        (ArrayList<Pair<AffinePoint, AffinePoint>>[]) new ArrayList[n];
    
    for (int image = 0; image != n; ++image) {
      ImagePosition position = imageSet.getPosition(image);
      AffineTransform t =
          transform.after(AffineTransform.getTranslation(position.getX(), position.getY()));
      
      for (int j = 0; j != 2; ++j)
        coords[j] = 0;
      t.map(coords, coords);
      points[image] = getPoint(coords);
      
      double[][] regionCorners = new double[4][2];
      for (int i = 0; i != 4; ++i)
        t.map(imageCorners[i], regionCorners[i]);
      
      regions[image] = getRectangle(regionCorners);
    }
    
    return geometry.CellFinder.buildCells(hull, points, regions);
  }
  
  @SuppressWarnings("unchecked")
  public static Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] getEdges(
      Collection<Pair<AffinePoint, AffinePoint>>[] regions) {
    ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>> result =
        new ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>>();
    
    for (int i = 0; i != regions.length; ++i) {
      for (Pair<AffinePoint, AffinePoint> segment : regions[i]) {
        Pair<Double, Double> a = getPoint(segment.getA());
        Pair<Double, Double> b = getPoint(segment.getB());
        boolean s = a.getB() < b.getB();
        
        Segment<Pair<Double, Double>> seg =
            new Segment<Pair<Double, Double>>((Pair<Double, Double>[]) new Pair[] {}, s ? a : b, s
                ? b : a);
        Pair<Integer, Integer> col = new Pair<Integer, Integer>(s ? i : -1, s ? -1 : i);
        result.add(new Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>(seg, col));
      }
    }
    
    return result
        .toArray((Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[]) new Pair[] {});
  }
  
  public static Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] computeVoroniEdges(
      ImageSetProperties imageSet, int channel, AffineTransform transform, double distance) {
    return getEdges(computeVoroniCells(imageSet, channel, transform, distance));
  }
  
  public static Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] computeVoroniEdges(
      StitchStackProperties stack, int stitch, int channel, double distance) {
    return getEdges(computeVoroniCells(stack.getImageSetProperties(stitch), channel,
        stack.getTransform(stitch), distance));
  }
  
}

package hm;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import segment.Segment;
import segment.SharpnessEvaluator;

import general.collections.Pair;
import general.numerals.BigRational;
import map.AffineTransform;
import map.Map;
import map.Utils;
import map.properties.ImagePosition;
import map.properties.ImageSetProperties;
import map.properties.ImageSize;
import map.properties.StitchStackProperties;

public class CellFinderLegacy {
  
  // with orientation
  public static class Point implements Comparable<Point> {
    private BigRational x, y, z;
    
    public Point(BigRational x, BigRational y, BigRational z) {
      if (!z.equals(BigRational.ZERO)) {
        this.x = x.divide(z.abs());
        this.y = y.divide(z.abs());
        this.z = z.divide(z.abs());
      } else if (!y.equals(BigRational.ZERO)) {
        this.x = x.divide(y.abs());
        this.y = y.divide(y.abs());
        this.z = BigRational.ZERO;
      } else if (!x.equals(BigRational.ZERO)) {
        this.x = x.divide(x.abs());
        this.y = BigRational.ZERO;
        this.z = BigRational.ZERO;
      } else
        throw new RuntimeException("non-point");
    }
    
    public Point(Line p, Line q) {
      //@formatter:off
      this(
        p.getB().multiply(q.getC()).subtract(p.getC().multiply(q.getB())),
        p.getC().multiply(q.getA()).subtract(p.getA().multiply(q.getC())),
        p.getA().multiply(q.getB()).subtract(p.getB().multiply(q.getA()))
      );
      //@formatter:on
    }
    
    public Point(BigRational x, BigRational y) {
      this(x, y, BigRational.ONE);
    }
    
    public Point(double x, double y) {
      this(new BigRational(x), new BigRational(y));
    }
    
    public Point(ImagePosition p) {
      this(p.getX(), p.getY());
    }
    
    public BigRational getX() {
      return x;
    }
    
    public BigRational getY() {
      return y;
    }
    
    public BigRational getZ() {
      return z;
    }
    
    public int compareTo(Point p) {
      int d = x.compareTo(p.x);
      if (d != 0)
        return d;
      
      d = y.compareTo(p.y);
      if (d != 0)
        return d;
      
      return z.compareTo(p.z);
    }
    
    public boolean equals(Object o) {
      return o instanceof Point && compareTo((Point) o) == 0;
    }
    
    public String toString() {
      return "(" + x.doubleValue() + ", " + y.doubleValue() + ", " + z.doubleValue() + ")";
    }
    
    public Point add(Point p) {
      return new Point(x.add(p.x), y.add(p.y));
    }
    
    public Point negate() {
      return new Point(x.negate(), y.negate());
    }
    
    public Point subtract(Point p) {
      return add(p.negate());
    }
    
    public Point scale(BigRational factor) {
      return new Point(factor.multiply(x), factor.multiply(y));
    }
    
    public Point scaleInv(BigRational factor) {
      return scale(factor.invert());
    }
    
    public BigRational normSq() {
      return x.multiply(x).add(y.multiply(y));
    }
    
    public BigRational distanceSq(Point p) {
      return subtract(p).normSq();
    }
    
    public double norm() {
      return Math.sqrt(normSq().doubleValue());
    }
    
    public double distance(Point p) {
      return subtract(p).norm();
    }
    
    public Point rotate90() {
      return new Point(y.negate(), x);
    }
    
    public boolean isPureProjective() {
      return z.equals(BigRational.ZERO);
    }
    
    public Point normalize() {
      if (z.signum() > 0
          || (z.signum() == 0 && (y.signum() > 0 || (y.signum() == 0 && x.signum() > 0))))
        return this;
      
      return new Point(x.negate(), y.negate(), z.negate());
    }
    
    public Pair<Double, Double> toDouble() {
      if (isPureProjective())
        throw new RuntimeException("cannot convert non-affine point to pair of doubles");
      
      Point n = normalize();
      return new Pair<Double, Double>(n.x.doubleValue(), n.y.doubleValue());
    }
  }
  
  // with orientation
  public static class Line implements Comparable<Line> {
    private BigRational a, b, c;
    
    public Line(BigRational a, BigRational b, BigRational c) {
      if (!c.equals(BigRational.ZERO)) {
        this.a = a.divide(c.abs());
        this.b = b.divide(c.abs());
        this.c = c.divide(c.abs());
      } else if (!b.equals(BigRational.ZERO)) {
        this.a = a.divide(b.abs());
        this.b = b.divide(b.abs());
        this.c = BigRational.ZERO;
      } else if (!a.equals(BigRational.ZERO)) {
        this.a = a.divide(a.abs());
        this.b = BigRational.ZERO;
        this.c = BigRational.ZERO;
      } else
        throw new RuntimeException("non-line");
    }
    
    public Line(Point p, Point q) {
      //@formatter:off
      this(
        p.getY().multiply(q.getZ()).subtract(p.getZ().multiply(q.getY())),
        p.getZ().multiply(q.getX()).subtract(p.getX().multiply(q.getZ())),
        p.getX().multiply(q.getY()).subtract(p.getY().multiply(q.getX()))
      );
      //@formatter:on
    }
    
    public BigRational getA() {
      return a;
    }
    
    public BigRational getB() {
      return b;
    }
    
    public BigRational getC() {
      return c;
    }
    
    public int compareTo(Line l) {
      int d = a.compareTo(l.a);
      if (d != 0)
        return d;
      
      d = b.compareTo(l.b);
      if (d != 0)
        return d;
      
      return c.compareTo(l.c);
    }
    
    public boolean equals(Object o) {
      return o instanceof Line && compareTo((Line) o) == 0;
    }
    
    public String toString() {
      return "(" + a.doubleValue() + ", " + b.doubleValue() + ", " + c.doubleValue() + ")";
    }
    
    public boolean isPureProjective() {
      return a.equals(BigRational.ZERO) && b.equals(BigRational.ZERO);
    }
    
    public boolean parallel(Line l) {
      return equals(l) || new Point(this, l).isPureProjective();
    }
    
    public BigRational prod(Point p) {
      BigRational u = a.multiply(p.getX());
      BigRational v = b.multiply(p.getY());
      BigRational w = c.multiply(p.getZ());
      return u.add(v).add(w);
    }
  }

  public static Point getPoint(ImagePosition position, AffineTransform transform) {
    double[] array = new double[] {position.getX(), position.getY()};
    transform.map(array, array);
    return new Point(array[0], array[1]);
  }
  
  public static Point[][] findCells(ImageSetProperties imageSet, AffineTransform transform) {
    double[] boundary = Utils.initBoundary();
    Utils.getBoundary(imageSet, transform, boundary);
    
    Point[] boundaryPoints = new Point[4];
    boundaryPoints[0] = new Point(new BigRational(boundary[0]), new BigRational(boundary[1]));
    boundaryPoints[1] = new Point(new BigRational(boundary[2]), new BigRational(boundary[1]));
    boundaryPoints[2] = new Point(new BigRational(boundary[2]), new BigRational(boundary[3]));
    boundaryPoints[3] = new Point(new BigRational(boundary[0]), new BigRational(boundary[3]));
    
    ImageSize size = imageSet.getSize();
    
    int sx = size.getSx();
    int sy = size.getSy();
    
    BigRational maxDistSq = new BigRational(sx * sx + sy * sy);
    
    Point[][] result = new Point[imageSet.getNumImages()][];
    
    for (int image = 0; image != imageSet.getNumImages(); ++image) {
      Point p = getPoint(imageSet.getPosition(image), transform);
      ArrayList<Line> lines = new ArrayList<Line>();
      lines.add(new Line(boundaryPoints[0], boundaryPoints[1]));
      lines.add(new Line(boundaryPoints[1], boundaryPoints[2]));
      lines.add(new Line(boundaryPoints[2], boundaryPoints[3]));
      lines.add(new Line(boundaryPoints[3], boundaryPoints[0]));
      
      for (int j = 0; j != imageSet.getNumImages(); ++j)
        if (j != image) {
          Point q = getPoint(imageSet.getPosition(j), transform);
          if (p.distanceSq(q).compareTo(maxDistSq) < 0) {
            Point base = p.add(q).scaleInv(new BigRational(2));
            Point dir = q.subtract(p).rotate90();
            lines.add(new Line(base, base.add(dir)));
          }
        }
      
      for (int i = 0; i != lines.size(); ++i)
        for (int j = Math.max(4, i + 1); j != lines.size(); ++j)
          if (lines.get(i).parallel(lines.get(j)))
            throw new RuntimeException("cannot handle parallel lines yet");
      
      TreeMap<Integer, TreeSet<Point>> points = new TreeMap<Integer, TreeSet<Point>>();
      TreeMap<Point, TreeSet<Integer>> lineMap = new TreeMap<Point, TreeSet<Integer>>();
      
      for (int i = 0; i != lines.size(); ++i)
        outer: for (int j = i + 1; j != lines.size(); ++j) {
          Point intersection = new Point(lines.get(i), lines.get(j));
          if (!intersection.isPureProjective()) {
            intersection = intersection.normalize();
            
            for (int k = 0; k != lines.size(); ++k) {
              BigRational r = lines.get(k).prod(intersection);
              if (r.signum() < 0)
                continue outer;
            }
            
            TreeSet<Point> set = points.get(i);
            if (set == null) {
              set = new TreeSet<Point>();
              points.put(i, set);
            }
            set.add(intersection);
            
            set = points.get(j);
            if (set == null) {
              set = new TreeSet<Point>();
              points.put(j, set);
            }
            set.add(intersection);
            
            TreeSet<Integer> lineSet = lineMap.get(intersection);
            if (lineSet == null) {
              lineSet = new TreeSet<Integer>();
              lineMap.put(intersection, lineSet);
            }
            lineSet.add(i);
            lineSet.add(j);
          }
        }
      
      TreeSet<Integer> properLines = new TreeSet<Integer>();
      for (int i = 0; i != lines.size(); ++i) {
        TreeSet<Point> pointSet = points.get(i);
        if (pointSet != null && pointSet.size() >= 2) {
          if (pointSet.size() != 2)
            throw new RuntimeException();
          
          properLines.add(i);
        }
      }
      
      for (Point point : lineMap.keySet()) {
        TreeSet<Integer> lineSet = lineMap.get(point);
        lineSet.retainAll(properLines);
        if (lineSet.size() != 2)
          throw new RuntimeException();
      }
      
      LinkedList<Point> properPoints = new LinkedList<Point>();
      
      int line = properLines.first();
      Point point = points.get(line).first();
      
      while (true) {
        properPoints.add(point);
        
        TreeSet<Integer> lineSet = lineMap.get(point);
        if (line == lineSet.first())
          line = lineSet.last();
        else if (line == lineSet.last())
          line = lineSet.first();
        else
          throw new RuntimeException();
        
        TreeSet<Point> pointSet = points.get(line);
        if (point.equals(pointSet.first()))
          point = pointSet.last();
        else if (point.equals(pointSet.last()))
          point = pointSet.first();
        else
          throw new RuntimeException();
        
        if (line == properLines.first())
          break;
      }
      
      if (new Line(properPoints.get(0), properPoints.get(1)).prod(properPoints.get(2)).signum() < 0)
        Collections.reverse(properPoints);
      
      result[image] = new Point[properPoints.size()];
      
      int index = 0;
      for (Point pt : properPoints)
        result[image][index++] = pt;
    }
    
    return result;
  }
  
  @SuppressWarnings("unchecked")
  public static Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] computeEdges(
      Point[][] points) {
    ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>> result =
        new ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>>();
    
    for (int image = 0; image != points.length; ++image) {
      Point[] list = points[image];
      for (int i = 0; i != list.length; ++i) {
        Pair<Double, Double> a = list[i].toDouble();
        Pair<Double, Double> b = list[(i + 1) % list.length].toDouble();
        
        int place = 0;
        
        if (!(a.getB().compareTo(b.getB()) < 0)) {
          Pair<Double, Double> t = a;
          a = b;
          b = t;
          
          place = 1 - place;
        }
        
        Segment<Pair<Double, Double>> key =
            new Segment<Pair<Double, Double>>((Pair<Double, Double>[]) new Pair[] {}, a, b);
        int[] c = new int[] {-1, -1};
        c[place] = image;
        result.add(new Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>(key,
            new Pair<Integer, Integer>(c[0], c[1])));
      }
    }
    
    return result
        .toArray((Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[]) new Pair[] {});
  }
  
  public static short[][] buildMaxNormMap(StitchStackProperties stack, int stitch, int channel) {
    ImageSetProperties imageSet = stack.getImageSetProperties(stitch);
    
    double[][] centers = new double[imageSet.getNumImages()][2];
    for (int i = 0; i != imageSet.getNumImages(); ++i) {
      centers[i][0] = imageSet.getPosition(i).getX();
      centers[i][1] = imageSet.getPosition(i).getY();
      
      stack.getTransform(stitch).map(centers[i], centers[i]);
      
      centers[i][0] -= stack.getX0();
      centers[i][1] -= stack.getY0();
    }
    
    short[][] result = new short[stack.getY1() - stack.getY0()][stack.getX1() - stack.getX0()];
    float[][] distance = new float[result.length][result[0].length];
    
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result[0].length; ++x) {
        distance[y][x] = Float.MAX_VALUE;
        result[y][x] = -1;
      }
    
    ImageSize size = imageSet.getSize();
    int sx = size.getSx();
    int sy = size.getSy();
    
    double maxX = 0.6 * sx;
    double maxY = 0.6 * sy;
    
    for (int i = 0; i != centers.length; ++i) {
      System.err.println(i);
      
      int x0 = Math.max(0, (int) (centers[i][0] - maxX));
      int y0 = Math.max(0, (int) (centers[i][1] - maxY));
      
      int x1 = Math.min(result[0].length, (int) (centers[i][0] + maxX) + 1);
      int y1 = Math.min(result.length, (int) (centers[i][1] + maxY) + 1);
      
      for (int y = y0; y != y1; ++y)
        for (int x = x0; x != x1; ++x) {
          float d =
              (float) Math.max(Math.abs(x - centers[i][0]) / sx, Math.abs(y - centers[i][1]) / sy);
          if (d < distance[y][x]) {
            distance[y][x] = d;
            result[y][x] = (short) i;
          }
        }
    }
    
    Map[] maps = Utils.getMapsFromStack(stack, stitch, channel, true);
    double[] in = new double[2];
    
    for (int y = 0; y != result.length; ++y) {
      System.err.println(y);
      
      for (int x = 0; x != result[0].length; ++x) {
        int v = result[y][x];
        if (v != -1) {
          in[0] = x + 0.5;
          in[1] = y + 0.5;
          
          maps[v].unmap(in, in);
          
          int xx = (int) Math.round(in[0] - 0.5);
          int yy = (int) Math.round(in[1] - 0.5);
          
          if (!(xx >= 0 && yy >= 0 && xx < sx && yy < sy))
            result[y][x] = -1;
        }
      }
    }
    
    return result;
  }
  
  public static short[][] buildVoroniMap(StitchStackProperties stack, int stitch, int channel) {
    ImageSetProperties imageSet = stack.getImageSetProperties(stitch);
    Point[][] points = findCells(imageSet, stack.getTransform(stitch));
    Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges = computeEdges(points);
    TreeMap<Integer, TreeMap<Integer, Integer>> lineColors =
        SharpnessEvaluator.computeLineColorMaps(1, edges);
    
    double[][] positions = new double[imageSet.getNumImages()][2];
    for (int i = 0; i != positions.length; ++i) {
      positions[i][0] = imageSet.getPosition(i).getX();
      positions[i][1] = imageSet.getPosition(i).getY();
    }
    
    short[][] result = new short[stack.getY1() - stack.getY0()][stack.getX1() - stack.getX0()];
    
    ImageSize size = imageSet.getSize();
    int sx = size.getSx();
    int sy = size.getSy();
    
    Map[] maps = Utils.getMapsFromStack(stack, stitch, channel, true);
    double[] in = new double[2];
    
    for (int y = 0; y != result.length; ++y) {
      if (y % 1 == 0)
        System.err.println(y);
      
      TreeMap<Integer, Integer> line = lineColors.get(y);
      if (line == null) {
        line = new TreeMap<Integer, Integer>();
        line.put(-1, -1);
      }
      
      for (int x = 0; x != result[0].length; ++x) {
        int v = line.floorEntry(x).getValue();
        if (v != -1) {
          in[0] = x + 0.5;
          in[1] = y + 0.5;
          
          maps[v].unmap(in, in);
          
          int xx = (int) Math.round(in[0] - 0.5);
          int yy = (int) Math.round(in[1] - 0.5);
          
          if (!(xx >= 0 && yy >= 0 && xx < sx && yy < sy)) {
            double dist = Double.MAX_VALUE;
            v = -1;
            
            for (int i = 0; i != maps.length; ++i) {
              in[0] = x + 0.5;
              in[1] = y + 0.5;
              
              maps[i].unmap(in, in);
              
              xx = (int) Math.round(in[0] - 0.5);
              yy = (int) Math.round(in[1] - 0.5);
              
              if (xx >= 0 && yy >= 0 && xx < sx && yy < sy) {
                int dx = 2 * xx + 1 - sx;
                int dy = 2 * yy + 1 - sy;
                double d = dx * dx + dy * dy;
                if (d < dist) {
                  dist = d;
                  v = i;
                }
              }
            }
          }
        }
        
        result[y][x] = (short) v;
      }
    }
    
    return result;
  }
  
  public static BufferedImage renderImageMap(short[][] imageMap) {
    int[] colors = new int[257];
    for (int i = 0; i != colors.length; ++i)
      colors[i] = 0xff000000 | (SharpnessEvaluator.RENDER_COLOR_FACTOR * i);
    
    BufferedImage result =
        new BufferedImage(imageMap[0].length, imageMap.length, BufferedImage.TYPE_INT_RGB);
    
    for (int y = 0; y != imageMap.length; ++y) {
      System.err.println(y);
      for (int x = 0; x != imageMap[0].length; ++x)
        result.setRGB(x, y, colors[imageMap[y][x] + 1]);
    }
    
    return result;
  }
  
  public static void renderImageMap(short[][] imageMap, String filename) throws IOException {
    DataOutputStream out =
        new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
    
    out.writeInt(imageMap[0].length);
    out.writeInt(imageMap.length);
    
    int[] colors = new int[257];
    for (int i = 0; i != colors.length; ++i)
      colors[i] = 0xff000000 | (SharpnessEvaluator.RENDER_COLOR_FACTOR * i);
    
    for (int y = 0; y != imageMap.length; ++y) {
      System.err.println(y);
      for (int x = 0; x != imageMap[0].length; ++x) {
        int c = colors[imageMap[y][x] + 1];
        out.writeByte((byte) c);
        out.writeByte((byte) (c >> 8));
        out.writeByte((byte) (c >> 16));
      }
    }
    
    out.close();
  }
  
  public static BufferedImage render(StitchStackProperties stack, int stitch, int channel,
      BufferedImage[] images, short[][] imageMap) {
    BufferedImage result =
        new BufferedImage(stack.getX1() - stack.getX0(), stack.getY1() - stack.getY0(),
            images[0].getType());
    WritableRaster raster = result.getRaster();
    
    WritableRaster[] rasters = new WritableRaster[images.length];
    for (int image = 0; image != images.length; ++image)
      rasters[image] = images[image].getRaster();
    
    Map[] maps = map.Utils.getMapsFromStack(stack, stitch, channel, true);
    double[] in = new double[2];
    int[] pixel = new int[4];
    
    for (int y = 0; y != result.getHeight(); ++y) {
      if (y % 1000 == 0)
        System.err.println(y);
      
      for (int x = 0; x != result.getWidth(); ++x) {
        int v = imageMap[y][x];
        if (v != -1) {
          in[0] = x + 0.5;
          in[1] = y + 0.5;
          
          maps[v].unmap(in, in);
          
          int xx = (int) Math.round(in[0] - 0.5);
          int yy = (int) Math.round(in[1] - 0.5);
          
          raster.setPixel(x, y, rasters[v].getPixel(xx, yy, pixel));
        }
      }
    }
    
    return result;
  }
  
  public static void render(StitchStackProperties stack, int stitch, int channel,
      BufferedImage[] images, short[][] imageMap, String filename) throws IOException {
    DataOutputStream out =
        new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
    
    int sx = stack.getX1() - stack.getX0();
    int sy = stack.getY1() - stack.getY0();
    
    out.writeInt(sx);
    out.writeInt(sy);
    
    Map[] maps = map.Utils.getMapsFromStack(stack, stitch, channel, true);
    double[] in = new double[2];
    int[] pixel = new int[4];
    
    WritableRaster[] rasters = new WritableRaster[images.length];
    for (int image = 0; image != images.length; ++image)
      rasters[image] = images[image].getRaster();
    
    for (int y = 0; y != sy; ++y) {
      System.err.println(y);
      
      for (int x = 0; x != sx; ++x) {
        int v = imageMap[y][x];
        if (v != -1) {
          in[0] = x + 0.5;
          in[1] = y + 0.5;
          
          maps[v].unmap(in, in);
          
          int xx = (int) Math.round(in[0] - 0.5);
          int yy = (int) Math.round(in[1] - 0.5);
          
          rasters[v].getPixel(xx, yy, pixel);
        } else
          for (int i = 0; i != 3; ++i)
            pixel[i] = 0;
        
        for (int i = 0; i != 3; ++i)
          out.writeByte(pixel[i]);
      }
    }
    
    out.close();
  }
  
}

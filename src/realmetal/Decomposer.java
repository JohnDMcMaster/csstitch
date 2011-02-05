package realmetal;

import java.util.TreeSet;

import operations.image.ImageOpsDouble;
import operations.line.FlattenFilter;
import operations.line.LineOperation;

public final class Decomposer {
  
  public final class Segment implements Comparable<Segment> {
    private int dir;
    private int index;
    private int begin, end;
    private double value;
    
    public Segment(final int dir, final int index, final int begin, final int end,
        final double value) {
      this.dir = dir;
      this.index = index;
      this.begin = begin;
      this.end = end;
      this.value = value;
      
      for (int i = begin; i != end; ++i)
        segments[dir][index][i] = this;
      queue.add(this);
    }
    
    public int getDir() {
      return dir;
    }
    
    public int getIndex() {
      return index;
    }
    
    public int getBegin() {
      return begin;
    }
    
    public int getEnd() {
      return end;
    }
    
    public int getLength() {
      return end - begin;
    }
    
    public double getValue() {
      return value;
    }
    
    public void addToValues(final double v) {
      if (v < value) {
        queue.remove(this);
        value = v;
        queue.add(this);
      }
    }
    
    public void remove() {
      queue.remove(this);
      for (int i = begin; i != end; ++i)
        segments[dir][index][i] = null;
    }
    
    public int compareTo(final Segment segment) {
      int d = getLength() - segment.getLength();
      if (d != 0)
        return d;
      
      d = (int) Math.signum(value - segment.value);
      if (d != 0)
        return d;
      
      d = dir - segment.dir;
      if (d != 0)
        return d;
      
      d = index - segment.index;
      if (d != 0)
        return d;
      
      return begin - segment.begin;
    }
  }
  
  private int filterLength;
  private LineOperation filter;
  
  private double[][] image;
  
  private int[] limits;
  private double[][][] components;
  private double[][][] filtered;
  private Segment[][][] segments;
  private TreeSet<Segment> queue;
  
  private int counter;
  
  private void buildSegments(final int dir, final int index, final int begin, final int end) {
    int start = begin;
    double min = Float.POSITIVE_INFINITY;
    for (int i = begin; i <= end; ++i) {
      final double val = i == end ? 0 : filtered[dir][index][i];
      if (val > 0)
        min = Math.min(min, val);
      
      if (val <= 0) {
        if (i != start)
          new Segment(dir, index, start, i, min);
        start = i + 1;
      }
    }
  }
  
  private void updatePoint(final int dir, final int index, final int i) {
    final Segment segment = segments[dir][index][i];
    if (segment == null)
      return;
    
    final double val = filtered[dir][index][i];
    if (val > 0) {
      segment.addToValues(val);
      return;
    }
    
    if (++counter % 100000 == 0)
      System.err.println(counter);
    
    segment.remove();
    buildSegments(dir, index, segment.getBegin(), i);
    buildSegments(dir, index, i + 1, segment.getEnd());
  }
  
  private void
      updateMedian(final int dir, final int index, int begin, int end, final boolean update) {
    begin = Math.max(0, begin - filterLength);
    end = Math.min(limits[1 - dir], end + filterLength);
    
    final int copyBegin = Math.max(0, begin - filterLength);
    final int copyEnd = Math.min(limits[1 - dir], end + filterLength);
    
    double[] copy = new double[copyEnd - copyBegin];
    for (int i = copyBegin; i != copyEnd; ++i)
      copy[i - copyBegin] = dir == 0 ? image[index][i] : image[i][index];
    
    copy = filter.transform(copy);
    for (int i = begin; i != end; ++i)
      filtered[dir][index][i] = copy[i - copyBegin];
    
    if (update)
      for (int i = begin; i != end; ++i)
        updatePoint(dir, index, i);
  }
  
  public Decomposer(final double[][] image, final int filterLength) {
    this.filterLength = filterLength;
    filter = new FlattenFilter(filterLength, true);
    
    limits = new int[2];
    for (int dir = 0; dir != 2; ++dir)
      limits[dir] = ImageOpsDouble.getLimit(image, dir);
    
    this.image = ImageOpsDouble.copy(image);
    
    components = new double[2][][];
    filtered = new double[2][][];
    segments = new Segment[2][][];
    for (int dir = 0; dir != 2; ++dir) {
      components[dir] = new double[limits[dir]][limits[1 - dir]];
      filtered[dir] = new double[limits[dir]][limits[1 - dir]];
      segments[dir] = new Segment[limits[dir]][limits[1 - dir]];
    }
    
    queue = new TreeSet<Segment>();
    
    for (int dir = 0; dir != 2; ++dir)
      for (int index = 0; index != limits[dir]; ++index) {
        filtered[dir][index] = filter.transform(ImageOpsDouble.getLine(image, index, dir));
        buildSegments(dir, index, 0, limits[1 - dir]);
      }
    
    counter = 0;
  }
  
  public int getSegmentLength() {
    return queue.isEmpty() ? 0 : queue.last().getLength();
  }
  
  public double[][][] getComponents() {
    final double[][][] result = new double[2][image.length][image[0].length];
    for (int dir = 0; dir != 2; ++dir)
      for (int y = 0; y != image.length; ++y)
        for (int x = 0; x != image[0].length; ++x)
          result[dir][y][x] = dir == 0 ? components[dir][y][x] : components[dir][x][y];
    
    final double[] means = new double[2];
    for (int dir = 0; dir != 2; ++dir)
      means[dir] = ImageOpsDouble.mean(result[dir]);
    
    for (int dir = 0; dir != 2; ++dir)
      for (int y = 0; y != image.length; ++y)
        for (int x = 0; x != image[0].length; ++x)
          result[dir][y][x] += (means[1 - dir] - means[dir]) / 2;
    return result;
  }
  
  public void doStep() {
    final Segment segment = queue.last();
    final int dir = segment.getDir();
    final int index = segment.getIndex();
    final int begin = segment.getBegin();
    final int end = segment.getEnd();
    final double value = segment.getValue();
    segment.remove();
    
    for (int i = begin; i != end; ++i)
      if (dir == 0)
        image[index][i] -= value;
      else
        image[i][index] -= value;
    
    for (int i = begin; i != end; ++i)
      components[dir][index][i] += value;
    
    for (int i = begin; i != end; ++i)
      updateMedian(1 - dir, i, index, index + 1, true);
    
    updateMedian(dir, index, begin, end, false);
    buildSegments(dir, index, begin, end);
  }
  
  public static double[][][][] decompose(double[][] image, final int filterLength,
      final int[] minSegmentLengths, final boolean down) {
    if (!down)
      image = ImageOpsDouble.negate(image);
    
    final double threshold = Math.floor(ImageOpsDouble.min(image));
    image = ImageOpsDouble.subtract(image, threshold);
    
    final Decomposer decomposer = new Decomposer(image, filterLength);
    final double[][][][] result = new double[minSegmentLengths.length][][][];
    
    for (int i = 0; i != result.length; ++i) {
      while (decomposer.getSegmentLength() >= minSegmentLengths[i]) {
        if (Math.random() < 0.0001)
          System.err.println(decomposer.getSegmentLength() + ", " + decomposer.queue.size());
        decomposer.doStep();
      }
      
      result[i] = decomposer.getComponents();
      for (int dir = 0; dir != 2; ++dir) {
        result[i][dir] = ImageOpsDouble.add(result[i][dir], threshold / 2);
        if (!down)
          result[i][dir] = ImageOpsDouble.negate(result[i][dir]);
      }
    }
    return result;
  }
  
  public static double[][][] decompose(final double[][] image, final int filterLength,
      final int minSegmentLength, final boolean down) {
    return decompose(image, filterLength, new int[] {minSegmentLength}, down)[0];
  }
  
}

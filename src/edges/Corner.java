package edges;

import java.io.Serializable;

public class Corner implements Comparable<Corner>, Serializable {
  
  private static final long serialVersionUID = 1L;
  
  public static final int CORNER_SCALE = 4;
  
  private long edge;
  private int end;
  
  private double x, y;
  private int xx, yy;
  
  public Corner(long id, Edge edge, int end) {
    this.edge = id;
    this.end = end;
    
    int dir = edge.getDir();
    
    x = dir == 0 ? edge.getEnd(end) : edge.getEndIndex(end);
    y = dir == 1 ? edge.getEnd(end) : edge.getEndIndex(end);
    
    xx = (int) (x / CORNER_SCALE);
    yy = (int) (y / CORNER_SCALE);
  }
  
  public double getX() {
    return x;
  }
  
  public double getY() {
    return y;
  }
  
  public long getEdge() {
    return edge;
  }
  
  public int getEnd() {
    return end;
  }
  
  public int getXX() {
    return xx;
  }
  
  public int getYY() {
    return yy;
  }
  
  public int compareTo(Corner corner) {
    int d = Long.signum(edge - corner.edge);
    if (d != 0)
      return d;
    
    return end - corner.end;
  }
}

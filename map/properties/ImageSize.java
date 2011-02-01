package map.properties;

import java.io.Serializable;

public class ImageSize implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private int[] size;
  
  public ImageSize(int[] size) {
    if (!(size.length == 2))
      throw new IllegalArgumentException();
    
    this.size = size.clone();
  }
  
  public ImageSize(int x, int y) {
    this(new int[] {x, y});
  }
  
  public int get(int i) {
    return size[i];
  }
  
  public int getSx() {
    return get(0);
  }
  
  public int getSy() {
    return get(1);
  }
  
  public String toString() {
    return "(" + getSx() + ", " + getSy() + ")";
  }
  
}

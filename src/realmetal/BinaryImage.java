package realmetal;

import java.awt.image.BufferedImage;
import java.io.Serializable;

public class BinaryImage implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private int sx, sy;
  private long[][] bits;
  
  public BinaryImage(int sx, int sy) {
    this.sx = sx;
    this.sy = sy;
    
    bits = new long[sy][(sx - 1) / Long.SIZE + 1];
  }
  
  public BinaryImage(boolean[][] image) {
    sx = image[0].length;
    sy = image.length;
    
    bits = new long[sy][(sx - 1) / Long.SIZE + 1];
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        if (image[y][x])
          bits[y][x / Long.SIZE] |= (1l << (x % Long.SIZE));
  }
  
  public int getSx() {
    return sx;
  }
  
  public int getSy() {
    return sy;
  }
  
  public boolean[][] render() {
    boolean[][] result = new boolean[sy][sx];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        result[y][x] = get(x, y);
    
    return result;
  }
  
  public BufferedImage toImage() {
    BufferedImage result = new BufferedImage(sx, sy, BufferedImage.TYPE_BYTE_BINARY);
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        result.setRGB(x, y, get(x, y) ? 0x00ffffff : 0x00000000);
    
    return result;
  }
  
  public boolean get(int x, int y) {
    return (bits[y][x / Long.SIZE] & (1l << (x % Long.SIZE))) != 0;
  }
  
  public void set(int x, int y, boolean value) {
    if (value)
      check(x, y);
    else
      uncheck(x, y);
  }
  
  public void check(int x, int y) {
    bits[y][x / Long.SIZE] |= 1l << (x % Long.SIZE);
  }
  
  public void uncheck(int x, int y) {
    bits[y][x / Long.SIZE] &= ~(1l << (x % Long.SIZE));
  }
  
  public boolean contains(BinaryImage image, int px, int py) {
    for (int y = 0; y != image.sy; ++y) {
      for (int bx = 0; bx != image.bits[0].length; ++bx) {
        int rx = px + Long.SIZE * bx;
        if (((~bits[py + y][rx / Long.SIZE + 0] >>> (rx % Long.SIZE)) & image.bits[y][bx]) != 0)
          return false;
        
        if (px + image.sx > Long.SIZE * (rx / Long.SIZE + 1))
          if (((~bits[py + y][rx / Long.SIZE + 1] << (Long.SIZE - (rx % Long.SIZE))) & image.bits[y][bx]) != 0)
            return false;
      }
    }
    
    return true;
  }
  
}

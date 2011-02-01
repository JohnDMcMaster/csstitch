package old.block_compression;

public class ImageSizeIndexImpl implements ImageSize {
  
  protected int sx = -1, sy = -1;
  protected int x = 0, y = 0;
  
  protected boolean increase() {
    if (++x == sx) {
      x = 0;
      if (++y == sy) {
        y = 0;
        return true;
      }
    }
    
    return false;
  }
  
  public int getSX() {
    return sx;
  }
  
  public int getSY() {
    return sy;
  }
  
  public int getX() {
    return x;
  }
  
  public int getY() {
    return y;
  }
  
}

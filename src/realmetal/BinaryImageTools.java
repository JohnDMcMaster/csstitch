package realmetal;

public class BinaryImageTools {
  
  private static final int[][] NEIGHBOURS_4 = new int[][] { {0, -1}, {-1, 0}, {0, 0}, {1, 0},
      {0, 1}};
  private static final int[][] NEIGHBOURS_8 = new int[][] { {-1, -1}, {0, -1}, {1, -1}, {-1, 0},
      {0, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
  
  public static BinaryImage neg(BinaryImage image) {
    int sx = image.getSx();
    int sy = image.getSy();
    
    BinaryImage result = new BinaryImage(sx, sy);
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        result.set(x, y, !image.get(x, y));
    
    return result;
  }
  
  public static BinaryImage and(BinaryImage a, BinaryImage b) {
    int sx = a.getSx();
    int sy = a.getSy();
    
    BinaryImage result = new BinaryImage(sx, sy);
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        result.set(x, y, a.get(x, y) && b.get(x, y));
    
    return result;
  }
  
  public static BinaryImage or(BinaryImage a, BinaryImage b) {
    int sx = a.getSx();
    int sy = a.getSy();
    
    BinaryImage result = new BinaryImage(sx, sy);
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        result.set(x, y, a.get(x, y) || b.get(x, y));
    
    return result;
  }
  
  public static BinaryImage xor(BinaryImage a, BinaryImage b) {
    int sx = a.getSx();
    int sy = a.getSy();
    
    BinaryImage result = new BinaryImage(sx, sy);
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        result.set(x, y, a.get(x, y) != b.get(x, y));
    
    return result;
  }
  
  public static BinaryImage inflate(BinaryImage image, int[][] neighbours) {
    int sx = image.getSx();
    int sy = image.getSy();
    
    BinaryImage result = new BinaryImage(sx, sy);
    for (int y = 0; y != sy; ++y)
      loop: for (int x = 0; x != sx; ++x) {
        for (int[] neighbour : neighbours) {
          int xx = x + neighbour[0];
          int yy = y + neighbour[1];
          
          if (xx >= 0 && yy >= 0 && xx < sx && yy < sy)
            if (image.get(xx, yy)) {
              result.check(x, y);
              continue loop;
            }
        }
      }
    
    return result;
  }
  
  public static BinaryImage deflate(BinaryImage image, int[][] neighbours) {
    int sx = image.getSx();
    int sy = image.getSy();
    
    BinaryImage result = new BinaryImage(sx, sy);
    for (int y = 0; y != sy; ++y)
      loop: for (int x = 0; x != sx; ++x) {
        for (int[] neighbour : neighbours) {
          int xx = x + neighbour[0];
          int yy = y + neighbour[1];
          
          if (xx >= 0 && yy >= 0 && xx < sx && yy < sy)
            if (!image.get(xx, yy))
              continue loop;
        }
        
        result.check(x, y);
      }
    
    return result;
  }
  
  public static BinaryImage inflate4(BinaryImage image) {
    return inflate(image, NEIGHBOURS_4);
  }
  
  public static BinaryImage deflate4(BinaryImage image) {
    return deflate(image, NEIGHBOURS_4);
  }
  
  public static BinaryImage inflate8(BinaryImage image) {
    return inflate(image, NEIGHBOURS_8);
  }
  
  public static BinaryImage deflate8(BinaryImage image) {
    return deflate(image, NEIGHBOURS_8);
  }
  
  public static BinaryImage embedComponent(BinaryImage image, int channel) {
    int sx = image.getSx();
    int sy = image.getSy();
    
    BinaryImage result = new BinaryImage(2 * sx, 2 * sy);
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        result.set(2 * x + channel % 2, 2 * y + channel / 2, image.get(x, y));
    
    return result;
  }
  
}

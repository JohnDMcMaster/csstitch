package metal;

import java.io.IOException;
import java.nio.ByteBuffer;

import old.storage.Point;
import old.storage.QuadTreeAccessor2;

import configuration.Config;
import data.DataTools;

import stitcher.StitchInfo;

public class Selector {
  
  // universal deflation configuration for stitches 0, 1, 2:
  public static final int NUM_EXPONENT_BITS = 3;
  public static final int EXPONENT_OFFSET = 123;
  
  private Selector() {
  }
  
  public static Point[][] select(final int stitch, final int image) throws IOException {
    QuadTreeAccessor2 accessor =
        new QuadTreeAccessor2(DataTools.DIR + "/single/" + stitch + "/" + image + ".dat");
    
    int sx = StitchInfo.IMAGE_DIMENSIONS[stitch][0];
    int sy = StitchInfo.IMAGE_DIMENSIONS[stitch][1];
    
    final Point[][] result = new Point[sy][sx];
    
    accessor.selectAll(new QuadTreeAccessor2.SimpleHandler() {
      public void handle(ByteBuffer file) throws IOException {
        Point point = new Point();
        point.x = file.getFloat();
        point.y = file.getFloat();
        point.val = file.getFloat();
        point.flags = file.getInt();
        
        int channel = point.flags & 0x00000003;
        
        point.val /= StitchInfo.MEANS[stitch][channel];
        
        int x = (point.flags >>> 2) & 0x000007ff;
        int y = (point.flags >>> 13) & 0x000007ff;
        
        result[2 * y + (channel / 2)][2 * x + (channel % 2)] = point;
      }
    });
    
    accessor.close();
    return result;
  }
  
}

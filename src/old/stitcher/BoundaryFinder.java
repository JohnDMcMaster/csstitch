package old.stitcher;

import java.io.IOException;
import java.nio.ByteBuffer;

import old.storage.QuadTreeAccessor2;

import data.DataTools;
import distributed.Bootstrap;

import stitcher.StitchInfo;

public class BoundaryFinder {
  
  public static float[] findBoundary(String filename) throws IOException {
    QuadTreeAccessor2 accessor = new QuadTreeAccessor2(filename);
    
    final float[] max = new float[2];
    final int[] counter = new int[1];
    
    accessor.selectRectangle(new QuadTreeAccessor2.SimpleHandler() {
      public void handle(ByteBuffer file) {
        if (++counter[0] % 1000000 == 0)
          System.out.println(counter[0]);
        
        float x = file.getFloat();
        float y = file.getFloat();
        float val = file.getFloat();
        int flags = file.getInt();
        
        max[0] = Math.max(max[0], x);
        max[1] = Math.max(max[1], y);
      }
    }, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    
    return max;
  }
  
  public static final int STITCH = 4;
  
  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(args);
    
    float[] max = findBoundary(DataTools.DIR + "stitch" + StitchInfo.SUFFICES[STITCH] + ".dat");
    System.out.println(max[0]);
    System.out.println(max[1]);
  }
  
}

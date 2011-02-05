package old.stitcher;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import old.storage.Point;
import old.storage.PointProcessor;
import old.storage.QuadTreeAccessor2;
import old.storage.QuadTreeCreator2;

import data.DataTools;
import distributed.Bootstrap;
import distributed.server.Servers;

import stitcher.StitchInfo;

public class Splitter {
  
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws IOException {
    //Bootstrap.bootstrap(Servers.CIP_90);
    
    QuadTreeAccessor2 accessor = new QuadTreeAccessor2("stitch-combined.dat");
    
    final QuadTreeCreator2<Point>[][] creators = new QuadTreeCreator2[3][];
    for (int s = 0; s != 3; ++s) {
      creators[s] = new QuadTreeCreator2[StitchInfo.NUM_IMAGES[s]];
      for (int image = 0; image != StitchInfo.NUM_IMAGES[s]; ++image) {
        creators[s][image] =
            new QuadTreeCreator2<Point>(DataTools.DIR, "single/" + s + "/" + image + "-input.dat",
                new PointProcessor(0));
      }
    }
    
    final Point point = new Point();
    final int[] counter = new int[] {0};
    
    accessor.selectAll(new QuadTreeAccessor2.SimpleHandler() {
      public void handle(ByteBuffer file) throws IOException {
        if (++counter[0] % 1000000 == 0)
          System.out.println(counter[0]);
        
        point.x = file.getFloat();
        point.y = file.getFloat();
        point.val = file.getFloat();
        point.flags = file.getInt();
        
        int s = point.flags >>> 30;
        int image = (point.flags >>> 24) & 0x0000003f;
        
        creators[s][image].add(point);
      }
    });
    
    // HACK
    //new File("stitch-combined").delete();
    
    System.out.println(counter[0]);
    
    for (int s = 0; s != 3; ++s)
      for (int image = 0; image != StitchInfo.NUM_IMAGES[s]; ++image) {
        System.out.println("writing image " + s + "-" + image);
        creators[s][image].write("single/" + s + "-" + image + ".dat", 1);
        System.out.println();
      }
  }
  
}

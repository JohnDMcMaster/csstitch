package old.stitcher;

import java.io.IOException;

import old.storage.Point;
import old.storage.PointProcessor;
import old.storage.QuadTreeCreator2;

import data.DataTools;

public class Resume {
  
  public static void main(String[] args) throws IOException {
    new QuadTreeCreator2<Point>(DataTools.DIR, new PointProcessor(0), DataTools.DIR + args[0], 1,
        14, 0, 0);
  }
  
}

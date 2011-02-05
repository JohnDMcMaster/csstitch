package old.stitcher;

import java.io.IOException;
import java.nio.ByteBuffer;

import old.storage.QuadTreeAccessor2;

import data.DataTools;
import distributed.Bootstrap;

import stitcher.StitchInfo;

public class MeanFinder {

  public static void find(String filename) throws IOException {
    QuadTreeAccessor2 accessor = new QuadTreeAccessor2(filename);

    final int[] nums = new int[4];
    final double[] sums = new double[4];
    final double[] maximums = new double[4];

    final int[] counter = new int[1];

    accessor.selectRectangle(new QuadTreeAccessor2.SimpleHandler() {
      public void handle(ByteBuffer file) {
        if (++counter[0] % 1000000 == 0)
          System.out.println(counter[0]);

        file.getFloat();
        file.getFloat();
        float val = file.getFloat();
        int flags = file.getInt();

        int s = ((flags % 4) + 4) % 4;
        ++nums[s];
        sums[s] += val;
        maximums[s] = Math.max(maximums[s], val);
      }
    }, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    for (int i = 0; i != 4; ++i)
      sums[i] /= nums[i];

    for (int i = 0; i != 4; ++i)
      System.out.println(sums[i]);

    for (int i = 0; i != 4; ++i)
      System.out.println(maximums[i]);
  }

  public static final int STITCH = 3;

  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(args);
    find(DataTools.DIR + "stitch" + StitchInfo.SUFFICES[STITCH] + ".dat");
  }

}

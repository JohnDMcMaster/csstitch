package old.stitcher;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
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

public strictfp class Demosaicer3 {
  
  public static final int[] STITCHES = {0, 1, 2};
  
  public static final double[][] values = new double[][] {
      {1.0, -0.0029911917142704776, 0.0013225407383610334, 1.0, 0.0, 0.0},
      {0.9999677374708568, -0.001974427205493879, 4.907943379827699E-4, 0.9998254452321185,
          89.27990064594864, 78.70119809523271},
      {0.9998873783459207, -0.0015510563810195838, -4.390198869702378E-5, 1.00069607560882,
          -9.377344601364525, 33.623387609195646}};

  public static void transform(double[] values, float[] input, float[] output) {
    output[0] = (float) (values[0] * input[0] + values[1] * input[1] + values[4]);
    output[1] = (float) (values[2] * input[0] + values[3] * input[1] + values[5]);
  }
  
  public static void main(String[] args) throws IOException {
    //Bootstrap.bootstrap(Servers.CIP_91);
    
    final float[] input = new float[2];
    final float[] output = new float[2];
    
    final int[] counter = new int[1];
    
    final float[] min = new float[] {Float.MAX_VALUE, Float.MAX_VALUE};
    for (int i = 0; i != STITCHES.length; ++i) {
      System.out.println("stitch " + i);
      
      QuadTreeAccessor2 accessor = new QuadTreeAccessor2(StitchInfo.getFilename(STITCHES[i]));
      final int index = i;
      
      accessor.selectAll(new QuadTreeAccessor2.SimpleHandler() {
        public void handle(ByteBuffer file) {
          if (++counter[0] % 1000000 == 0)
            System.out.println(counter[0]);
          
          input[0] = file.getFloat();
          input[1] = file.getFloat();
          file.getFloat();
          file.getInt();
          
          transform(values[index], input, output);
          
          min[0] = Math.min(min[0], output[0]);
          min[1] = Math.min(min[1], output[1]);
        }
      });
      
      System.out.println();
    }
    
    System.out.println(counter[0]);
    
    System.out.println("minimums:");
    System.out.println(min[0]);
    System.out.println(min[1]);
    System.out.println();
    
    final Point point = new Point();
    counter[0] = 0;
    
    final QuadTreeCreator2<Point> creator =
        new QuadTreeCreator2<Point>(DataTools.DIR, new PointProcessor(0));
      
    for (int i = 0; i != STITCHES.length; ++i) {
      System.out.println("stitch " + i);
      
      QuadTreeAccessor2 accessor = new QuadTreeAccessor2(StitchInfo.getFilename(STITCHES[i]));
      final int index = STITCHES[i];
      
      accessor.selectAll(new QuadTreeAccessor2.SimpleHandler() {
        public void handle(ByteBuffer file) throws IOException {
          if (++counter[0] % 1000000 == 0)
            System.out.println(counter[0]);
          
          input[0] = file.getFloat();
          input[1] = file.getFloat();
          point.val = file.getFloat();
          point.flags = file.getInt();
          
          transform(values[index], input, output);
          
          point.x = output[0] - min[0];
          point.y = output[1] - min[1];
          
          creator.add(point);
        }
      });
      
      System.out.println();
    }
    
    System.out.println(counter[0]);
    creator.write("stitch-combined.dat", 1);
  }
  
}

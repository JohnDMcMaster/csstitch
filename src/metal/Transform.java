package metal;

import general.Streams;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import data.Tools;
import distributed.Bootstrap;
import distributed.server.Servers;

public class Transform {
  
  public static void main(String[] args) throws IOException {
    File decap = new File("/media/book/decapsulation");
    File pure = new File(decap.getAbsoluteFile() + "/pure-data");
    File raw = new File(decap.getAbsoluteFile() + "/backup/raw-batches");
    
    outer: for (File name : pure.listFiles()) {
      System.err.println(name.getName());
      
      for (File file : raw.listFiles())
        for (File preview : file.listFiles())
          if (preview.getName().equals(name.getName() + ".JPG")) {
            FileInputStream in = new FileInputStream(preview);
            FileOutputStream out = new FileOutputStream(name.getAbsoluteFile() + "/preview.jpg");
            Streams.readFully(in, out);
            in.close();
            out.close();
            continue outer;
          }
      
      throw new RuntimeException("no preview found for image " + name);
    }
    
    System.exit(0);
    
    Bootstrap.bootstrap(Servers.CIP_90);
    
    int[] pixel = new int[1];
    
    for (String name : new File("grey-data").list()) {
      String id = name.substring(0, name.lastIndexOf('.'));
      
      BufferedImage image = ImageIO.read(new File("grey-data/" + name));
      WritableRaster raster = image.getRaster();
      
      int sx = image.getWidth();
      int sy = image.getHeight();
      
      for (int i = 0; i != 4; ++i) {
        BufferedImage result = new BufferedImage(sx / 2, sy / 2, image.getType());
        WritableRaster resultRaster = result.getRaster();
        
        for (int y = 0; y != sy / 2; ++y)
          for (int x = 0; x != sx / 2; ++x)
            resultRaster.setPixel(x, y, raster.getPixel(2 * x + (i % 2), 2 * y + (i / 2), pixel));
        
        String path = "pure-data/" + id + "/" + i + ".png";
        Tools.ensurePath(path);
        ImageIO.write(result, "png", new File(path));
      }
      
      System.err.println(id);
    }
    
  }
}

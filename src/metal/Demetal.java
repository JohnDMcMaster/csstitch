package metal;

import general.execution.SSHAddress;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import old.storage.QuadTreeAccessor2;

import configuration.Config;

import stitcher.StitchInfo;

import data.Tools;
import distributed.server.AbstractServer;
import distributed.server.Server;
import distributed.server.Servers;
import distributed.tunnel.ObjectTunnel;
import distributed.tunnel.Tunnel;

public class Demetal {
  
  public static final int X0 = 8000;
  public static final int Y0 = 8000;
  public static final int X1 = 12000;
  public static final int Y1 = 12000;
  
  public static final float THRESHOLD = 0.9f;
  public static final float RADIUS = 1.5f;
  
  public static void doStuff(ObjectTunnel tunnel) throws IOException {
    final int stitch = (Integer) tunnel.read();
    
    QuadTreeAccessor2 accessor =
        new QuadTreeAccessor2(Config.getOption("cip-data-dir") + "/stitch-final.dat");
    
    final int[][] mask = new int[Y1 - Y0][X1 - X0];
    
    accessor.selectRectangle(new QuadTreeAccessor2.SimpleHandler() {
      public void handle(ByteBuffer file) throws IOException {
        float x = file.getFloat();
        float y = file.getFloat();
        float val = file.getFloat();
        int flags = file.getInt();
        
        if ((flags >>> 30) != stitch)
          return;
        
        val /= StitchInfo.MEANS[stitch][flags & 0x00000003];
        if (val < THRESHOLD)
          return;
        
        int channel = flags & 0x00000003;
        int color = (channel / 2) + (channel % 2);
        
        for (int yy = (int) (y - RADIUS); yy <= y + RADIUS; ++yy)
          for (int xx = (int) (x - RADIUS); xx <= x + RADIUS; ++xx)
            if (xx >= X0 && yy >= Y0 && xx < X1 && yy < Y1) {
              float dx = (x + 0.5f) - xx;
              float dy = (y + 0.5f) - yy;
              if (dx * dx + dy * dy <= RADIUS)
                mask[yy - Y0][xx - X0] |= (1 << color);
            }
      }
    }, X0, Y0, X1, Y1);
    
    BufferedImage result = new BufferedImage(X1 - X0, Y1 - Y0, BufferedImage.TYPE_BYTE_BINARY);
    for (int y = Y0; y != Y1; ++y)
      for (int x = X0; x != X1; ++x) {
        int color;
        if (mask[y - Y0][x - X0] != 0x00000000)
          color = 0x00ffffff;
        else
          color = 0x00000000;
        result.setRGB(x - X0, y - Y0, color);
      }
    
    System.err.println("writing image");
    ImageIO.write(result, "png", tunnel.getOut());
  }
  
  public static void main(String[] args) throws IOException, InvocationTargetException {
    Server[] servers =
        new Server[] {new AbstractServer(new SSHAddress("localhost", 2222, "mdd63bj"),
            Servers.CIP_90.getDir())};
    
    Tunnel.init(servers);
    ObjectTunnel tunnel;
    
    tunnel =
        Tunnel.startObjectTunnel(servers, new String[] {"-server", "-Xmx1G"},
            Tunnel.getMethod("doStuff"));
    tunnel.write(0);
    BufferedImage result0 = ImageIO.read(tunnel.getIn());
    System.out.println("image read");
    
    long a = System.currentTimeMillis();
    BufferedImage image0 = Excerpts.getExcerpt(0, X0, Y0, X1, Y1);
    long b = System.currentTimeMillis();
    
    System.out.println(b - a);
    
    Tools.displayImages(new BufferedImage[] {image0, result0});
  }
}

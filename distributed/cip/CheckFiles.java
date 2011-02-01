package distributed.cip;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.TreeSet;

import distributed.Bootstrap;
import distributed.execution.Tools;
import distributed.server.CipServer;
import distributed.server.Servers;
import distributed.tunnel.Tunnel;

public class CheckFiles {

  public static long check() throws IOException {
    String dir = new CipServer(Servers.HOSTNAME).getDir();
    if (!new File(dir).exists())
      return -1;
      
    File a = new File(dir + "/stitch.dat");
    File b = new File(dir + "/stitch-cpu-3.dat");
    File c = new File(dir + "/stitch-ppu.dat");

    if (!(a.exists() && b.exists() && c.exists()))
      return -1;

    return a.length() + b.length() + c.length();
  }

  public static void main(String[] args) throws IOException, InvocationTargetException {
    Bootstrap.bootstrap(args);
    
    String[] slaves = Main.getSlaves(true, true, true, true);
    TreeSet<String> set = new TreeSet<String>(Arrays.asList(slaves));
    set.remove("cip11");
    slaves = set.toArray(new String[] {});
    
    Method method = Tunnel.getMethod("check");
    long n = (Long) Tunnel.callRemotely(Servers.getTunnel(Main.GATEWAY), null, method);

    TreeSet<String> missing = new TreeSet<String>(Arrays.asList(slaves));
    for (String slave : slaves) {
      try {
        long m = (Long) Tunnel.callRemotely(Servers.getTunnel(new CipServer(slave)),
            null, Tunnel.getMethod("check"));
        if (m == n)
          missing.remove(slave);
      } catch (InvocationTargetException e) {
        e.getCause().printStackTrace();
      }
    }
    
    System.out.println(missing);
  }

}

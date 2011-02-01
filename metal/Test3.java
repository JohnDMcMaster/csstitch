package metal;

import general.collections.Sets;
import general.execution.Bash;
import general.execution.SSH;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

import operations.image.ImageOps;
import operations.image.ImageOpsDouble;
import operations.line.FlattenFilter;
import realmetal.Decomposer;

import distributed.Bootstrap;
import distributed.cip.Main;
import distributed.server.CipServer;
import distributed.server.Server;
import distributed.server.Servers;
import distributed.tunnel.Tunnel;

public class Test3 {
  
  public static TreeSet<String> getNames(String dir) throws IOException {
    TreeSet<String> result =
        new TreeSet<String>(Arrays.asList(Bash.command("ls -R /media/book/decapsulation/" + dir)
            .getOutput().split("[\n\t ]+")));
    for (Iterator<String> i = result.iterator(); i.hasNext();)
      if (!i.next().startsWith("P"))
        i.remove();
    return result;
  }
  
  public static void main(String[] args) throws IOException {
    TreeSet<String> a = getNames("backup");
    TreeSet<String> b = getNames("micro");
    System.err.println(a.size());
    System.err.println(b.size());
    System.err.println(Sets.difference(a, b));
    System.err.println(Sets.difference(b, a));

    System.exit(0);
    
    Bootstrap.bootstrap(Servers.CIP_91);
    
    TreeSet<String> slaves =
        new TreeSet<String>(Arrays.asList(Main.getSlaves(true, true, true, true)));
    for (String slave : slaves) {
      System.err.println(slave);
      if (SSH.command(new CipServer(slave).getAddress(),
          Bash.command("rm -r /data/micrographs/grey-data")).executeErr() == 0)
        System.err.println("deleted file on " + slave);
    }
  }
}

package distributed.cip;

import general.execution.Command;
import general.execution.SSH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import distributed.server.CipServer;
import distributed.server.Server;

public class Install {

  public static String[] install(String[] hosts) throws IOException {
    ArrayList<String> added = new ArrayList<String>();

    for (String host : hosts) {
      System.err.println("installing " + host);
      Server server = new CipServer(host);

      Command command = new Command(new String[] {"mkdir", server.getDir()});
      command = SSH.command(server.getAddress(), command);
      command = SSH.command(Main.GATEWAY.getAddress(), command);

      if (command.executeErr() != 0)
        System.err.println("failure when installing " + host);
      else
        added.add(host);
    }

    return added.toArray(new String[] {});
  }
  
  public static void main(String[] args) throws IOException {
    String[] added = install(Main.getSlaves(true, true, true, true));
    System.out.println(Arrays.asList(added));
  }

}

package distributed.cip;

import general.execution.Bash;
import general.execution.SSH;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import distributed.server.CipServer;
import distributed.server.Server;
import distributed.tunnel.Tunnel;

public class Clean {

  public static void clean(String[] hosts) throws IOException {
    for (String host : hosts)
      SSH.command(new CipServer(host).getAddress(), Bash.command("killall -s 9 java")).executeErr();
  }
  
  public static void cleanRemotely(String[] hosts) throws IOException {
    try {
      Tunnel.callRemotely(new Server[] {Main.GATEWAY}, null, Tunnel.getMethod("clean"), new Object[] {hosts});
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }
  
}

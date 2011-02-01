package distributed.server;

import general.execution.Command;

import java.io.IOException;

import configuration.Config;

import distributed.cip.Main;
import distributed.execution.Tools;

public class Servers {

  public static final Server LAPTOP = new LaptopServer();

  public static final Server CIP_90 = new CipServer("cip90.mathematik.uni-muenchen.de");
  public static final Server CIP_91 = new CipServer("cip91.mathematik.uni-muenchen.de");

  public static final Server UXUL = new UxulServer();

  public static final String HOSTNAME = getHostname();

  private static final String getHostname() {
    try {
      return new Command("hostname").getOutput().trim();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static final Server getLocalServer() {
    if (HOSTNAME.equals(Config.getOption("local-hostname")))
      return LAPTOP;

    if (HOSTNAME.equals("cip90"))
      return CIP_90;

    if (HOSTNAME.equals("cip91"))
      return CIP_91;

    if (HOSTNAME.equals("uxul"))
      return UXUL;

    if (HOSTNAME.startsWith("cip"))
      return new CipServer(HOSTNAME);

    throw new RuntimeException("unknown local server");
  }

  public static Server[] getTunnel(Server from, Server to) {
    if (from.equals(to))
      return new Server[] {};

    if (to == CIP_90 || to == CIP_91 || to == UXUL)
      return new Server[] {to};

    if (to.getAddress().getHost().startsWith("cip")) {
      if (from.getAddress().getHost().startsWith("cip"))
        return new Server[] {to};

      return new Server[] {Main.GATEWAY, to};
    }

    if (to == LAPTOP) {
      if (from == CIP_90 || from == CIP_91)
        return new Server[] {to};

      return new Server[] {Main.GATEWAY, to};
    }

    throw new RuntimeException("unknown server " + to);
  }

  public static Server[] getTunnel(Server to) {
    return getTunnel(getLocalServer(), to);
  }

}

package distributed.server;

import general.execution.SSHAddress;

import java.io.IOException;

import configuration.Config;

import distributed.execution.Tools;

public class LaptopServer extends AbstractServer {

  public static int parsePort(String s) {
    return s == null ? 22 : Integer.parseInt(s);
  }

  public LaptopServer() {
    super(new SSHAddress(Config.getOption("local-address"),
        parsePort(Config.getOption("local-ssh-port")), Config.getOption("local-user")), Config
        .getOption("local-dir"));
  }

  public long getFreeSpace() throws IOException {
    return Tools.getFreeSpace(getAddress(), Config.getOption("local-dir-partition"));
  }

}

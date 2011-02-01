package distributed.server;

import general.execution.SSHAddress;

import java.io.IOException;

import configuration.Config;

import distributed.execution.Tools;

public class UxulServer extends AbstractServer {

  public UxulServer() {
    super(new SSHAddress("uxul.org", 63332, Config.getOption("uxul-user")), "/home/"
        + Config.getOption("uxul-user"));
  }

  public long getFreeSpace() throws IOException {
    return Tools.getFreeSpace(getAddress(), "/");
  }

}

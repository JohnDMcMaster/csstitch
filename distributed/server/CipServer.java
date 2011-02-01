package distributed.server;

import general.execution.SSHAddress;

import java.io.IOException;

import configuration.Config;

import distributed.execution.Tools;

public class CipServer extends AbstractServer {
  
  public CipServer(String host) {
    super(new SSHAddress(host, Config.getOption("cip-user")), Config.getOption("cip-write-dir"));
  }
  
  public long getFreeSpace() throws IOException {
    return Tools.getFreeSpace(getAddress(), "/data");
  }
  
}

package distributed.server;

import general.execution.SSHAddress;

import java.io.IOException;


public interface Server extends Comparable<Server> {

  public SSHAddress getAddress();

  public String getDir();

  public long getFreeSpace() throws IOException;

}

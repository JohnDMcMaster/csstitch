package distributed.server;

import general.execution.SSHAddress;

import java.io.IOException;


public class AbstractServer implements Server {

  private SSHAddress address;
  private String dir;

  public AbstractServer(SSHAddress address, String dir) {
    this.address = address;
    this.dir = dir;
  }
  
  public SSHAddress getAddress() {
    return address;
  }

  public String getDir() {
    return dir;
  }
  
  public long getFreeSpace() throws IOException {
    return -1;
  }
  
  public boolean equals(Object object) {
    if (!(object instanceof AbstractServer))
      return false;

    AbstractServer server = (AbstractServer) object;
    if (!address.equals(server.address))
      return false;

    return dir.equals(server.dir);
  }

  public int compareTo(Server server) {
    return address.compareTo(server.getAddress());
  }

  public String toString() {
    return address.toString() + ":" + dir;
  }

}

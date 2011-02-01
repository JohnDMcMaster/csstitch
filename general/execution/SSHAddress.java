package general.execution;

public class SSHAddress implements Comparable<SSHAddress> {
  
  public static int DEFAULT_PORT = 22;
  
  private String host;
  private int port;
  private String user;
  
  public SSHAddress(String host, int port, String user) {
    if (host == null)
      host = "localhost";
    
    this.host = host;
    this.port = port;
    this.user = user;
  }
  
  public SSHAddress(String host, String user) {
    this(host, DEFAULT_PORT, user);
  }
  
  public SSHAddress(String host) {
    this(host, null);
  }
  
  public String getHost() {
    return host;
  }
  
  public int getPort() {
    return port;
  }
  
  public String getUser() {
    return user;
  }
  
  public boolean equals(Object object) {
    if (!(object instanceof SSHAddress))
      return false;
    
    SSHAddress address = (SSHAddress) object;
    if (!host.equals(address.host))
      return false;
    
    if (port != address.port)
      return false;
    
    return user.equals(address.user);
  }
  
  public int compareTo(SSHAddress address) {
    int d = host.compareTo(address.host);
    if (d != 0)
      return d;
    
    d = port - address.port;
    if (d != 0)
      return d;
    
    if (user == null || address.user == null)
      return (user == null ? 0 : 1) - (address.user == null ? 0 : 1);
    
    d = user.compareTo(address.user);
    return d;
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder(user.length() + host.length() + 16);
    if (user != null) {
      builder.append(user);
      builder.append("@");
    }
    builder.append(host);
    if (port != DEFAULT_PORT) {
      builder.append(" (port ");
      builder.append(port);
      builder.append(")");
    }
    return builder.toString();
  }
  
}

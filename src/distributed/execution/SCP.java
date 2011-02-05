package distributed.execution;

import general.execution.Command;
import general.execution.SSH;
import general.execution.SSHAddress;

import java.util.ArrayList;
import java.util.Arrays;

import distributed.server.Server;

public class SCP {
  
  private static String formatFile(SSHAddress address, String file) {
    if (address == null)
      return file;
    
    if (address.getUser() == null)
      return address.getHost() + ":" + file;
    
    return address.getUser() + "@" + address.getHost() + ":" + file;
  }
  
  public static Command command(SSHAddress fromAddress, String[] fromFiles, SSHAddress toAddress,
      String toFile, String dir, String[] options) {
    if (fromAddress != null && toAddress != null && fromAddress.getPort() != toAddress.getPort())
      throw new RuntimeException("SSH ports differ (anomaly in scp)");
    
    int port = 22;
    if (fromAddress != null)
      port = fromAddress.getPort();
    else if (toAddress != null)
      port = toAddress.getPort();
    
    ArrayList<String> tokens = new ArrayList<String>();
    tokens.add("scp");
    
    if (options != null)
      for (String option : options) {
        tokens.add("-o");
        tokens.add(option);
      }
    
    if (port != 22) {
      tokens.add("-P");
      tokens.add(Integer.toString(port));
    }
    
    tokens.add("-r");
    
    for (String from : fromFiles)
      tokens.add(formatFile(fromAddress, from));
    
    tokens.add(formatFile(toAddress, toFile));
    
    return new Command(tokens, dir);
  }
  
  public static Command command(SSHAddress fromAddress, String[] fromFiles, SSHAddress toAddress,
      String toFile, String dir) {
    return command(fromAddress, fromFiles, toAddress, toFile, dir, SSH.DEFAULT_OPTIONS);
  }
  
  public static Command copyFromCommand(Server from, Server to, String[] files) {
    files = files.clone();
    for (int i = 0; i != files.length; ++i)
      files[i] = from.getDir() + "/" + files[i];
    
    return command(from.getAddress(), files, null, to.getDir(), null);
  }
  
  public static Command copyToCommand(Server from, Server to, String[] files) {
    return command(null, files, to.getAddress(), to.getDir(), from.getDir());
  }
  
}

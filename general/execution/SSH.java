package general.execution;

import java.util.ArrayList;

public class SSH {
  
  public static String[] DEFAULT_OPTIONS = new String[] {"ConnectTimeout=3",
      "StrictHostKeyChecking=no"};
  
  public static Command command(SSHAddress address, String[] options, Command command) {
    ArrayList<String> tokenList = new ArrayList<String>();
    
    tokenList.add("ssh");
    tokenList.add("-T");
    
    if (options != null)
      for (String option : options) {
        tokenList.add("-o");
        tokenList.add(option);
      }
    
    if (address.getPort() != 22) {
      tokenList.add("-p");
      tokenList.add(Integer.toString(address.getPort()));
    }
    
    if (address.getUser() != null) {
      tokenList.add("-l");
      tokenList.add(address.getUser());
      
    }
    tokenList.add(address.getHost());
    tokenList.add(Bash.quote(command));
    return new Command(tokenList, null);
  }
  
  public static Command command(SSHAddress address, Command command) {
    return command(address, DEFAULT_OPTIONS, command);
  }
  
}

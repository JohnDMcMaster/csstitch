package general.execution;

import java.util.Arrays;
import java.util.List;

public class Bash {
  
  public static String quote(String s) {
    return "'" + s.replace("'", "'\\''") + "'";
  }
  
  public static String quote(List<String> tokens) {
    StringBuilder buffer = new StringBuilder();
    for (String token : tokens) {
      if (buffer.length() != 0)
        buffer.append(' ');
      
      buffer.append(quote(token));
    }
    
    return buffer.toString();
  }
  
  public static String quote(String... tokens) {
    return quote(Arrays.asList(tokens));
  }
  
  public static String quote(Command command) {
    String result = quote(command.getTokens());
    if (command.getDir() != null)
      result = and("cd " + quote(command.getDir()), result);
    return result;
  }
  
  public static Command command(String s, String dir) {
    return new Command(new String[] {"bash", "-c", s}, dir);
  }
  
  public static Command command(String s) {
    return command(s, null);
  }
  
  public static String read(String filename) {
    return "cat <" + Bash.quote(filename);
  }
  
  public static String write(String filename) {
    return "cat >" + Bash.quote(filename);
  }
  
  public static String pipe(String from, String to) {
    return from + " | " + to;
  }
  
  public static String and(String from, String to) {
    return from + " && " + to;
  }
  
}

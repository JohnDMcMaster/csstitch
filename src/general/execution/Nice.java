package general.execution;

import java.util.ArrayList;

public class Nice {
  
  public static int DEFAULT_NICENESS = 19;
  
  public static Command command(Command command) {
    return command(DEFAULT_NICENESS, command);
  }

  public static Command command(int level, Command command) {
    ArrayList<String> tokens = new ArrayList<String>();
    tokens.add("nice");
    tokens.add("-n");
    tokens.add(Integer.toString(level));
    tokens.addAll(command.getTokens());
    return new Command(tokens, command.getDir());
  }

}

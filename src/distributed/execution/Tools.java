package distributed.execution;

import general.execution.Command;
import general.execution.SSH;
import general.execution.SSHAddress;

import java.io.IOException;

import configuration.Config;

public class Tools {

  public static long getFreeSpace(SSHAddress address, String mountPoint) throws IOException {
    String out = SSH.command(address, new Command("df -B 1")).getOutput();
    if (out == null)
      return -1;

    for (String line : out.split("\n")) {
      String[] fields = line.split(" +");
      if (fields[5].equals(mountPoint))
        return Long.parseLong(line.split(" +")[3]);
    }

    return -1;
  }
  
  public static String toUnixPath(String path) {
    try {
      String result = new Command(new String[] {"cygpath", path}).getOutput();
      result = result.substring(0, result.length() - 1);
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static String unixify(String path) {
    if (!Config.getOption("local-use-cygwin").equals("true"))
      return path;
    
    return toUnixPath(path);
  }

}

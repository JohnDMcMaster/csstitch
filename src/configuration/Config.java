package configuration;

import general.Streams;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.TreeMap;

public class Config {
  
  public static final File CONFIG_FILE = new File("config/config.txt");
  private static final String CONFIG_RESOURCE = "/" + CONFIG_FILE.getName();
  
  private static final TreeMap<String, String> config = load(readResource(CONFIG_RESOURCE));
  
  public static String getOption(String key) {
    return config.get(key);
  }
  
  public static void putOption(String key, String value) {
    config.put(key, value);
    if (CONFIG_FILE.exists())
      save(CONFIG_FILE, config);
  }
  
  private static String encode(String s) {
    boolean simple = true;
    for (int i = 0; i != s.length(); ++i) {
      char c = s.charAt(i);
      if (c <= '\u0020' || c == '=' || c == '\\') {
        simple = false;
        break;
      }
    }
    
    if (simple)
      return s;
    
    StringBuilder builder = new StringBuilder();
    builder.append('"');
    for (int i = 0; i != s.length(); ++i) {
      char c = s.charAt(i);
      if (c == '"' || c == '\\' || c == '\n' || c == '\t')
        builder.append('\\');
      if (c == '\n')
        builder.append('n');
      else if (c == '\t')
        builder.append('t');
      else
        builder.append(c);
    }
    builder.append('"');
    
    return builder.toString();
  }
  
  private static String decode(String s) {
    if (!s.startsWith("\""))
      return s;
    
    s = s.substring(1, s.length() - 1);
    
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i != s.length(); ++i) {
      char c = s.charAt(i);
      if (c == '\\') {
        c = s.charAt(++i);
        if (c == 'n')
          builder.append('\n');
        else if (c == 't')
          builder.append('\t');
        else
          builder.append(c);
      } else
        builder.append(c);
    }
    return builder.toString();
  }
  
  private static String readResource(String resource) {
    try {
      return Streams.readIntoString(Config.class.getResourceAsStream(resource));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private static TreeMap<String, String> load(String data) {
    TreeMap<String, String> map = new TreeMap<String, String>();
    for (String line : data.split("\n")) {
      int i = line.indexOf("=");
      if (i != -1) {
        map.put(decode(line.substring(0, i).trim()), decode(line.substring(i + 1).trim()));
      }
    }
    return map;
  }
  
  private static void save(File file, TreeMap<String, String> map) {
    int maxKeyLength = 0;
    for (String key : map.keySet())
      maxKeyLength = Math.max(maxKeyLength, encode(key).length());
    
    try {
      PrintStream out = new PrintStream(file);
      for (String key : map.keySet())
        out.printf("%-" + maxKeyLength + "s = %s\n", encode(key), encode(map.get(key)));
      out.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static void main(String[] args) {
    putOption("local-use-cygwin", "true");
  }
  
}

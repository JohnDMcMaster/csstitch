package hm;

import general.Streams;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagSwitch {
  
  public static void main(String[] args) throws IOException {
    String in = "/home/noname/di/tags.txt";
    String out = "/home/noname/di/tags-out.txt";
    
    String text = Streams.readText(in);
    
    Matcher matcher = Pattern.compile("step \\(([0-9]+), ([0-9]+)\\)").matcher(text);
    
    StringBuilder builder = new StringBuilder();
    int index = 0;
    
    while (matcher.find()) {
      builder.append(text.substring(index, matcher.start()));
      builder.append("step (" + matcher.group(2) + ", " + matcher.group(1) + ")");
      index = matcher.end();
    }
    
    builder.append(text.substring(index));
    
    Streams.writeText(out, builder.toString());
  }
  
}

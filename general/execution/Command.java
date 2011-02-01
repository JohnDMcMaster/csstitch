package general.execution;

import general.Link;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public class Command {
  
  private static boolean DEBUG = false;
  
  public static int waitFor(Process process) {
    try {
      return process.waitFor();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  public String[] tokens;
  public String dir;
  
  public Command(String command) {
    this(command, null);
  }
  
  public Command(String command, String dir) {
    StringTokenizer tokenizer = new StringTokenizer(command);
    tokens = new String[tokenizer.countTokens()];
    for (int i = 0; i != tokens.length; ++i)
      tokens[i] = tokenizer.nextToken();
    
    this.dir = dir;
  }
  
  public Command(String... tokens) {
    this(tokens, null);
  }
  
  public Command(String[] tokens, String dir) {
    this.tokens = tokens.clone();
    this.dir = dir;
  }
  
  public Command(Collection<String> tokens) {
    this(tokens, null);
  }
  
  public Command(Collection<String> tokens, String dir) {
    this.tokens = tokens.toArray(new String[] {});
    this.dir = dir;
  }
  
  public List<String> getTokens() {
    return Collections.unmodifiableList(Arrays.asList(tokens));
  }
  
  public String getDir() {
    return dir;
  }
  
  public Process start() throws IOException {
    if (DEBUG) {
      for (String token : tokens)
        System.err.print(token + " ");
      System.err.println();
    }
    
    return Runtime.getRuntime().exec(tokens, null, dir == null ? null : new File(dir));
  }
  
  public Process startErr() throws IOException {
    Process process = start();
    Link.link(process.getErrorStream(), System.err);
    return process;
  }
  
  public int execute() throws IOException {
    Process process = start();
    int result = waitFor(process);

    process.getInputStream().close();
    process.getOutputStream().close();
    process.getErrorStream().close();
    return result;
  }
  
  public int executeErr() throws IOException {
    Process process = startErr();
    int result = waitFor(process);

    process.getInputStream().close();
    process.getOutputStream().close();
    return result;
  }
  
  public void executeChecked() throws IOException {
    int result = executeErr();
    if (result != 0)
      throw new IOException("command failed: " + this);
  }
  
  public String getOutput() throws IOException {
    Process process = startErr();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Link.run(process.getInputStream(), out);
    String result = waitFor(process) == 0 ? out.toString() : null;
    
    process.getOutputStream().close();
    process.getErrorStream().close();
    return result;
  }
  
  public int executeDirect() throws IOException {
    Process process = start();
    Link.link(System.in, process.getOutputStream());
    Thread b = Link.link(process.getInputStream(), System.out);
    Thread c = Link.link(process.getErrorStream(), System.err);
    
    try {
      b.join();
      c.join();
      return process.waitFor();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (String token : tokens) {
      if (builder.length() != 0)
        builder.append(' ');
      
      builder.append(token);
    }
    return builder.toString();
  }
  
}

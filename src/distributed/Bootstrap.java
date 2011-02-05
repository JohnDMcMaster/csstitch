package distributed;

import general.execution.Command;
import general.execution.Nice;
import general.execution.SSHAddress;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

import configuration.Config;

import distributed.cip.DistributeFiles;
import distributed.cip.Main;
import distributed.execution.Tools;
import distributed.server.Server;
import distributed.server.Servers;
import distributed.tunnel.Tunnel;

public class Bootstrap {
  
  public static final String WORKSPACE = Config.getOption("workspace");
  
  public static String getJarName(String entry) {
    return entry.substring(entry.lastIndexOf(".") + 1) + ".jar";
  }
  
  public static Command createJarCommand(String entry) {
    ArrayList<String> tokens = new ArrayList<String>();
    tokens.add("jar");
    tokens.add("cfe");
    
    String jarName = getJarName(entry);
    tokens.add(jarName);
    
    tokens.add(entry);
    
    for (String file : new File(WORKSPACE + "/FeatureRecognition/bin").list()) {
      tokens.add("-C");
      tokens.add("FeatureRecognition/bin");
      tokens.add(file);
    }
    
    for (String file : new File(WORKSPACE + "/Tools/bin").list()) {
      tokens.add("-C");
      tokens.add("Tools/bin");
      tokens.add(file);
    }
    
    tokens.add("-C");
    tokens.add("FeatureRecognition/" + Config.CONFIG_FILE.getParent());
    tokens.add(Config.CONFIG_FILE.getName());
    
    return new Command(tokens, WORKSPACE);
  }
  
  public static Command jarCommand(String jarName, String[] vmArgs, String[] args, String dir)
      throws IOException {
    ArrayList<String> tokens = new ArrayList<String>();
    tokens.add("java");
    
    if (vmArgs != null)
      tokens.addAll(Arrays.asList(vmArgs));
    
    tokens.add("-jar");
    tokens.add(jarName);
    
    if (args != null)
      tokens.addAll(Arrays.asList(args));
    
    return new Command(tokens, dir);
  }
  
  public static final Server BOOTSTRAP_SERVER = Servers.CIP_90;
  
  public static void bootstrap() throws IOException {
    bootstrap((String[]) null);
  }
  
  public static void bootstrap(String[] args) throws IOException {
    bootstrap(BOOTSTRAP_SERVER, args);
  }
  
  public static void bootstrap(Server server) throws IOException {
    bootstrap(server, null);
  }
  
  public static void bootstrap(Server server, String[] args) throws IOException {
    bootstrap(Servers.getTunnel(server), args, null);
  }
  
  public static void bootstrap(Server[] serverTunnel) throws IOException {
    bootstrap(serverTunnel, null);
  }
  
  public static void bootstrap(Server[] serverTunnel, String[] args) throws IOException {
    bootstrap(serverTunnel, args, null);
  }
  
  public static void bootstrap(Server[] serverTunnel, String[] args, String name)
      throws IOException {
    if (!Servers.HOSTNAME.equals(Config.getOption("local-hostname")))
      return;
    
    if (name == null) {
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      for (int i = trace.length; i != 0; --i) {
        if (!trace[i - 1].getClassName().equals(Bootstrap.class.getName())) {
          name = trace[i - 1].getClassName();
          break;
        }
      }
    }
    
    Server server = serverTunnel[serverTunnel.length - 1];
    SSHAddress[] tunnel = Tunnel.getTunnel(serverTunnel);
    
    long a = System.currentTimeMillis();
    
    String entry, jarName;
    
    entry = Tunnel.class.getName();
    createJarCommand(entry).executeChecked();
    jarName = getJarName(Tunnel.class.getName());
    Tunnel.tunnelCopyCommand(tunnel, WORKSPACE + "/" + jarName, server.getDir() + "/" + jarName)
        .executeChecked();
    
    entry = name;
    createJarCommand(entry).executeChecked();
    jarName = getJarName(entry);
    Tunnel.tunnelCopyCommand(tunnel, WORKSPACE + "/" + jarName, server.getDir() + "/" + jarName)
        .executeChecked();
    
    Tunnel
        .tunnelCommand(
            tunnel,
            Nice.command(jarCommand(jarName, new String[] {"-server", "-Xmx1G", "-Xss32M"}, args,
                server.getDir()))).executeDirect();
    
    long b = System.currentTimeMillis();
    System.out.println(b - a);
    
    System.exit(0);
  }

}

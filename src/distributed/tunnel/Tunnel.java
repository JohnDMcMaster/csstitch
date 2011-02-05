package distributed.tunnel;

import general.execution.Bash;
import general.execution.Command;
import general.execution.Nice;
import general.execution.SSH;
import general.execution.SSHAddress;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import distributed.Bootstrap;
import distributed.cip.DistributeFiles;
import distributed.cip.Main;
import distributed.server.Server;
import distributed.server.Servers;

public class Tunnel {
  
  public static SSHAddress[] getTunnel(Server[] servers) {
    SSHAddress[] addresses = new SSHAddress[servers.length];
    for (int i = 0; i != servers.length; ++i)
      addresses[i] = servers[i].getAddress();
    return addresses;
  }
  
  public static Command tunnelCommand(SSHAddress[] addresses, Command command) {
    for (int i = addresses.length; i != 0; --i)
      command = SSH.command(addresses[i - 1], command);
    return command;
  }
  
  public static Command tunnelCopyCommand(SSHAddress[] addresses, String from, String to) {
    return Bash.command(Bash.pipe(Bash.read(from), Bash.quote(tunnelCommand(addresses, Bash
        .command(Bash.write(to))))));
  }
  
  public static void init(Server[] serverTunnel) throws IOException {
    SSHAddress[] tunnel = Tunnel.getTunnel(serverTunnel);
    
    String entry = Tunnel.class.getName();
    Bootstrap.createJarCommand(entry).executeChecked();
    String jarName = Bootstrap.getJarName(Tunnel.class.getName());
    Tunnel.tunnelCopyCommand(tunnel, Bootstrap.WORKSPACE + "/" + jarName,
        serverTunnel[0].getDir() + "/" + jarName).executeChecked();
  }
  
  public static void init(Server server) throws IOException {
    init(Servers.getTunnel(server));
  }
  
  public static String[] initSlaveTunnels(String[] slaves) throws IOException {
    String jarName = Bootstrap.getJarName(Tunnel.class.getName());
    Command command =
        tunnelCopyCommand(new SSHAddress[] {Main.GATEWAY.getAddress()}, Bootstrap.WORKSPACE + "/"
            + jarName, Main.GATEWAY.getDir() + "/" + jarName);
    
    String[] files = new String[] {jarName};
    String[] to = slaves;
    
    System.err.println(Arrays.asList(slaves));
    
    if (Servers.getLocalServer() == Servers.LAPTOP) {
      command.executeChecked();
      
      try {
        String[] from = new String[] {Main.GATEWAY.getAddress().getHost()};
        return (String[]) callRemotely(new Server[] {Main.GATEWAY}, null, getMethod(
            "distributeFiles", DistributeFiles.class), files, from, to);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e.getCause());
      }
    } else if (Servers.getLocalServer() == Servers.CIP_90
        || Servers.getLocalServer() == Servers.CIP_91) {
      //SSH.command(Servers.LAPTOP.getAddress(), command).executeChecked();
      
      String[] from = new String[] {Servers.getLocalServer().getAddress().getHost()};
      return DistributeFiles.distributeFiles(files, from, to);
    } else
      throw new RuntimeException("missing tunnel instructions");
  }
  
  private static ObjectTunnel startObjectTunnel(Server[] servers, String[] vmArgs)
      throws IOException {
    SSHAddress[] addresses = getTunnel(servers);
    
    String jarName = Bootstrap.getJarName(Tunnel.class.getName());
    Server last = servers.length == 0 ? Servers.getLocalServer() : servers[servers.length - 1];
    /*tunnelCopyCommand(addresses, Bootstrap.WORKSPACE + "/" + jarName, last.getDir() + "/" + jarName)
        .executeErr();*/

    Command command = Nice.command(Bootstrap.jarCommand(jarName, vmArgs, null, last.getDir()));
    Process process = tunnelCommand(addresses, command).startErr();
    return new ObjectTunnel(process);
  }
  
  public static ObjectTunnel startObjectTunnel(Server[] servers, String[] vmArgs, Method method)
      throws IOException {
    ObjectTunnel tunnel = startObjectTunnel(servers, vmArgs);
    tunnel.getOut().writeUnshared(method.getDeclaringClass());
    tunnel.getOut().writeUTF(method.getName());
    tunnel.getOut().writeBoolean(true);
    tunnel.getOut().flush();
    return tunnel;
  }
  
  public static ObjectTunnel startObjectTunnel(Server server, String[] args, Method method)
      throws IOException {
    return startObjectTunnel(Servers.getTunnel(server), args, method);
  }
  
  public static Object callRemotely(Server[] servers, String[] vmArgs, Method method,
      Object... args) throws IOException, InvocationTargetException {
    Class<?>[] types = method.getParameterTypes();
    if (args.length != types.length)
      throw new RuntimeException("argument counts do not match");
    
    ObjectTunnel tunnel = startObjectTunnel(servers, vmArgs);
    tunnel.getOut().writeUnshared(method.getDeclaringClass());
    tunnel.getOut().writeUTF(method.getName());
    tunnel.getOut().writeBoolean(false);
    
    tunnel.getOut().writeInt(types.length);
    for (Class<?> type : types)
      tunnel.getOut().writeUnshared(type);
    
    for (Object argument : args)
      tunnel.write(argument);
    
    tunnel.flush();
    
    Throwable exception = (Throwable) tunnel.read();
    Object result = tunnel.read();
    if (exception != null)
      throw new InvocationTargetException(exception);
    
    return result;
  }
  
  public static Method getMethod(String methodName, Class<?> c) {
    if (c == null) {
      try {
        c = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    
    Method[] methods = c.getMethods();
    for (Method method : methods)
      if (method.getName().equals(methodName) && Modifier.isStatic(method.getModifiers()))
        return method;
    
    throw new RuntimeException("no static method " + c.getName() + "." + methods);
  }
  
  public static Method getMethod(String methodName) {
    try {
      return getMethod(methodName, Class.forName(Thread.currentThread().getStackTrace()[2]
          .getClassName()));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static void main(String[] args) throws Throwable {
    if (Servers.HOSTNAME.equals("laptop")) {
      Bootstrap.createJarCommand(Tunnel.class.getName()).executeChecked();
      return;
    }
    
    ObjectTunnel tunnel = new ObjectTunnel();
    
    Class<?> c = (Class<?>) tunnel.getIn().readUnshared();
    String name = tunnel.getIn().readUTF();
    boolean streams = tunnel.getIn().readBoolean();
    
    if (streams) {
      Method method = c.getMethod(name, ObjectTunnel.class);
      try {
        method.invoke(null, tunnel);
      } catch (InvocationTargetException e) {
        throw e.getCause();
      }
    } else {
      Class<?>[] types = new Class[tunnel.getIn().readInt()];
      for (int i = 0; i != types.length; ++i)
        types[i] = (Class<?>) tunnel.getIn().readUnshared();
      
      Method method = c.getMethod(name, types);
      
      Object[] arguments = new Object[types.length];
      for (int i = 0; i != types.length; ++i)
        arguments[i] = tunnel.read();
      
      Throwable exception = null;
      Object result = null;
      try {
        result = method.invoke(null, arguments);
      } catch (InvocationTargetException e) {
        exception = e.getCause();
      }
      
      tunnel.write(exception);
      tunnel.write(result);
      tunnel.flush();
    }
  }
  
}

package distributed.slaves;

import general.execution.Command;
import general.execution.SSHAddress;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import distributed.server.Server;
import distributed.server.Servers;
import distributed.tunnel.ObjectTunnel;
import distributed.tunnel.Tunnel;

public class Slave implements Comparable<Slave> {

  private SlaveHandler handler;
  private Server server;
  private Server[] servers;
  private String id;

  Slave(SlaveHandler handler, Server server) {
    this(handler, server, server.getAddress().getHost());
  }
  
  Slave(SlaveHandler handler, Server server, String id) {
    this.handler = handler;
    this.server = server;
    this.servers = Servers.getTunnel(Servers.getLocalServer(), server);
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public List<Server> getServers() {
    return Collections.unmodifiableList(Arrays.asList(servers));
  }

  public Server getServer() {
    return server;
  }

  public String getDir() {
    return server.getDir();
  }

  public SSHAddress[] getTunnel() {
    SSHAddress[] tunnel = new SSHAddress[servers.length];
    for (int i = 0; i != tunnel.length; ++i)
      tunnel[i] = servers[i].getAddress();
    return tunnel;
  }

  public Command command(String[] tokens) {
    return new Command(tokens, getDir());
  }

  public Command tunnel(Command command) {
    return Tunnel.tunnelCommand(getTunnel(), command);
  }

  public ObjectTunnel startObjectTunnel(String[] vmArgs, Method method) {
    try {
      return Tunnel.startObjectTunnel(servers, vmArgs, method);
    } catch (IOException e) {
      e.printStackTrace();
      handler.reportMalfunction(this);
      return null;
    }
  }

  public Object callRemotely(String[] vmArgs, Method method, Object... args) throws Exception {
    Object result = null;
    try {
      result = Tunnel.callRemotely(servers, vmArgs, method, args);
    } catch (IOException e) {
      System.err.println("slave " + getId() + " malfunctioning: " + e.getMessage());
    } catch (InvocationTargetException e) {
      if (!(e.getCause() instanceof Error)) // todo
        throw (Exception) e.getCause();

      System.err.println("slave " + getId() + " malfunctioning:");
      System.err.println(e.getCause());
    }

    if (result == null)
      handler.reportMalfunction(this);
    return result;

  }

  public int compareTo(Slave slave) {
    return getId().compareTo(slave.getId());
  }
  
  public String toString() {
    return getId();
  }

}

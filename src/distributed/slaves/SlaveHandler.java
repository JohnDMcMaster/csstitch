package distributed.slaves;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.TreeSet;

import distributed.cip.Clean;
import distributed.cip.Main;
import distributed.server.CipServer;
import distributed.server.Servers;
import distributed.tunnel.Tunnel;

public class SlaveHandler {
  
  private static int DEFAULT_MIN_SLAVES = 1;
  
  private LinkedList<Slave> slaves = new LinkedList<Slave>();
  private LinkedList<Slave> free = new LinkedList<Slave>();
  private TreeSet<Slave> malfunctioning = new TreeSet<Slave>();
  
  private int minSlaves;
  private int[] hook = new int[] {1};
  
  public SlaveHandler() throws IOException {
    this(DEFAULT_MIN_SLAVES);
  }
  
  public SlaveHandler(int minSlaves) throws IOException {
    this.minSlaves = minSlaves;
    
    for (int i = 0; i != 2; ++i) {
      //slaves.add(new Slave(this, Servers.CIP_90, "cip90-" + i));
      //slaves.add(new Slave(this, Servers.CIP_91, "cip91-" + i));
    }
    
    TreeSet<String> slaveArray;
    //TreeSet<String> slaveArray = new TreeSet<String>(Arrays.asList(new String[] {}));
    slaveArray = new TreeSet<String>(Arrays.asList(Main.getSlaves(false, true, false, false)));
    //slaveArray.add("cip78");
    //slaveArray.remove("cip11");
    //for (int n = 70; n != 70; ++n)
    //  slaveArray.add("cip" + n);
    
    for (String host : slaveArray) {
      slaves.add(new Slave(this, new CipServer(host), host + "-0"));
      //slaves.add(new Slave(this, new CipServer(host), host + "-1"));
    }
    
    TreeSet<String> cipSlaves = new TreeSet<String>();
    for (Slave slave : slaves)
      cipSlaves.add(slave.getId().substring(0, 5));
    cipSlaves.remove(Servers.getLocalServer().getAddress().getHost().substring(0, 5));
    
    cipSlaves.removeAll(Arrays.asList(Tunnel.initSlaveTunnels(cipSlaves.toArray(new String[] {}))));
    for (Slave slave : new TreeSet<Slave>(slaves))
      if (!slave.getServer().equals(Servers.getLocalServer())
          && !cipSlaves.contains(slave.getServer().getAddress().getHost().substring(0, 5)))
        slaves.remove(slave);
    
    //Clean.cleanRemotely(cipSlaves.toArray(new String[] {}));
    
    free.addAll(slaves);
  }
  
  public String[] getSlaves() {
    ArrayList<String> hosts = new ArrayList<String>();
    for (Slave slave : slaves)
      hosts.add(slave.getServer().getAddress().getHost());
    return hosts.toArray(new String[] {});
  }
  
  public int getNumSlaves() {
    return slaves.size();
  }
  
  public int getResult() {
    synchronized (slaves) {
      return hook[0];
    }
  }
  
  public void giveBack(Slave slave) {
    synchronized (slaves) {
      if (!malfunctioning.contains(slave)) {
        free.addLast(slave);
        slaves.notify();
      }
    }
  }
  
  public Slave getFree() {
    synchronized (slaves) {
      while (free.isEmpty() && hook[0] == 1)
        try {
          slaves.wait();
        } catch (InterruptedException e) {
        }
      
      if (hook[0] != 1)
        return null;
      
      return free.pollFirst();
    }
  }
  
  public void reportMalfunction(Slave slave) {
    synchronized (slaves) {
      malfunctioning.add(slave);
      if (slaves.size() - malfunctioning.size() < minSlaves) {
        System.err.println("less than " + minSlaves + " slaves left");
        hook[0] = -1;
        slaves.notifyAll();
      }
    }
  }
  
}

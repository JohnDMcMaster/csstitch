package distributed.cip;

import general.execution.Command;
import general.execution.Nice;
import general.execution.SSH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import distributed.Bootstrap;
import distributed.execution.SCP;
import distributed.server.CipServer;
import distributed.server.Server;

public class DistributeFiles {
  
  public static boolean copy(String[] files, String from, String to) throws IOException {
    Server fromServer = new CipServer(from);
    Server toServer = new CipServer(to);
    System.err.println("copying from " + fromServer + " to " + toServer);
    
    Command command = SCP.copyToCommand(fromServer, toServer, files);
    command = Nice.command(command);
    command = SSH.command(fromServer.getAddress(), command);
    command = Nice.command(command);
    
    int result = command.executeErr();
    System.err.println("copy result: " + result);
    return result == 0;
  }
  
  public static String[] distributeFiles(final String[] files, String[] from, String[] to)
      throws IOException {
    TreeSet<String> set = new TreeSet<String>(Arrays.asList(to));
    set.removeAll(Arrays.asList(from));
    to = set.toArray(new String[] {});
    
    final TreeSet<String> free = new TreeSet<String>();
    free.addAll(Arrays.asList(from));
    
    final ArrayList<String> missing = new ArrayList<String>();
    
    final TreeSet<String> copying = new TreeSet<String>();
    
    class Worker implements Runnable {
      public String host;
      
      public Worker(String host) {
        this.host = host;
      }
      
      public void run() {
        for (int i = 0; i != 2; ++i) {
          String source;
          synchronized (free) {
            while (free.isEmpty())
              try {
                free.wait();
              } catch (InterruptedException e) {
              }
            
            source = free.toArray(new String[] {})[(int) (Math.random() * free.size())];
            free.remove(source);
          }
          
          synchronized (copying) {
            copying.add(host);
          }
          
          boolean result = false;
          try {
            result = copy(files, source, host);
          } catch (IOException e) {
            System.err.println("when copying from " + source + " to " + host + ":");
            e.printStackTrace();
          }
          
          synchronized (copying) {
            copying.remove(host);
          }
          
          synchronized (free) {
            free.add(source);
            if (result)
              free.add(host);
            
            free.notifyAll();
          }
          
          if (result)
            return;
        }
        
        synchronized (missing) {
          missing.add(host);
        }
      }
    }
    
    ExecutorService service = Executors.newFixedThreadPool(16);
    ArrayList<Future<?>> futures = new ArrayList<Future<?>>();
    
    for (String host : to)
      futures.add(service.submit(new Worker(host)));
    
    for (Future<?> future : futures)
      try {
        future.get();
      } catch (InterruptedException e) {
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    
    service.shutdown();
    try {
      while (!service.awaitTermination(10, TimeUnit.SECONDS))
        System.out.println(copying);
    } catch (InterruptedException e) {
    }
    
    return missing.toArray(new String[] {});
  }
  
  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(args);
    
    TreeSet<String> to = new TreeSet<String>(Arrays.asList(Main.getSlaves(true, true, true, true)));
    to.remove("cip11");
    String[] rem =
        distributeFiles(new String[] {"stitch-final.dat"}, new String[] {"cip78", "cip80", "cip90",
            "cip91"}, to.toArray(new String[] {}));
    System.out.println(Arrays.asList(rem));
  }
  
}

package distributed.slaves;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import distributed.cip.DistributeFiles;
import distributed.server.Servers;

public class SlavePool {
  
  public static interface Callback<S, T> {
    public T callback(S result);
  }
  
  public static interface SimpleCallback<S> {
    public void callback(S result);
  }
  
  SlaveHandler handler;
  ExecutorService pool;
  String[] vmArgs;
  ArrayList<Future<?>> futures = new ArrayList<Future<?>>();
  
  public SlavePool() throws IOException {
    this(new String[] {"-Xmx2G"}, -1);
  }
  
  public SlavePool(String[] vmArgs, int numThreads) throws IOException {
    handler = new SlaveHandler();
    pool = Executors.newFixedThreadPool(numThreads == -1 ? handler.getNumSlaves() : numThreads);
    this.vmArgs = vmArgs.clone();
  }
  
  public void distributeFiles(String[] files) throws IOException {
    TreeSet<String> to = new TreeSet<String>(Arrays.asList(handler.getSlaves()));
    to.remove(Servers.getLocalServer().getAddress().getHost());
    DistributeFiles.distributeFiles(files, new String[] {Servers.getLocalServer().getAddress()
        .getHost()}, to.toArray(new String[] {}));
  }
  
  public int estimateNumSlaves() {
    return (int) (0.9 * handler.getNumSlaves());
  }
  
  public void waitTillFinished() throws Exception {
    for (Future<?> future : futures)
      try {
        future.get();
      } catch (InterruptedException e) {
      } catch (ExecutionException e) {
        throw (Exception) e.getCause();
      }
    
    System.err.println("all jobs have terminated");
    pool.shutdown();
    pool.awaitTermination(1000000, TimeUnit.DAYS);
    System.err.println("pool is shut down");
    
    if (handler.getResult() == -1)
      throw new Exception("slave handler operation disrupted");
  }
  
  public <T> Future<T> submit(Method method, Object... args) {
    return submit(new Callback<T, T>() {
      public T callback(T result) {
        return result;
      }
      
    }, method, args);
  }
  
  public <S> void submit(final SimpleCallback<S> callback, final Method method,
      final Object... args) {
    submit(new Callback<S, Void>() {
      public Void callback(S result) {
        callback.callback(result);
        return null;
      }
    }, method, args);
  }
  
  public <S, T> Future<T> submit(final Callback<S, T> callback, final Method method,
      final Object... args) {
    Future<T> future = pool.submit(new Callable<T>() {
      @SuppressWarnings("unchecked")
      public T call() throws Exception {
        Object result = null;
        Slave slave = null;
        while (result == null) {
          slave = handler.getFree();
          if (slave == null)
            return null;
          
          try {
            result = slave.callRemotely(vmArgs, method, args);
            handler.giveBack(slave);
          } catch (Exception e) {
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(byteArray);
            out.println("slave " + slave.getId());
            e.printStackTrace(out);
            System.err.println(byteArray.toString());
            throw e;
          }
        }
        
        System.err.println("job done");
        return callback.callback((S) result);
      }
    });
    
    futures.add(future);
    return future;
  }
}

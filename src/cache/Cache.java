package cache;

import general.Streams;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import data.DataTools;
import data.Tools;

public class Cache {
  
  public static final File CACHE = new File(DataTools.DIR + "cache");
  
  @SuppressWarnings("unchecked")
  public static <T> T cache(String filename, Object... args) throws IOException {
    StackTraceElement[] trace = Thread.currentThread().getStackTrace();
    if (trace.length > 3 && (trace[3].isNativeMethod() || trace[3].getFileName() == null))
      return null;
    
    String path = CACHE.getAbsolutePath() + "/" + String.format(filename, args);
    if (new File(path).exists())
    {
      System.out.println("Bad compile");
      System.exit(1);
      //return Streams.readObject(path);
    }
    
    StackTraceElement caller = trace[2];
    Class<?> clasz = null;
    try {
      clasz = Class.forName(caller.getClassName());
    } catch (ClassNotFoundException e) {
    }
    
    String methodName = caller.getMethodName();
    
    for (Method method : clasz.getMethods())
      if (method.getName().equals(methodName)) {
        T result;
        
        try {
          result = (T) method.invoke(null, args);
        } catch (IllegalArgumentException e) {
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
          Throwable cause = e.getCause();
          if (cause instanceof IOException)
            throw (IOException) cause;
          
          if (cause instanceof RuntimeException)
            throw (RuntimeException) cause;
          
          throw new RuntimeException(e);
        }
        
        if (result != null) {
          Tools.ensurePath(path);
          Streams.writeObject(path, result);
        }
        
        return result;
      }
    
    throw new RuntimeException("no suitable method found");
  }
}

package distributed.manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import general.execution.Command;

public class Manager {
  
  private static final int MAX_CHUNK_LENGTH = 64 * 1024;
  
  public static final Manager MANAGER = new Manager();
  
  private Runnable runnable = new Runnable() {
    public void run() {
      try {
        DataInputStream in = new DataInputStream(new BufferedInputStream(process.getInputStream()));
        byte[] buffer = new byte[MAX_CHUNK_LENGTH];
        
        for (;;) {
          int id;
          try {
            id = in.readInt();
          } catch (EOFException e) {
            break;
          }
          
          int type = in.readInt();
          if (type == 0 || type == 1) {
            int length = in.readInt();
            if (length > buffer.length)
              throw new RuntimeException("chunk length " + length + " exceeds buffer size "
                  + buffer.length);
            in.readFully(buffer, 0, length);
            
            OutputStream destination;
            if (type == 0)
              destination = System.err;
            else
              synchronized (outputStreams) {
                destination = outputStreams.get(id);
              }
            
            if (destination != null)
              destination.write(buffer, 0, length);
          } else if (type == 2) {
            OutputStream destination;
            synchronized (outputStreams) {
              destination = outputStreams.get(id);
            }
            
            destination.close();
            
            synchronized (outputStreams) {
              outputStreams.set(id, outputStreams.get(outputStreams.size() - 1));
              outputStreams.remove(outputStreams.size() - 1);
              if (outputStreams.isEmpty()) {
                stop();
                break;
              }
            }
          } else
            throw new RuntimeException("unknown type " + type);
        }
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(-1);
      }
    }
  };
  
  private Process process;
  private ArrayList<OutputStream> outputStreams;
  private DataOutputStream out;
  
  private Manager() {
    outputStreams = new ArrayList<OutputStream>();
  }
  
  private void start() throws IOException {
    process = new Command("manager").startErr();
    out = new DataOutputStream(new BufferedOutputStream(process.getOutputStream()));
    new Thread(runnable).start();
  }
  
  private void stop() throws IOException {
    out.writeInt(1);
  }
  
  private void writeString(String string) throws IOException {
    out.writeInt(string.length());
    out.write(string.getBytes());
  }
  
  public void add(Command command, OutputStream output) throws IOException {
    synchronized (outputStreams) {
      if (outputStreams.isEmpty())
        start();
      
      outputStreams.add(output);
      
      out.writeInt(0);
      out.writeInt(command.getTokens().size());
      for (String token : command.getTokens())
        writeString(token);
    }
  }
  
}

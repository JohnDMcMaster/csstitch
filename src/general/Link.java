package general;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Link implements Runnable {

  private static final int DEFAULT_BUFFER_SIZE = 16 * 1024;

  public static void run(InputStream in, OutputStream out, int bufferSize) throws IOException {
    byte[] buffer = new byte[bufferSize];

    for (;;) {
      int length = in.read(buffer);
      if (length == -1)
        break;

      synchronized (out) {
        out.write(buffer, 0, length);
        out.flush();
      }
    }
  }

  public static void run(InputStream in, OutputStream out) throws IOException {
    run(in, out, DEFAULT_BUFFER_SIZE);
  }

  private InputStream in;
  private OutputStream out;
  private boolean close;
  private int bufferSize;

  public Link(InputStream in, OutputStream out) {
    this(in, out, false);
  }

  public Link(InputStream in, OutputStream out, boolean close) {
    this(in, out, close, DEFAULT_BUFFER_SIZE);
  }

  public Link(InputStream in, OutputStream out, boolean close, int bufferSize) {
    this.in = in;
    this.out = out;
    this.close = close;
    this.bufferSize = bufferSize;
  }

  public void run() {
    try {
      run(in, out, bufferSize);

      if (close)
        out.close();
    } catch (IOException e) {
      System.err.println(e);
    }
  }

  public static Thread link(InputStream in, OutputStream out, boolean close, int bufferSize) {
    if (in == null || out == null)
      return null;

    Thread thread = new Thread(new Link(in, out, close, bufferSize));
    thread.start();
    return thread;
  }
  
  public static Thread link(InputStream in, OutputStream out, boolean close) {
    return link(in, out, close, DEFAULT_BUFFER_SIZE);
  }

  public static Thread link(InputStream in, OutputStream out) {
    return link(in, out, false);
  }

}

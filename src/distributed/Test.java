package distributed;

import java.io.IOException;

public class Test {

  public static String test(int a, double b) {
    System.err.println("a = " + a);
    System.err.println("b = " + b);
    return "asd";
  }

  public static void main(String[] args) throws IOException {
    //Object result = Tunnel.callRemotely(new Server[] {Servers.CIP_90}, new String[] {"-Xmx2G"},
    //    Tunnel.getMethod("test"), 2, 3.0);
    Bootstrap.bootstrap(args);
    System.err.println("asd");
  }

}

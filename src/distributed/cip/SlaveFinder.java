package distributed.cip;

import general.execution.SSHAddress;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.TreeSet;

import distributed.execution.Tools;
import distributed.server.Server;
import distributed.tunnel.Tunnel;

public class SlaveFinder {

  public static TreeMap<String, Long> findSlavesHelper(boolean bu135, boolean bu136, boolean test)
      throws IOException {
    TreeSet<String> candidates = new TreeSet<String>();

    if (bu135)
      candidates.addAll(Arrays.asList(Main.CIP_BU_135));

    if (bu136)
      candidates.addAll(Arrays.asList(Main.CIP_BU_136));

    if (test)
      candidates.addAll(Arrays.asList(Main.CIP_TEST));

    TreeMap<String, Long> result = new TreeMap<String, Long>();
    for (String candidate : candidates) {
      long mem = Tools.getFreeSpace(new SSHAddress(candidate, "mdd63bj"), "/data");
      if (mem != -1)
        result.put(candidate, mem);
    }

    return result;
  }

  public static String[] findSlaves(long minSpace, boolean bu135, boolean bu136, boolean test)
      throws IOException {
    TreeMap<String, Long> slaves = findSlavesHelper(bu135, bu136, test);

    ArrayList<String> good = new ArrayList<String>();
    for (String host : slaves.keySet())
      if (slaves.get(host) >= minSpace)
        good.add(host);

    return good.toArray(new String[] {});
  }

  public static String[] findSlavesRemotely(long minSpace, boolean bu135, boolean bu136,
      boolean test) throws IOException, InvocationTargetException {
    return (String[]) Tunnel.callRemotely(new Server[] {Main.GATEWAY}, null,
        Tunnel.getMethod("findSlaves"), minSpace, bu135, bu136, test);
  }

  public static String[] findSlavesRemotely(boolean bu135, boolean bu136, boolean test)
      throws IOException, InvocationTargetException {
    return findSlavesRemotely(50 * 1024 * 1024 * 1024, bu135, bu136, test);
  }

  public static String[] findSlavesRemotely() throws IOException, InvocationTargetException {
    return findSlavesRemotely(true, true, true);
  }

}

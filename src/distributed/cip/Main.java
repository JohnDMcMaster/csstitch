package distributed.cip;

import java.util.ArrayList;
import java.util.Arrays;

import distributed.server.Server;
import distributed.server.Servers;

public class Main {

  public static final String[] CIP_BU_135 = {"cip11", "cip12", "cip13", "cip14", "cip15", "cip16",
      "cip17", "cip18", "cip19", "cip20", "cip21", "cip22", "cip23", "cip24", "cip25", "cip26",
      "cip27", "cip28", "cip29", "cip30", "cip31", "cip32", "cip33", "cip34", "cip35", "cip36",
      "cip37"};

  public static final String[] CIP_BU_136 = {"cip38", "cip39", "cip40", "cip41", "cip42", "cip43",
      "cip44", "cip45", "cip46", "cip47", "cip48", "cip49", "cip50", "cip51", "cip52", "cip53",
      "cip54", "cip55", "cip56", "cip57", "cip58", "cip59", "cip60", "cip61", "cip62", "cip63",
      "cip64", "cip65", "cip66", "cip67", "cip68", "cip69", "cip70", "cip71", "cip72", "cip73",
      "cip74", "cip75", "cip76"};

  public static final String[] CIP_BU_137 = {"cip77"};

  public static final String[] CIP_TEST = {"cip78", "cip80"};

  public static final String[] CIP_SERVER = {"cip90", "cip91"};

  public static final Server GATEWAY = Servers.CIP_90;

  public static String[] getSlaves(boolean bu135, boolean bu136, boolean bu137, boolean test) {
    ArrayList<String> slaves = new ArrayList<String>();
    if (bu135)
      slaves.addAll(Arrays.asList(CIP_BU_135));
    if (bu136)
      slaves.addAll(Arrays.asList(CIP_BU_136));
    if (bu137)
      slaves.addAll(Arrays.asList(CIP_BU_137));
    if (test)
      slaves.addAll(Arrays.asList(CIP_TEST));
    return slaves.toArray(new String[] {});
  }
  
}

package general;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;

public class Encodings {
  
  public static int align(int a, int m) {
    return ((a % m) + m) % m;
  }
  
  public static int getValue(byte b) {
    return align(b, 0x0100);
  }
  
  public static String encodeHex(byte[] array, int numSpaces) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i != numSpaces; ++i)
      builder.append(' ');
    String spaces = builder.toString();
    
    builder = new StringBuilder();
    for (byte entry : array) {
      builder.append(String.format("%02X", entry));
      builder.append(spaces);
    }
    return builder.toString();
  }
  
  public static String encodeHex(byte[] c) {
    return encodeHex(c, 0);
  }
  
  public static String encodeHex(int[] array) {
    StringBuilder b = new StringBuilder();
    for (int entry : array)
      b.append(String.format("%02X ", entry));
    return b.toString();
  }
  
  public static String encodeHex(char[] array) {
    StringBuilder builder = new StringBuilder();
    for (char entry : array)
      builder.append(String.format("%02X ", entry));
    return builder.toString();
  }
  
  public static byte[] decodeNumbers(String source, int numSpaces, int base) {
    byte[] c = new byte[(source.length() + 1) / (2 + numSpaces)];
    for (int i = 0; i != (source.length() + 1) / (2 + numSpaces); ++i)
      c[i] =
          (byte) Integer.parseInt(source.substring((2 + numSpaces) * i, (2 + numSpaces) * i + 2),
              base);
    return c;
  }
  
  public static byte[] decodeHex(String source, int numSpaces) {
    return decodeNumbers(source, numSpaces, 16);
  }
  
  public static byte[] decodeHex(String source) {
    return decodeHex(source, 0);
  }
  
  public static String reverse(String source) {
    char[] array = source.toCharArray();
    for (int i = 0; i != array.length / 2; ++i) {
      char t = array[i];
      array[i] = array[array.length - 1 - i];
      array[array.length - 1 - i] = t;
    }
    return new String(array);
  }
  
  public static int getBase64CharValue(char c) {
    if (c >= 'A' && c <= 'Z')
      return c - 'A';
    if (c >= 'a' && c <= 'z')
      return 26 + c - 'a';
    if (c >= '0' && c <= '9')
      return 52 + c - '0';
    if (c == '+' || c == '%' || c == '-')
      return 62;
    return 63;
  }
  
  public static char getBase64Char(int v) {
    if (v < 26)
      return (char) ('A' + v);
    if (v < 52)
      return (char) ('a' + v - 26);
    if (v < 62)
      return (char) ('0' + v - 52);
    if (v == 62)
      return '+';
    return '/';
  }
  
  public static String encodeBase64(byte[] b) {
    char[] c = new char[4 * ((b.length + 2) / 3)];
    for (int i = 0; i != (b.length + 2) / 3; ++i) {
      int v = 0;
      int p = 1;
      for (int j = 3; j != 0;) {
        --j;
        if (3 * i + j < b.length)
          v += p * getValue(b[3 * i + j]);
        p *= 0x0100;
      }
      for (int j = 0; j != 4; ++j) {
        p /= 64;
        int temp = v / p;
        c[4 * i + j] = getBase64Char(temp);
        v -= p * temp;
      }
    }
    if (b.length % 3 != 0)
      c[c.length - 1] = '=';
    if (b.length % 3 == 1)
      c[c.length - 2] = '=';
    return new String(c);
  }
  
  public static byte[] decodeBase64(String s) {
    byte[] r;
    if (s.endsWith("=="))
      r = new byte[3 * s.length() / 4 - 2];
    else if (s.endsWith("="))
      r = new byte[3 * s.length() / 4 - 1];
    else
      r = new byte[3 * s.length() / 4];
    for (int i = 0; i != s.length() / 4; ++i) {
      int v = 0;
      int p = 1;
      for (int j = 4; j != 0;) {
        --j;
        v += p * getBase64CharValue(s.charAt(4 * i + j));
        p *= 64;
      }
      for (int j = 0; j != 3; ++j)
        if (3 * i + j < r.length) {
          p /= 256;
          r[3 * i + j] = (byte) (v / p);
          v -= p * r[3 * i + j];
        }
    }
    return r;
  }
  
  public static String fromUTF8(byte[] buffer) {
    try {
      return new String(buffer, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static byte[] toUTF8(String string) {
    try {
      return string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static String encodeParams(Map<String, String> map) {
    StringBuilder builder = new StringBuilder();
    for (String key : map.keySet()) {
      if (builder.length() != 0)
        builder.append('&');
      
      builder.append(Http.encodeUrl(key) + "=" + Http.encodeUrl(map.get(key)));
    }
    
    return builder.toString();
  }
  
  public static TreeMap<String, String> decodeParams(String string) {
    TreeMap<String, String> params = new TreeMap<String, String>();
    for (String param : string.split("&")) {
      int index = param.indexOf("=");
      if (index != -1)
        params.put(Http.decodeUrl(param.substring(0, index)),
            Http.decodeUrl(param.substring(index + 1)));
    }
    
    return params;
  }
  
  public static String escapeQuotes(String string) {
    string = string.replaceAll("'", "\\'");
    string = string.replaceAll("\"", "\\\"");
    return string;
  }
  
  public static String unescapeQuotes(String string) {
    string = string.replaceAll("\\\\\"", "\"");
    string = string.replaceAll("\\\\'", "'");
    return string;
  }
  
}

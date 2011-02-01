package general;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Http {
  
  public static String cookieToString(Map<String, String> cookie) {
    StringBuilder b = new StringBuilder();
    for (Iterator<String> i = cookie.keySet().iterator(); i.hasNext();) {
      String key = i.next();
      b.append(key);
      b.append("=");
      b.append(cookie.get(key));
      b.append(";");
    }
    return b.toString();
  }
  
  public static TreeMap<String, String> parseCookie(String c) {
    TreeMap<String, String> map = new TreeMap<String, String>();
    while (c.length() != 0) {
      int i = c.indexOf('=');
      if (i == -1)
        break;
      String key = c.substring(0, i);
      
      c = c.substring(i + 1);
      boolean quoted = c.charAt(0) == '"';
      i = c.indexOf(quoted ? '"' : ';');
      if (i == -1)
        break;
      // throw new RuntimeException("Cookie parsing error");
      
      map.put(key, c.substring(quoted ? 1 : 0, i));
      c = c.substring(quoted ? i + 2 : i + 1);
      
    }
    return map;
  }
  
  public static String encodeUrl(String s) {
    try {
      return java.net.URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  public static String decodeUrl(String s) {
    try {
      return java.net.URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  public static HttpURLConnection commonUrlInit(String address, Map<String, String> cookie,
      boolean redirect) throws IOException {
    HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
    c.setRequestProperty("Cookie", cookie == null ? "" : cookieToString(cookie));
    c.setInstanceFollowRedirects(redirect);
    return c;
  }
  
  public static String commonUrl(HttpURLConnection c, Map<String, String> cookie,
      String[] setCookie, byte[][] content) throws IOException {
    List<String> list = c.getHeaderFields().get("Set-Cookie");
    StringBuilder builder = new StringBuilder();
    if (list != null)
      for (String entry : list)
        builder.append(entry);
    String temp = builder.toString();
    if (setCookie != null)
      setCookie[0] = temp;
    else if (cookie != null)
      cookie.putAll(parseCookie(temp));
    
    if (content != null) {
      content[0] = Streams.readIntoBytes(c.getInputStream());
      return null;
    }
    
    return Streams.readIntoString(c.getInputStream());
  }
  
  public static String getUrl(String address) throws IOException {
    return getUrl(address, null);
  }
  
  public static String getUrl(String address, Map<String, String> cookie) throws IOException {
    return getUrl(address, cookie, null);
  }
  
  public static String getUrl(String address, Map<String, String> cookie, String[] setCookie)
      throws IOException {
    return getUrl(address, cookie, setCookie, true);
  }
  
  public static String getUrl(String address, Map<String, String> cookie, String[] setCookie,
      boolean redirect) throws IOException {
    return getUrl(address, cookie, setCookie, redirect, null);
  }
  
  public static String getUrl(String address, Map<String, String> cookie, String[] setCookie,
      boolean redirect, byte[][] content) throws IOException {
    HttpURLConnection c = commonUrlInit(address, cookie, redirect);
    return commonUrl(c, cookie, setCookie, content);
  }
  
  public static String postUrl(String address) throws IOException {
    return postUrl(address, null);
  }
  
  public static String postUrl(String address, Map<String, String> params) throws IOException {
    return postUrl(address, params, null);
  }
  
  public static String postUrl(String address, Map<String, String> params,
      Map<String, String> cookie) throws IOException {
    return postUrl(address, params, cookie, null);
  }
  
  public static String postUrl(String address, Map<String, String> params,
      Map<String, String> cookie, String[] setCookie) throws IOException {
    return postUrl(address, params, cookie, setCookie, true);
  }
  
  public static String postUrl(String address, Map<String, String> params,
      Map<String, String> cookie, String[] setCookie, boolean redirect) throws IOException {
    return postUrl(address, params, cookie, setCookie, redirect, null);
  }
  
  public static String postUrl(String address, Map<String, String> params,
      Map<String, String> cookie, String[] setCookie, boolean redirect, byte[][] content)
      throws IOException {
    HttpURLConnection c = commonUrlInit(address, cookie, redirect);
    c.setRequestMethod("POST");
    c.setDoOutput(true);
    c.connect();
    PrintStream out = new PrintStream(c.getOutputStream());
    for (Iterator<String> i = params.keySet().iterator(); i.hasNext();) {
      String key = i.next();
      out.print(encodeUrl(key) + "=" + encodeUrl(params.get(key)) + (i.hasNext() ? "&" : ""));
    }
    out.flush();
    out.close();
    return commonUrl(c, cookie, setCookie, content);
  }
  
  public static String
      postCookieManually(String address, Map<String, String> params, String cookie)
          throws IOException {
    HttpURLConnection c = commonUrlInit(address, null, true);
    c.setRequestProperty("Cookie", cookie);
    c.setRequestMethod("POST");
    c.setDoOutput(true);
    c.connect();
    PrintStream out = new PrintStream(c.getOutputStream());
    for (Iterator<String> i = params.keySet().iterator(); i.hasNext();) {
      String key = i.next();
      out.print(encodeUrl(key) + "=" + encodeUrl(params.get(key)) + (i.hasNext() ? "&" : ""));
    }
    out.flush();
    out.close();
    return commonUrl(c, null, null, null);
  }
  
}

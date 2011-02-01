package hm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import test.Tagger;

public class PrintNames {
  
  public static void main(String[] args) throws IOException {
    for (TreeMap<String, String> tags : Tagger.load("/home/noname/di/tags.txt"))
      System.out.println(tags.get("name"));
  }
  
}

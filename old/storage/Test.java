package old.storage;

import java.io.IOException;

public class Test {

  public static void main(String[] args) throws IOException {
    Accessor accessor = new Accessor("/media/book/decapsulation/s.dat");
    
    accessor.selectRectangle(new Accessor.SimpleHandler() {
      public void handle(float x, float y, float val, int flags) {
        System.out.println(x + ", " + y);
      }
    }, 3998, 4000, 4000, 4002);
  }

}

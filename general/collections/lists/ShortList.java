package general.collections.lists;

import java.util.AbstractList;

public class ShortList extends AbstractList<Short> {
  
  private short[] array;
  
  public ShortList(short[] array) {
    this.array = array;
  }
  
  public int size() {
    return array.length;
  }
  
  public Short get(int index) {
    return array[index];
  }
  
  public Short set(int index, Short element) {
    short previous = array[index];
    array[index] = element;
    return previous;
  }
  
}

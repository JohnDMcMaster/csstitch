package general.collections.lists;

import java.util.AbstractList;

public class IntegerList extends AbstractList<Integer> {
  
  private int[] array;
  
  public IntegerList(int[] array) {
    this.array = array;
  }
  
  public int size() {
    return array.length;
  }
  
  public Integer get(int index) {
    return array[index];
  }
  
  public Integer set(int index, Integer element) {
    int previous = array[index];
    array[index] = element;
    return previous;
  }
  
}

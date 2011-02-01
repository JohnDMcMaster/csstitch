package general.collections.lists;

import java.util.AbstractList;

public class BooleanList extends AbstractList<Boolean> {
  
  private boolean[] array;
  
  public BooleanList(boolean[] array) {
    this.array = array;
  }
  
  public int size() {
    return array.length;
  }
  
  public Boolean get(int index) {
    return array[index];
  }
  
  public Boolean set(int index, Boolean element) {
    boolean previous = array[index];
    array[index] = element;
    return previous;
  }
  
}

package general.collections.lists;

import java.util.AbstractList;

public class CharacterList extends AbstractList<Character> {
  
  private char[] array;
  
  public CharacterList(char[] array) {
    this.array = array;
  }
  
  public int size() {
    return array.length;
  }
  
  public Character get(int index) {
    return array[index];
  }
  
  public Character set(int index, Character element) {
    char previous = array[index];
    array[index] = element;
    return previous;
  }
  
}

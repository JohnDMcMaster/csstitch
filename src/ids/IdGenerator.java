package ids;

import general.collections.CollectionComparator;

import java.io.Serializable;
import java.util.Iterator;
import java.util.TreeSet;

public class IdGenerator implements Serializable, Comparable<IdGenerator>, Cloneable {
  
  private static final long serialVersionUID = 1L;
  
  private TreeSet<Long> set;
  
  public IdGenerator() {
    this(new TreeSet<Long>());
  }
  
  private IdGenerator(TreeSet<Long> set) {
    this.set = set;
  }
  
  private void flip(long x) {
    if (set.contains(x))
      set.remove(x);
    else
      set.add(x);
  }
  
  public long generate() {
    Iterator<Long> i = set.iterator();
    if (!i.hasNext()) {
      set.add(0l);
      set.add(1l);
      return 0;
    }
    
    long a = i.next(), id;
    if (a == 0) {
      id = i.next();
      if (id < 0)
        throw new RuntimeException("no free ids");
      
      i.remove();
      a = i.next();
    } else
      id = 0;
    
    long idPlusOne = id + 1;
    if (a == idPlusOne)
      i.remove();
    else
      set.add(idPlusOne);
    
    if (a != 0)
      set.add(0l);
    
    return id;
  }
  
  public void release(long id) {
    flip(id);
    flip(id + 1);
  }
  
  public int compareTo(IdGenerator generator) {
    CollectionComparator<Long> comparator = CollectionComparator.get();
    return comparator.compare(set, generator.set);
  }
  
  @SuppressWarnings("unchecked")
  protected IdGenerator clone() throws CloneNotSupportedException {
    return new IdGenerator((TreeSet<Long>) set.clone());
  }
  
}

package edges;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import general.collections.Pair;
import ids.TaggedObjects;

public class Manager implements Externalizable {
  
  private static class EdgeInfo {
    Edge edge;
    boolean virtual;
    
    Corner[] corners = new Corner[2];
    long[] next = new long[] {-1, -1};
    int[] nextEnds = new int[2];
  }
  
  private TaggedObjects<Edge> edges;
  private TreeMap<Long, EdgeInfo> edgeInfos;
  
  private int sx, sy;
  private long[][][] edgeArray;
  private TreeSet<Pair<Pair<Long, Integer>, Pair<Long, Integer>>> matches;
  private TreeSet<Corner>[][] freeCorners;
  
  public Manager(int sx, int sy) {
    this.sx = sx;
    this.sy = sy;
    
    initArrays();
    
    edges = new TaggedObjects<Edge>();
    edgeInfos = new TreeMap<Long, Manager.EdgeInfo>();
    matches = new TreeSet<Pair<Pair<Long,Integer>,Pair<Long,Integer>>>();
  }
  
  @SuppressWarnings("unchecked")
  private void initArrays() {
    edgeArray = new long[2][sy][sx];
    for (int i = 0; i != 2; ++i)
      for (int y = 0; y != sy; ++y)
        for (int x = 0; x != sx; ++x)
          edgeArray[i][y][x] = -1;
    
    freeCorners = new TreeSet[sy / Corner.CORNER_SCALE + 1][sx / Corner.CORNER_SCALE + 1];
    for (int y = 0; y != freeCorners.length; ++y)
      for (int x = 0; x != freeCorners[0].length; ++x)
        freeCorners[y][x] = new TreeSet<Corner>();
  }
  
  private void addFreeCorner(Corner corner) {
    freeCorners[corner.getYY()][corner.getXX()].add(corner);
  }
  
  private void removeFreeCorner(Corner corner) {
    freeCorners[corner.getYY()][corner.getXX()].remove(corner);
  }
  
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(sx);
    out.writeInt(sy);
    
    out.writeObject(edges);
    for (Entry<Long, EdgeInfo> entry : edgeInfos.entrySet())
      out.writeBoolean(entry.getValue().virtual);
    
    out.writeObject(matches);
  }
  
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    sx = in.readInt();
    sy = in.readInt();
    initArrays();
    
    edges = (TaggedObjects<Edge>) in.readObject();
    
    edgeInfos = new TreeMap<Long, EdgeInfo>();
    for (Entry<Long, Edge> entry : edges.getMap().entrySet()) {
      long id = entry.getKey();
      Edge edge = entry.getValue();
      
      EdgeInfo info = new EdgeInfo();
      info.edge = edge;
      info.virtual = in.readBoolean();
      info.corners[0] = new Corner(id, edge, 0);
      info.corners[1] = new Corner(id, edge, 1);
      edgeInfos.put(id, info);
      
      int dir = edge.getDir();
      if (!info.virtual)
        for (int i : edge.getIndices())
          for (int j : edge.getSegments(i))
            if (dir == 0)
              edgeArray[0][i][j] = id;
            else
              edgeArray[1][j][i] = id;
    }
    
    matches = (TreeSet<Pair<Pair<Long, Integer>, Pair<Long, Integer>>>) in.readObject();
    for (Pair<Pair<Long, Integer>, Pair<Long, Integer>> match : matches) {
      Pair<Long, Integer> a = match.getA();
      long aId = a.getA();
      int aEnd = a.getB();
      
      Pair<Long, Integer> b = match.getB();
      long bId = b.getA();
      int bEnd = b.getB();
      
      EdgeInfo aInfo = edgeInfos.get(aId);
      aInfo.next[aEnd] = bId;
      aInfo.nextEnds[aEnd] = bEnd;
      
      EdgeInfo bInfo = edgeInfos.get(bId);
      bInfo.next[bEnd] = aId;
      bInfo.nextEnds[bEnd] = aEnd;
    }
    
    for (Entry<Long, EdgeInfo> entry : edgeInfos.entrySet()) {
      EdgeInfo info = entry.getValue();
      for (int i = 0; i != 2; ++i)
        if (info.next[i] == -1)
          addFreeCorner(info.corners[i]);
    }
  }
  
}

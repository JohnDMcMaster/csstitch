package realmetal;

import general.collections.Pair;

import ids.TaggedObjects;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.TreeSet;

import realmetal.Edge.EdgeException;
import tools.PointChooser;

public class EdgeSegmenter {
  
  private TaggedObjects<Edge> edges;
  
  private int sx, sy;
  private long[][][] edgeArray;
  
  private TreeSet<Corner>[][] corners;
  private TreeSet<Corner> cornerUpdates;
  
  @SuppressWarnings("unchecked")
  private EdgeSegmenter(int image, int minLength) throws IOException {
    rendered = EdgeTracer.plotImage(0, image);
    
    sx = 3250;
    sy = 2450;
    
    edgeArray = new long[2][sy][sx];
    for (int i = 0; i != 2; ++i)
      for (int y = 0; y != sy; ++y)
        for (int x = 0; x != sx; ++x)
          edgeArray[i][y][x] = -1;
    
    corners = new TreeSet[sy / Corner.CORNER_SCALE + 1][sx / Corner.CORNER_SCALE + 1];
    for (int y = 0; y != corners.length; ++y)
      for (int x = 0; x != corners[0].length; ++x)
        corners[y][x] = new TreeSet<Corner>();
    
    cornerUpdates = new TreeSet<Corner>();
    
    Pair<Double, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>[] edgeParts =
        EdgeTracer.organizeComponents(image, 10);
  }
  
  private void addCorner(Corner corner) {
    corners[corner.getYY()][corner.getXX()].add(corner);
    cornerUpdates.add(corner);
  }
  
  private void removeCorner(Corner corner) {
    corners[corner.getYY()][corner.getXX()].remove(corner);
  }
  
  private void addCorners(long edgeId, Edge edge) {
    for (int i = 0; i != 2; ++i) {
      if (edge.getNext(i) == -1)
        addCorner(new Corner(edgeId, edge, i));
    }
  }
  
  private void removeCorners(long edgeId, Edge edge) {
    for (int i = 0; i != 2; ++i)
      if (edge.getNext(i) == -1)
        removeCorner(new Corner(edgeId, edge, i));
  }
  
  private void checkCornerUpdates() {
    for (Corner c : cornerUpdates) {
      Edge edgeC = edges.get(c.getEdge());
      TreeSet<Corner> matches = new TreeSet<Corner>();
      
      for (int yy = c.getYY() - 1; yy <= c.getYY() + 1; ++yy)
        for (int xx = c.getXX() - 1; xx <= c.getXX() + 1; ++xx)
          if (xx >= 0 && yy >= 0 && xx < corners[0].length && yy < corners.length)
            for (Corner d : corners[yy][xx]) {
              Edge edgeD = edges.get(d.getEdge());
              if (edgeC.getDir() != edgeD.getDir())
                if (Edge.match(edgeC, c.getEnd(), edgeD, d.getEnd()))
                  matches.add(d);
            }
      
      if (!matches.isEmpty()) {
        if (matches.size() != 1)
          resolveMultipleCornerMatches(matches);
        
        Corner d = matches.first();
        Edge edgeD = edges.get(d.getEdge());
        Edge.link(c.getEdge(), edgeC, c.getEnd(), d.getEdge(), edgeD, d.getEnd());
        
        removeCorner(c);
        removeCorner(d);
      }
    }
    
    cornerUpdates.clear();
  }
  
  private void addSegment(int dir, int index, int from, int to) throws EdgeException {
    Edge edge = new Edge(edges, dir);
    edge.addSegment(index, from, to);
    
    TreeSet<Long> oldEdges = new TreeSet<Long>();
    for (int a = index - 1; a <= index + 1; ++a)
      for (int b = from - 1; b <= to + 1; ++b)
        if ((a + b) == 1 && a >= 0 && a < (dir == 0 ? sy : sx) && b >= 0
            && b < (dir == 0 ? sx : sy)) {
          long oldEdge = dir == 0 ? edgeArray[0][a][b] : edgeArray[0][b][a];
          if (oldEdge != -1)
            oldEdges.add(oldEdge);
        }
    
    for (long oldEdge : oldEdges)
      edge.addEdge(edges.get(oldEdge));
    edge.computeInformation();
    
    for (int i : edge.getIndices())
      for (int j : edge.getSegments(index))
        if (dir == 0)
          edgeArray[0][i][j] = edge.getId();
        else
          edgeArray[1][j][i] = edge.getId();
    
    for (long oldEdge : oldEdges)
      removeCorners(oldEdge, edges.get(oldEdge));
    addCorners(edge.getId(), edge);
    
    checkCornerUpdates();
  }
  
  private void resolveMultipleCornerMatches(TreeSet<Corner> matches) {
    
  }
  
  private BufferedImage rendered;
  private PointChooser chooser;
  
}

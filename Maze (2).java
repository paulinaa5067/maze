import java.util.*;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// represents a vertex in the maze
class Vertex {

  Posn position;

  Vertex top;
  Vertex bottom;
  Vertex left;
  Vertex right;

  Vertex(Posn position, Vertex top, Vertex bottom, Vertex left, Vertex right) {
    this.position = position;
    this.top = top;
    this.bottom = bottom;
    this.left = left;
    this.right = right;
  }

  Vertex(Posn position) {
    this.position = position;
    this.top = null;
    this.bottom = null;
    this.left = null;
    this.right = null;
  }

  // creates connections within the board
  void createConnections(Vertex top, Vertex bottom, Vertex left, Vertex right) {
    if (this.top == null) {
      this.top = top;
    }
    if (this.bottom == null) {
      this.bottom = bottom;
    }
    if (this.left == null) {
      this.left = left;
    }
    if (this.right == null) {
      this.right = right;
    }
  }
}

// represents an edge in the maze
class Edge {
  int weight;
  Vertex from;
  Vertex to;

  Edge(int weight, Vertex from, Vertex to) {
    this.to = to;
    this.from = from;
    this.weight = weight;
  }

  Edge(Vertex from, Vertex to) {
    this.to = to;
    this.from = from;
    this.weight = (int) (Math.random() * 60);
  }
}

// represents a graph in the maze
class Graph {
  ArrayList<ArrayList<Vertex>> listOfVertices;

  Graph(ArrayList<ArrayList<Vertex>> listOfVertices) {
    this.listOfVertices = listOfVertices;
  }

  // method that produces a list of edges sorted by their weight
  ArrayList<Edge> produceEdge() {
    ArrayList<Edge> listOfEdges = new ArrayList<Edge>();

    for (int rows = 0; rows < this.listOfVertices.size(); rows++) {
      for (int cols = 0; cols < this.listOfVertices.get(0).size() - 1; cols++) {
        listOfEdges.add(
            new Edge(listOfVertices.get(rows).get(cols), listOfVertices.get(rows).get(cols + 1)));
      }
    }
    for (int rows = 0; rows < this.listOfVertices.size() - 1; rows++) {
      for (int cols = 0; cols < this.listOfVertices.get(0).size(); cols++) {
        listOfEdges.add(
            new Edge(listOfVertices.get(rows).get(cols), listOfVertices.get(rows + 1).get(cols)));
      }
    }

    // this will sort the listOfEdges by edge weight
    Collections.sort(listOfEdges, new ByEWComp());
    return listOfEdges;
  }

  // this method constructs the spanning tree of the graph
  ArrayList<Edge> spanning() {
    HashMap<Posn, Posn> representatives = new HashMap<Posn, Posn>();
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    ArrayList<Edge> worklist = produceEdge(); // (sorted by edge weight)

    for (ArrayList<Vertex> singleRow : this.listOfVertices) {
      for (Vertex vertex : singleRow) {
        representatives.put(vertex.position, vertex.position);
      }
    }
    while (edgesInTree.size() < this.listOfVertices.size() * this.listOfVertices.get(0).size()
        - 1) {
      Edge cheapest = worklist.get(0);
      if (find(representatives, cheapest.from.position)
          .equals(find(representatives, cheapest.to.position))) {
        worklist.remove(0);
      }
      else {
        updateConnections(worklist.get(0));
        edgesInTree.add(worklist.get(0));
        representatives = union(representatives, find(representatives, cheapest.from.position),
            find(representatives, cheapest.to.position));
      }
    }
    return edgesInTree;
  }

  // this method updates the connections on the graph
  void updateConnections(Edge edge) {
    for (int rows = 0; rows < this.listOfVertices.size(); rows++) {
      for (int cols = 0; cols < this.listOfVertices.get(0).size() - 1; cols++) {
        if (edge.to.equals(this.listOfVertices.get(rows).get(cols + 1))
            && edge.from.equals(this.listOfVertices.get(rows).get(cols))) {
          this.listOfVertices.get(rows).get(cols).right = this.listOfVertices.get(rows)
              .get(cols + 1);
          this.listOfVertices.get(rows).get(cols + 1).left = this.listOfVertices.get(rows)
              .get(cols);
        }
      }
    }

    for (int rows = 0; rows < this.listOfVertices.size() - 1; rows++) {
      for (int cols = 0; cols < this.listOfVertices.get(0).size(); cols++) {
        if (edge.to.equals(this.listOfVertices.get(rows + 1).get(cols))
            && edge.from.equals(this.listOfVertices.get(rows).get(cols))) {
          this.listOfVertices.get(rows).get(cols).bottom = this.listOfVertices.get(rows + 1)
              .get(cols);
          this.listOfVertices.get(rows + 1).get(cols).top = this.listOfVertices.get(rows).get(cols);
        }
      }
    }

  }

  // finds vertex in representatives
  Posn find(HashMap<Posn, Posn> representatives, Posn position) {
    if (representatives.get(position).equals(position)) {
      return position;
    }
    else {
      return find(representatives, representatives.get(position));
    }
  }

  // sets child to parent
  HashMap<Posn, Posn> union(HashMap<Posn, Posn> representatives, Posn child, Posn parent) {
    representatives.put(child, find(representatives, parent));
    return representatives;
  }
}

// this class is a comparator that orders by edge weight
class ByEWComp implements Comparator<Edge> {
  public int compare(Edge edge1, Edge edge2) {
    if (edge2.weight > edge1.weight) {
      return -1;
    }
    else if (edge1.weight == edge2.weight) {
      return 0;
    }
    else {
      return 1;
    }
  }
}

// this class is a comparator that orders sequentially
class ByNumComp implements Comparator<Integer> {
  public int compare(Integer num1, Integer num2) {
    if (num1 < num2) {
      return -1;
    }
    else {
      return 1;
    }
  }
}

// represents the maze world
class MazeWorld extends World {

  Graph graph;
  ArrayList<Edge> spanningTree;
  ArrayList<Vertex> searchFinished;
  ArrayList<Vertex> touchedTiles;

  static final int WP_BOARD = 1000;
  static final int HP_BOARD = (int) (.5 * WP_BOARD);
  static final int BOARD_WIDTH = 50;
  static final int BOARD_HEIGHT = (int) (.5 * BOARD_WIDTH);

  static final int TILE_SIZE = WP_BOARD / BOARD_WIDTH;
  static final int X_CENTER = BOARD_WIDTH * TILE_SIZE / 2;
  static final int Y_CENTER = BOARD_HEIGHT * TILE_SIZE / 2;

  MazeWorld() {
    this.graph = new Graph(createVertices());
    this.spanningTree = graph.spanning();
    this.searchFinished = new ArrayList<Vertex>();
    this.touchedTiles = new ArrayList<Vertex>();

  }

  // represents the key events
  public void onKeyEvent(String key) {

    // triggers depth first search
    if (key.equals("d")) {
      touchedTiles = new ArrayList<Vertex>();
      this.searchFinished = depthfirst();

    }
    // triggers breadth first search
    else if (key.equals("b")) {
      touchedTiles = new ArrayList<Vertex>();
      this.searchFinished = breadthfirst();

    }

    // resets maze
    if (key.equals("r")) {
      this.graph = new Graph(createVertices());
      this.spanningTree = graph.spanning();
      this.searchFinished = new ArrayList<Vertex>();
      this.touchedTiles = new ArrayList<Vertex>();
     
    }
  }

  // displays world on screen
  public WorldScene makeScene() {
    int shift = TILE_SIZE / 2;

    WorldScene scene = new WorldScene(WP_BOARD, HP_BOARD);
    scene.placeImageXY(new RectangleImage(WP_BOARD + TILE_SIZE * 2, HP_BOARD + TILE_SIZE * 2,
        "solid", Color.LIGHT_GRAY), X_CENTER, Y_CENTER);

    // draws the tiles that were touched when solving the maze
    for (Vertex vertex : this.touchedTiles) {
      scene.placeImageXY(
          new RectangleImage(TILE_SIZE, TILE_SIZE, OutlineMode.SOLID, new Color(139, 0, 139)),
          vertex.position.x * TILE_SIZE + shift, vertex.position.y * TILE_SIZE + shift);
    }

    // responsible for drawing the finished search
    for (Vertex vertex : this.searchFinished) {
      if (vertex == null) {
        throw new RuntimeException("the vertex is null");
      }
      scene.placeImageXY(
          new RectangleImage(TILE_SIZE, TILE_SIZE, OutlineMode.SOLID, new Color(186, 85, 211)),
          vertex.position.x * TILE_SIZE + shift, vertex.position.y * TILE_SIZE + shift);
    }

    // this draws the bottom right and top left corners
    scene.placeImageXY(
        new RectangleImage(TILE_SIZE, TILE_SIZE, OutlineMode.SOLID, new Color(147, 112, 219)),
        WP_BOARD - shift, HP_BOARD - shift);

    scene.placeImageXY(
        new RectangleImage(TILE_SIZE, TILE_SIZE, OutlineMode.SOLID, new Color(102, 205, 170)),
        shift, shift);

  
    // draws the walls in the maze
    for (ArrayList<Vertex> arrary : graph.listOfVertices) {
      for (Vertex vertex : arrary) {

        if (vertex.top == null) {
          scene.placeImageXY(
              new RectangleImage(MazeWorld.TILE_SIZE, 2, OutlineMode.SOLID, Color.black),
              vertex.position.x * TILE_SIZE + shift, vertex.position.y * TILE_SIZE);
        }
        if (vertex.bottom == null) {
          scene.placeImageXY(
              new RectangleImage(MazeWorld.TILE_SIZE, 2, OutlineMode.SOLID, Color.black),
              vertex.position.x * TILE_SIZE + shift, vertex.position.y * TILE_SIZE + shift + shift);
        }
        if (vertex.left == null) {
          scene.placeImageXY(
              new RectangleImage(2, MazeWorld.TILE_SIZE, OutlineMode.SOLID, Color.black),
              vertex.position.x * TILE_SIZE, vertex.position.y * TILE_SIZE + shift);
        }
        if (vertex.right == null) {
          scene.placeImageXY(
              new RectangleImage(2, MazeWorld.TILE_SIZE, OutlineMode.SOLID, Color.black),
              vertex.position.x * TILE_SIZE + shift + shift, vertex.position.y * TILE_SIZE + shift);
        }

      }

    }
    scene.placeImageXY(new TextImage("Press 'r' to reset the board!", 15, new Color(240, 255, 240)),
        X_CENTER + 375, Y_CENTER / 15 - 5);
    scene.placeImageXY(
        new TextImage("Press 'b' for breadth-first search", 15, new Color(240, 255, 240)),
        X_CENTER + 375, Y_CENTER / 15 + 13);
    scene.placeImageXY(
        new TextImage("Press 'd' for depth-first search", 15, new Color(240, 255, 240)),
        X_CENTER + 375, Y_CENTER / 15 + 30);
   
    return scene;
  }

  // reconstruct the creates a new path between the start and the finish of the maze

  public ArrayList<Vertex> reconstruct(HashMap<Vertex, Vertex> cameFromEdge, Vertex finish) {
    ArrayList<Vertex> finished = new ArrayList<Vertex>();
    Vertex prev = finish;
    finished.add(finish);

    while (!(prev.position.equals(new Posn(0, 0)))) {
      prev = cameFromEdge.get(prev);
      finished.add(prev);
    }
    return finished;
  }

  // arraylist of list of vertices
  public ArrayList<ArrayList<Vertex>> createVertices() {
    ArrayList<ArrayList<Vertex>> listOfVertices = new ArrayList<ArrayList<Vertex>>();
    ArrayList<Vertex> singleRow = new ArrayList<Vertex>();
    for (int rows = 0; rows < BOARD_HEIGHT; rows++) {

      singleRow = new ArrayList<Vertex>();
      for (int cols = 0; cols < BOARD_WIDTH; cols++) {
        singleRow.add(new Vertex(new Posn(cols, rows)));
      }
      listOfVertices.add(singleRow);
    }
    return listOfVertices;
  }

  // this method is responsible for the depth-first search
  public ArrayList<Vertex> depthfirst() {

    HashMap<Vertex, Vertex> cameFromEdge = new HashMap<Vertex, Vertex>();
    ArrayList<Vertex> worklist = new ArrayList<Vertex>(); // this is a stack

    worklist.add(graph.listOfVertices.get(0).get(0));

    while (worklist.size() > 0) {
      Vertex next = worklist.get(worklist.size() - 1);

      if (touchedTiles.contains(next)) {
        worklist.remove(worklist.size() - 1);
      }
      else if (next.position.equals(new Posn(BOARD_WIDTH - 1, BOARD_HEIGHT - 1))) {
        return reconstruct(cameFromEdge, next);
      }
      else {
        touchedTiles.add(next);
        if (next.top != null) {

          if (!cameFromEdge.containsKey(next.top)) {
            cameFromEdge.put(next.top, next);

          }
          worklist.add(next.top);
        }
        if (next.bottom != null) {
          if (!cameFromEdge.containsKey(next.bottom)) {
            cameFromEdge.put(next.bottom, next);
          }
          worklist.add(next.bottom);
        }
        if (next.left != null) {
          if (!cameFromEdge.containsKey(next.left)) {
            cameFromEdge.put(next.left, next);
          }
          worklist.add(next.left);
        }
        if (next.right != null) {
          if (!cameFromEdge.containsKey(next.right)) {
            cameFromEdge.put(next.right, next);
          }
          worklist.add(next.right);
        }
      }
    }

    throw new RuntimeException("This maze cannot be solved by depth-first search");

  }

  // this method is responsible for the breadth-first search
  public ArrayList<Vertex> breadthfirst() {
    HashMap<Vertex, Vertex> cameFromEdge = new HashMap<Vertex, Vertex>();
    ArrayList<Vertex> worklist = new ArrayList<Vertex>(); // this is a queue

    worklist.add(graph.listOfVertices.get(0).get(0));
    while (worklist.size() > 0) {
      Vertex next = worklist.get(0);
      if (touchedTiles.contains(next)) {
        worklist.remove(0);
      }
      else if (next.position.equals(new Posn(BOARD_WIDTH - 1, BOARD_HEIGHT - 1))) {
        return reconstruct(cameFromEdge, next);
      }
      else {
        touchedTiles.add(next);
        if (next.top != null) {
          if (!cameFromEdge.containsKey(next.top)) {
            cameFromEdge.put(next.top, next);
          }
          worklist.add(next.top);
        }
        if (next.bottom != null) {
          if (!cameFromEdge.containsKey(next.bottom)) {
            cameFromEdge.put(next.bottom, next);
          }
          worklist.add(next.bottom);
        }

        if (next.left != null) {
          if (!cameFromEdge.containsKey(next.left)) {
            cameFromEdge.put(next.left, next);
          }
          worklist.add(next.left);
        }
        if (next.right != null) {
          if (!cameFromEdge.containsKey(next.right)) {
            cameFromEdge.put(next.right, next);
          }
          worklist.add(next.right);
        }

      }
    }

    throw new RuntimeException("This maze cannot be solved by breadth-first search");
  }

}

// this class represents the Examples class for the maze
class ExamplesMaze {

  MazeWorld maze;
  MazeWorld exMaze1;

  MazeWorld testM;
  MazeWorld testM2;

  Posn pos1;
  Posn pos2;
  Posn pos3;
  Posn pos4;
  Posn pos5;
  Posn pos6;
  Posn pos7;
  Posn pos8;
  Posn pos9;
  Posn pos10;

  Vertex vert1;
  Vertex vert2;
  Vertex vert3;
  Vertex vert4;
  Vertex vert5;
  Vertex vert6;

  Edge edge1;
  Edge edge2;
  Edge edge3;
  Edge edge4;

  Comparator<Integer> byNumOrder1;
  Comparator<Edge> byEWOrder1;

  HashMap<Posn, Posn> hmap;

  // Examples of vertices
  Vertex vertex1 = new Vertex(new Posn(0, 0));
  Vertex vertex2 = new Vertex(new Posn(0, 0));
  Vertex vertex3 = new Vertex(new Posn(50, 0));
  Vertex vertex4 = new Vertex(new Posn(50, 50));
  Vertex vertex5 = new Vertex(new Posn(25, 25));
  Vertex vertex6 = new Vertex(new Posn(0, 25));
  Vertex vertex7 = new Vertex(new Posn(25, 0));
  Vertex vertex8 = new Vertex(new Posn(50, 25));
  Vertex vertex9 = new Vertex(new Posn(25, 50));

  Vertex vertex10 = new Vertex(new Posn(0, 0));
  Vertex vertex11 = new Vertex(new Posn(0, 75));
  Vertex vertex12 = new Vertex(new Posn(75, 0));
  Vertex vertex13 = new Vertex(new Posn(75, 75));
  Vertex vertex14 = new Vertex(new Posn(50, 50));
  Vertex vertex15 = new Vertex(new Posn(0, 50));
  Vertex vertex16 = new Vertex(new Posn(75, 0));
  Vertex vertex17 = new Vertex(new Posn(75, 50));
  Vertex vertex18 = new Vertex(new Posn(50, 75));

  // Examples of ArrayLists
  ArrayList<Integer> array1 = new ArrayList<Integer>();
  ArrayList<Integer> array2 = new ArrayList<Integer>();
  ArrayList<Integer> array3 = new ArrayList<Integer>();
  ArrayList<Integer> array4 = new ArrayList<Integer>();

  ArrayList<Integer> array5 = new ArrayList<Integer>();
  ArrayList<Integer> array6 = new ArrayList<Integer>();
  ArrayList<Integer> array7 = new ArrayList<Integer>();
  ArrayList<Integer> array8 = new ArrayList<Integer>();

  Comparator<Integer> byNumOrder = new ByNumComp();
  Comparator<Edge> byEWOrder = new ByEWComp();

  // this initializes the data
  void initData() {
    this.testM = new MazeWorld();
    this.testM2 = new MazeWorld();
    byNumOrder1 = new ByNumComp();
    byEWOrder1 = new ByEWComp();
    this.pos1 = new Posn(32, 32);
    this.pos2 = new Posn(32, 32);
    this.pos3 = new Posn(64, 16);
    this.pos4 = new Posn(64, 16);
    this.pos5 = new Posn(160, 128);
    this.pos6 = new Posn(64, 16);
    this.pos7 = new Posn(160, 96);
    this.pos8 = new Posn(160, 96);
    this.pos9 = new Posn(320, 0);
    this.pos10 = new Posn(320, 0);

    this.vert1 = new Vertex(pos1, null, null, null, null);
    this.vert2 = new Vertex(pos2, null, null, null, null);
    this.vert3 = new Vertex(pos3, null, null, null, null);
    this.vert4 = new Vertex(pos4, null, null, null, null);
    this.vert5 = new Vertex(pos5, null, null, null, null);
    this.vert6 = new Vertex(pos6, null, null, null, null);

    this.edge1 = new Edge(16, vert1, vert2);
    this.edge2 = new Edge(4, vert3, vert4);
    this.edge3 = new Edge(12, vert5, vert6);
    this.edge4 = new Edge(16, vert3, vert2);

    hmap = new HashMap<Posn, Posn>();
    hmap.put(pos1, pos2);
    hmap.put(pos3, pos4);
    hmap.put(pos5, pos6);

  }

  // this initializes the maze
  void initMazeGame() {
    maze = new MazeWorld();
    exMaze1 = new MazeWorld();
  }

  // this initializes the first set of arrays
  void initArraysV1() {
    array1.add(-9);
    array1.add(-9);
    array1.add(-3);
    array1.add(0);
    array1.add(0);
    array1.add(0);
    array1.add(1);
    array1.add(12);
    array1.add(34);
    array1.add(45);

    array2.add(-9);
    array2.add(-9);
    array2.add(-3);
    array2.add(0);
    array2.add(0);
    array2.add(0);
    array2.add(1);
    array2.add(12);
    array2.add(34);
    array2.add(45);

    array3.add(0);
    array4.add(0);

  }

  // testing the createConnections method
  void testCreateConnections() {
    initArraysV1();
  }

  // testing the produceEdges method
  void testProduceEdges() {
    initArraysV1();
  }

  // testing the find method
  void testFind(Tester t) {
    this.initData();
    t.checkExpect(this.hmap.size(), 3);
    t.checkExpect(this.hmap.get(pos3), pos4);
    t.checkExpect(this.testM.graph.find(hmap, pos1), pos1);
    t.checkExpect(this.testM.graph.find(hmap, pos2), pos1);
    t.checkExpect(this.testM.graph.find(hmap, pos3), pos3);
    t.checkExpect(this.testM.graph.find(hmap, pos3), pos4);
  }

  // testing the union method
  void testUnion(Tester t) {
    this.initData();

    t.checkExpect(this.testM.graph.union(this.hmap, pos1, pos2), this.hmap);
    this.hmap.put(this.pos7, this.pos8);
    t.checkExpect(this.testM.graph.union(this.hmap, pos7, pos8), this.hmap);
    this.hmap.put(this.pos9, this.pos10);
    t.checkExpect(this.testM.graph.union(this.hmap, pos9, pos10), this.hmap);
  }

  // this is the tester method for the maze
  void test(Tester t) {
    initMazeGame();
    initArraysV1();

    // runs the actual maze
    maze.bigBang(MazeWorld.WP_BOARD, MazeWorld.HP_BOARD, 1);

    // test quicksort
    Collections.sort(array1, byNumOrder);
    Collections.sort(array3, byNumOrder);
    t.checkExpect(array1, array2);
    t.checkExpect(array3, array4);

    // testing the createVertices method
    t.checkExpect(maze.graph.listOfVertices.size(), MazeWorld.BOARD_HEIGHT);
    t.checkExpect(maze.graph.listOfVertices.get(0).size(), MazeWorld.BOARD_WIDTH);
    String highlightPosition;
    for (int rows = 0; rows < maze.graph.listOfVertices.size() - 1; rows++) {
      for (int cols = 0; cols < maze.graph.listOfVertices.get(0).size() - 1; cols++) {

        if (maze.graph.listOfVertices.get(rows).get(cols).top != null
            || maze.graph.listOfVertices.get(rows).get(cols).bottom != null
            || maze.graph.listOfVertices.get(rows).get(cols).left != null
            || maze.graph.listOfVertices.get(rows).get(cols).right != null) {
          highlightPosition = "";
        }
        else {
          highlightPosition = cols + ", " + rows;
        }
        t.checkExpect(highlightPosition, "");
      }
    }

    // testing the produceEdge method
    ArrayList<String> testProduceEdge = new ArrayList<String>();
    for (Edge edge : maze.graph.produceEdge()) {
      testProduceEdge.add(edge.weight + ": (" + edge.from.position.x + ", " + edge.from.position.y
          + ") => (" + edge.to.position.x + ", " + edge.to.position.y + ")");
    }

    t.checkExpect(testProduceEdge.size(), MazeWorld.BOARD_HEIGHT * MazeWorld.BOARD_WIDTH * 2
        - MazeWorld.BOARD_HEIGHT - MazeWorld.BOARD_WIDTH);

    // testing the spanning method
    t.checkExpect(maze.spanningTree.size(),
        maze.graph.listOfVertices.size() * maze.graph.listOfVertices.get(0).size() - 1);

    // testing the createVertices method (continued)
    t.checkExpect(exMaze1.createVertices().size(), MazeWorld.BOARD_HEIGHT);

    // testing the depthfirst method
    boolean expected = false;
    this.exMaze1.touchedTiles = new ArrayList<Vertex>();

    Posn examplePos = new Posn(MazeWorld.BOARD_WIDTH - 1, MazeWorld.BOARD_HEIGHT - 1);
    for (Vertex vertex : exMaze1.depthfirst()) {
      expected = expected
          || (vertex.position.x == examplePos.x && vertex.position.y == examplePos.y);
    }
    t.checkExpect(expected, true);

    // testing the breadthfirst method
    boolean expected1 = false;
    exMaze1.touchedTiles = new ArrayList<Vertex>();

    Posn examplePos1 = new Posn(MazeWorld.BOARD_WIDTH - 1, MazeWorld.BOARD_HEIGHT - 1);
    for (Vertex vert : exMaze1.breadthfirst()) {
      expected1 = expected1
          || (vert.position.x == examplePos1.x && vert.position.y == examplePos1.y);
    }
    t.checkExpect(expected1, true);

  }
}
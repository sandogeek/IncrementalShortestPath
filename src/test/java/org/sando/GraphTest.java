package org.sando;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.IntVertexDijkstraShortestPath;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.builder.GraphBuilder;
import org.jgrapht.util.SupplierUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;

/**
 * @author Sando
 * @version 1.0
 * @since 2024/5/3
 */
class GraphTest {
    private static final String GRAPH = "Graph";
    private static final String shortestPath = "shortestPath";
    private static final String addEdge = "addEdge";
    private static final long SEED = 19L;
    private static Random rnd = new Random(SEED);
    private Graph<Integer> graph;
    private ShortestPathTreeCache<Integer> pathTreeCache;
    private DefaultDirectedWeightedGraph<Integer, Edge> directedGraph;

    @Test
    void leetCode() {
        String input1Str = "[\"Graph\",\"shortestPath\",\"addEdge\",\"shortestPath\",\"addEdge\",\"addEdge\",\"addEdge\",\"addEdge\",\"addEdge\",\"addEdge\",\"shortestPath\",\"shortestPath\",\"addEdge\",\"shortestPath\",\"addEdge\",\"shortestPath\",\"addEdge\",\"addEdge\",\"addEdge\"]";
        List<String> input1 = JsonUtils.string2Object(input1Str, new TypeReference<List<String>>() {
        });
        String input2 = "[[13,[[7,2,131570],[9,4,622890],[9,1,812365],[1,3,399349],[10,2,407736],[6,7,880509],[1,4,289656],[8,0,802664],[6,4,826732],[10,3,567982],[5,6,434340],[4,7,833968],[12,1,578047],[8,5,739814],[10,9,648073],[1,6,679167],[3,6,933017],[0,10,399226],[1,11,915959],[0,12,393037],[11,5,811057],[6,2,100832],[5,1,731872],[3,8,741455],[2,9,835397],[7,0,516610],[11,8,680504],[3,11,455056],[1,0,252721]]],[9,3],[[11,1,873094]],[3,10],[[0,9,601498]],[[12,0,824080]],[[12,4,459292]],[[6,9,7876]],[[11,7,5479]],[[11,12,802]],[2,9],[2,6],[[0,11,441770]],[3,7],[[11,0,393443]],[4,2],[[10,5,338]],[[6,1,305]],[[5,0,154]]]";
        ArrayNode arrayNode = (ArrayNode) JsonUtils.readTree(input2);
        int size = input1.size();
        for (int i = 0; i < size; i++) {
            switch (input1.get(i)) {
                case GRAPH: {
                    DefaultDirectedWeightedGraph<Integer, Edge> weightedGraph = new DefaultDirectedWeightedGraph<>(Edge.class);
                    directedGraph = weightedGraph;
                    List<Edge> edges = new ArrayList<>();
                    ArrayNode graph = (ArrayNode) arrayNode.get(i);
                    graph = (ArrayNode) graph.get(1);
                    graph.forEach(node -> {
                        ArrayNode edge = (ArrayNode) node;
                        int start = edge.get(0).asInt();
                        int end = edge.get(1).asInt();
                        long weight = edge.get(2).asLong();
                        Edge e = new Edge(start, end, weight);
                        edges.add(e);
                        weightedGraph.addVertex(start);
                        weightedGraph.addVertex(end);
                        weightedGraph.addEdge(start, end, e);
                        weightedGraph.setEdgeWeight(e, weight);
                    });
                    this.graph = new Graph<>(edges, true);
                    this.pathTreeCache = new ShortestPathTreeCache<>(this.graph);
                    break;
                }
                case shortestPath: {
                    ArrayNode path = (ArrayNode) arrayNode.get(i);
                    int start = path.get(0).asInt();
                    int end = path.get(1).asInt();
                    IntVertexDijkstraShortestPath intVertexDijkstraShortestPath = new IntVertexDijkstraShortestPath(directedGraph);
                    double weight = intVertexDijkstraShortestPath.getPathWeight(start, end);
                    ShortestPathTree<Integer> shortestPathTree = pathTreeCache.getOrCreateShortestPathTree(start);
                    Assertions.assertEquals((long) weight, shortestPathTree.getDistance(end));
                    break;
                }
                case addEdge: {
                    ArrayNode arr = (ArrayNode) arrayNode.get(i);
                    ArrayNode edge = (ArrayNode) arr.get(0);
                    int start = edge.get(0).asInt();
                    int end = edge.get(1).asInt();
                    long weight = edge.get(2).asLong();
                    Edge e = new Edge(start, end, weight);
                    graph.addEdge(e);
                    directedGraph.addVertex(start);
                    directedGraph.addVertex(end);
                    directedGraph.addEdge(start, end, e);
                    directedGraph.setEdgeWeight(e, weight);
                    break;
                }
            }
        }
    }

    @Test
    void randomTest() {
        for (int i = 0; i < 2000; i++) {
            doRandomTest();
        }
    }

    private void doRandomTest() {
        DirectedWeightedMultigraph<Integer, WeightedEdge> multigraph = generateGraph();
        ArrayList<Integer> vertexList = new ArrayList<>(multigraph.vertexSet());
        Collections.shuffle(vertexList);
        Integer start = vertexList.get(0);
        Integer end = vertexList.get(1);
        List<Edge> edges = new ArrayList<>();
        multigraph.iterables().edges().forEach(edge -> {
            edges.add(new Edge((Integer) edge.getSource(), (Integer) edge.getTarget(), (long) edge.getWeight()));
        });
        String edgeStr = JsonUtils.object2String(edges);
        graph = new Graph<>(edges, true);
        pathTreeCache = new ShortestPathTreeCache<>(graph);
        ShortestPathTree<Integer> pathTree = pathTreeCache.getOrCreateShortestPathTree(start);
        Vertex<Integer> endVertex = graph.getVertex(end);
        Vertex<Integer> startVertex = pathTree.getPrevious(end);
        if (startVertex == null) {
            return;
        }
        for (int i = 0; i < rnd.nextInt(20); i++) {
            Vertex<Integer> previousNew = pathTree.getPrevious(startVertex.getK());
            if (previousNew == startVertex) {
                break;
            }
            endVertex = startVertex;
            startVertex = previousNew;
        }
        IEdge<Integer> edge = graph.getEdge(startVertex.getK(), endVertex.getK());
        // 模拟权重增加
        int diff = rnd.nextInt(100);
        diff = diff == 0 ? 1 : diff;
        long weightOld = edge.getWeight();
        long weightNew = weightOld + diff;
        graph.updateWeight(edge.getStart(), edge.getEnd(), weightNew);
        Assertions.assertEquals(true, pathTree.checkAllReset());
        multigraph.setEdgeWeight(edge.getStart(), edge.getEnd(), weightNew);
        IntVertexDijkstraShortestPath<WeightedEdge> shortestPath = new IntVertexDijkstraShortestPath<>(multigraph);
        ShortestPathAlgorithm.SingleSourcePaths<Integer, WeightedEdge> sourcePaths = shortestPath.getPaths(start);
        Vertex<Integer> finalStartVertex = startVertex;
        Vertex<Integer> finalEndVertex = endVertex;
        int finalDiff = diff;
        pathTree.getPrevious(null);
        graph.walkVertex(vertex -> {
            long distance1 = pathTree.getDistance(vertex.getK());
            long distance2 = (long) sourcePaths.getWeight(vertex.getK());
            if (distance2 != distance1) {
                try {
                    save2File(start, end, edgeStr, finalStartVertex, finalEndVertex, finalDiff);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            Assertions.assertEquals(distance2, distance1);
        });
    }

    private static void save2File(Integer start, Integer end, String edgeStr, Vertex<Integer> startVertex, Vertex<Integer> endVertex, int diff) throws IOException {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("edges", edgeStr);
        objectNode.put("start", start);
        objectNode.put("end", end);
        objectNode.put("edgeStart", startVertex.getK());
        objectNode.put("edgeEnd", endVertex.getK());
        objectNode.put("diff", diff);
        File file = new File("src/test/resources/case1.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(objectNode.toString());
        }
    }

    static class WeightedEdge extends DefaultWeightedEdge {
        @Override
        public Object getSource() {
            return super.getSource();
        }

        @Override
        public Object getTarget() {
            return super.getTarget();
        }

        @Override
        public double getWeight() {
            return super.getWeight();
        }
    }

    /**
     * Generates G(n,p) random graph with 10 vertices and edge probability of 0.5.
     *
     * @return generated graph.
     */
    private static DirectedWeightedMultigraph<Integer, WeightedEdge> generateGraph() {
        DirectedWeightedMultigraph<Integer, WeightedEdge> graph
                = new DirectedWeightedMultigraph<>(WeightedEdge.class);
        graph.setVertexSupplier(SupplierUtil.createIntegerSupplier(1));

        GnpRandomGraphGenerator<Integer, WeightedEdge> generator
                = new GnpRandomGraphGenerator<>(40, 0.4);
        generator.generateGraph(graph);

        for (WeightedEdge edge : graph.edgeSet()) {
            long weight = (long) (rnd.nextDouble() * 100);
            if (weight == 0) {
                weight = 1;
            }
            graph.setEdgeWeight(edge, weight);
        }
        return graph;
    }

    @Test
    void case1() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/case1.txt");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String jsonNodeStr = bufferedReader.readLine();
        JsonNode jsonNode = JsonUtils.readTree(jsonNodeStr);
        List<Edge> edgeList = JsonUtils.string2Object(jsonNode.get("edges").asText(), new TypeReference<List<Edge>>() {
        });
        graph = new Graph<>(edgeList, true);
        ShortestPathTreeCache<Integer> treeCache = new ShortestPathTreeCache<>(graph);
        int start = jsonNode.get("start").asInt();
        int end = jsonNode.get("end").asInt();
        ShortestPathTree<Integer> pathTree = treeCache.getOrCreateShortestPathTree(start);
        pathTree.getPrevious(end);
        pathTree.printTmpPath();
        int edgeStart = jsonNode.get("edgeStart").asInt();
        int edgeEnd = jsonNode.get("edgeEnd").asInt();
        IEdge<Integer> edge = graph.getEdge(edgeStart, edgeEnd);
        int diff = jsonNode.get("diff").asInt();
        graph.updateWeight(edgeStart, edgeEnd, edge.getWeight() + diff);
        System.out.println(String.format("权重变更：%s %s diff:%s", edgeStart, edgeEnd, diff));
        pathTree.printAllPath();

        ShortestPathTree<Integer> tree = new ShortestPathTree<>(graph, start);
        System.out.println("正确版本：");
        tree.printAllPath();

        Assertions.assertTrue(pathTree.checkAllReset());
        GraphBuilder<Integer, WeightedEdge, ? extends DirectedWeightedMultigraph<Integer, WeightedEdge>> builder = DirectedWeightedMultigraph.createBuilder(WeightedEdge.class);
        edgeList.forEach(edgeTmp -> {
            WeightedEdge weightedEdge = new WeightedEdge();
            builder.addEdge(edgeTmp.getStart(), edgeTmp.getEnd(), weightedEdge, edgeTmp.getWeight());
        });
        DirectedWeightedMultigraph<Integer, WeightedEdge> multigraph = builder.build();
        IntVertexDijkstraShortestPath<WeightedEdge> shortestPath = new IntVertexDijkstraShortestPath<>(multigraph);
        ShortestPathAlgorithm.SingleSourcePaths<Integer, WeightedEdge> paths = shortestPath.getPaths(start);
        checkDistanceSame(graph, pathTree, paths);
    }

    /**
     * 检测两个最短路径树的所有节点的最短距离是否相同
     */
    void checkDistanceSame(Graph<Integer> graph, ShortestPathTree<Integer> pathTree1, ShortestPathAlgorithm.SingleSourcePaths<Integer, WeightedEdge> pathTree2) {
        graph.walkVertex(vertex -> {
            Integer k = vertex.getK();
            long distance1 = pathTree1.getDistance(k);
            long distance2 = (long) pathTree2.getWeight(k);
            Assertions.assertEquals(distance2, distance1, String.format("最短路径树1和2到顶点%s的最短距离不一致", k));
        });
    }

    /**
     * 论文中的示例
     */
    @Test
    void articleDemoUseCompleteTree() {
        doDemoTest(true);
    }

    /**
     * 论文中的示例
     */
    @Test
    void articleDemo() {
        doDemoTest(false);
    }

    private static void doDemoTest(boolean useCompleteTree) {
        List<StrEdge> edges = new ArrayList<>();
        edges.add(new StrEdge("a", "f", 3));
        edges.add(new StrEdge("a", "e", 5));
        edges.add(new StrEdge("b", "g", 3));
        edges.add(new StrEdge("c", "h", 8));
        edges.add(new StrEdge("c", "g", 6));
        edges.add(new StrEdge("c", "b", 3));
        edges.add(new StrEdge("c", "d", 5));
        edges.add(new StrEdge("d", "h", 5));
        edges.add(new StrEdge("e", "f", 4));
        edges.add(new StrEdge("e", "m", 6));
        edges.add(new StrEdge("f", "m", 7));
        edges.add(new StrEdge("f", "l", 7));
        edges.add(new StrEdge("f", "g", 6));
        edges.add(new StrEdge("g", "a", 4));
        edges.add(new StrEdge("g", "l", 5));
        edges.add(new StrEdge("g", "k", 8));
        edges.add(new StrEdge("g", "j", 6));
        edges.add(new StrEdge("g", "h", 10));
        edges.add(new StrEdge("h", "j", 6));
        edges.add(new StrEdge("h", "i", 4));
        edges.add(new StrEdge("i", "d", 10));
        edges.add(new StrEdge("j", "p", 9));
        edges.add(new StrEdge("j", "q", 3));
        edges.add(new StrEdge("j", "i", 4));
        edges.add(new StrEdge("k", "p", 4));
        edges.add(new StrEdge("k", "j", 4));
        edges.add(new StrEdge("l", "o", 4));
        edges.add(new StrEdge("l", "k", 7));
        edges.add(new StrEdge("m", "n", 10));
        edges.add(new StrEdge("m", "o", 5));
        edges.add(new StrEdge("n", "u", 5));
        edges.add(new StrEdge("o", "n", 8));
        edges.add(new StrEdge("o", "u", 7));
        edges.add(new StrEdge("o", "p", 7));
        edges.add(new StrEdge("p", "u", 7));
        edges.add(new StrEdge("p", "r", 5));
        edges.add(new StrEdge("p", "q", 15));
        edges.add(new StrEdge("q", "i", 9));
        edges.add(new StrEdge("r", "q", 14));
        edges.add(new StrEdge("r", "t", 4));
        edges.add(new StrEdge("s", "a", 6));
        edges.add(new StrEdge("s", "b", 4));
        edges.add(new StrEdge("s", "c", 7));
        edges.add(new StrEdge("u", "v", 5));
        edges.add(new StrEdge("u", "w", 7));
        edges.add(new StrEdge("u", "t", 6));
        edges.add(new StrEdge("v", "w", 5));
        edges.add(new StrEdge("v", "t", 7));
        edges.add(new StrEdge("w", "n", 7));
        Graph<String> graph1 = new Graph<>(edges, true);
        ShortestPathTreeCache<String> treeCache = new ShortestPathTreeCache<>(graph1);
        ShortestPathTree<String> pathTree = treeCache.getOrCreateShortestPathTree("s");
        if (useCompleteTree) {
            pathTree.getPrevious(null);
        } else {
            pathTree.getPrevious("o");
        }
        graph1.updateWeight("b", "g", 10);
        pathTree.printCurAllPath();
        pathTree.printAllPath();
    }

    static class StrEdge implements IEdge<String> {
        private String start;
        private String end;
        private long weight;

        public StrEdge(String start, String end, long weight) {
            this.start = start;
            this.end = end;
            this.weight = weight;
        }

        @Override
        public String getStart() {
            return start;
        }

        @Override
        public String getEnd() {
            return end;
        }

        @Override
        public long getWeight() {
            return weight;
        }

        @Override
        public void setWeight(long weight) {
            this.weight = weight;
        }

        @Override
        public String toString() {
            return "StrEdge{" +
                    "start='" + start + '\'' +
                    ", end='" + end + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }
}
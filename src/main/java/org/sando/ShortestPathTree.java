package org.sando;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 最短路径树
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/2
 */
public class ShortestPathTree<K> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShortestPathTree.class);
    Map<K, DijkstraVertex<K>> vertexMap;
    private DijkstraVertex<K> root;
    private final Graph<K> graph;
    /**
     * 内部包含最短路径树
     */
    DijkHeapWrapper heapWrapper;
    /**
     * 已经全部更新完成，此时最短路径树是完整的
     */
    boolean complete;
    private ShortestPathTreeUpdater<K> treeUpdater;
    /**
     * 当前最短路径树上的节点的数量
     */
    private int selectedCount;

    public ShortestPathTree(Graph<K> graph, K root) {
        this(graph, root, true);
    }

    public ShortestPathTree(Graph<K> graph, K root, boolean mergeUpdate) {
        if (graph.hasNegativeEdge) {
            throw new IllegalStateException("dijkstra算法不支持负权重边");
        }
        this.graph = graph;
        this.vertexMap = new HashMap<>(graph.size());
        this.root = getOrCreateVertex(root);
        this.treeUpdater = new ShortestPathTreeUpdater<>(this, mergeUpdate);
    }

    void dijkstra(K target) {
        if (complete) {
            return;
        }
        // 初始化
        if (heapWrapper == null) {
            this.root.setDistance(0);
            // 遍历所有顶点
            graph.walkVertex(kVertex -> getOrCreateVertex(kVertex.getK()));
            heapWrapper = new DijkHeapWrapper();
        }

        VertexIndex<K> start;
        while (!heapWrapper.isEmpty()) {
            start = heapWrapper.poll();
//            LOGGER.debug("选中节点：" + start);K
            start.selected = true;
            selectedCount++;
            start.changePrevious(start.getPrevious());
            // 遍历所有邻接顶点
            for (Map.Entry<K, IEdge<K>> entry : start.dVertex.vertex.outEdges.entrySet()) {
                K end = entry.getKey();
                VertexIndex<K> viEnd = heapWrapper.getVertexIndex(end);
                if (viEnd.selected) {
                    continue;
                }
                IEdge<K> edge = entry.getValue();
                relax(heapWrapper, start, viEnd, edge);
            }
//            LOGGER.debug("堆状态：" + heapWrapper);
            if (start.dVertex.vertex.getK().equals(target)) {
                break;
            }
        }
        if (heapWrapper.isEmpty()) {
            complete = true;
            heapWrapper = null;
        }
    }

    /**
     * 松弛操作
     */
    private void relax(DijkHeapWrapper heap, VertexIndex<K> start, VertexIndex<K> end, IEdge<K> edge) {
        DijkstraVertex<K> vertex = end.dVertex;
        long weight = edge.getWeight();
        long distanceNew = start.dVertex.getDistance() + weight;
        if (distanceNew < vertex.getDistance()) {
            vertex.setDistance(distanceNew);
            end.changePrevious(start);
            if (end.index == Heap.NOT_IN_HEAP) {
                heap.offer(end);
                return;
            }
            heap.priorityChange(end.index, -1);
        }
    }

    public void edgeUpdate(IEdge<K> edge, long oldWeight) {
        long weight = edge.getWeight();
        if (weight == oldWeight) {
            return;
        }
//        if (!complete && heapWrapper != null) {
//            int pct = selectedCount * 100 / vertexMap.size();
//            if (pct > 60) {
//                // 最短路径树已经比较完整了，直接补充完整后用DSPT算法更新
//                dijkstra(null);
//                treeUpdater.edgeUpdate(edge, oldWeight);
//                return;
//            }
//            edgeUpdateOnNotComplete(edge);
//            return;
//        }
        treeUpdater.edgeUpdate(edge, oldWeight);
        treeUpdater.tryMergeUpdate();
        printTmpPath();
        System.out.println("打印临时路径结束");
        printCurAllPath();
    }

    private void edgeUpdateOnNotComplete(IEdge<K> edge) {
        K end = edge.getEnd();
        VertexIndex<K> viEnd = heapWrapper.getVertexIndex(end);
        PathTreeHelper.handleSuccessorAndSelfRecursive(viEnd, vertex -> {
            Map<K, IEdge<K>> inEdges = vertex.getVertex().inEdges;
            vertex.changePrevious(null);
            vertex.selected = false;
            selectedCount--;
            vertex.removeFromHeap();
            vertex.dVertex.resetDistance();
            for (Map.Entry<K, IEdge<K>> entry : inEdges.entrySet()) {
                VertexIndex<K> viStart = heapWrapper.getVertexIndex(entry.getKey());
                if (!viStart.selected) {
                    continue;
                }
                relax(heapWrapper, viStart, vertex, entry.getValue());
            }
        });
    }

    public void edgeAdd(IEdge<K> edge) {
        // TODO 未完善
        this.vertexMap = new HashMap<>(graph.size());
        this.root = getOrCreateVertex(root.vertex.getK());
        this.treeUpdater = new ShortestPathTreeUpdater<>(this, true);
        this.complete = false;
    }

    public boolean checkAllReset() {
        return treeUpdater.checkAllReset();
    }

    class DijkHeapWrapper {
        private final Heap<VertexIndex<K>> heap;
        Map<K, VertexIndex<K>> map;
        VertexIndex<K> root;

        public DijkHeapWrapper() {
            map = new HashMap<>(vertexMap.size());
            vertexMap.values().forEach(vertex -> map.put(vertex.getVertex().getK(), new VertexIndex<>(vertex)));
            heap = new Heap<>();
            root = map.get(ShortestPathTree.this.root.getVertex().getK());
            root.changePrevious(root);
            heap.add(root);
        }

        public VertexIndex<K> poll() {
            return heap.poll();
        }

        public boolean isEmpty() {
            return heap.isEmpty();
        }

        public VertexIndex<K> getVertexIndex(K k) {
            return map.get(k);
        }

        public boolean offer(VertexIndex<K> kVertexIndex) {
            return heap.offer(kVertexIndex);
        }

        public void priorityChange(int index, int compareResult) {
            heap.priorityChange(index, compareResult);
        }

        public void clear() {
            heap.clear();
        }

        @Override
        public String toString() {
            return heap.toString();
        }
    }

    /**
     * 打印当前已知的所有最短路径
     */
    public void printCurAllPath() {
        for (DijkstraVertex<K> vertex : vertexMap.values()) {
            StringBuilder stringBuilder = getPathStringBuilder(vertex);
            System.out.println(stringBuilder);
        }
    }

    public static <K, V extends BaseDijkVertex<K, V>> StringBuilder getPathStringBuilder(BaseDijkVertex<K, V> target) {
        List<BaseDijkVertex<K, V>> vertexList = new ArrayList<>();
        vertexList.add(target);
        while (target != null && target.getVertex() != null && target.getPrevious() != target) {
            vertexList.add(target.getPrevious());
            target = target.getPrevious();
        }
        StringBuilder stringBuilder = new StringBuilder();
        int size = vertexList.size();
        boolean first = true;
        for (int i = size - 1; i >= 0; i--) {
            if (first) {
                stringBuilder.append(vertexList.get(i));
                first = false;
            } else {
                stringBuilder.append(" -> ").append(vertexList.get(i));
            }
        }
        return stringBuilder;
    }

    public void printTmpPath() {
        Map<K, ? extends BaseDijkVertex<K, ?>> vertexMap = this.vertexMap;
        if (!complete) {
            System.out.println(heapWrapper);
            vertexMap = heapWrapper.map;
        }
        for (BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>> vertex : vertexMap.values()) {
            StringBuilder stringBuilder = getPathStringBuilder(vertex);
            System.out.println(stringBuilder);
        }
    }


    /**
     * 打印完整最短路径
     */
    public void printAllPath() {
        if (!complete) {
            dijkstra(null);
        }
        treeUpdater.tryMergeUpdate();
        printCurAllPath();
    }

    private DijkstraVertex<K> getOrCreateVertex(K k) {
        return vertexMap.computeIfAbsent(k, key -> new DijkstraVertex<>(graph.getVertex(k)));
    }

    private DijkstraVertex<K> getVertex(K k) {
        return vertexMap.get(k);
    }

    public Vertex<K> getPrevious(K k) {
        treeUpdater.tryMergeUpdate();
        DijkstraVertex<K> vertex = tryDoDijkstra(k);
        if (vertex == null) {
            return null;
        }
        DijkstraVertex<K> previous = vertex.getPrevious();
        if (previous == null) {
            return null;
        }
        return previous.getVertex();
    }

    /**
     * 获取到end的最短距离
     */
    public long getDistance(K end) {
        treeUpdater.tryMergeUpdate();
        DijkstraVertex<K> vertex = tryDoDijkstra(end);
        if (vertex == null) {
            return Long.MAX_VALUE;
        }
        return vertex.getDistance();
    }

    private DijkstraVertex<K> tryDoDijkstra(K end) {
        DijkstraVertex<K> vertex = getVertex(end);
        if (vertex == null) {
            dijkstra(end);
            vertex = getVertex(end);
            return vertex;
        }
        if (!complete && vertex.getPrevious() == null) {
            dijkstra(end);
            return vertex;
        }
        return vertex;
    }

    static class VertexIndex<K> extends BaseDijkVertex<K, VertexIndex<K>> implements IHeapIndex, Comparable<VertexIndex<K>> {
        private int index = Heap.NOT_IN_HEAP;
        private DijkstraVertex<K> dVertex;
        boolean selected;
        /**
         * 当前所在堆
         */
        private Heap<?> heap;

        public VertexIndex(DijkstraVertex<K> vertex) {
            this.dVertex = vertex;
        }

        @Override
        public void changePrevious(VertexIndex<K> previous) {
            super.changePrevious(previous);
            if (selected) {
                DijkstraVertex<K> dVertex;
                if (previous == null) {
                    dVertex = null;
                } else {
                    dVertex = previous.dVertex;
                }
                this.dVertex.changePrevious(dVertex);
            }
        }

        @Override
        public int compareTo(VertexIndex<K> o) {
            return (int) (dVertex.getDistance() - o.dVertex.getDistance());
        }

        @Override
        public void indexChange(Heap<?> heap, int index) {
            if (index == Heap.NOT_IN_HEAP) {
                this.heap = null;
            } else {
                this.heap = heap;
            }
            this.index = index;
        }

        public void removeFromHeap() {
            if (index == Heap.NOT_IN_HEAP) {
                return;
            }
            heap.remove(this);
        }

        @Override
        public long changeDistance(long diff) {
            if (diff == 0) {
                return getDistance();
            }
            long result = dVertex.changeDistance(diff);
            if (index != Heap.NOT_IN_HEAP) {
                heap.priorityChange(index,diff > 0 ? 1 : -1);
            }
            return result;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public Vertex<K> getVertex() {
            return dVertex.getVertex();
        }

        @Override
        public long getDistance() {
            return dVertex.getDistance();
        }

        @Override
        public String toString() {
            return "("+getVertex()+","+getDistance()+ ","+ selected+")";
        }
    }
}

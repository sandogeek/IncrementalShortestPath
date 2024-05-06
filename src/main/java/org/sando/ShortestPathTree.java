package org.sando;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 最短路径树T2
 * <p>
 * 路径更新参考文献：<a href="https://www.jsjkx.com/CN/article/openArticlePDF.jsp?id=3759">...</a>
 * </p>
 * {@link VertexIndex}构成的最短路径树T1，是当前不考虑其它节点的最短路径树,当节点未遍历完时，{@link DijkstraVertex}构成的路径树T2必定是T1
 * 的子树；当节点遍历完时，{@link DijkstraVertex}构成的路径树T2必定等于T1。因此，当T2不完整时，做路径更新时应使用T1。
 * 并且，在执行路径更新时，如果{@link VertexIndex#selected}
 * 为true，则对{@link VertexIndex}的路径更新也要更新到路径树T2中。
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/2
 */
public class ShortestPathTree<K> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShortestPathTree.class);
    Map<K, DijkstraVertex<K>> vertexMap;
    DijkstraVertex<K> root;
    private final Graph<K> graph;
    /**
     * 内部包含最短路径树T1
     */
    DijkHeapWrapper heapWrapper;
    /**
     * 已经全部更新完成，此时最短路径树是完整的
     */
    boolean complete;
    private ShortestPathTreeUpdater<K> treeUpdater;

    public ShortestPathTree(Graph<K> graph, K root) {
        if (graph.hasNegativeEdge) {
            throw new IllegalStateException("dijkstra算法不支持负权重边");
        }
        this.graph = graph;
        this.vertexMap = new HashMap<>(graph.size());
        this.root = getOrCreateVertex(root);
        this.treeUpdater = new ShortestPathTreeUpdater<>(this);
    }

    private void dijkstra(K target) {
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
//            LOGGER.debug("选中节点：" + start);
            start.selected = true;
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
        LOGGER.debug("------------结束--------------\n");
    }

    public void edgeUpdate(IEdge<K> edge, long oldWeight) {
//        if (!complete) {
//            dijkstra(null);
//        }
        treeUpdater.edgeUpdate(edge, oldWeight);
    }

    public void edgeAdd(IEdge<K> edge) {
        // TODO 未完善
        this.vertexMap = new HashMap<>(graph.size());
        this.root = getOrCreateVertex(root.vertex.getK());
        this.treeUpdater = new ShortestPathTreeUpdater<>(this);
        this.complete = false;
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
            List<DijkstraVertex<K>> vertexList = new ArrayList<>();
            vertexList.add(vertex);
            while (vertex != null && vertex.getVertex() != null && vertex.getPrevious() != vertex) {
                vertexList.add((DijkstraVertex<K>) vertex.getPrevious());
                vertex = (DijkstraVertex<K>) vertex.getPrevious();
            }
            StringBuilder stringBuilder = new StringBuilder();
            int size = vertexList.size();
            boolean first = true;
            for (int i = size - 1; i >= 0; i--) {
                if (first) {
                    stringBuilder.append(vertexList.get(i));
                    first = false;
                } else {
                    stringBuilder.append(" -> " + vertexList.get(i));
                }
            }
            LOGGER.info(stringBuilder.toString());
        }
    }

    /**
     * 打印完整最短路径
     */
    public void printAllPath() {
        if (!complete) {
            dijkstra(null);
        }
        printCurAllPath();
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
//                LOGGER.debug("更新节点offer：" + end);
                return;
            }
            heap.priorityChange(end.index, -1);
//            LOGGER.debug("更新节点update：" + end);
//            heap.remove(end);
//            heap.offer(end);
        }
    }

    private DijkstraVertex<K> getOrCreateVertex(K k) {
        return vertexMap.computeIfAbsent(k, key -> new DijkstraVertex<>(graph.getVertex(k)));
    }

    private DijkstraVertex<K> getVertex(K k) {
        return vertexMap.get(k);
    }

    public Vertex<K> getPrevious(K k) {
        DijkstraVertex<K> vertex = tryDoDijkstra(k);
        if (vertex == null) {
            return null;
        }
        return vertex.getPrevious().getVertex();
    }

    /**
     * 获取到end的最短距离
     */
    public long getDistance(K end) {
        DijkstraVertex<K> vertex = tryDoDijkstra(end);
        if (vertex == null) {
            return Long.MAX_VALUE;
        }
        return vertex.distance;
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

    static class VertexIndex<K> extends BaseDijkVertex<K> implements IHeapIndex, Comparable<VertexIndex<K>> {
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
        public void changePrevious(BaseDijkVertex<K> previous) {
            VertexIndex vertexIndex = (VertexIndex) previous;
            super.changePrevious(vertexIndex);
            if (selected) {
                dVertex.changePrevious(vertexIndex.dVertex);
            }
        }

        @Override
        public int compareTo(VertexIndex<K> o) {
            return (int) (dVertex.distance - o.dVertex.distance);
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

        @Override
        public long changeDistance(long diff) {
            long result = dVertex.changeDistance(diff);
            if (index != Heap.NOT_IN_HEAP) {
                heap.priorityChange(index, diff == 0 ? 0 :(diff > 0 ? 1 : -1));
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
            return dVertex.toString();
        }
    }
}

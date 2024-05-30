package org.sando;

import org.sando.heap.fiboheap.*;
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
            LOGGER.debug("选中节点：" + start);
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
    }

    /**
     * 松弛操作
     */
    private void relax(DijkHeapWrapper heap, VertexIndex<K> start, VertexIndex<K> end, IEdge<K> edge) {
        DijkstraVertex<K> vertex = end.dVertex;
        long weight = edge.getWeight();
        long distanceNew = start.dVertex.getDistance() + weight;
        LOGGER.debug("松弛边：start={},end={}", edge.getStart(), heap.getVertexIndex(edge.getEnd()));
        if (distanceNew < vertex.getDistance()) {
            vertex.setDistance(distanceNew);
            LOGGER.debug("更新节点：{}", vertex);
            end.changePrevious(start);
            if (end.getHeap() == null) {
                heap.offer(end);
                return;
            }
            end.decreaseKey();
        }
    }

    public void edgeUpdate(IEdge<K> edge, long oldWeight) {
        long weight = edge.getWeight();
        if (weight == oldWeight) {
            return;
        }
        if (!complete) {
            if (heapWrapper == null) {
                return;
            }
            List<VertexIndex<K>> vertexList = generateRelateVertex(edge);
            treeUpdater.edgeUpdate(edge, oldWeight);
            treeUpdater.tryMergeUpdate();
            handleRelateVertex(vertexList);
            printTmpPath();
            System.out.println("打印临时路径结束");
        } else {
            treeUpdater.edgeUpdate(edge, oldWeight);
        }
    }

    private void handleRelateVertex(List<VertexIndex<K>> vertexList) {
        for (VertexIndex<K> vertex : vertexList) {
            Map<K, IEdge<K>> inEdges = vertex.getVertex().inEdges;
            long minDistance = vertex.getDistance();
            VertexIndex<K> parent = vertex.getTmpPrevious();
            boolean allInSelected = true;
            for (Map.Entry<K, IEdge<K>> entry : inEdges.entrySet()) {
                K start = entry.getKey();
                VertexIndex<K> vertexStart = heapWrapper.getVertexIndex(start);
                boolean selected = vertexStart.selected;
                if (!selected) {
                    allInSelected = false;
                    continue;
                }
                long distanceNew;
                if (vertex.getTmpPrevious() == vertexStart) {
                    distanceNew = vertex.getDistance();
                } else {
                    distanceNew = vertexStart.getDistance() + entry.getValue().getWeight();
                }
                if (distanceNew < minDistance) {
                    parent = vertexStart;
                    minDistance = distanceNew;
                }
            }
            if (allInSelected) {
                if (!vertex.selected) {
                    vertex.selected = true;
                }
                if (vertex.getTmpPrevious() != parent) {
                    vertex.changePrevious(parent);
                    vertex.updateDistance(minDistance);
                    debugRelateVertex(vertex, parent);
                }
            } else {
                if (vertex.getTmpPrevious() != parent) {
                    vertex.changePrevious(parent);
                    vertex.updateDistance(minDistance);
                    debugRelateVertex(vertex, parent);
                }
                if (!parent.selected) {
                    vertex.removeFromHeap();
                    vertex.resetDistance();
                }
                if (vertex.selected) {
                    LOGGER.debug("节点{}取消选择", vertex);
                    vertex.cancelSelect();
                    if (parent.selected) {
                        LOGGER.debug("节点{}重新进入堆中", vertex);
                        heapWrapper.offer(vertex);
                    }
                }
            }
        }
    }

    private static <K> void debugRelateVertex(VertexIndex<K> vertex, VertexIndex<K> parent) {
        LOGGER.debug("节点{}更新距离,修改父节点：{}", vertex, parent);
    }

    private List<VertexIndex<K>> generateRelateVertex(IEdge<K> edge) {
        List<VertexIndex<K>> result = new ArrayList<>();
        K end = edge.getEnd();
        VertexIndex<K> viEnd = heapWrapper.getVertexIndex(end);
        result.add(viEnd);
        PathTreeHelper.handleSuccessorRecursive(viEnd, result::add, null, BaseDijkVertex::walkSuccessorWithTmp);
        return result;
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
        private final Queue<VertexIndex<K>> heap;
        Map<K, VertexIndex<K>> map;
        VertexIndex<K> root;

        public DijkHeapWrapper() {
            map = new HashMap<>(vertexMap.size());
            vertexMap.values().forEach(vertex -> map.put(vertex.getVertex().getK(), new VertexIndex<>(vertex)));
            heap = FiboHeap.create(VertexIndex.class);
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
        while (target != null && target.getVertex() != null && target.getTmpPrevious() != target) {
            vertexList.add(target.getTmpPrevious());
            target = target.getTmpPrevious();
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

    static class VertexIndex<K> extends BaseDijkVertex<K, VertexIndex<K>> implements IFiboHeapAware<VertexIndex<K>>, Comparable<VertexIndex<K>> {
        private DijkstraVertex<K> dVertex;
        boolean selected;
        /**
         * 当前所在堆
         */
        private IFiboHeap<VertexIndex<K>> heap;
        private IHandle<VertexIndex<K>> entry;

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

        public boolean cancelSelect() {
            if (!selected) {
                return false;
            }
            this.dVertex.changePrevious(null);
            selected = false;
            return true;
        }

        @Override
        public int compareTo(VertexIndex<K> o) {
            return (int) (dVertex.getDistance() - o.dVertex.getDistance());
        }

        public void removeFromHeap() {
            heap.delete(entry);
        }

        @Override
        public long changeDistance(long diff) {
            if (diff == 0) {
                return getDistance();
            }
            long result = dVertex.changeDistance(diff);
            priorityChange(diff);
            return result;
        }

        private void priorityChange(long diff) {
            if (diff > 0) {
                increaseKey();
            } else {
                decreaseKey();
            }
        }

        @Override
        public IFiboHeap<VertexIndex<K>> getHeap() {
            return heap;
        }

        @Override
        public void setHeap(IFiboHeap<VertexIndex<K>> heap) {
            this.heap = heap;
        }

        @Override
        public IHandle<VertexIndex<K>> getHandle() {
            return entry;
        }

        @Override
        public void setHandle(IHandle<VertexIndex<K>> entry) {
            this.entry = entry;
        }

        @Override
        public void changeDistanceRecursive(long diff) {
            if (diff == 0) {
                return;
            }
            changeDistance(diff);
            LOGGER.debug("更新最短路径距离:{}", this);
            walkSuccessorWithTmp(v -> {
                v.changeDistanceRecursive(diff);
            });
        }

        public void updateDistance(long distanceNew) {
            long distance = getDistance();
            if (distance == distanceNew) {
                return;
            }
            dVertex.setDistance(distanceNew);
            priorityChange(distanceNew > distance ? 1 : -1);
        }

        public void resetDistance() {
            long distance = getDistance();
            dVertex.resetDistance();
            priorityChange(getDistance() > distance ? 1 : -1);
        }

        @Override
        public boolean isNotSelected() {
            return !selected;
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
            return "(" + getVertex() + "," + getDistance() + "," + selected + ")";
        }
    }
}

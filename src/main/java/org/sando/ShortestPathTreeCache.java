package org.sando;

import java.util.HashMap;
import java.util.Map;

/**
 * 最短路径树缓存
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/4
 */
public class ShortestPathTreeCache<K> {
    private Graph<K> graph;
    /**
     * 顶点 -> 从顶点出发的最短路径树
     */
    private Map<K, ShortestPathTree<K>> sptMap = new HashMap<>();
    private Graph.IEdgeUpdate<K> edgeUpdate = (edge, oldWeight) -> {
        sptMap.values().forEach(tree -> tree.edgeUpdate(edge, oldWeight));
    };
    private Graph.IEdgeAdd<K> addEdge = (edge) -> {
        sptMap.values().forEach(tree -> tree.edgeAdd(edge));
    };

    public ShortestPathTreeCache(Graph<K> graph) {
        this.graph = graph;
        graph.onAddEdge(addEdge);
        graph.onEdgeUpdate(edgeUpdate);
    }

    /**
     * 获取从顶点start出发的最短路径树
     *
     * @param start 顶点
     */
    public ShortestPathTree<K> getOrCreateShortestPathTree(K start) {
        return sptMap.computeIfAbsent(start, k -> new ShortestPathTree<>(graph, start));
    }
}

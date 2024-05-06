package org.sando;

import java.util.*;

/**
 * 顶点
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/2
 */
public class Vertex<K> {
    /**
     * 入边
     */
    Map<K, IEdge<K>> inEdges = Collections.emptyMap();
    /**
     * 出边
     */
    Map<K, IEdge<K>> outEdges = Collections.emptyMap();
    private final K k;

    public Vertex(K value) {
        this.k = value;
    }

    /**
     * 增加终点与对应边的映射
     *
     * @param end  终点
     * @param edge 边
     */
    public void addOutEdge(Vertex<K> end, IEdge<K> edge) {
        if (outEdges.isEmpty()) {
            outEdges = new HashMap<>();
        }
        outEdges.put(end.getK(), edge);
    }

    /**
     * 增加起点与对应边的映射
     *
     * @param start 起点
     * @param edge  边
     */
    public void addInEdge(Vertex<K> start, IEdge<K> edge) {
        if (inEdges.isEmpty()) {
            inEdges = new HashMap<>();
        }
        inEdges.put(start.getK(), edge);
    }

    public K getK() {
        return k;
    }

    @Override
    public String toString() {
        return "" + k;
    }
}

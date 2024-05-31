package org.sando;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;

/**
 * 图
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/2
 */
public class Graph<K> {
    /**
     * 图的所有顶点
     */
    private final Map<K, Vertex<K>> vertexMap = new HashMap<>();
    private final boolean directed;
    private List<WeakReference<IEdgeAdd<K>>> addEdgeListeners = Collections.emptyList();
    private List<WeakReference<IEdgeUpdate<K>>> edgeUpdates = Collections.emptyList();
    /**
     * 是否存在负权重的边
     */
    boolean hasNegativeEdge;
    /**
     * 是否是稠密图，false则表明是稀疏图
     */
    private boolean dense;

    /**
     * 构造函数
     *
     * @param edges    边
     * @param directed 是否是有向图
     */
    public Graph(List<? extends IEdge<K>> edges, boolean directed) {
        this.directed = directed;
        for (IEdge<K> edge : edges) {
            if (edge.getWeight() < 0) {
                hasNegativeEdge = true;
            }
            K start = edge.getStart();
            K end = edge.getEnd();
            // TODO 检测是否存在重复边
            Vertex<K> vertexStart = vertexMap.computeIfAbsent(start, Vertex::new);
            Vertex<K> vertexEnd = vertexMap.computeIfAbsent(end, Vertex::new);
            vertexStart.addOutEdge(vertexEnd, edge);
            vertexEnd.addInEdge(vertexStart, edge);
            if (!directed) {
                vertexEnd.addOutEdge(vertexStart, edge);
                vertexStart.addInEdge(vertexEnd, edge);
            }
        }
        int vertexSize = vertexMap.size();
        if (edges.size() > vertexSize * vertexSize) {
            dense = true;
        }
    }

    public Set<K> getVertexSet() {
        return vertexMap.keySet();
    }

    public Vertex<K> getVertex(K K) {
        return vertexMap.get(K);
    }

    public int size() {
        return vertexMap.size();
    }

    public void onAddEdge(IEdgeAdd<K> addEdge) {
        if (addEdgeListeners.isEmpty()) {
            addEdgeListeners = new ArrayList<>();
        }
        addEdgeListeners.add(new WeakReference<>(addEdge));
    }

    public void onEdgeUpdate(IEdgeUpdate<K> edgeUpdate) {
        if (edgeUpdates.isEmpty()) {
            edgeUpdates = new ArrayList<>();
        }
        edgeUpdates.add(new WeakReference<>(edgeUpdate));
    }

    public void addEdge(IEdge<K> edge) {
        K start = edge.getStart();
        K end = edge.getEnd();
        Vertex<K> vertexStart = vertexMap.computeIfAbsent(start, Vertex::new);
        Vertex<K> vertexEnd = vertexMap.computeIfAbsent(end, Vertex::new);
        vertexStart.addOutEdge(vertexEnd, edge);
        if (!directed) {
            vertexEnd.addOutEdge(vertexStart, edge);
        }
        addEdgeListeners.removeIf(weakReference -> {
            IEdgeAdd<K> addEdge = weakReference.get();
            if (addEdge == null) {
                return true;
            }
            addEdge.onEdgeAdd(edge);
            return false;
        });
        System.out.println("add edge " + edge);
    }

    public boolean updateWeight(K start, K end, long weight) {
        Vertex<K> vertex = vertexMap.get(start);
        if (vertex == null) {
            return false;
        }
        IEdge<K> edge = vertex.outEdges.get(end);
        if (edge == null) {
            return false;
        }
        long oldWeight = edge.getWeight();
        edge.setWeight(weight);
        edgeUpdates.removeIf(weakReference -> {
            IEdgeUpdate<K> edgeUpdate = weakReference.get();
            if (edgeUpdate == null) {
                return true;
            }
            edgeUpdate.onEdgeUpdate(edge, oldWeight);
            return false;
        });
        return true;
    }

    public void walkVertex(Consumer<Vertex<K>> consumer) {
        vertexMap.values().forEach(consumer);
    }

    public IEdge<K> getEdge(K start, K end) {
        return getVertex(start).outEdges.get(end);
    }

    interface IEdgeAdd<K> {
        void onEdgeAdd(IEdge<K> edge);
    }

    interface IEdgeUpdate<K> {
        /**
         * 边的权重发生变化
         * @param edge 发生变化的边
         * @param oldWeight 变化前的权重
         */
        void onEdgeUpdate(IEdge<K> edge, long oldWeight);
    }
}
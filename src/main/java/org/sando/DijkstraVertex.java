package org.sando;

import java.util.Objects;

/**
 * dijkstra计算过程中使用的顶点
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/2
 */
class DijkstraVertex<K> extends BaseDijkVertex<K, DijkstraVertex<K>> {
    /**
     * 对应的顶点
     */
    public Vertex<K> vertex;
    /**
     * 当前顶点到起始顶点的距离
     */
    private long distance;


    public DijkstraVertex(Vertex<K> vertex) {
        this.vertex = Objects.requireNonNull(vertex);
        this.distance = Long.MAX_VALUE;
    }

    public Vertex<K> getVertex() {
        return vertex;
    }

    public long getDistance() {
        return distance;
    }

    public void setDistance(long distance) {
        this.distance = distance;
    }

    public void resetDistance() {
        this.distance = Long.MAX_VALUE;
    }

    @Override
    public long changeDistance(long diff) {
        this.distance += diff;
        return distance;
    }

    @Override
    public void changeDistanceRecursive(long diff) {
        if (diff == 0) {
            return;
        }
        changeDistance(diff);
        walkSuccessor(v -> {
            v.changeDistanceRecursive(diff);
        });
    }

    @Override
    public boolean isNotSelected() {
        return false;
    }

    @Override
    public String toString() {
        return "(" + vertex + "," + distance + ")";
    }
}

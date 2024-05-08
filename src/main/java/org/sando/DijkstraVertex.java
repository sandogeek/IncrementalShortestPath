package org.sando;

/**
 * dijkstra计算过程中使用的顶点
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/2
 */
class DijkstraVertex<K> extends BaseDijkVertex<K> {
    /**
     * 对应的顶点
     */
    public Vertex<K> vertex;
    /**
     * 当前顶点到起始顶点的距离
     */
    public long distance;


    public DijkstraVertex(Vertex<K> vertex) {
        this.vertex = vertex;
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
    public String toString() {
        return "(" + vertex + "," + distance + ")";
    }
}

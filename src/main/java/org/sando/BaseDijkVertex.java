package org.sando;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * dijk节点基类
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/5
 */
public abstract class BaseDijkVertex<K, V extends BaseDijkVertex<K, V>> {
    /**
     * 最短路径上的前驱顶点
     */
    private V previous;
    /**
     * 最短路径上的后继节点，图动态更新边的权重、增加、删除边时会使用到
     * 注意：这里使用链表，因为可能存在多条最短路径
     */
    private List<V> successorVertexList;
    /**
     * 节点状态，边的权重发生变化时使用
     */
    private int state;
    /**
     * 节点状态：在节点集M中
     */
    private static final int IN_M = 1;
    private static final int VISITED = 1 << 1;
    /**
     * 最小的权重变化
     */
    private ShortestPathTreeUpdater.EdgeDiff<K> minEdgeDiff;

    public boolean hasSuccessor() {
        return successorVertexList != null && !successorVertexList.isEmpty();
    }

    /**
     * 该顶点是否已经在最短路径上了
     */
    public boolean inShortestPath() {
        return previous != null;
    }

    void addSuccessor(V successorVertex) {
        if (successorVertexList == null) {
            successorVertexList = new ArrayList<>();
        }
        successorVertexList.add(successorVertex);
    }

    void removeSuccessor(V successorVertex) {
        if (successorVertexList != null) {
            successorVertexList.remove(successorVertex);
        }
    }

    public void walkSuccessor(Consumer<V> consumer) {
        if (successorVertexList == null) {
            return;
        }
        for (V successorVertex : new ArrayList<>(successorVertexList)) {
            if (successorVertex.isNotSelected()) {
                continue;
            }
            consumer.accept(successorVertex);
        }
    }

    public void walkSuccessorWithTmp(Consumer<V> consumer) {
        if (successorVertexList == null) {
            return;
        }
        for (V successorVertex : new ArrayList<>(successorVertexList)) {
            consumer.accept(successorVertex);
        }
    }

    public boolean isInM() {
        return (state & IN_M) > 0;
    }

    public boolean isVisited() {
        return (state & VISITED) > 0;
    }

    public void resetStateAndEdgeDiff() {
        state = 0;
        minEdgeDiff = null;
    }

    public void markInM() {
        state |= IN_M;
    }

    public void unmarkInM() {
        state &= ~IN_M;
    }

    public void markVisited() {
        state |= VISITED;
    }

    public V getPrevious() {
        if (isNotSelected()) {
            return null;
        }
        return previous;
    }

    public V getTmpPrevious() {
        return previous;
    }

    public abstract boolean isNotSelected();

    @SuppressWarnings("unchecked")
    public void changePrevious(V previous) {
        if (this.previous == previous) {
            return;
        }
        if (this.previous != null) {
            this.previous.removeSuccessor((V) this);
        }
        this.previous = previous;
        // 前驱节点不等于自身才需要在当前节点的前驱节点加入当前节点
        if (this.previous != this && this.previous != null) {
            this.previous.addSuccessor((V) this);
        }
    }

    public abstract long changeDistance(long diff);

    /**
     * 递归地修改当前节点以及后继节点地距离
     */
    public abstract void changeDistanceRecursive(long diff);

    public abstract long getDistance();

    public abstract Vertex<K> getVertex();

    public ShortestPathTreeUpdater.EdgeDiff<K> getMinEdgeDiff() {
        return minEdgeDiff;
    }

    /**
     * 替换minEdgeDiff,被替换的EdgeDiff可能会因为不再被需要而从Heap中移除
     * @param minEdgeDiff
     */
    public void replaceMinEdgeDiff(ShortestPathTreeUpdater.EdgeDiff<K> minEdgeDiff) {
        if (this.minEdgeDiff == minEdgeDiff) {
            return;
        }
        if (minEdgeDiff != null) {
            minEdgeDiff.incCount();
        }
        if (this.minEdgeDiff != null) {
            this.minEdgeDiff.decCount();
        }
        this.minEdgeDiff = minEdgeDiff;
    }
}

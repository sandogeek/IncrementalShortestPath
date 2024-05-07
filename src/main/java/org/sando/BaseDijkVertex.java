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
public abstract class BaseDijkVertex<K> {
    /**
     * 最短路径上的前驱顶点
     */
    private BaseDijkVertex<K> previous;
    /**
     * 最短路径上的后继节点，图动态更新边的权重、增加、删除边时会使用到
     * 注意：这里使用链表，因为可能存在多条最短路径
     */
    private List<BaseDijkVertex<K>> successorVertexList;
    /**
     * 是否是候选节点，边的权重发生变化时使用
     */
    private boolean waitSelect;
    /**
     * 最小的权重变化
     */
    ShortestPathTreeUpdater.EdgeDiff<K> minEdgeDiff;

    public boolean hasSuccessor() {
        return successorVertexList != null && !successorVertexList.isEmpty();
    }

    /**
     * 该顶点是否已经在最短路径上了
     */
    public boolean inShortestPath() {
        return previous != null;
    }

    private void addSuccessor(BaseDijkVertex<K> successorVertex) {
        if (successorVertexList == null) {
            successorVertexList = new ArrayList<>();
        }
        successorVertexList.add(successorVertex);
    }

    private void removeSuccessor(BaseDijkVertex<K> successorVertex) {
        if (successorVertexList != null) {
            successorVertexList.remove(successorVertex);
        }
    }

    public void walkSuccessor(Consumer<BaseDijkVertex<K>> consumer) {
        if (successorVertexList == null) {
            return;
        }
        for (BaseDijkVertex<K> successorVertex : successorVertexList) {
            consumer.accept(successorVertex);
        }
    }

    public boolean isWaitSelect() {
        return waitSelect;
    }

    public void resetWaitSelectAndEdgeDiff() {
        waitSelect = false;
        minEdgeDiff = null;
    }

    public void markWaitSelect() {
        waitSelect = true;
    }

    public BaseDijkVertex<K> getPrevious() {
        return previous;
    }

    public void changePrevious(BaseDijkVertex<K> previous) {
        if (this.previous != null) {
            this.previous.removeSuccessor(this);
        }
        this.previous = previous;
        // 前驱节点不等于自身才需要在当前节点的前驱节点加入当前节点
        if (this.previous != this && this.previous != null) {
            this.previous.addSuccessor(this);
        }
    }

    public abstract long changeDistance(long diff);

    public abstract long getDistance();

    public abstract Vertex<K> getVertex();
}

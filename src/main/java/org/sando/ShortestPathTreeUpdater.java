package org.sando;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 最短路径树更新器
 * <p>参考文献：<a href="https://citeseerx.ist.psu.edu/document?repid=rep1&type=pdf&doi=5802c2c7b31f6739d228d8af997bb4d19eda2597">...</a></p>
 * {@link ShortestPathTree.VertexIndex}构成的最短路径树T1，是当前不考虑其它节点的最短路径树,当节点未遍历完时，{@link DijkstraVertex}构成的路径树T2必定是T1
 * 的子树；当节点遍历完时，{@link DijkstraVertex}构成的路径树T2必定等于T1。因此，当T2不完整时，做路径更新时应使用T1。
 * 并且，在执行路径更新时，如果{@link ShortestPathTree.VertexIndex#selected}
 * 为true，则对{@link ShortestPathTree.VertexIndex}的路径更新也要更新到路径树T2中。
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/5
 */
public class ShortestPathTreeUpdater<K> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShortestPathTreeUpdater.class);
    private final ShortestPathTree<K> pathTree;
    private Map<K, ? extends BaseDijkVertex<K>> vertexMap;

    public ShortestPathTreeUpdater(ShortestPathTree<K> pathTree) {
        this.pathTree = pathTree;
    }

    public void edgeUpdate(IEdge<K> edge, long oldWeight) {
        if (!pathTree.complete) {
            ShortestPathTree<K>.DijkHeapWrapper heapWrapper = pathTree.heapWrapper;
            if (heapWrapper == null) {
                // 意味着当前最短路径树还是空的
                return;
            }
            vertexMap = heapWrapper.map;
        } else {
            vertexMap = pathTree.vertexMap;
        }
        long weight = edge.getWeight();
        if (weight == oldWeight) {
            return;
        }
        K start = edge.getStart();
        K end = edge.getEnd();
        long diff = weight - oldWeight;
        if (diff > 0) {
            // 权重增加
            BaseDijkVertex<K> startVertex = vertexMap.get(start);
            BaseDijkVertex<K> endVertex = vertexMap.get(end);
            if (endVertex.getPrevious() != startVertex) {
                // 说明这条边不在最短路径树上，不会对原来的最短路径树造成影响
                return;
            }
            // 递归收集后继节点，并更新最短路径值
            handleSuccessorAndSelfRecursive(endVertex, vertex -> {
                vertex.markWaitSelect();
            });
            QueueWrapper<K> queueWrapper = new QueueWrapper<>();
            handleDirectConnectInEdge(queueWrapper, endVertex);
            step5(queueWrapper);
        } else {
            // 权重减少
        }
        vertexMap = null;
    }

    /**
     * 论文中的步骤5
     */
    private void step5(QueueWrapper<K> queueWrapper) {
        while (!queueWrapper.isEmpty()) {
            EdgeDiff<K> poll = queueWrapper.poll();
            LOGGER.debug("选中最短路径:{}", poll);
            poll.end.changePrevious(poll.start);
            poll.end.resetWaitSelectAndEdgeDiff();
            handleSuccessorAndSelfRecursive(poll.end, vertex -> {
//                if (vertex.minEdgeDiff != poll) {
//                    throw new IllegalStateException();
//                }
                vertex.changeDistance(poll.diff);
                LOGGER.debug("更新最短路径距离:{}", vertex);
                List<EdgeDiff<K>> edgeDiffs = queueWrapper.getEdgeDiffByEnd(vertex);
                if (!edgeDiffs.isEmpty()) {
                    edgeDiffs.forEach(kEdgeDiff -> {
                        queueWrapper.removeEdgeDiff(kEdgeDiff, true);
                    });
                }
                vertex.resetWaitSelectAndEdgeDiff();
            });
            handleSuccessorAndSelfRecursive(poll.end, vertex -> {
                Map<K, IEdge<K>> outEdges = vertex.getVertex().outEdges;
                for (Map.Entry<K, IEdge<K>> entry : outEdges.entrySet()) {
                    BaseDijkVertex<K> end = vertexMap.get(entry.getKey());
                    IEdge<K> edge = entry.getValue();
                    if (!end.isWaitSelect()) {
                        LOGGER.debug("出边{}的终点:{}非候选节点，跳过", edge, end);
                        continue;
                    }
                    long distanceNew = vertex.getDistance() + edge.getWeight();
                    long distanceOld = end.getDistance();
                    long diff = distanceNew - distanceOld;
                    LOGGER.debug("处理出边：{} diff:{}", edge, diff);
                    if (diff < end.minEdgeDiff.diff) {
                        EdgeDiff<K> edgeDiff = queueWrapper.getEdgeDiff(vertex, end);
                        if (edgeDiff == null) {
                            end.minEdgeDiff = new EdgeDiff<>(vertex, end, diff);
                            queueWrapper.offer(end.minEdgeDiff);
                        } else {
                            // should not get here
                            if (diff < edgeDiff.diff) {
                                edgeDiff.diff = diff;
                                queueWrapper.priorityChange(edgeDiff.index, -1);
                            }
                        }
                    }
                }
            });
        }
    }


    private void handleDirectConnectInEdge(QueueWrapper<K> queueWrapper, BaseDijkVertex<K> end) {
        handleSuccessorAndSelfRecursive(end, vertex -> {
            BaseDijkVertex<K> parent = vertex.getPrevious();
            Long minDiff = parent.minEdgeDiff == null ?
                    null : parent.minEdgeDiff.diff;
            BaseDijkVertex<K> minEdgeDiffStart = null;
            EdgeDiff<K> minEdgeDiff = parent.minEdgeDiff;
            Map<K, IEdge<K>> inEdges = vertex.getVertex().inEdges;
            for (Map.Entry<K, IEdge<K>> entry : inEdges.entrySet()) {
                BaseDijkVertex<K> start = vertexMap.get(entry.getKey());
                if (start.getPrevious() == null) {
                    continue;
                }
                if (start.isWaitSelect()) {
                    continue;
                }
                long distanceNew = start.getDistance() + entry.getValue().getWeight();
                long distanceOld = vertex.getDistance();
                long diff = distanceNew - distanceOld;
                if (minDiff == null) {
                    minDiff = diff;
                    minEdgeDiffStart = start;
                } else if (diff < minDiff) {
                    minDiff = diff;
                    minEdgeDiffStart = start;
                }
            }
            if (minEdgeDiffStart != null) {
                minEdgeDiff = new EdgeDiff<>(minEdgeDiffStart, vertex, minDiff);
                queueWrapper.offer(minEdgeDiff);
            }
            vertex.minEdgeDiff = minEdgeDiff;
        });
    }

    private void handleDirectConnectOutEdge(QueueWrapper<K> queueWrapper, BaseDijkVertex<K> start) {
        handleSuccessorAndSelfRecursive(start, vertex -> {
            Map<K, IEdge<K>> outEdges = vertex.getVertex().outEdges;
            for (Map.Entry<K, IEdge<K>> entry : outEdges.entrySet()) {
                BaseDijkVertex<K> end = vertexMap.get(entry.getKey());
                if (!end.isWaitSelect()) {
                    continue;
                }
                long distanceNew = vertex.getDistance() + entry.getValue().getWeight();
                long distanceOld = end.getDistance();
                long diff = distanceNew - distanceOld;
                if (diff < 0) {
                    queueWrapper.offer(new EdgeDiff<>(vertex, end, diff));
                }
            }
        }, vertex -> vertex.isWaitSelect());
    }

    private void handleSuccessorAndSelfRecursive(BaseDijkVertex<K> vertexRoot,
                                                 Consumer<BaseDijkVertex<K>> vertexConsumer) {
        vertexConsumer.accept(vertexRoot);
        handleSuccessorRecursive(vertexRoot, vertexConsumer, null);
    }

    private void handleSuccessorAndSelfRecursive(BaseDijkVertex<K> vertexRoot,
                                                 Consumer<BaseDijkVertex<K>> vertexConsumer, Function<BaseDijkVertex<K>, Boolean> stopConusmeAndRecursive) {
        if (stopConusmeAndRecursive != null && stopConusmeAndRecursive.apply(vertexRoot)) {
            return;
        }
        vertexConsumer.accept(vertexRoot);
        handleSuccessorRecursive(vertexRoot, vertexConsumer, stopConusmeAndRecursive);
    }

    /**
     * 递归处理最短路径上的后继节点
     *
     * @param vertexRoot     起点
     * @param vertexConsumer 消费节点
     * @param stopRecursive  是否停止递归该节点的后继节点,并且该节点不会被消费
     */
    private void handleSuccessorRecursive(BaseDijkVertex<K> vertexRoot,
                                          Consumer<BaseDijkVertex<K>> vertexConsumer, Function<BaseDijkVertex<K>, Boolean> stopRecursive) {
        vertexRoot.walkSuccessor(vertex -> {
            if (stopRecursive != null && stopRecursive.apply(vertex)) {
                return;
            }
            vertexConsumer.accept(vertex);
            handleSuccessorRecursive(vertex, vertexConsumer, stopRecursive);
        });
    }

    public boolean checkAllReset() {
        Map<K, ? extends BaseDijkVertex<K>> vertexMap;
        if (!pathTree.complete) {
            ShortestPathTree<K>.DijkHeapWrapper heapWrapper = pathTree.heapWrapper;
            if (heapWrapper == null) {
                // 意味着当前最短路径树还是空的
                return true;
            }
            vertexMap = heapWrapper.map;
        } else {
            vertexMap = pathTree.vertexMap;
        }
        for (BaseDijkVertex<K> kBaseDijkVertex : vertexMap.values()) {
            boolean waitSelect = kBaseDijkVertex.isWaitSelect();
            if (waitSelect) {
                return false;
            }
            if (kBaseDijkVertex.minEdgeDiff != null) {
                return false;
            }
        }
        return true;
    }


    static class EdgeDiff<K> implements Comparable<EdgeDiff<K>>, IHeapIndex {
        BaseDijkVertex<K> start;
        BaseDijkVertex<K> end;
        private long diff;
        int index;

        @Override
        public void indexChange(Heap<?> heap, int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public int compareTo(EdgeDiff<K> o) {
            return (int) (diff - o.diff);
        }

        public EdgeDiff(BaseDijkVertex<K> start, BaseDijkVertex<K> end, long diff) {
            this.start = start;
            this.end = end;
            this.diff = diff;
        }

        @Override
        public String toString() {
            return "EdgeDiff{" +
                    "start=" + start +
                    ", end=" + end +
                    ", diff=" + diff +
                    '}';
        }
    }

    static class QueueWrapper<K> {
        private Heap<EdgeDiff<K>> queue = new Heap<>();
        private Map<BaseDijkVertex<K>, Map<BaseDijkVertex<K>, EdgeDiff<K>>> start2End = new HashMap<>();
        private Map<BaseDijkVertex<K>, Map<BaseDijkVertex<K>, EdgeDiff<K>>> end2Start = new HashMap<>();

        public void offer(EdgeDiff<K> edgeDiff) {
            queue.offer(edgeDiff);
            LOGGER.debug("增加edgeDiff:{}", edgeDiff);
            start2End.computeIfAbsent(edgeDiff.start, edge -> new HashMap<>()).put(edgeDiff.end, edgeDiff);
            end2Start.computeIfAbsent(edgeDiff.end, edge -> new HashMap<>()).put(edgeDiff.start, edgeDiff);
        }

        public EdgeDiff<K> poll() {
            EdgeDiff<K> edgeDiff = queue.poll();
            removeEdgeDiff(edgeDiff, false);
            return edgeDiff;
        }

        public void priorityChange(int index, int compareResult) {
            queue.priorityChange(index, compareResult);
        }

        public void removeContainVertex(BaseDijkVertex<K> vertex) {
            Map<BaseDijkVertex<K>, EdgeDiff<K>> map = start2End.get(vertex);
            if (map != null) {
                Iterator<EdgeDiff<K>> iterator = map.values().iterator();
                while (iterator.hasNext()) {
                    EdgeDiff<K> next = iterator.next();
                    queue.remove(next);
                    iterator.remove();
                    removeEdgeFromEnd(next);
                }
            }

            map = end2Start.get(vertex);
            if (map != null) {
                Iterator<EdgeDiff<K>> iterator = map.values().iterator();
                while (iterator.hasNext()) {
                    EdgeDiff<K> next = iterator.next();
                    queue.remove(next);
                    iterator.remove();
                    removeEdgeFromStart(next);
                }
            }
        }

        public void removeEdgeDiff(EdgeDiff<K> edgeDiff, boolean removeFromQueue) {
            if (removeFromQueue) {
                queue.remove(edgeDiff);
            }
            LOGGER.debug("移除edgeDiff:{}", edgeDiff);
            removeEdgeFromStart(edgeDiff);
            removeEdgeFromEnd(edgeDiff);
        }

        public void removeEdgeFromStart(EdgeDiff<K> edgeDiff) {
            Map<BaseDijkVertex<K>, EdgeDiff<K>> map = start2End.get(edgeDiff.start);
            if (map != null) {
                map.remove(edgeDiff.end);
            }
        }

        public void removeEdgeFromEnd(EdgeDiff<K> edgeDiff) {
            Map<BaseDijkVertex<K>, EdgeDiff<K>> map = end2Start.get(edgeDiff.end);
            if (map != null) {
                map.remove(edgeDiff.start);
            }
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }

        /**
         * 根据起点，终点获取队列中EdgeDiff
         */
        public EdgeDiff<K> getEdgeDiff(BaseDijkVertex<K> start, BaseDijkVertex<K> end) {
            Map<BaseDijkVertex<K>, EdgeDiff<K>> map = start2End.get(start);
            if (map != null) {
                return map.get(end);
            }
            return null;
        }

        /**
         * 根据起点，终点获取队列中EdgeDiff
         */
        public List<EdgeDiff<K>> getEdgeDiffByEnd(BaseDijkVertex<K> end) {
            List<EdgeDiff<K>> result = Collections.emptyList();
            Map<BaseDijkVertex<K>, EdgeDiff<K>> map = end2Start.get(end);
            if (map != null) {
                return new ArrayList<>(map.values());
            }
            return result;
        }

        public boolean containVertex(BaseDijkVertex<K> vertex) {
            if (start2End.containsKey(vertex)) {
                return true;
            }
            if (end2Start.containsKey(vertex)) {
                return true;
            }
            return false;
        }
    }
}

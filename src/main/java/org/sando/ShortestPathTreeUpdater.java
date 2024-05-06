package org.sando;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 最短路径树更新器
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/5
 */
public class ShortestPathTreeUpdater<K> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShortestPathTreeUpdater.class);
    private final ShortestPathTree<K> pathTree;
    private BaseDijkVertex<K> root;
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
            root = heapWrapper.root;
        } else {
            vertexMap = pathTree.vertexMap;
            root= pathTree.root;
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
            if (!endVertex.hasSuccessor()) {
                // 说明变动的是最短路径的最后一条边，不会影响最短路径上的节点
                endVertex.changeDistance(diff);
                return;
            }
            // 候选节点(节点集合N), 最短路径上的节点一定不会重复
            Set<BaseDijkVertex<K>> waitSelect = new HashSet<>();
            // 递归收集后继节点，并更新最短路径值
            handleSuccessorAndSelfRecursive(endVertex, vertex -> {
                vertex.changeDistance(diff);
                waitSelect.add(vertex);
                vertex.waitSelect = true;
            });
            QueueWrapper<K> queueWrapper = new QueueWrapper<>();
            queueWrapper.offer(new EdgeDiff<>(startVertex, endVertex, diff));
            int total = vertexMap.size();
            int selectedSize = total - waitSelect.size();
            // 查找已选节点到待选节点的直接相连的边
            if (selectedSize < waitSelect.size()) {
                // 已选节点比较少，通过查找已选节点的出边执行的次数可能会更少
                handleDirectConnectOutEdge(queueWrapper, root);
            } else {
                // 候选节点比较少，通过查找候选节点的入边执行的次数可能会更少
                handleDirectConnectInEdge(queueWrapper, endVertex);
            }
            step5(queueWrapper);
            waitSelect.forEach(dVertex -> {
                // 还原供下次权重变更时使用
                dVertex.waitSelect = false;
            });
        } else {
            // 权重减少
        }
    }

    /**
     * 论文中的步骤5
     */
    private void step5(QueueWrapper<K> queueWrapper) {
        while (!queueWrapper.isEmpty()) {
            EdgeDiff<K> poll = queueWrapper.poll();
            LOGGER.debug("选中最短路径:{}", poll);
            poll.end.changePrevious(poll.start);
            poll.end.waitSelect = false;
            queueWrapper.removeContainVertex(poll.end);
            if (poll.diff < 0) {
                handleSuccessorAndSelfRecursive(poll.end, vertex -> {
                    vertex.changeDistance(poll.diff);
                    LOGGER.debug("更新最短路径距离:{}", vertex);
                    // 更新队列中受影响的元素
                    List<EdgeDiff<K>> edgeDiffs = queueWrapper.getEdgeDiffByEnd(vertex);
                    edgeDiffs.forEach(edgeDiff -> {
                        edgeDiff.diff -= poll.diff;
                        if (edgeDiff.diff > 0) {
                            queueWrapper.removeEdgeDiff(edgeDiff, true);
                        } else {
                            queueWrapper.priorityChange(edgeDiff.index, 1);
                        }
                    });
                });
            }
            handleSuccessorAndSelfRecursive(poll.end, vertex -> {
                Map<K, IEdge<K>> outEdges = vertex.getVertex().outEdges;
                for (Map.Entry<K, IEdge<K>> entry : outEdges.entrySet()) {
                    BaseDijkVertex<K> end = vertexMap.get(entry.getKey());
                    IEdge<K> edge = entry.getValue();
                    if (!end.waitSelect) {
                        LOGGER.debug("出边{}的终点:{}非候选节点，跳过", edge, end);
                        continue;
                    }
                    long diff;
                    if (end.getPrevious() == vertex) {
                        diff = 0;
                    } else {
                        long distanceNew = vertex.getDistance() + edge.getWeight();
                        long distanceOld = end.getDistance();
                        diff = distanceNew - distanceOld;
                    }
                    LOGGER.debug("处理出边：{} diff:{}", edge, diff);
                    if (diff < 0) {
                        EdgeDiff<K> edgeDiff = queueWrapper.getEdgeDiff(vertex, end);
                        if (edgeDiff == null) {
                            queueWrapper.offer(new EdgeDiff<>(vertex, end, diff));
                        } else {
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
            Map<K, IEdge<K>> inEdges = vertex.getVertex().inEdges;
            for (Map.Entry<K, IEdge<K>> entry : inEdges.entrySet()) {
                BaseDijkVertex<K> start = vertexMap.get(entry.getKey());
                if (!start.waitSelect) {
                    long distanceNew = start.getDistance() + entry.getValue().getWeight();
                    long distanceOld = vertex.getDistance();
                    long diff = distanceNew - distanceOld;
                    if (diff < 0) {
                        queueWrapper.offer(new EdgeDiff<>(start, vertex, diff));
                    }
                }
            }
        });
    }

    private void handleDirectConnectOutEdge(QueueWrapper<K> queueWrapper, BaseDijkVertex<K> start) {
        handleSuccessorAndSelfRecursive(start, vertex -> {
            updateOutEdgeAndQueue(queueWrapper, vertex);
        }, vertex -> vertex.waitSelect);
    }

    private void updateOutEdgeAndQueue(QueueWrapper<K> queueWrapper, BaseDijkVertex<K> vertex) {
        Map<K, IEdge<K>> outEdges = vertex.getVertex().outEdges;
        for (Map.Entry<K, IEdge<K>> entry : outEdges.entrySet()) {
            BaseDijkVertex<K> end = vertexMap.get(entry.getKey());
            if (!end.waitSelect) {
                continue;
            }
            long distanceNew = vertex.getDistance() + entry.getValue().getWeight();
            long distanceOld = end.getDistance();
            long diff = distanceNew - distanceOld;
            if (diff < 0) {
                queueWrapper.offer(new EdgeDiff<>(vertex, end, diff));
            }
        }
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

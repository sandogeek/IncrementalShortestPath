package org.sando;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.sando.PathTreeHelper.handleSuccessorAndSelfRecursive;

/**
 * 最短路径树更新器
 * <p>参考文献：<a href="https://citeseerx.ist.psu.edu/document?repid=rep1&type=pdf&doi=5802c2c7b31f6739d228d8af997bb4d19eda2597">...</a></p>
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/5
 */
@SuppressWarnings("unchecked")
public class ShortestPathTreeUpdater<K> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShortestPathTreeUpdater.class);
    private final ShortestPathTree<K> pathTree;
    private Map<K, ? extends BaseDijkVertex<K, ?>> vertexMap;

    public ShortestPathTreeUpdater(ShortestPathTree<K> pathTree) {
        this.pathTree = pathTree;
        vertexMap = pathTree.vertexMap;
    }

    public <V extends BaseDijkVertex<K, V>> void edgeUpdate(IEdge<K> edge, long oldWeight) {
        if (!pathTree.complete) {
            throw new UnsupportedOperationException("ShortestPathTreeUpdater only support complete path tree");
        }
        long weight = edge.getWeight();
        if (weight == oldWeight) {
            return;
        }
        K start = edge.getStart();
        K end = edge.getEnd();
        V startVertex = (V) vertexMap.get(start);
        if (startVertex.getPrevious() == null) {
            // 该边起点不可达
            return;
        }
        V endVertex = (V) vertexMap.get(end);
        if (weight > oldWeight) {
            // 权重增加
            if (endVertex.getPrevious() != startVertex) {
                // 说明这条边不在最短路径树上，不会对原来的最短路径树造成影响
                return;
            }
            handleSuccessorAndSelfRecursive(endVertex, BaseDijkVertex::markWaitSelect);
            QueueWrapper<K> queueWrapper = new QueueWrapper<>();
            handleDirectInEdge(queueWrapper, endVertex);
            step3(queueWrapper);
        } else {
            // 权重减少
            long distanceNew = startVertex.getDistance() + edge.getWeight();
            long distanceOld = endVertex.getDistance();
            // D(i) + w'(e) < D(j)
            if (distanceNew >= distanceOld) {
                // 说明权值变小的边影响不到最短路径树
                return;
            }
            long diff = distanceNew - distanceOld;
            handleSuccessorAndSelfRecursive(endVertex, vertex -> {
                vertex.changeDistance(diff);
            });
            // P(j) = i
            endVertex.changePrevious(startVertex);
            QueueWrapper<K> queueWrapper = new QueueWrapper<>();
            handleOutEdge(queueWrapper,endVertex);
            step4(queueWrapper);
        }
    }

    private <V extends BaseDijkVertex<K, V>> void step4(QueueWrapper<K> queueWrapper) {
        while (!queueWrapper.isEmpty()) {
            EdgeDiff<K> poll = queueWrapper.poll();
            LOGGER.debug("选中最短路径:{}", poll);
            poll.end.changePrevious(poll.start);
            handleSuccessorAndSelfRecursive(poll.end, vertex -> {
                vertex.changeDistance(poll.diff);
                vertex.minEdgeDiff = null;
                LOGGER.debug("更新最短路径距离:{}", vertex);
                List<EdgeDiff<K>> edgeDiffs = queueWrapper.getEdgeDiffByEnd(vertex);
                if (!edgeDiffs.isEmpty()) {
                    edgeDiffs.forEach(kEdgeDiff -> {
                        queueWrapper.removeEdgeDiff(kEdgeDiff, true);
                    });
                }
            });
            handleOutEdge(queueWrapper, poll.end);
        }
    }

    private <V extends BaseDijkVertex<K, V>> void handleOutEdge(QueueWrapper<K> queueWrapper, V endVertex) {
        // 初始化所有出边
        handleSuccessorAndSelfRecursive(endVertex, start -> {
            Map<K, IEdge<K>> outEdges = start.getVertex().outEdges;
            for (Map.Entry<K, IEdge<K>> entry : outEdges.entrySet()) {
                V end = (V) vertexMap.get(entry.getKey());
                IEdge<K> edge = entry.getValue();
                if (end.getPrevious() == null) {
                    LOGGER.debug("出边{}的终点:{}不在最短路径上，跳过", edge, end);
                    continue;
                }
                if (end.getPrevious() == start) {
                    // 跳过最短路径上的边，因此此时start到end的距离diff必定为0
                    LOGGER.debug("跳过最短路径上的边:{}", edge);
                    continue;
                }
                long distanceNew = start.getDistance() + edge.getWeight();
                long distanceOld = end.getDistance();
                long diff = distanceNew - distanceOld;
                if (diff >= 0) {
                    continue;
                }

                if (end.minEdgeDiff == null) {
                    EdgeDiff<K> edgeDiff = new EdgeDiff<>(start, end, diff);
                    end.minEdgeDiff = edgeDiff;
                    queueWrapper.offer(edgeDiff);
                    continue;
                }
                if (diff < end.minEdgeDiff.diff) {
                    int index = end.minEdgeDiff.getIndex();
                    if (index != Heap.NOT_IN_HEAP) {
                        queueWrapper.removeEdgeDiff(end.minEdgeDiff, true);
                    }
                    EdgeDiff<K> edgeDiff = new EdgeDiff<>(start, end, diff);
                    end.minEdgeDiff = edgeDiff;
                    queueWrapper.offer(edgeDiff);
                }
            }
        });
    }

    /**
     * 论文中的步骤2
     */
    private <V extends BaseDijkVertex<K, V>> void step3(QueueWrapper<K> queueWrapper) {
        while (!queueWrapper.isEmpty()) {
            EdgeDiff<K> poll = queueWrapper.poll();
            LOGGER.debug("选中最短路径:{}", poll);
            poll.end.changePrevious(poll.start);
            poll.end.resetWaitSelectAndEdgeDiff();
            handleSuccessorAndSelfRecursive(poll.end, vertex -> {
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
            handleSuccessorAndSelfRecursive(poll.end, start -> {
                Map<K, IEdge<K>> outEdges = start.getVertex().outEdges;
                for (Map.Entry<K, IEdge<K>> entry : outEdges.entrySet()) {
                    V end = (V) vertexMap.get(entry.getKey());
                    IEdge<K> edge = entry.getValue();
                    if (!end.isWaitSelect()) {
                        LOGGER.debug("出边{}的终点:{}非候选节点，跳过", edge, end);
                        continue;
                    }
                    if (end.getPrevious() == null) {
                        LOGGER.debug("出边{}的终点:{}不在最短路径上，跳过", edge, end);
                        continue;
                    }
                    long distanceNew = start.getDistance() + edge.getWeight();
                    long distanceOld = end.getDistance();
                    long diff = distanceNew - distanceOld;
                    LOGGER.debug("处理出边：{} diff:{}", edge, diff);
                    if (diff < end.minEdgeDiff.diff) {
                        end.minEdgeDiff = new EdgeDiff<>(start, end, diff);
                        queueWrapper.offer(end.minEdgeDiff);
                    }
                }
            });
        }
    }


    private <V extends BaseDijkVertex<K, V>> void handleDirectInEdge(QueueWrapper<K> queueWrapper, V vertex) {
        // 文章中的des(j)
        handleSuccessorAndSelfRecursive(vertex, end -> {
            V parent = end.getPrevious();
            Long minDiff = parent.minEdgeDiff == null ?
                    null : parent.minEdgeDiff.diff;
            V minEdgeDiffStart = null;
            EdgeDiff<K> minEdgeDiff = parent.minEdgeDiff;
            Map<K, IEdge<K>> inEdges = end.getVertex().inEdges;
            for (Map.Entry<K, IEdge<K>> entry : inEdges.entrySet()) {
                V start = (V) vertexMap.get(entry.getKey());
                if (start.getPrevious() == null) {
                    continue;
                }
                if (start.isWaitSelect()) {
                    continue;
                }
                long distanceNew = start.getDistance() + entry.getValue().getWeight();
                long distanceOld = end.getDistance();
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
                minEdgeDiff = new EdgeDiff<>(minEdgeDiffStart, end, minDiff);
                queueWrapper.offer(minEdgeDiff);
            }
            end.minEdgeDiff = minEdgeDiff;
        });
    }

    public boolean checkAllReset() {
        for (BaseDijkVertex<K, ?> kBaseDijkVertex : vertexMap.values()) {
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


    @SuppressWarnings("rawtypes")
    static class EdgeDiff<K> implements Comparable<EdgeDiff<K>>, IHeapIndex {
        BaseDijkVertex start;
        BaseDijkVertex end;
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

        public EdgeDiff(BaseDijkVertex start, BaseDijkVertex end, long diff) {
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
        private Map<BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>>, Map<BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>>, EdgeDiff<K>>> start2End = new HashMap<>();
        private Map<BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>>, Map<BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>>, EdgeDiff<K>>> end2Start = new HashMap<>();

        @SuppressWarnings("unchecked")
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

        public void removeContainVertex(BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>> vertex) {
            Map<BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>>, EdgeDiff<K>> map = start2End.get(vertex);
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

        private void removeEdgeFromStart(EdgeDiff<K> edgeDiff) {
            Map<BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>>, EdgeDiff<K>> map = start2End.get(edgeDiff.start);
            if (map != null) {
                map.remove(edgeDiff.end);
            }
        }

        private void removeEdgeFromEnd(EdgeDiff<K> edgeDiff) {
            Map<BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>>, EdgeDiff<K>> map = end2Start.get(edgeDiff.end);
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
        public EdgeDiff<K> getEdgeDiff(BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>> start, BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>> end) {
            Map<BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>>, EdgeDiff<K>> map = start2End.get(start);
            if (map != null) {
                return map.get(end);
            }
            return null;
        }

        /**
         * 根据起点，终点获取队列中EdgeDiff
         */
        public List<EdgeDiff<K>> getEdgeDiffByEnd(BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>> end) {
            List<EdgeDiff<K>> result = Collections.emptyList();
            Map<BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>>, EdgeDiff<K>> map = end2Start.get(end);
            if (map != null) {
                return new ArrayList<>(map.values());
            }
            return result;
        }

        public boolean containVertex(BaseDijkVertex<K, ? extends BaseDijkVertex<K, ?>> vertex) {
            if (start2End.containsKey(vertex)) {
                return true;
            }
            return end2Start.containsKey(vertex);
        }
    }
}

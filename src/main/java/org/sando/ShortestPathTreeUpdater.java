package org.sando;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiPredicate;

import static org.sando.PathTreeHelper.handleSuccessorAndSelfRecursive;

/**
 * 最短路径树更新器
 * <p>参考文献：</p>
 * <a href="https://web.archive.org/web/20151016171618id_/http://www4.comp.polyu.edu.hk/~csbxiao/paper/2007/JCN-SPT.pdf">
 * An Efficient Algorithm for Dynamic Shortest Path Tree Update in Network Routing</a>
 * <a href="https://dl.acm.org/doi/pdf/10.1109/90.893870">New dynamic algorithms for shortest path tree computation</a>
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
    /**
     * 多边权重变更，是否合并更新
     */
    private boolean mergeUpdate;
    private Map<IEdge<K>, Long> incMap;
    private Map<IEdge<K>, Long> decMap;

    public ShortestPathTreeUpdater(ShortestPathTree<K> pathTree, boolean mergeUpdate) {
        this.pathTree = pathTree;
        vertexMap = pathTree.vertexMap;
        this.mergeUpdate = mergeUpdate;
        if (mergeUpdate) {
            incMap = new HashMap<>();
            decMap = new HashMap<>();
        }
    }

    private static <K, V extends BaseDijkVertex<K, V>> boolean decFilter(V start, V end) {
        boolean check = end.getPrevious() == start;
        if (check) {
            // 跳过最短路径上的边，因此此时start到end的距离diff必定为0
            LOGGER.debug("跳过最短路径上的边:{} {}", start, end);
        }
        return check;
    }

    /**
     * 边权重更新
     */
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
            if (mergeUpdate) {
                incMap.putIfAbsent(edge, oldWeight);
                return;
            }
            handleSuccessorAndSelfRecursive(endVertex, vertex -> {
                vertex.markInM();
                vertex.changeDistance(weight - oldWeight);
            });
            QueueWrapper<K> queueWrapper = new QueueWrapper<>();
            handleDirectInEdge(queueWrapper, endVertex);
            pollUntilEmpty(queueWrapper, (BiPredicate<V, V>) ShortestPathTreeUpdater::incFilter);
            handleSuccessorAndSelfRecursive(endVertex, BaseDijkVertex::resetStateAndEdgeDiff);
        } else {
            // 权重减少
            if (mergeUpdate) {
                decMap.put(edge, oldWeight);
                return;
            }
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
            handleOutEdge(queueWrapper, endVertex, ShortestPathTreeUpdater::decFilter);
            pollUntilEmpty(queueWrapper, (BiPredicate<V, V>) ShortestPathTreeUpdater::decFilter);
        }
    }

    /**
     * 尝试合并更新
     */
    public <V extends BaseDijkVertex<K, V>> void tryMergeUpdate() {
        if (!mergeUpdate) {
            return;
        }
        if (incMap.isEmpty() && decMap.isEmpty()) {
            return;
        }
        QueueWrapper<K> queueWrapper = new QueueWrapper<>();
        mergeUpdateInc(queueWrapper);
    }

    private <V extends BaseDijkVertex<K, V>> void mergeUpdateInc(QueueWrapper<K> queueWrapper) {
        Set<V> mSet = new HashSet<>();
        for (Map.Entry<IEdge<K>, Long> pair : incMap.entrySet()) {
            IEdge<K> edge = pair.getKey();
            Long oldWeight = pair.getValue();
            long weight = edge.getWeight();
            long diff = weight - oldWeight;
            V endVertex = (V) vertexMap.get(edge.getEnd());
            handleSuccessorAndSelfRecursive(endVertex, vertex -> {
                vertex.markInM();
                vertex.changeDistance(diff);
                LOGGER.debug("节点进入M集合:{}", vertex);
                mSet.add(vertex);
            });
        }
        for (Map.Entry<IEdge<K>, Long> pair : incMap.entrySet()) {
            V endVertex = (V) vertexMap.get(pair.getKey().getEnd());
            handleDirectInEdge(queueWrapper, endVertex);
        }
        pollUntilEmpty(queueWrapper, (BiPredicate<V, V>) ShortestPathTreeUpdater::incFilter);
        mSet.forEach(BaseDijkVertex::resetStateAndEdgeDiff);
        incMap.clear();
    }

    private <V extends BaseDijkVertex<K, V>> void pollUntilEmpty(QueueWrapper<K> queueWrapper, BiPredicate<V, V> edgeFilter) {
        while (!queueWrapper.isEmpty()) {
            EdgeDiff<K> poll = queueWrapper.poll();
            LOGGER.debug("选中最短路径:{}", poll);
            if (poll.diff != 0) {
                poll.end.changePrevious(poll.start);
            }
            handleSuccessorAndSelfRecursive(poll.end, vertex -> {
                if (poll.diff != 0) {
                    vertex.changeDistance(poll.diff);
                    LOGGER.debug("更新最短路径距离:{}", vertex);
                }
                vertex.minEdgeDiff = null;
                List<EdgeDiff<K>> edgeDiffs = queueWrapper.getEdgeDiffByEnd(vertex);
                if (!edgeDiffs.isEmpty()) {
                    edgeDiffs.forEach(kEdgeDiff -> {
                        queueWrapper.removeEdgeDiff(kEdgeDiff, true);
                    });
                }
            });
            handleOutEdge(queueWrapper, (V) poll.end, edgeFilter);
        }
    }

    private <V extends BaseDijkVertex<K, V>> void handleOutEdge(QueueWrapper<K> queueWrapper, V endVertex,
                                                                BiPredicate<V, V> edgeFilter) {
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
                if (edgeFilter.test(start, end)) {
                    continue;
                }
                long distanceNew = start.getDistance() + edge.getWeight();
                long distanceOld = end.getDistance();
                long diff = distanceNew - distanceOld;
                if (diff >= 0) {
                    continue;
                }

                if (end.minEdgeDiff == null || diff < end.minEdgeDiff.diff) {
                    EdgeDiff<K> edgeDiff = new EdgeDiff<>(start, end, diff);
                    end.minEdgeDiff = edgeDiff;
                    queueWrapper.offer(edgeDiff);
                }
            }
        });
    }

    private static <K, V extends BaseDijkVertex<K, V>> boolean incFilter(V start, V end) {
        boolean check = !end.isInM();
        if (check) {
            LOGGER.debug("起点：{} 终点:{},终点非候选节点，跳过", start, end);
        }
        return check;
    }


    private <V extends BaseDijkVertex<K, V>> void handleDirectInEdge(QueueWrapper<K> queueWrapper, V vertex) {
        // 文章中的des(j)
        handleSuccessorAndSelfRecursive(vertex, end -> {
            if (end.isVisited()) {
                return;
            }
            end.markVisited();
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
                if (start.isInM()) {
                    continue;
                }
                long distanceNew = start.getDistance() + entry.getValue().getWeight();
                long distanceOld = end.getDistance();
                long diff = distanceNew - distanceOld;
                if (diff > 0) {
                    continue;
                }
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
            boolean waitSelect = kBaseDijkVertex.isInM();
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
            if (diff == o.diff) {
                return (int) (end.getDistance() - o.end.getDistance());
            }
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

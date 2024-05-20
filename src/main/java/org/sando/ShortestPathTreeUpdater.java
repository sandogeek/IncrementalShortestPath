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
    /**
     * 多边权重变更，是否合并更新
     */
    private boolean mergeUpdate;
    private Map<IEdge<K>, Long> changeMap;

    public ShortestPathTreeUpdater(ShortestPathTree<K> pathTree, boolean mergeUpdate) {
        this.pathTree = pathTree;
        this.mergeUpdate = mergeUpdate;
        if (mergeUpdate) {
            changeMap = new HashMap<>();
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
//        if (!pathTree.complete) {
//            throw new UnsupportedOperationException("ShortestPathTreeUpdater only support complete path tree");
//        }
        long weight = edge.getWeight();
        if (weight == oldWeight) {
            return;
        }
        K start = edge.getStart();
        K end = edge.getEnd();
        V startVertex = (V) getVertexMap().get(start);
        if (startVertex.getPrevious() == null) {
            // 该边起点不可达
            return;
        }
        if (mergeUpdate) {
            Long old = changeMap.putIfAbsent(edge, oldWeight);
            if (old != null && edge.getWeight() == old) {
                changeMap.remove(edge);
            }
            return;
        }
        V endVertex = (V) getVertexMap().get(end);
        if (weight > oldWeight) {
            // 权重增加
            if (endVertex.getPrevious() != startVertex) {
                // 说明这条边不在最短路径树上，不会对原来的最短路径树造成影响
                return;
            }
            long diff = weight - oldWeight;
            endVertex.changeDistanceRecursive(diff);
            handleSuccessorAndSelfRecursive(endVertex, vertex -> {
                vertex.markInM();
            });
            QueueWrapper<K> queueWrapper = new QueueWrapper<>();
            handleDirectInEdge(queueWrapper, endVertex);
            pollUntilEmpty(queueWrapper, (BiPredicate<V, V>) ShortestPathTreeUpdater::incFilter);
            handleSuccessorAndSelfRecursive(endVertex, BaseDijkVertex::resetStateAndEdgeDiff);
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
            handleOutEdge(queueWrapper, endVertex, ShortestPathTreeUpdater::decFilter);
            pollUntilEmpty(queueWrapper, (BiPredicate<V, V>) ShortestPathTreeUpdater::decFilter);
        }
    }

    private Map<K, ? extends BaseDijkVertex<K, ?>> getVertexMap() {
        if (!pathTree.complete) {
            return pathTree.heapWrapper.map;
        } else {
            return pathTree.vertexMap;
        }
    }

    /**
     * 尝试合并更新
     */
    public <V extends BaseDijkVertex<K, V>> void tryMergeUpdate() {
        if (!mergeUpdate) {
            return;
        }
        if (changeMap.isEmpty()) {
            return;
        }
        QueueWrapper<K> queueWrapper = new QueueWrapper<>();
        mergeUpdateDec(queueWrapper);
        mergeUpdateInc(queueWrapper);
    }

    private <V extends BaseDijkVertex<K, V>> void mergeUpdateDec(QueueWrapper<K> queueWrapper) {
        List<Map.Entry<IEdge<K>, Long>> decList = new ArrayList<>();
        changeMap.entrySet().removeIf(pair -> {
            IEdge<K> edge = pair.getKey();
            Long oldWeight = pair.getValue();
            if (edge.getWeight() < oldWeight) {
                decList.add(Pair.of(edge, oldWeight));
                return true;
            }
            return false;
        });
        Map<K, ? extends BaseDijkVertex<K, ?>> vertexMap = getVertexMap();
        Iterator<Map.Entry<IEdge<K>, Long>> iterator = decList.iterator();
        while (iterator.hasNext()) {
            Map.Entry<IEdge<K>, Long> entry = iterator.next();
            IEdge<K> edge = entry.getKey();
            V startVertex = (V) vertexMap.get(edge.getStart());
            V endVertex = (V) vertexMap.get(edge.getEnd());
            long distanceNew = startVertex.getDistance() + edge.getWeight();
            long distanceOld = endVertex.getDistance();
            // D(i) + w'(e) < D(j)
            if (distanceNew >= distanceOld) {
                // 说明权值变小的边影响不到最短路径树
                iterator.remove();
                LOGGER.debug("跳过变动边{}", edge);
                continue;
            }
            long diff = distanceNew - distanceOld;
            endVertex.changeDistanceRecursive(diff);
            // P(j) = i
            endVertex.changePrevious(startVertex);
        }
        for (Map.Entry<IEdge<K>, Long> entry : decList) {
            IEdge<K> edge = entry.getKey();
            V endVertex = (V) vertexMap.get(edge.getEnd());
            handleOutEdge(queueWrapper, endVertex, ShortestPathTreeUpdater::decFilter);
        }
        pollUntilEmpty(queueWrapper, (BiPredicate<V, V>) ShortestPathTreeUpdater::decFilter);
        if (!queueWrapper.isEmpty()) {
            queueWrapper.clear();
        }
    }

    private <V extends BaseDijkVertex<K, V>> void mergeUpdateInc(QueueWrapper<K> queueWrapper) {
        Set<V> mSet = new HashSet<>();
        Map<K, ? extends BaseDijkVertex<K, ?>> vertexMap = getVertexMap();
        for (Map.Entry<IEdge<K>, Long> pair : changeMap.entrySet()) {
            IEdge<K> edge = pair.getKey();
            V startVertex = (V) vertexMap.get(edge.getStart());
            V endVertex = (V) vertexMap.get(edge.getEnd());
            if (endVertex.getPrevious() != startVertex) {
                // 说明这条边不在最短路径树上，不会对原来的最短路径树造成影响
                return;
            }
            Long oldWeight = pair.getValue();
            long weight = edge.getWeight();
            long diff = weight - oldWeight;
            endVertex.changeDistanceRecursive(diff);
            handleSuccessorAndSelfRecursive(endVertex, vertex -> {
                vertex.markInM();
                LOGGER.debug("节点进入M集合:{}", vertex);
                mSet.add(vertex);
            });
        }
        for (Map.Entry<IEdge<K>, Long> pair : changeMap.entrySet()) {
            V endVertex = (V) vertexMap.get(pair.getKey().getEnd());
            handleDirectInEdge(queueWrapper, endVertex);
        }
        pollUntilEmpty(queueWrapper, (BiPredicate<V, V>) ShortestPathTreeUpdater::incFilter);
        mSet.forEach(BaseDijkVertex::resetStateAndEdgeDiff);
        changeMap.clear();
    }

    private <V extends BaseDijkVertex<K, V>> void pollUntilEmpty(QueueWrapper<K> queueWrapper, BiPredicate<V, V> edgeFilter) {
        while (!queueWrapper.isEmpty()) {
            EdgeDiff<K> poll = queueWrapper.poll();
            LOGGER.debug("选中最短路径:{}", poll);
            if (poll.diff != 0) {
                poll.end.changePrevious(poll.start);
            }
            if (poll.diff != 0) {
                poll.end.changeDistanceRecursive(poll.diff);
            }
            handleSuccessorAndSelfRecursive(poll.end, vertex -> {
                vertex.replaceMinEdgeDiff(null);
            });
            handleOutEdge(queueWrapper, (V) poll.end, edgeFilter);
        }
    }

    private <V extends BaseDijkVertex<K, V>> void handleOutEdge(QueueWrapper<K> queueWrapper, V endVertex,
                                                                BiPredicate<V, V> edgeFilter) {
        // 初始化所有出边
        Map<K, ? extends BaseDijkVertex<K, ?>> vertexMap = getVertexMap();
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

                EdgeDiff<K> minEdgeDiff = end.getMinEdgeDiff();
                if (minEdgeDiff == null || diff < minEdgeDiff.diff) {
                    EdgeDiff<K> edgeDiff = new EdgeDiff<>(start, end, diff);
                    end.replaceMinEdgeDiff(edgeDiff);
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
        Map<K, ? extends BaseDijkVertex<K, ?>> vertexMap = getVertexMap();
        handleSuccessorAndSelfRecursive(vertex, end -> {
            end.markVisited();
            V parent = end.getPrevious();
            Long minDiff = parent.getMinEdgeDiff() == null ?
                    null : parent.getMinEdgeDiff().diff;
            V minEdgeDiffStart = null;
            EdgeDiff<K> minEdgeDiff = parent.getMinEdgeDiff();
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
            end.replaceMinEdgeDiff(minEdgeDiff);
        }, BaseDijkVertex::isVisited);
    }

    public boolean checkAllReset() {
        for (BaseDijkVertex<K, ?> kBaseDijkVertex : getVertexMap().values()) {
            boolean waitSelect = kBaseDijkVertex.isInM();
            if (waitSelect) {
                return false;
            }
            if (kBaseDijkVertex.getMinEdgeDiff() != null) {
                return false;
            }
        }
        return true;
    }


    static class QueueWrapper<K> {
        private Heap<EdgeDiff<K>> queue = new Heap<>();

        @SuppressWarnings("unchecked")
        public void offer(EdgeDiff<K> edgeDiff) {
            queue.offer(edgeDiff);
            LOGGER.debug("增加edgeDiff:{}", edgeDiff);
        }

        public EdgeDiff<K> poll() {
            EdgeDiff<K> edgeDiff = queue.poll();
            return edgeDiff;
        }

        public void clear() {
            queue.clear();
        }

        public void priorityChange(int index, int compareResult) {
            queue.priorityChange(index, compareResult);
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }
}

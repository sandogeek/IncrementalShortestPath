package org.sando;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sando
 * @version 1.0
 * @since 2024/5/19
 */
@SuppressWarnings("rawtypes")
class EdgeDiff<K> implements Comparable<EdgeDiff<K>>, IHeapIndex {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdgeDiff.class);
    BaseDijkVertex start;
    BaseDijkVertex end;
    long diff;
    private int index = Heap.NOT_IN_HEAP;
    private Heap<?> heap;
    /**
     * 持有当前对象的{@link BaseDijkVertex}数量
     */
    private int count;

    @Override
    public void indexChange(Heap<?> heap, int index) {
        this.index = index;
        if (this.heap != null && this.heap != heap) {
            throw new IllegalArgumentException("should not put this into two heap");
        }
        if (index == Heap.NOT_IN_HEAP) {
            LOGGER.debug("移除edgeDiff:{}", this);
        }
        this.heap = heap;
    }

    public void remove() {
        if (index == Heap.NOT_IN_HEAP) {
            return;
        }
        heap.remove(this);
    }

    @Override
    public int getIndex() {
        return index;
    }

    public void incCount() {
        count++;
    }

    public void decCount() {
        count--;
        if (count <= 0) {
            remove();
        }
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

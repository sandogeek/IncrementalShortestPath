package org.sando;

import org.sando.heap.fiboheap.IFiboHeap;
import org.sando.heap.fiboheap.IFiboHeapAware;
import org.sando.heap.fiboheap.IHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 走某条边给距离带来的变动
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/19
 */
@SuppressWarnings("rawtypes")
class EdgeDiff<K> implements Comparable<EdgeDiff<K>>, IFiboHeapAware<EdgeDiff<K>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdgeDiff.class);
    BaseDijkVertex start;
    BaseDijkVertex end;
    long diff;
    private IFiboHeap<EdgeDiff<K>> heap;
    private IHandle<EdgeDiff<K>> handle;
    /**
     * 持有当前对象的{@link BaseDijkVertex}数量
     */
    private int count;

    @Override
    public IFiboHeap<EdgeDiff<K>> getHeap() {
        return heap;
    }

    @Override
    public void setHeap(IFiboHeap<EdgeDiff<K>> heap) {
        this.heap = heap;
    }

    @Override
    public IHandle<EdgeDiff<K>> getHandle() {
        return handle;
    }

    @Override
    public void setHandle(IHandle<EdgeDiff<K>> handle) {
        this.handle = handle;
    }

    public void remove() {
        if (heap == null) {
            return;
        }
        heap.delete(handle);
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

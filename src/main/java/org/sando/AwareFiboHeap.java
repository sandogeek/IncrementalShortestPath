package org.sando;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;

/**
 * key可感知出堆/入堆的并支持increase/decrease的斐波那契堆
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/28
 */
public class AwareFiboHeap<Key extends AwareFiboHeap.IFiboHeapAware<Key>> extends AbstractQueue<Key> {
    private FiboHeap<Key> heap;

    public AwareFiboHeap() {
        heap = new FiboHeap<>();
    }

    public AwareFiboHeap(int initialCapacity) {
        heap = new FiboHeap<>(initialCapacity);
    }

    public AwareFiboHeap(int initialCapacity, Comparator<? super Key> comp) {
        heap = new FiboHeap<>(initialCapacity, comp);
    }

    public void insert(Key key) {
        FiboHeap.Entry<Key> entry = heap.insert(key);
        key.aware(heap, entry);
    }

    public void union(FiboHeap<Key> other) {
        other.forEach(key -> {
            key.union(heap);
        });
        heap.union(other);
    }

    public Key extractMin() {
        Key key = heap.extractMin();
        key.aware(null, null);
        return key;
    }

    public Key minKey() {
        return heap.minKey();
    }

    public void decreaseKey(FiboHeap.Entry<Key> entry) {
        heap.decreaseKey(entry);
    }

    public void increaseKey(FiboHeap.Entry<Key> entry) {
        heap.increaseKey(entry);
    }

    public void delete(FiboHeap.Entry<Key> entry) {
        Key key = entry.key;
        heap.delete(entry);
        key.aware(null, null);
    }

    public void print() {
        heap.print();
    }

    @Override
    public boolean offer(Key key) {
        return heap.offer(key);
    }

    @Override
    public Key poll() {
        return heap.poll();
    }

    @Override
    public Key peek() {
        return heap.peek();
    }

    @Override
    public int size() {
        return heap.size();
    }

    @Override
    public void clear() {
        heap.clear();
    }

    @Override
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    @Override
    public Iterator<Key> iterator() {
        return heap.iterator();
    }

    /**
     * 斐波那契堆Key出/入堆感知接口
     */
    @SuppressWarnings("unchecked")
    interface IFiboHeapAware<Key extends IFiboHeapAware<Key>> {
        /**
         * 当Key出/入堆时调用,用于知道自己入堆和出堆
         *
         * @param heap 入堆时非null，出堆时为null
         */
        default void aware(FiboHeap<Key> heap, FiboHeap.Entry<Key> entry) {
            setHeap(heap);
            setEntry(entry);
        }

        /**
         * 被union到一个新的堆
         *
         * @param heap 新堆
         */
        default void union(FiboHeap<Key> heap) {
            setHeap(heap);
        }

        /**
         * 获取对应的堆
         */
        FiboHeap<Key> getHeap();

        void setHeap(FiboHeap<Key> heap);

        /**
         * 获取key对应的entry
         */
        FiboHeap.Entry<Key> getEntry();

        void setEntry(FiboHeap.Entry<Key> entry);

        /**
         * key变大
         */
        default void increaseKey() {
            FiboHeap<Key> heap = getHeap();
            if (heap == null) {
                return;
            }
            heap.increaseKey(getEntry());
        }

        /**
         * key变小
         */
        default void decreaseKey() {
            FiboHeap<Key> heap = getHeap();
            if (heap == null) {
                return;
            }
            getHeap().decreaseKey(getEntry());
        }
    }
}

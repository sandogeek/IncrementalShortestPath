package org.sando.heap.fiboheap;

/**
 * 斐波那契堆Key出/入堆感知接口
 */
@SuppressWarnings("unchecked")
public interface IFiboHeapAware<Key extends IFiboHeapAware<Key>> {
    /**
     * 当Key出/入堆时调用,用于知道自己入堆和出堆
     *
     * @param heap 入堆时非null，出堆时为null
     */
    default void aware(IFiboHeap<Key> heap, Entry<Key> entry) {
        setHeap(heap);
        setEntry(entry);
    }

    /**
     * 被union到一个新的堆
     *
     * @param heap 新堆
     */
    default void union(IFiboHeap<Key> heap) {
        setHeap(heap);
    }

    /**
     * 获取对应的堆
     */
    IFiboHeap<Key> getHeap();

    void setHeap(IFiboHeap<Key> heap);

    /**
     * 获取key对应的entry
     */
    Entry<Key> getEntry();

    void setEntry(Entry<Key> entry);

    /**
     * key变大
     */
    default void increaseKey() {
        if (getHeap() == null) {
            return;
        }
        NormalFiboHeap<Key> heap = (NormalFiboHeap<Key>)getHeap();
        heap.increaseKey(getEntry());
    }

    /**
     * key变小
     */
    default void decreaseKey() {
        if (getHeap() == null) {
            return;
        }
        NormalFiboHeap<Key> heap = (NormalFiboHeap<Key>)getHeap();
        heap.decreaseKey(getEntry());
    }
}

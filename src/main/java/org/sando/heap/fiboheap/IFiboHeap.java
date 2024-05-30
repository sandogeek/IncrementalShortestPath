package org.sando.heap.fiboheap;

import org.sando.heap.IHeap;

/**
 * @author Sando
 * @version 1.0
 * @since 2024/5/28
 */
public interface IFiboHeap<Key> extends IHeap<Key> {
    /**
     * 将other合并到当前堆中，注意：此操作会导致other被清空。
     */
    void union(IFiboHeap<Key> other);
    /**
     * 移除指定节点，为了效率，不检查节点是否属于当前堆
     *
     * @param handle 被移除的节点
     */
    void delete(IHandle<Key> handle);

    void print();
}

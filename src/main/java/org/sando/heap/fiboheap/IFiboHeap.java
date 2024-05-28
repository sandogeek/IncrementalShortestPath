package org.sando.heap.fiboheap;

/**
 * @author Sando
 * @version 1.0
 * @since 2024/5/28
 */
public interface IFiboHeap<Key> {
    /**
     * 插入一个key。斐波那契堆的根链表是"双向链表"，这里将minimum节点看作双向联表的表头
     *
     * @param key 被插入的key
     */
    Entry<Key> insert(Key key);

    /**
     * 将other合并到当前堆中，注意：此操作会导致other被清空。
     */
    void union(IFiboHeap<Key> other);

    /**
     * 取出最小节点
     *
     * @return 最小节点, 如果堆为空，则返回null
     */
    Key extractMin();

    /**
     * 清空当前堆
     */
    void clear();

    /**
     * 获取堆中最小的key
     *
     * @return 堆中最小的key，如果堆为空，则返回null
     */
    Key minKey();

    /**
     * 移除指定节点，为了效率，不检查节点是否属于当前堆
     *
     * @param entry 被移除的节点
     */
    void delete(Entry<Key> entry);

    /**
     * 堆是否为空
     *
     * @return 如果为空返回true，否则返回false
     */
    boolean isEmpty();

    void print();
}

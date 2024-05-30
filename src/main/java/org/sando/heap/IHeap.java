package org.sando.heap;

import org.sando.heap.fiboheap.IHandle;

/**
 * 堆接口
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/30
 */
public interface IHeap<Key> {
    /**
     * 插入一个key。斐波那契堆的根链表是"双向链表"，这里将minimum节点看作双向联表的表头
     *
     * @param key 被插入的key
     */
    IHandle<Key> insert(Key key);

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
     * 堆是否为空
     *
     * @return 如果为空返回true，否则返回false
     */
    boolean isEmpty();
}

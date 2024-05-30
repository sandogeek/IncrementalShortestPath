package org.sando.heap.fiboheap;

/**
 * 句柄
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/31
 */
public interface IHandle<Key> {
    /**
     * 获取句柄中存放的key
     */
    Key getKey();
}

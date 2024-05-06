package org.sando;

/**
 * 堆中数组的下标变化感知接口
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/2
 */
public interface IHeapIndex {
    /**
     * 下标变化,如果为{@link Heap#NOT_IN_HEAP}意味着元素被移除了
     *
     * @param index 最新下标
     */
    void indexChange(Heap<?> heap, int index);

    /**
     * 获取当前下标
     * @return 当前下标
     */
    int getIndex();
}

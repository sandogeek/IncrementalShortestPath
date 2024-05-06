package org.sando;

/**
 * 边
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/2
 */
public interface IEdge<K> {
    K getStart();

    K getEnd();

    long getWeight();

    /**
     * 请勿直接调用该方法，应使用{@link Graph#updateWeight(Object, Object, long)}方法
     */
    void setWeight(long weight);
}

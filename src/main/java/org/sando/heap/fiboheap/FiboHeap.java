package org.sando.heap.fiboheap;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Queue;

/**
 * 斐波那契堆
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/28
 */
@SuppressWarnings(value = {"unchecked", "rawtypes"})
public class FiboHeap<Key> extends AbstractQueue<Key> implements IFiboHeap<Key> {
    IFiboHeap heap;
    private final Queue<Key> queue;

    public static <Key, T extends Key> FiboHeap<T> create(Class<Key> keyClass) {
        return new FiboHeap(keyClass);
    }

    public static <Key, T extends Key> FiboHeap<T> create(Class<Key> keyClass, Comparator<? super Key> comp) {
        return new FiboHeap(keyClass, comp);
    }

    FiboHeap(Class<Key> keyClass) {
        boolean assignableFrom = IFiboHeapAware.class.isAssignableFrom(keyClass);
        if (assignableFrom) {
            heap = new AwareFiboHeap<>();
        } else {
            heap = new NormalFiboHeap();
        }
        queue = (Queue<Key>) heap;
    }

    FiboHeap(Class<Key> keyClass, Comparator<? super Key> comp) {
        boolean assignableFrom = IFiboHeapAware.class.isAssignableFrom(keyClass);
        if (assignableFrom) {
            heap = new AwareFiboHeap<>(comp);
        } else {
            heap = new NormalFiboHeap(comp);
        }
        queue = (Queue<Key>) heap;
    }

    @Override
    public Iterator<Key> iterator() {
        return queue.iterator();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean offer(Key key) {
        return queue.offer(key);
    }

    @Override
    public Key poll() {
        return queue.poll();
    }

    @Override
    public Key peek() {
        return queue.peek();
    }

    @Override
    public IHandle<Key> insert(Key key) {
        return heap.insert(key);
    }

    @Override
    public void union(IFiboHeap<Key> other) {
        heap.union(other);
    }

    @Override
    public Key extractMin() {
        return (Key) heap.extractMin();
    }

    @Override
    public Key minKey() {
        return (Key) heap.minKey();
    }

    @Override
    public void delete(IHandle<Key> entry) {
        heap.delete(entry);
    }

    @Override
    public void print() {
        heap.print();
    }
}

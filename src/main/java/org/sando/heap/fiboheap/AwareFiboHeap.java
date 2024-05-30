package org.sando.heap.fiboheap;

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
@SuppressWarnings(value = {"unchecked", "rawtypes"})
class AwareFiboHeap<Key extends IFiboHeapAware<Key>> extends AbstractQueue<Key> implements IFiboHeap<Key> {
    NormalFiboHeap<Key> heap;

    AwareFiboHeap() {
        heap = new NormalFiboHeap<>();
    }

    AwareFiboHeap(Comparator comp) {
        heap = new NormalFiboHeap<>(comp);
    }

    public IHandle<Key> insert(Key key) {
        Entry<Key> entry = heap.insert(key);
        key.aware(heap, entry);
        return entry;
    }

    public void union(NormalFiboHeap<Key> other) {
        other.forEach(key -> key.union(heap));
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

    @Override
    public void union(IFiboHeap<Key> other) {
        heap.union(other);
    }

    public void delete(IHandle<Key> handle) {
        Key key = handle.getKey();
        heap.delete(handle);
        key.aware(null, null);
    }

    public void print() {
        heap.print();
    }

    @Override
    public boolean offer(Key key) {
        insert(key);
        return true;
    }

    @Override
    public Key poll() {
        return extractMin();
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

}

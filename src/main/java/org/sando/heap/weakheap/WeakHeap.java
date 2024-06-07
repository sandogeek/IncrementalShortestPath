package org.sando.heap.weakheap;

import org.sando.heap.IHeap;
import org.sando.heap.fiboheap.IHandle;

import java.util.*;

/**
 * 弱堆
 *
 * @author Sando
 * @version 1.0
 * @since 2024/6/7
 */
@SuppressWarnings("unchecked")
public class WeakHeap<E> extends AbstractQueue<E> implements IHeap<E> {
    transient Object[] queue;
    /**
     * 堆当前大小
     */
    private int size = 0;
    /**
     * Comparator.
     */
    private Comparator comp = Comparator.naturalOrder();

    private final BitSet bitSet = new BitSet();

    private int dAncestor(int j) {
        while (((j & 1) == 1) == bitSet.get(j >> 1)) {
            j >>= 1;
        }
        return j >> 1;
    }

    private boolean join(int i, int j) {
        if (smaller(j, i)) {
            // swap
            Object temp = queue[i];
            queue[i] = queue[j];
            queue[j] = temp;
            bitSet.flip(j);
            return false;
        }
        return true;
    }

    private boolean smaller(int i, int j) {
        return comp.compare(queue[i], queue[j]) < 0;
    }

    public WeakHeap() {
    }

    public WeakHeap(Collection<? extends E> c) {
        initElementsFromCollection(c);

    }

    private void initElementsFromCollection(Collection<? extends E> c) {
        Object[] a = c.toArray();
        int len = a.length;
        if (len == 1 || comp != null)
            for (Object o : a)
                if (o == null)
                    throw new NullPointerException();
        this.queue = a;
        this.size = a.length;
    }

    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean offer(E e) {
        return false;
    }

    @Override
    public E poll() {
        return null;
    }

    @Override
    public E peek() {
        return null;
    }

    @Override
    public IHandle<E> insert(E e) {
        return null;
    }

    @Override
    public E extractMin() {
        return null;
    }

    @Override
    public E minKey() {
        return null;
    }
}

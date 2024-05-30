package org.sando;

import org.jheaps.AddressableHeap;
import org.sando.heap.fiboheap.Entry;
import org.sando.heap.fiboheap.IFiboHeap;
import org.sando.heap.fiboheap.IFiboHeapAware;
import org.sando.heap.indexheap.Heap;
import org.sando.heap.indexheap.IHeapIndex;

/**
 * @author Sando
 * @version 1.0
 * @since 2024/5/30
 */
class IntKey implements IFiboHeapAware<IntKey>, Comparable<IntKey>, IHeapIndex {
    private IFiboHeap<IntKey> fiboBeap;
    private Entry<IntKey> entry;
    private int key;
    Heap<?> heap;
    private int index;

    AddressableHeap.Handle<IntKey, Void> handle;

    public IntKey(int key) {
        this.key = key;
    }

    @Override
    public int compareTo(IntKey o) {
        return Integer.compare(key, o.key);
    }

    public void delta(int diff) {
        if (diff == 0) {
            return;
        }
        key += diff;
        if (diff < 0) {
            decreaseKey();
        } else {
            increaseKey();
        }
    }

    public void priorityChange(int diff) {
        if (diff == 0) {
            return;
        }
        key += diff;
        heap.priorityChange(index, diff);
    }

    public void handleDelta(int diff) {
        IntKey newKey = new IntKey(key + diff);
        newKey.handle = this.handle;
        handle.decreaseKey(newKey);
        this.handle = null;
    }

    @Override
    public IFiboHeap<IntKey> getHeap() {
        return fiboBeap;
    }

    @Override
    public Entry<IntKey> getEntry() {
        return entry;
    }

    @Override
    public void setHeap(IFiboHeap<IntKey> heap) {
        this.fiboBeap = heap;
    }

    @Override
    public void setEntry(Entry<IntKey> entry) {
        this.entry = entry;
    }

    @Override
    public void indexChange(Heap<?> heap, int index) {
        this.heap = heap;
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IntKey intKey = (IntKey) o;
        return key == intKey.key;
    }

    @Override
    public int hashCode() {
        return key;
    }

    @Override
    public String toString() {
//            int hash = super.hashCode();
//            return key + " " + Integer.toHexString(hash);
        return key + "";
    }
}

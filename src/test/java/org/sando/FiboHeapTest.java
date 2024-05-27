package org.sando;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;

/**
 * @author Sando
 * @version 1.0
 * @since 2024/5/23
 */
class FiboHeapTest {
//    private static final long SEED = 21L;
    private static Random rnd = new Random();

    @Test
    void union() {
        FiboHeap<Integer> fiboHeap1 = new FiboHeap<>();
        FiboHeap<Integer> fiboHeap2 = new FiboHeap<>();
        Heap<Integer> queue = new Heap<>();
        int size = 300_0000;
        for (int i = 0; i < size; i++) {
            int random = rnd.nextInt(size);
            queue.offer(random);
            if (rnd.nextInt(5) < 1) {
                fiboHeap2.insert(random);
            } else {
                fiboHeap1.insert(random);
            }
        }
        fiboHeap1.union(fiboHeap2);
        for (int i = 0; i < size; i++) {
            pollAndCheck(fiboHeap1, queue);
        }
    }

    @Test
    void extractMin() {
        FiboHeap<Integer> fiboHeap = new FiboHeap<>();
        Heap<Integer> queue = new Heap<>();
        int size = 300_0000;
        int half = size >> 1;
        for (int i = 0; i < size; i++) {
            int random = rnd.nextInt(size) - half;
            fiboHeap.insert(random);
            queue.offer(random);
            if (rnd.nextInt(5) < 1) {
                pollAndCheck(fiboHeap, queue);
            }
        }
        int sizeLeft = queue.size();
        for (int i = 0; i < sizeLeft; i++) {
            pollAndCheck(fiboHeap, queue);
        }
    }

    private static void pollAndCheck(FiboHeap<? extends Comparable<?>> fiboHeap, Queue<? extends Comparable<?>> queue) {
        Comparable<?> comparable1 = fiboHeap.poll();
        Comparable<?> comparable2 = queue.poll();
//        System.out.println("poll result:" + comparable1 + " " + comparable2);
        Assertions.assertEquals(comparable2, comparable1);
    }

    @Test
    void minKey() {
        FiboHeap<Integer> fiboHeap = new FiboHeap<>();
        PriorityQueue<Integer> queue = new PriorityQueue<>();
        int size = 300_0000;
        for (int i = 0; i < size; i++) {
            int random = rnd.nextInt(size);
            fiboHeap.insert(random);
            queue.offer(random);
            if (rnd.nextInt(5) < 1) {
                pollAndCheck(fiboHeap, queue);
            }
        }
        int sizeLeft = queue.size();
        for (int i = 0; i < sizeLeft; i++) {
            pollAndCheck(fiboHeap, queue);
        }
    }

    private static void pollAndCheckMinKey(FiboHeap<Integer> fiboHeap, Queue<Integer> queue) {
        fiboHeap.extractMin();
        queue.poll();
        Assertions.assertEquals(fiboHeap.minKey(), queue.peek());
    }

    @Test
    public void decreaseKey() {
        changeKey(30_0000, size -> -rnd.nextInt(size));
    }

    @Test
    public void increaseKey() {
        changeKey(30_0000, size -> rnd.nextInt(size/4));
    }

    private static void changeKey(int size ,Function<Integer, Integer> function) {
        FiboHeap<IntKey> fiboHeap = new FiboHeap<>();
        Heap<IntKey> queue = new Heap<>();
        List<Runnable> decreaseKeyList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int random = rnd.nextInt(size);
            IntKey key = new IntKey(random);
            fiboHeap.insert(key);
            queue.offer(key);
            if (rnd.nextInt(5) < 1) {
                decreaseKeyList.add(() -> {
                    if (key.getHeap() == null) {
                        return;
                    }
                    String old = key.toString();
                    Integer diff = function.apply(size);
                    key.delta(diff);
                    queue.priorityChange(key.getIndex(), diff);
                    System.out.println("change " + old + " -> " + key);
                });
            }
        }
        pollAndCheck(fiboHeap, queue);
        decreaseKeyList.forEach(Runnable::run);
        for (int i = 0; i < size - 1; i++) {
            pollAndCheck(fiboHeap, queue);
        }
    }

    @Test
    void delete() {
        FiboHeap<IntKey> fiboHeap = new FiboHeap<>();
        PriorityQueue<IntKey> queue = new PriorityQueue<>();
        int size = 100_0000;
        List<FiboHeap.Entry<IntKey>> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int random = rnd.nextInt(size);
            IntKey key = new IntKey(random);
//            System.out.println(key);
            fiboHeap.insert(key);
            if (rnd.nextInt(5) < 3) {
                entries.add(key.getEntry());
            } else {
                queue.offer(key);
            }
        }
        entries.forEach(fiboHeap::delete);

        int left = fiboHeap.size();
        for (int i = 0; i < left; i++) {
            pollAndCheck(fiboHeap, queue);
        }
    }

    public static void main(String[] args) {
        new FiboHeapTest().delete();
    }

    static class IntKey implements FiboHeap.IFiboHeapAware<IntKey>, Comparable<IntKey>,IHeapIndex {
        private FiboHeap<IntKey> fiboBeap;
        private FiboHeap.Entry<IntKey> entry;
        private int key;
        private Heap<?> heap;
        private int index;

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

        @Override
        public FiboHeap<IntKey> getHeap() {
            return fiboBeap;
        }

        @Override
        public FiboHeap.Entry<IntKey> getEntry() {
            return entry;
        }

        @Override
        public void setHeap(FiboHeap<IntKey> heap) {
            this.fiboBeap = heap;
        }

        @Override
        public void setEntry(FiboHeap.Entry<IntKey> entry) {
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
}
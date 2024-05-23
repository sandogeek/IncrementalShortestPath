package org.sando;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.PriorityQueue;
import java.util.Random;

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
        PriorityQueue<Integer> queue = new PriorityQueue<>();
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
        PriorityQueue<Integer> queue = new PriorityQueue<>();
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

    private static void pollAndCheck(FiboHeap<? extends Comparable<?>> fiboHeap, PriorityQueue<? extends Comparable<?>> queue) {
        Comparable<?> comparable1 = fiboHeap.poll();
        Comparable<?> comparable2 = queue.poll();
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

    private static void pollAndCheckMinKey(FiboHeap<Integer> fiboHeap, PriorityQueue<Integer> queue) {
        fiboHeap.extractMin();
        queue.poll();
        Assertions.assertEquals(fiboHeap.minKey(), queue.peek());
    }

    @Test
    public void decreaseKey() {
        FiboHeap<IntKey> fiboHeap = new FiboHeap<>();
        PriorityQueue<IntKey> queue = new PriorityQueue<>();
        int size = 30_0000;
        for (int i = 0; i < size; i++) {
            int random = rnd.nextInt(size);
            IntKey key = new IntKey(random);
//            System.out.println(key);
            fiboHeap.insert(key);
            if (rnd.nextInt(5) < 3) {
                key.delta(-rnd.nextInt(size));
//                System.out.println("after:"+key);
            }
            queue.offer(key);
        }

        for (int i = 0; i < size; i++) {
            pollAndCheck(fiboHeap, queue);
        }
    }

    static class IntKey implements FiboHeap.IFiboHeapAware<IntKey>, Comparable<IntKey> {
        private FiboHeap<IntKey> heap;
        private FiboHeap.Entry<IntKey> entry;
        private int key;

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
        public void aware(FiboHeap<IntKey> heap, FiboHeap.Entry<IntKey> entry) {
            this.heap = heap;
            this.entry = entry;
        }

        @Override
        public void union(FiboHeap<IntKey> heap) {
            this.heap = heap;
        }

        @Override
        public FiboHeap<IntKey> getHeap() {
            return heap;
        }

        @Override
        public FiboHeap.Entry<IntKey> getEntry() {
            return entry;
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
            return "IntKey{" +
                    "key=" + key +
                    '}';
        }
    }
}
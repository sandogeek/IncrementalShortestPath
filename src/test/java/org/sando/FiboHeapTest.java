package org.sando;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sando.heap.fiboheap.FiboHeap;
import org.sando.heap.fiboheap.IFiboHeap;
import org.sando.heap.fiboheap.IHandle;
import org.sando.heap.indexheap.Heap;

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
        FiboHeap<Integer> fiboHeap1 = FiboHeap.create(Integer.class);
        FiboHeap<Integer> fiboHeap2 = FiboHeap.create(Integer.class);
        Heap<Integer> queue = new Heap<>();
        int size = 50_0000;
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
        FiboHeap<Integer> fiboHeap = FiboHeap.create(Integer.class);
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

    private static void pollAndCheck(Queue<? extends Comparable<?>> fiboHeap, Queue<? extends Comparable<?>> queue) {
        Comparable<?> comparable1 = fiboHeap.poll();
        Comparable<?> comparable2 = queue.poll();
//        System.out.println("poll result:" + comparable1 + " " + comparable2);
        Assertions.assertEquals(comparable2, comparable1);
    }

    @Test
    void minKey() {
        FiboHeap<Integer> fiboHeap = FiboHeap.create(Integer.class);
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

    private static void pollAndCheckMinKey(IFiboHeap<Integer> fiboHeap, Queue<Integer> queue) {
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
        Queue<IntKey> fiboHeap = FiboHeap.create(IntKey.class);
        Heap<IntKey> queue = new Heap<>();
        List<Runnable> decreaseKeyList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int random = rnd.nextInt(size);
            IntKey key = new IntKey(random);
            fiboHeap.offer(key);
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
        FiboHeap<IntKey> fiboHeap = FiboHeap.create(IntKey.class);
        PriorityQueue<IntKey> queue = new PriorityQueue<>();
        int size = 100_0000;
        List<IHandle<IntKey>> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int random = rnd.nextInt(size);
            IntKey key = new IntKey(random);
//            System.out.println(key);
            fiboHeap.offer(key);
            if (rnd.nextInt(5) < 3) {
                entries.add(key.getHandle());
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

}
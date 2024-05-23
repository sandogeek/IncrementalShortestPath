package org.sando;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.PriorityQueue;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sando
 * @version 1.0
 * @since 2024/5/23
 */
class FiboHeapTest {
    private static final long SEED = 21L;
    private static Random rnd = new Random(SEED);
    @Test
    void insert() {
    }

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

    private static void pollAndCheck(FiboHeap<Integer> fiboHeap, PriorityQueue<Integer> queue) {
        Integer min = fiboHeap.extractMin();
        Integer poll = queue.poll();
        Assertions.assertEquals(poll, min);
    }

    @Test
    void minKey() {
    }
}
package org.sando;

import org.jheaps.AddressableHeap;
import org.jheaps.dag.HollowHeap;
import org.jheaps.tree.FibonacciHeap;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.sando.heap.fiboheap.FiboHeap;
import org.sando.heap.indexheap.Heap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 堆性能测试
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/30
 */
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@Fork(1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
public class HeapJmhTest {
    @Param({"10000"})
    private int seed;
    private boolean print = true;
    private static int size = 60_0000;
    private static int bound = size / 2;
    private static int decreaseCount = 4;

    @Benchmark
    @Test
    public void fiboHeapBench() {
        seed += 3;
        Random rnd = new Random(seed);
        FiboHeap<IntKey> fiboHeap = FiboHeap.create(IntKey.class);
        changeKey(rnd, size, fiboHeap::offer, key -> {
            if (key.getHeap() == null) {
                return;
            }
            int diff = -rnd.nextInt(bound);
            for (int i = 0; i < decreaseCount; i++) {
                key.delta(diff);
            }
        }, fiboHeap::poll);
    }

    @Benchmark
    @Test
    public void indexHeapBench() {
        seed += 3;
        Random rnd = new Random(seed);
        Heap<IntKey> heap = new Heap<>();
        changeKey(rnd, size, heap::offer, key -> {
            if (key.getIndex() == Heap.NOT_IN_HEAP) {
                return;
            }
            int diff = -rnd.nextInt(bound);
            for (int i = 0; i < decreaseCount; i++) {
                key.priorityChange(diff);
            }
        }, heap::poll);
    }

    @Benchmark
    public void fiboHeapBench2() {
        seed += 3;
        Random rnd = new Random(seed);
        AddressableHeap<IntKey, Void> fibonacciHeap = new FibonacciHeap<>();
        changeKey(rnd, size, key -> {
            key.handle = fibonacciHeap.insert(key);
        }, key -> {
            AddressableHeap.Handle<IntKey, Void> handle = key.handle;
            if (handle == null) {
                return;
            }
            int diff = -rnd.nextInt(bound);
            for (int i = 0; i < decreaseCount; i++) {
                handle.getKey().handleDelta(diff);
            }
        }, () -> {
            AddressableHeap.Handle<IntKey, Void> handle = fibonacciHeap.deleteMin();
            handle.getKey().handle = null;
        });
    }

    @Benchmark
    @Test
    public void hollowHeapBench() {
        seed += 3;
//        if (print) {
//            System.out.println("hollowHeapBench" + seed);
//            print = false;
//        }
        Random rnd = new Random(seed);
        HollowHeap<IntKey, Void> hollowHeap = new HollowHeap<>();
        changeKey(rnd, size, intKey -> {
            intKey.handle = hollowHeap.insert(intKey);
        }, key -> {
            AddressableHeap.Handle<IntKey, Void> handle = key.handle;
            if (handle == null) {
                return;
            }
            int diff = -rnd.nextInt(bound);
            for (int i = 0; i < decreaseCount; i++) {
                handle.getKey().handleDelta(diff);
            }
        }, () -> {
            AddressableHeap.Handle<IntKey, Void> handle = hollowHeap.deleteMin();
            handle.getKey().handle = null;
        });
    }

    private static void changeKey(Random rnd, int size, Consumer<IntKey> consumer, Consumer<IntKey> decrease, Runnable poll) {
        List<Runnable> decreaseKeyList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int random = rnd.nextInt(size);
            IntKey key = new IntKey(random);
            consumer.accept(key);
            int rate = rnd.nextInt(5);
            if (rate < 3) {
                decreaseKeyList.add(() -> {
                    decrease.accept(key);
                });
            }
        }
        poll.run();
        decreaseKeyList.forEach(Runnable::run);
        for (int i = 0; i < size - 1; i++) {
            poll.run();
        }
    }

    public static void main(String[] args) throws RunnerException {
        int seed = new Random().nextInt();
        Options opt = new OptionsBuilder()
                .include(HeapJmhTest.class.getSimpleName())
                .param("seed", "" + seed)
                .build();
        new Runner(opt).run();
    }
}

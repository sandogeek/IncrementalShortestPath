package org.sando;

import java.util.Comparator;
import java.util.Iterator;

/**
 * 斐波那契堆
 *
 * @param <Key> 堆中元素的类型
 * @author Sando
 * @version 1.0
 * @since 2024/5/22
 */
public class FiboHeap<Key> implements Iterable<Key> {
    @SuppressWarnings("unchecked")
    private static final Comparator<Object> DEFAULT_COMP = (o1, o2) -> ((Comparable<Object>) o1).compareTo(o2);
    private static final double LOG2 = Math.log(2.0);
    /**
     * Comparator.
     */
    private Comparator<? super Key> comp = DEFAULT_COMP;
    /**
     * 堆中最小的节点
     */
    private Entry<Key> minimum = null;
    /**
     * 堆中节点的数量
     */
    private int size = 0;
    /**
     * cons数组，调整堆的辅助数组
     */
    private Entry<Key>[] cons = null;
    /**
     * 下使得cons数组容量上升的size临界值
     */
    private int dnSize;

    public FiboHeap() {
        this(2 << 8);
    }

    public FiboHeap(int initialCapacity) {
        ensureConsLength(initialCapacity);
    }

    public FiboHeap(int initialCapacity, Comparator<? super Key> comp) {
        this(initialCapacity);
        if (comp != null) {
            this.comp = comp;
        }
    }

    /**
     * 插入一个key。斐波那契堆的根链表是"双向链表"，这里将{@link #minimum}节点看作双向联表的表头
     *
     * @param key 被插入的key
     */
    public void insert(Key key) {
        Entry<Key> entry = new Entry<>(key);
        entry.parent = null;
        entry.marked = false;
        if (minimum == null) {
            minimum = entry;
            minimum.right = minimum;
            minimum.left = minimum;
        } else {
            // 这里提前执行比较，因为内部可能报ClassCastException,
            // 通过fail fast减少执行的逻辑，同时保护链表不被破坏
            boolean smaller = smaller(entry, minimum);
            insert(entry, minimum);
            if (smaller) {
                minimum = entry;
            }
        }
        size++;
    }

    /**
     * 将other合并到当前堆中，注意：此操作会导致other被清空。
     */
    public void union(FiboHeap<Key> other) {
        if (other == null) {
            return;
        }
        Entry<Key> otherMin = other.minimum;
        if (minimum == null) {
            minimum = otherMin;
        } else {
            if (otherMin != null) {
                appendList(minimum, other.minimum);
                size += other.size;
                minimum = getSmaller(otherMin, minimum);
            }
        }
        // 清理另一个堆
        other.clear();
    }

    /**
     * 取出最小节点
     *
     * @return 最小节点, 如果堆为空，则返回null
     */
    public Key extractMin() {
        if (minimum == null) {
            return null;
        }
        Entry<Key> oldMin = minimum;
        // 将minimum每一个儿子(儿子和儿子的兄弟)都添加到"斐波那契堆的根链表"中
        if (minimum.child != null) {
            Entry<Key> tmp = minimum.child;
            // 此处因为已经知道tmp不为null，所以其父节点必定不为null，
            // 因此选择do while，相比while可以减少一次tmp.parent != null判断
            do {
                tmp.parent = null;
                tmp = tmp.right;
            } while (tmp.parent != null);
            appendList(tmp, minimum);
        }

        // 若oldMin是堆中唯一节点，则设置堆的最小节点为null；
        // 否则，设置堆的最小节点为次小节点(oldMin.right)，然后再进行调节。
        if (oldMin.right == oldMin)
            minimum = null;
        else {
            minimum = oldMin.right;
            // 将oldMin从根链表中移除
            removeEntry(oldMin);
            consolidate();
        }
        size--;
        return oldMin.key;
    }

    /**
     * 合并斐波那契堆的根链表中左右相同度数的树
     * 性质：一个数如果是斐波那契数，那么这个数在斐波那契数列上的位置下标不大于其数值关于黄金比例的对数
     * 堆的节点总数对应斐波那契数，让节点的度对应斐波那契数列上的位置下标
     */
    private void consolidate() {
        ensureConsLength(size);
//        for (int i = 0; i < D; i++)
//            cons[i] = null;
//
//        // 合并相同度的根节点，使每个度数的树唯一
//        while (minimum != null) {
//            Entry<Key> x = extractMin();            // 取出堆中的最小树(最小节点所在的树)
//            int d = x.degree;                        // 获取最小树的度数
//            // cons[d] != null，意味着有两棵树(x和y)的"度数"相同。
//            while (cons[d] != null) {
//                Entry<Key> y = cons[d];                // y是"与x的度数相同的树"
//                if (x.key > y.key) {    // 保证x的键值比y小
//                    Entry<Key> tmp = x;
//                    x = y;
//                    y = tmp;
//                }
//
//                link(y, x);    // 将y链接到x中
//                cons[d] = null;
//                d++;
//            }
//            cons[d] = x;
//        }
//        min = null;
//
//        // 将cons中的结点重新加到根表中
//        for (int i = 0; i < D; i++) {
//
//            if (cons[i] != null) {
//                if (min == null)
//                    min = cons[i];
//                else {
//                    insert(cons[i], min);
//                    if ((cons[i]).key < min.key)
//                        min = cons[i];
//                }
//            }
//        }
    }

    /**
     * 确保cons数组大小充足
     */
    private void ensureConsLength(int size) {
        if (size < dnSize) {
            // 堆大小没有达到临界值，cons数组大小就是够用的
            return;
        }
        // 根据斐波那契性质，计算出最大的度
        // ex. log2(13) = 3，向上取整为4。
        int floor = (int) Math.floor(Math.log(size) / LOG2);
        int needLen = floor + 2;
        if (cons == null || needLen > cons.length) {
            // 最大的度为floor+1,度从0开始，所以数组容量要加1
            cons = new Entry[needLen];
            // 计算下一个使得cons数组容量上升的size
            dnSize = 2 << (floor + 1);
        }
    }

    /*
     * 将entry从所在的双链表移除
     */
    private void removeEntry(Entry<Key> entry) {
        entry.left.right = entry.right;
        entry.right.left = entry.left;
    }

    /**
     * 清空当前堆
     */
    public void clear() {
        minimum = null;
        size = 0;
    }

    /**
     * 向双向循环链表a后追加链表b，从而合并成一个新的双向循环链表
     * 例子：
     * <p>
     * m <--> k <--> a <--> m
     * n <--> j <--> b <--> n
     * m = tmp = a.right n = b.right
     * a.right       = b.right; 对应： a的右边变成n
     * b.right.left  = a; 对应： n的左边变成a
     * b.right       = tmp; 对应： b的右边变成m
     * tmp.left      = b; 对应： m的左边变成b
     * 结果：m <--> k <--> a <--> n <--> j <-->b <--> m
     * </p>
     *
     * @param a 双向循环链表a
     * @param b 双向循环链表b
     */
    private void appendList(Entry<Key> a, Entry<Key> b) {
//        if (a == null) return b;
//        if (b == null) return a;
        Entry<Key> tmp;
        tmp = a.right;
        a.right = b.right;
        b.right.left = a;
        b.right = tmp;
        tmp.left = b;
    }

    /**
     * 获取堆中最小的key
     *
     * @return 堆中最小的key，如果堆为空，则返回null
     */
    public Key minKey() {
        if (minimum == null) {
            return null;
        }
        return minimum.key;
    }

    /**
     * 将entry插入到双向链表中,head节点前
     * 例如：原本E <-> head , 调整后顺序变为 E <-> entry <-> head
     *
     * @param entry 被插入的节点
     * @param head  头节点
     */
    private void insert(Entry<Key> entry, Entry<Key> head) {
        // E = head.left
        entry.left = head.left;
        head.left.right = entry;
        entry.right = head;
        head.left = entry;
    }

    /**
     * key值变小
     * 最差摊还复杂度： O(1)
     *
     * @param key 变小的key
     */
    private void decreaseKey(Key key) {

    }

    /**
     * key值变大
     * 最差复杂度： O(log(n))
     *
     * @param key 变大的key
     */
    private void increaseKey(Key key) {

    }

    /**
     * entry1是否小于entry2, key为null，意味着处于最小值
     * TODO 检查所有调用，看看是否把==null判断挪出去
     *
     * @param entry1 第一个entry,可以为null
     * @param entry2 第二个entry,可以为null
     * @return 如果小于返回true，否则返回false
     */
    private boolean smaller(Entry<Key> entry1, Entry<Key> entry2) {
        if (entry1.key == null) {
            return true;
        }
        if (entry2.key == null) {
            return false;
        }
        return comp.compare(entry1.key, entry2.key) < 0;
    }

    /**
     * 获取entry1和entry2中较小的一个
     *
     * @param entry1 第一个entry,可以为null
     * @param entry2 第二个entry,可以为null
     * @return 较小的一个
     */
    private Entry<Key> getSmaller(Entry<Key> entry1, Entry<Key> entry2) {
        if (smaller(entry1, entry2)) {
            return entry1;
        }
        return entry2;
    }

    /**
     * 堆是否为空
     *
     * @return 如果为空返回true，否则返回false
     */
    public boolean isEmpty() {
        return size == 0;
    }

    public int getSize() {
        return size;
    }

    @Override
    public Iterator<Key> iterator() {
        return null;
    }

    /**
     * 斐波那契堆节点
     */
    private static class Entry<Key> {
        Key key; // 键
        Entry<Key> left; // 左兄弟
        Entry<Key> right; // 右兄弟
        Entry<Key> parent; // 父节点
        Entry<Key> child; // 第一个孩子
        int degree; // 度(当前节点的孩子数目)
        boolean marked; // 第一个孩子是否被删除（在删除节点时有用）

        public Entry(Key key) {
            this.key = key;
        }
    }

    /**
     * 斐波那契堆Key出/入堆感知接口
     */
    @SuppressWarnings("unchecked")
    interface IFiboHeapAware<Key extends IFiboHeapAware<Key>> {
        /**
         * 当Key出/入堆时调用
         *
         * @param heap 入堆时非null，出堆时为null
         */
        void aware(FiboHeap<Key> heap);

        /**
         * 获取对应的堆
         */
        FiboHeap<Key> getHeap();

        /**
         * key变大
         */
        default void increaseKey() {
            FiboHeap<Key> heap = getHeap();
            if (heap == null) {
                return;
            }
            heap.increaseKey((Key) this);
        }

        /**
         * key变小
         */
        default void decreaseKey() {
            FiboHeap<Key> heap = getHeap();
            if (heap == null) {
                return;
            }
            getHeap().decreaseKey((Key) this);
        }
    }
}

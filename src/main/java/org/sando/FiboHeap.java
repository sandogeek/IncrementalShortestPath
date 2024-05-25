package org.sando;

import java.util.*;

/**
 * 斐波那契堆
 *
 * @param <Key> 堆中元素的类型
 * @author Sando
 * @version 1.0
 * @since 2024/5/22
 */
@SuppressWarnings(value = {"unchecked", "rawtypes"})
public class FiboHeap<Key> extends AbstractQueue<Key> {
    private static final Comparator<Object> DEFAULT_COMP = (o1, o2) -> ((Comparable<Object>) o1).compareTo(o2);
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
    private Entry[] cons;
    /**
     * 下使得cons数组容量上升的size临界值
     */
    private int dnSize;

    public FiboHeap() {
        this(1 << 10);
    }

    public FiboHeap(int initialCapacity) {
        int needLen = log2Floor(initialCapacity) + 2;
        dnSize = 1 << needLen;
        cons = new Entry[needLen];
    }

    public FiboHeap(int initialCapacity, Comparator<? super Key> comp) {
        this(initialCapacity);
        if (comp != null) {
            this.comp = comp;
        }
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
        return minKey();
    }

    @Override
    public int size() {
        return size;
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
        if (key instanceof IFiboHeapAware) {
            ((IFiboHeapAware) key).aware(this, entry);
        }
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
        other.forEach(key -> {
            if (key instanceof IFiboHeapAware) {
                ((IFiboHeapAware) key).union(this);
            }
        });
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
        Key key = oldMin.key;
        oldMin.key = null;
        allChild2RootList(oldMin);

        // 若oldMin是堆中唯一节点，则设置堆的最小节点为null；
        // 否则，设置堆的最小节点为次小节点(oldMin.right)，然后再进行调节。
        if (oldMin.right == oldMin) {
            minimum = null;
        } else {
            minimum = oldMin.right;
            // 将oldMin从根链表中移除
            removeEntry(oldMin);
            consolidate();
        }
        size--;
        if (key instanceof IFiboHeapAware) {
            ((IFiboHeapAware) key).aware(null, null);
        }
        return key;
    }

    /**
     * 将entry每一个儿子(儿子和儿子的兄弟)都添加到"斐波那契堆的根链表"中
     */
    private void allChild2RootList(Entry<Key> entry) {
        if (entry.child == null) {
            return;
        }
        Entry<Key> tmp = entry.child;
        // 此处因为已经知道tmp不为null，所以其父节点必定不为null，
        // 因此选择do while，相比while可以减少一次tmp.parent != null判断
        do {
            tmp.parent = null;
            tmp = tmp.right;
        } while (tmp.parent != null);
        appendList(tmp, minimum);
    }

    /**
     * 合并斐波那契堆的根链表中左右相同度数的树
     * 性质：一个数如果是斐波那契数，那么这个数在斐波那契数列上的位置下标不大于其数值关于黄金比例的对数
     * 堆的节点总数对应斐波那契数，让节点的度对应斐波那契数列上的位置下标
     */
    private void consolidate() {
        ensureConsLength();
        // cur当前节点，当cur等于iter时，循环终止
        Entry<Key> iter = minimum, cur = minimum;
        Entry<Key> smaller;
        Entry<Key> bigger;
        Entry<Key> temp;
        int d;
        do {
            smaller = cur;
            d = smaller.degree;
            if (cons[d] != smaller) {
                while (cons[d] != null) {
                    bigger = cons[d];
                    if (smaller(bigger, smaller)) {
                        temp = smaller;
                        smaller = bigger;
                        bigger = temp;
                    }
                    // 让bigger成为smaller的孩子
                    link(bigger, smaller);
                    iter = smaller;
                    cur = smaller;
                    cons[d] = null;
                    d += 1;
                }
                cons[d] = smaller;
            }
            cur = cur.right;
        } while (cur != iter);
        minimum = iter;

        // 寻找最小元素
        do {
            // 最开始cur必定等于iter等于minimum，
            // 此处先cur = cur.right能减少一次不必要的大小比较,当只有一个根节点时，执行比较次数为0次
            cons[cur.degree] = null;
            cur = cur.right;
            if (cur == iter) {
                break;
            }
            minimum = getSmaller(cur, minimum);
        } while (true);
    }

    /*
     * 把child从所在链表移除,并把child变成parent的孩子节点
     */
    private void link(final Entry<Key> child, final Entry<Key> parent) {
        removeEntry(child);
        if (parent.child == null) {
            child.right = child;
            child.left = child;
            parent.child = child;
        } else {
            insert(child, parent.child);
        }
        child.parent = parent;
        parent.degree += 1;
        child.marked = false;
    }

    /**
     * 确保cons数组大小充足
     */
    private void ensureConsLength() {
        if (size < dnSize) {
            // 堆大小没有达到临界值，cons数组大小就是够用的
            return;
        }
        // 根据斐波那契性质，计算出最大的度 = (int) Math.floor(Math.log(size) / LOG2) + 1
        // 度从0开始，所以数组容量要加1
        // 这里实际+2就足够了，选择+3是为了减少new Entry[]的次数（dnSize每次扩大1<<3倍）
        int needLen = log2Floor(size) + 3;
        if (needLen > cons.length) {
            cons = new Entry[needLen];
            // 计算下一个使得cons数组容量上升的size
            dnSize = 1 << needLen;
        }
    }

    /**
     * 计算n以2为底数的对数值执行floor后的结果
     *
     * @return n以2为底数的对数值执行floor后的结果
     */
    private static int log2Floor(int n) {
        // Integer.SIZE - 1 == 31
        return 31 - Integer.numberOfLeadingZeros(n);
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
     * @param entry 变小的entry
     */
    private void decreaseKey(Entry<Key> entry) {
        Entry<Key> parent = entry.parent;
        if (parent != null && smaller(entry, parent)) {
            cut(entry, parent, true);
            cascadingCut(parent);
        }

        if (smaller(entry, minimum)) {
            minimum = entry;
        }
    }

    /*
     * 对节点node进行"级联剪切"
     *
     * 级联剪切：如果减小后的结点破坏了最小堆性质，
     *     则把它切下来(即从所在双向链表中删除，并将
     *     其插入到由最小树根节点形成的双向链表中)，
     *     然后再从"被切节点的父节点"到所在树根节点递归执行级联剪枝
     */
    private void cascadingCut(Entry<Key> entry) {
        Entry<Key> parent = entry.parent;

        if (parent != null) {
            if (!entry.marked)
                entry.marked = true;
            else {
                cut(entry, parent, true);
                cascadingCut(parent);
            }
        }
    }

    /**
     * 将x从当前所在的链表中剥离出来，
     *
     * @param x 需要被剥离的节点
     * @param insert 是否使x成为"堆的根链表"中的一员
     */
    private void cut(Entry<Key> x, Entry<Key> parent, boolean insert) {
        if (x.right == x) {
            parent.child = null;
        } else {
            parent.child = x.right;
        }
        removeEntry(x);
        parent.degree--;
        // Add x to the root list.
        if (insert) {
            insert(x, minimum);
        }
        x.parent = null;
        x.marked = false;
    }

    /**
     * key值变大
     * 最差复杂度： O(log(n))
     *
     * @param entry 变大的entry
     */
    private void increaseKey(Entry<Key> entry) {

    }

    /**
     * 移除指定节点，为了效率，不检查节点是否属于当前堆
     *
     * @param entry 被移除的节点
     */
    public void delete(Entry<Key> entry) {
        if (entry == minimum) {
            extractMin();
            return;
        }
        // key为null则意味着entry是最小值
        Key key = entry.key;
        entry.key = null;
        // 以下相当于decrease(entry)，但为了避免调用smaller，所以采用了复制代码的方式
        Entry<Key> parent = entry.parent;
        if (parent != null) {
            cut(entry, parent, false);
            cascadingCut(parent);
        } else {
            removeEntry(entry);
        }
        allChild2RootList(entry);
        consolidate();
        size--;
        if (key instanceof IFiboHeapAware) {
            ((IFiboHeapAware) key).aware(null, null);
        }
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

    @Override
    public Iterator<Key> iterator() {
        return null;
    }

    /**
     * 斐波那契堆节点
     */
    public static class Entry<Key> {
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
         * 当Key出/入堆时调用,用于知道自己入堆和出堆
         *
         * @param heap 入堆时非null，出堆时为null
         */
        default void aware(FiboHeap<Key> heap, Entry<Key> entry) {
            setHeap(heap);
            setEntry(entry);
        }

        /**
         * 被union到一个新的堆
         *
         * @param heap 新堆
         */
        default void union(FiboHeap<Key> heap) {
            setHeap(heap);
        }

        /**
         * 获取对应的堆
         */
        FiboHeap<Key> getHeap();

        void setHeap(FiboHeap<Key> heap);

        /**
         * 获取key对应的entry
         */
        Entry<Key> getEntry();

        void setEntry(Entry<Key> entry);

        /**
         * key变大
         */
        default void increaseKey() {
            FiboHeap<Key> heap = getHeap();
            if (heap == null) {
                return;
            }
            heap.increaseKey(getEntry());
        }

        /**
         * key变小
         */
        default void decreaseKey() {
            FiboHeap<Key> heap = getHeap();
            if (heap == null) {
                return;
            }
            getHeap().decreaseKey(getEntry());
        }
    }
}

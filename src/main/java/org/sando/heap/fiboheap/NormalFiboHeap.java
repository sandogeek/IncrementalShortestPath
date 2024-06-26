package org.sando.heap.fiboheap;

import java.util.*;
import java.util.function.Consumer;

/**
 * 斐波那契堆
 *
 * @param <Key> 堆中元素的类型
 * @author Sando
 * @version 1.0
 * @since 2024/5/22
 */
@SuppressWarnings(value = {"unchecked", "rawtypes"})
class NormalFiboHeap<Key> extends AbstractQueue<Key> implements IFiboHeap<Key> {
    private static final int CONS_LENGTH = log2Floor(Integer.MAX_VALUE);
    private static final ThreadLocal<Entry[]> CONS = ThreadLocal.withInitial(() -> new Entry[CONS_LENGTH]);
    /**
     * Comparator.
     */
    private Comparator comp = Comparator.naturalOrder();
    /**
     * 堆中最小的节点
     */
    private Entry<Key> minimum = null;
    /**
     * 次小节点
     */
    private Entry<Key> secondMin = null;
    /**
     * 堆中节点的数量
     */
    private int size = 0;
    /**
     * The mod count.
     */
    private transient int mod_count;

    NormalFiboHeap() {
    }

    NormalFiboHeap(Comparator<? super Key> comp) {
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
    @Override
    public Entry<Key> insert(Key key) {
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
            tryReplaceMin(entry, smaller);
        }
        size++;
        mod_count++;
        return entry;
    }

    private boolean tryReplaceMin(Entry<Key> entry) {
        boolean smaller = smaller(entry, minimum);
        return tryReplaceMin(entry, smaller);
    }

    private boolean tryReplaceMin(Entry<Key> entry, boolean smaller) {
        if (smaller) {
            secondMin = minimum;
            minimum = entry;
            return true;
        } else {
            secondMin = null;
        }
        return false;
    }

    /**
     * 将other合并到当前堆中，注意：此操作会导致other被清空。
     */
    @Override
    public void union(IFiboHeap<Key> other) {
        if (other == null) {
            return;
        }
        NormalFiboHeap<Key> normalFiboHeap;
        if (other instanceof FiboHeap) {
            other = ((FiboHeap<Key>) other).heap;
        }
        if (other instanceof AwareFiboHeap) {
            normalFiboHeap = (NormalFiboHeap<Key>)((AwareFiboHeap) other).heap;
        } else {
            normalFiboHeap = (NormalFiboHeap<Key>) other;
        }
        Entry<Key> otherMin = normalFiboHeap.minimum;
        if (minimum == null) {
            minimum = otherMin;
        } else {
            if (otherMin != null) {
                appendList(minimum, normalFiboHeap.minimum);
                size += normalFiboHeap.size;
                tryReplaceMin(otherMin);
            }
        }
        mod_count++;
        // 清理另一个堆
        other.clear();
    }

    /**
     * 取出最小节点
     *
     * @return 最小节点, 如果堆为空，则返回null
     */
    @Override
    public Key extractMin() {
        if (minimum == null) {
            return null;
        }
        Entry<Key> oldMin = minimum;
        Key key = oldMin.key;
        oldMin.key = null;
        allChild2RootList(oldMin);

        // 若oldMin是堆中唯一节点，则设置堆的最小节点为null；
        if (oldMin.right == oldMin) {
            minimum = null;
        } else {
            if (secondMin != null) {
                minimum = secondMin;
                secondMin = null;
                removeEntry(oldMin);
            } else {
                // 否则，设置堆的最小节点为oldMin.right，然后再进行调节。
                minimum = oldMin.right;
                // 将oldMin从根链表中移除
                removeEntry(oldMin);
                consolidate();
            }
        }
        size--;
        mod_count++;
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
        appendList(minimum, tmp);
        entry.child = null;
        entry.degree = 0;
    }

    /**
     * 合并斐波那契堆的根链表中左右相同度数的树
     * 性质：一个数如果是斐波那契数，那么这个数在斐波那契数列上的位置下标不大于其数值关于黄金比例的对数
     * 堆的节点总数对应斐波那契数，让节点的度对应斐波那契数列上的位置下标
     */
    private void consolidate() {
        Entry[] cons = CONS.get();
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

        loopSibling(minimum, entry -> cons[entry.degree] = null);
        refreshMinimum();
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
    @Override
    public void clear() {
        minimum = null;
        size = 0;
        mod_count++;
        secondMin = null;
    }

    /**
     * 向双向循环链表a后追加链表b，从而合并成一个新的双向循环链表
     * 例子：
     * <p>
     * a <--> m <--> k
     * b <--> n <--> j
     * 结果： a <--> b <--> n <--> j <--> m <--> k
     * </p>
     *
     * @param a 双向循环链表a
     * @param b 双向循环链表b
     */
    private void appendList(Entry<Key> a, Entry<Key> b) {
        b.left.right = a.right;
        a.right.left = b.left;
        a.right = b;
        b.left = a;
    }

    /**
     * 获取堆中最小的key
     *
     * @return 堆中最小的key，如果堆为空，则返回null
     */
    @Override
    public Key minKey() {
        if (minimum == null) {
            return null;
        }
        return minimum.key;
    }

    /**
     * 将entry插入到双向链表中,head节点前
     * 例如：原本E <-> head , 调整后顺序变为 E <-> entry <-> head
     * <p>
     * 这个实际等价于：
     * <pre>
     *    if (entry.right == null) {
     *         entry.right = entry;
     *     }
     *     appendList(entry, head);
     * </pre>
     * 这里单独实现是为了减少一次判断，以及entry.right = entry的赋值
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
    final void decreaseKey(Entry<Key> entry) {
        Entry<Key> parent = entry.parent;
        if (parent != null && smaller(entry, parent)) {
            cut(entry, parent, true);
            cascadingCut(parent);
        }

        tryReplaceMin(entry);
        mod_count++;
    }

    /*
     * 对节点entry进行"级联剪切"
     *
     * 级联剪切：如果减小后的结点破坏了最小堆性质，
     *     则把它切下来(即从所在双向链表中删除，并将
     *     其插入到由最小树根节点形成的双向链表中)，
     *     然后再从"被切节点的父节点"到所在树根节点递归执行级联剪枝
     */
    private void cascadingCut(Entry<Key> entry) {
        for (; ; ) {
            Entry<Key> parent = entry.parent;
            if (parent == null) {
                break;
            }
            if (!entry.marked) {
                entry.marked = true;
                break;
            }
            cut(entry, parent, true);
            entry = parent;
        }
    }

    /**
     * 将x从当前所在的链表中剥离出来
     *
     * @param x      需要被剥离的节点
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
    void increaseKey(Entry<Key> entry) {
        // 将node每一个儿子(不包括孙子,重孙,...)都添加到"斐波那契堆的根链表"中
        allChild2RootList(entry);

        // 如果entry不在根链表中，
        //     则将entry从父节点parent的子链接中剥离出来，
        //     并使entry成为"堆的根链表"中的一员，
        //     然后进行"级联剪切"
        // 否则，则判断是否需要更新堆的最小节点
        Entry<Key> parent = entry.parent;
        if (parent != null) {
            cut(entry, parent, true);
            cascadingCut(parent);
            if (entry == secondMin) {
                secondMin = null;
            }
        } else if (minimum == entry) {
            refreshMinimum();
        } else if (entry == secondMin) {
            secondMin = null;
        }
    }

    /**
     * 最小值可能已经发生变化,需要刷新minimum
     */
    private void refreshMinimum() {
        if (secondMin != null) {
            boolean success = tryReplaceMin(secondMin);
            if (success) {
                return;
            }
        }
        Entry<Key> right = minimum.right;
        while (right != minimum) {
            tryReplaceMin(right);
            right = right.right;
        }
    }

    /**
     * 移除指定节点，为了效率，不检查节点是否属于当前堆
     *
     * @param handle 被移除的节点
     */
    @Override
    public void delete(IHandle<Key> handle) {
        Entry<Key> entry = (Entry<Key>) handle;
        if (entry == minimum) {
            extractMin();
            return;
        }
        // key为null则意味着entry是最小值
        entry.key = null;
        // 以下相当于decrease(entry)，但为了避免调用smaller，所以采用了复制代码的方式
        Entry<Key> parent = entry.parent;
        if (parent != null) {
            cut(entry, parent, false);
            cascadingCut(parent);
        } else {
            removeEntry(entry);
        }
        if (entry == secondMin) {
            secondMin = null;
        }
        allChild2RootList(entry);
        consolidate();
        size--;
        mod_count++;
    }

    /**
     * entry1是否小于entry2, key为null，意味着处于最小值
     *
     * @param entry1 第一个entry,可以为null
     * @param entry2 第二个entry,可以为null
     * @return 如果小于返回true，否则返回false
     */
    private boolean smaller(Entry<Key> entry1, Entry<Key> entry2) {
        return comp.compare(entry1.key, entry2.key) < 0;
    }

    /**
     * 堆是否为空
     *
     * @return 如果为空返回true，否则返回false
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public Iterator<Key> iterator() {
        return new EntryIterator();
    }

    private class EntryIterator
            implements Iterator<Key> {

        /**
         * The next entry.
         */
        private Entry<Key> next;

        /**
         * The mod count.
         */
        private final int my_mod_count;

        /**
         * Constructor.
         */
        EntryIterator() {
            super();

            // Start at min.
            next = NormalFiboHeap.this.minimum;

            // Copy mod count.
            my_mod_count = NormalFiboHeap.this.mod_count;
        }

        /**
         * @see Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            if (my_mod_count != NormalFiboHeap.this.mod_count) {
                throw new ConcurrentModificationException();
            }

            return (next != null);
        }

        /**
         * @see Iterator#next()
         */
        @Override
        public Key next()
                throws NoSuchElementException, ConcurrentModificationException {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Entry<Key> n = next;
            next = getSuccessor(next);
            return n.key;
        }

        /**
         * Return the successor entry to the specified entry.
         *
         * @param entry the given entry.
         * @return the successor entry.
         */
        private Entry<Key> getSuccessor(Entry<Key> entry) {
            if (entry.child != null) {
                return entry.child;
            }
            Entry<Key> first;
            do {
                first = (entry.parent == null) ? NormalFiboHeap.this.minimum : entry.parent.child;
                if (entry.right != first) {
                    return entry.right;
                }
                entry = entry.parent;
            }
            while (entry != null);
            return null;
        }

        /**
         * @see Iterator#remove()
         */
        @Override
        public void remove()
                throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void print() {
        if (minimum == null) {
            return;
        }
        printHelper(minimum, "", 0);
    }

    private void printHelper(Entry<Key> root, String start, int preSpace) {
        loopSibling(root, tmp -> {
            StringBuilder pre = new StringBuilder("└-->");
            for (int i = 0; i < preSpace; i++) {
                pre.insert(0, " ");
            }
            String keyStr = "[" + tmp.key + "]";
            System.out.println(start + pre + keyStr);
            Entry<Key> child = tmp.child;
            if (child != null) {
                printHelper(child, start + "\t", preSpace + keyStr.length() - 2);
            }
        });
    }

    private void loopSibling(Entry<Key> entry, Consumer<? super Entry<Key>> consumer) {
        Entry<Key> tmp = entry;
        do {
            consumer.accept(tmp);
            tmp = tmp.right;
        } while (entry != tmp);
    }

}

package org.sando;
/**
 * 性能测试{@link https://github.com/gabormakrai/dijkstra-performance}
 * 实现上有修改：1 移除了priority字段 2 compare为0时，不做后续操作
 */

/***********************************************************************
 * File: FibonacciHeap.java
 * Author: Keith Schwarz (htiek@cs.stanford.edu)
 *
 * An implementation of a priority queue backed by a Fibonacci heap,
 * as described by Fredman and Tarjan.  Fibonacci heaps are interesting
 * theoretically because they have asymptotically good runtime guarantees
 * for many operations.  In particular, insert, peek, and decrease-key all
 * run in amortized O(1) time.  dequeueMin and delete each run in amortized
 * O(lg n) time.  This allows algorithms that rely heavily on decrease-key
 * to gain significant performance boosts.  For example, Dijkstra's algorithm
 * for single-source shortest paths can be shown to run in O(m + n lg n) using
 * a Fibonacci heap, compared to O(m lg n) using a standard binary or binomial
 * heap.
 *
 * Internally, a Fibonacci heap is represented as a circular, doubly-linked
 * list of trees obeying the min-heap property.  Each node stores pointers
 * to its parent (if any) and some arbitrary child.  Additionally, every
 * node stores its degree (the number of children it has) and whether it
 * is a "marked" node.  Finally, each Fibonacci heap stores a pointer to
 * the tree with the minimum value.
 *
 * To insert a node into a Fibonacci heap, a singleton tree is created and
 * merged into the rest of the trees.  The merge operation works by simply
 * splicing together the doubly-linked lists of the two trees, then updating
 * the min pointer to be the smaller of the minima of the two heaps.  Peeking
 * at the smallest element can therefore be accomplished by just looking at
 * the min element.  All of these operations complete in O(1) time.
 *
 * The tricky operations are dequeueMin and decreaseKey.  dequeueMin works
 * by removing the root of the tree containing the smallest element, then
 * merging its children with the topmost roots.  Then, the roots are scanned
 * and merged so that there is only one tree of each degree in the root list.
 * This works by maintaining a dynamic array of trees, each initially null,
 * pointing to the roots of trees of each dimension.  The list is then scanned
 * and this array is populated.  Whenever a conflict is discovered, the
 * appropriate trees are merged together until no more conflicts exist.  The
 * resulting trees are then put into the root list.  A clever analysis using
 * the potential method can be used to show that the amortized cost of this
 * operation is O(lg n), see "Introduction to Algorithms, Second Edition" by
 * Cormen, Rivest, Leiserson, and Stein for more details.
 *
 * The other hard operation is decreaseKey, which works as follows.  First, we
 * update the key of the node to be the new value.  If this leaves the node
 * smaller than its parent, we're done.  Otherwise, we cut the node from its
 * parent, add it as a root, and then mark its parent.  If the parent was
 * already marked, we cut that node as well, recursively mark its parent,
 * and continue this process.  This can be shown to run in O(1) amortized time
 * using yet another clever potential function.  Finally, given this function,
 * we can implement delete by decreasing a key to -\infty, then calling
 * dequeueMin to extract it.
 */

import java.util.*; // For ArrayList

/**
 * A class representing a Fibonacci heap.
 *
 * @author Keith Schwarz (htiek@cs.stanford.edu)
 */
public class FibonacciHeap<T> {
    /* In order for all of the Fibonacci heap operations to complete in O(1),
     * clients need to have O(1) access to any element in the heap.  We make
     * this work by having each insertion operation produce a handle to the
     * node in the tree.  In actuality, this handle is the node itself, but
     * we guard against external modification by marking the internal fields
     * private.
     */
    public static final class Entry<T> {
        private int degree = 0;       // Number of children
        private boolean marked = false; // Whether this node is marked

        private Entry<T> right;   // Next and previous elements in the list
        private Entry<T> left;

        private Entry<T> parent; // Parent in the tree, if any.

        private Entry<T> child;  // Child node, if any.

        private T elem;     // Element being stored here

        /**
         * Returns the element represented by this heap entry.
         *
         * @return The element represented by this heap entry.
         */
        public T getValue() {
            return elem;
        }

        /**
         * Sets the element associated with this heap entry.
         *
         * @param value The element to associate with this heap entry.
         */
        public void setValue(T value) {
            elem = value;
        }

        /**
         * Constructs a new Entry that holds the given element with the indicated
         * priority.
         *
         * @param elem The element stored in this node.
         */
        private Entry(T elem) {
            right = left = this;
            this.elem = elem;
        }
    }

    /* Pointer to the minimum element in the heap. */
    private Entry<T> minimum = null;

    /* Cached size of the heap, so we don't have to recompute this explicitly. */
    private int size = 0;
    /**
     * Comparator.
     */
    private final Comparator<? super T> comp;

    public FibonacciHeap() {
        this(null);
    }

    public FibonacciHeap(Comparator<? super T> comp) {
        this.comp = comp;
    }

    /**
     * Inserts the specified element into the Fibonacci heap with the specified
     * priority.  Its priority must be a valid double, so you cannot set the
     * priority to NaN.
     *
     * @param value    The value to insert.
     * @return An Entry representing that element in the tree.
     */
    public Entry<T> enqueue(T value) {

        /* Create the entry object, which is a circularly-linked list of length
         * one.
         */
        Entry<T> result = new Entry<>(value);

        /* Merge this singleton list with the tree list. */
        minimum = mergeLists(minimum, result);

        /* Increase the size of the heap; we just added something. */
        ++size;

        /* Return the reference to the new element. */
        return result;
    }

    /**
     * Returns an Entry object corresponding to the minimum element of the
     * Fibonacci heap, throwing a NoSuchElementException if the heap is
     * empty.
     *
     * @return The smallest element of the heap.
     * @throws NoSuchElementException If the heap is empty.
     */
    public Entry<T> min() {
        if (isEmpty())
            throw new NoSuchElementException("Heap is empty.");
        return minimum;
    }

    /**
     * Returns whether the heap is empty.
     *
     * @return Whether the heap is empty.
     */
    public boolean isEmpty() {
        return minimum == null;
    }

    /**
     * Returns the number of elements in the heap.
     *
     * @return The number of elements in the heap.
     */
    public int size() {
        return size;
    }

    /**
     * 将另一个FibonacciHeap合并到当前的FibonacciHeap中。
     */
    public void merge(FibonacciHeap<T> other) {
        /* Merge the two Fibonacci heap root lists together.  This helper function
         * also computes the min of the two lists, so we can store the result in
         * the mMin field of the new heap.
         */
        minimum = mergeLists(minimum, other.minimum);

        /* The size of the new heap is the sum of the sizes of the input heaps. */
        size = size + other.size;
    }

    /**
     * Dequeues and returns the minimum element of the Fibonacci heap.  If the
     * heap is empty, this throws a NoSuchElementException.
     *
     * @return The smallest element of the Fibonacci heap.
     * @throws NoSuchElementException If the heap is empty.
     */
    public Entry<T> dequeueMin() {
        /* Check for whether we're empty. */
        if (isEmpty())
            throw new NoSuchElementException("Heap is empty.");

        /* Otherwise, we're about to lose an element, so decrement the number of
         * entries in this heap.
         */
        --size;

        /* Grab the minimum element so we know what to return. */
        Entry<T> minElem = minimum;

        /* Now, we need to get rid of this element from the list of roots.  There
         * are two cases to consider.  First, if this is the only element in the
         * list of roots, we set the list of roots to be null by clearing mMin.
         * Otherwise, if it's not null, then we write the elements next to the
         * min element around the min element to remove it, then arbitrarily
         * reassign the min.
         */
        if (minimum.right == minimum) { // Case one
            minimum = null;
        } else { // Case two
            minimum.left.right = minimum.right;
            minimum.right.left = minimum.left;
            minimum = minimum.right; // Arbitrary element of the root list.
        }

        /* Next, clear the parent fields of all of the min element's children,
         * since they're about to become roots.  Because the elements are
         * stored in a circular list, the traversal is a bit complex.
         */
        if (minElem.child != null) {
            /* Keep track of the first visited node. */
            Entry<?> curr = minElem.child;
            do {
                curr.parent = null;

                /* Walk to the next node, then stop if this is the node we
                 * started at.
                 */
                curr = curr.right;
            } while (curr != minElem.child);
        }

        /* Next, splice the children of the root node into the topmost list,
         * then set mMin to point somewhere in that list.
         */
        minimum = mergeLists(minimum, minElem.child);

        /* If there are no entries left, we're done. */
        if (minimum == null) return minElem;

        /* Next, we need to coalsce all of the roots so that there is only one
         * tree of each degree.  To track trees of each size, we allocate an
         * ArrayList where the entry at position i is either null or the
         * unique tree of degree i.
         */
        List<Entry<T>> treeTable = new ArrayList<>();

        /* We need to traverse the entire list, but since we're going to be
         * messing around with it we have to be careful not to break our
         * traversal order mid-stream.  One major challenge is how to detect
         * whether we're visiting the same node twice.  To do this, we'll
         * spent a bit of overhead adding all of the nodes to a list, and
         * then will visit each element of this list in order.
         */
        List<Entry<T>> toVisit = new ArrayList<>();

        /* To add everything, we'll iterate across the elements until we
         * find the first element twice.  We check this by looping while the
         * list is empty or while the current element isn't the first element
         * of that list.
         */
        for (Entry<T> curr = minimum; toVisit.isEmpty() || toVisit.get(0) != curr; curr = curr.right)
            toVisit.add(curr);

        /* Traverse this list and perform the appropriate unioning steps. */
        for (Entry<T> curr : toVisit) {
            /* Keep merging until a match arises. */
            while (true) {
                /* Ensure that the list is long enough to hold an element of this
                 * degree.
                 */
                while (curr.degree >= treeTable.size())
                    treeTable.add(null);

                /* If nothing's here, we're can record that this tree has this size
                 * and are done processing.
                 */
                if (treeTable.get(curr.degree) == null) {
                    treeTable.set(curr.degree, curr);
                    break;
                }

                /* Otherwise, merge with what's there. */
                Entry<T> other = treeTable.get(curr.degree);
                treeTable.set(curr.degree, null); // Clear the slot

                /* Determine which of the two trees has the smaller root, storing
                 * the two tree accordingly.
                 */
                boolean smaller = compare(other.elem, curr.elem) < 0;
                Entry<T> min =  smaller ? other : curr;
                Entry<T> max = smaller ? curr : other;

                /* Break max out of the root list, then merge it into min's child
                 * list.
                 */
                max.right.left = max.left;
                max.left.right = max.right;

                /* Make it a singleton so that we can merge it. */
                max.right = max.left = max;
                min.child = mergeLists(min.child, max);

                /* Reparent max appropriately. */
                max.parent = min;

                /* Clear max's mark, since it can now lose another child. */
                max.marked = false;

                /* Increase min's degree; it now has another child. */
                ++min.degree;

                /* Continue merging this tree. */
                curr = min;
            }

            /* Update the global min based on this node.  Note that we compare
             * for <= instead of < here.  That's because if we just did a
             * reparent operation that merged two different trees of equal
             * priority, we need to make sure that the min pointer points to
             * the root-level one.
             */
            if (compare(curr.elem, minimum.elem) < 0) {
                minimum = curr;
            }
        }
        return minElem;
    }

    /**
     * Deletes this Entry from the Fibonacci heap that contains it.
     * <p>
     * It is assumed that the entry belongs in this heap.  For efficiency
     * reasons, this is not checked at runtime.
     *
     * @param entry The entry to delete.
     */
    public void delete(Entry<T> entry) {
        /* Use decreaseKey to drop the entry's key to -infinity.  This will
         * guarantee that the node is cut and set to the global minimum.
         */
        decreaseKeyUnchecked(entry);

        /* Call dequeueMin to remove it. */
        dequeueMin();
    }

    /**
     * Utility function which, given a user-specified priority, checks whether
     * it's a valid double and throws an IllegalArgumentException otherwise.
     *
     * @param priority The user's specified priority.
     * @throws IllegalArgumentException If it is not valid.
     */
    private void checkPriority(double priority) {
        if (Double.isNaN(priority))
            throw new IllegalArgumentException(priority + " is invalid.");
    }

    /**
     * Utility function which, given two pointers into disjoint circularly-
     * linked lists, merges the two lists together into one circularly-linked
     * list in O(1) time.  Because the lists may be empty, the return value
     * is the only pointer that's guaranteed to be to an element of the
     * resulting list.
     * <p>
     * This function assumes that one and two are the minimum elements of the
     * lists they are in, and returns a pointer to whichever is smaller.  If
     * this condition does not hold, the return value is some arbitrary pointer
     * into the doubly-linked list.
     *
     * @param one A pointer into one of the two linked lists.
     * @param two A pointer into the other of the two linked lists.
     * @return A pointer to the smallest element of the resulting list.
     */
    private Entry<T> mergeLists(Entry<T> one, Entry<T> two) {
        /* There are four cases depending on whether the lists are null or not.
         * We consider each separately.
         */
        if (one == null && two == null) { // Both null, resulting list is null.
            return null;
        } else if (one != null && two == null) { // Two is null, result is one.
            return one;
        } else if (one == null) { // One is null, result is two.
            return two;
        } else { // Both non-null; actually do the splice.
            /* This is actually not as easy as it seems.  The idea is that we'll
             * have two lists that look like this:
             *
             * +----+     +----+     +----+
             * |    |--N->|one |--N->|    |
             * |    |<-P--|    |<-P--|    |
             * +----+     +----+     +----+
             *
             *
             * +----+     +----+     +----+
             * |    |--N->|two |--N->|    |
             * |    |<-P--|    |<-P--|    |
             * +----+     +----+     +----+
             *
             * And we want to relink everything to get
             *
             * +----+     +----+     +----+---+
             * |    |--N->|one |     |    |   |
             * |    |<-P--|    |     |    |<+ |
             * +----+     +----+<-\  +----+ | |
             *                  \  P        | |
             *                   N  \       N |
             * +----+     +----+  \->+----+ | |
             * |    |--N->|two |     |    | | |
             * |    |<-P--|    |     |    | | P
             * +----+     +----+     +----+ | |
             *              ^ |             | |
             *              | +-------------+ |
             *              +-----------------+
             *
             */
            Entry<T> oneNext = one.right; // Cache this since we're about to overwrite it.
            one.right = two.right;
            one.right.left = one;
            two.right = oneNext;
            two.right.left = two;

            /* Return a pointer to whichever's smaller. */
            return compare(two.elem, one.elem) < 0 ? two : one;
        }
    }

    /**
     * Decreases the key of a node in the tree without doing any checking to ensure
     * that the new priority is valid.
     *
     * @param entry The node whose key should be decreased.
     */
    private void decreaseKeyUnchecked(Entry<T> entry) {
        /* If the node no longer has a higher priority than its parent, cut it.
         * Note that this also means that if we try to run a delete operation
         * that decreases the key to -infinity, it's guaranteed to cut the node
         * from its parent.
         */
        if (entry.parent != null && compare(entry.elem, entry.parent.elem) < 0) {
            cutNode(entry);
        }

        /* If our new value is the new min, mark it as such.  Note that if we
         * ended up decreasing the key in a way that ties the current minimum
         * priority, this will change the min accordingly.
         */
        if (compare(entry.elem, minimum.elem) < 0) {
            minimum = entry;
        }
    }

    /**
     * Cuts a node from its parent.  If the parent was already marked, recursively
     * cuts that node from its parent as well.
     *
     * @param entry The node to cut from its parent.
     */
    private void cutNode(Entry<T> entry) {
        /* Begin by clearing the node's mark, since we just cut it. */
        entry.marked = false;

        /* Base case: If the node has no parent, we're done. */
        if (entry.parent == null) return;

        /* Rewire the node's siblings around it, if it has any siblings. */
        if (entry.right != entry) { // Has siblings
            entry.right.left = entry.left;
            entry.left.right = entry.right;
        }

        /* If the node is the one identified by its parent as its child,
         * we need to rewrite that pointer to point to some arbitrary other
         * child.
         */
        if (entry.parent.child == entry) {
            /* If there are any other children, pick one of them arbitrarily. */
            if (entry.right != entry) {
                entry.parent.child = entry.right;
            }
            /* Otherwise, there aren't any children left and we should clear the
             * pointer and drop the node's degree.
             */
            else {
                entry.parent.child = null;
            }
        }

        /* Decrease the degree of the parent, since it just lost a child. */
        --entry.parent.degree;

        /* Splice this tree into the root list by converting it to a singleton
         * and invoking the merge subroutine.
         */
        entry.left = entry.right = entry;
        minimum = mergeLists(minimum, entry);

        /* Mark the parent and recursively cut it if it's already been
         * marked.
         */
        if (entry.parent.marked) {
            cutNode(entry.parent);
        } else {
            entry.parent.marked = true;
        }

        /* Clear the relocated node's parent; it's now a root. */
        entry.parent = null;
    }

    public Comparator<? super T> getComparator() {
        return comp;
    }

    /**
     * Key compare utility method.
     * <p>
     * Note that if this heap uses natural ordering, a <code>null</code> key is
     * always considered <i>smaller</i> than a non-null key. This is different
     * from <code>SortedMap</code>, which will generally reject
     * <code>null</code> keys when using natural ordering. This behavior is
     * debateably useful (and/or correct) but you'll just have to learn to like
     * it. Or use your own comparator. Or override this method.
     *
     * @param k1 the first key.
     * @param k2 the second key.
     * @return an integer as like <code>Comparable.compare()</code>.
     * @throws ClassCastException If <code>k1</code> and <code>k2</code> are
     *             not mutually comparable.
     * @see java.util.Comparator#compare(Object,Object)
     */
    @SuppressWarnings("unchecked")
    protected int compare(final T k1, final T k2)
            throws ClassCastException
    {
        return (getComparator() == null ? (((Comparable<T>) k1).compareTo(k2))
                : getComparator().compare(k1, k2));
    }
}

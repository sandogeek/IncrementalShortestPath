package org.sando.heap.fiboheap;

/**
 * 斐波那契堆节点
 */
class Entry<Key> implements IHandle<Key> {
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

    @Override
    public Key getKey() {
        return key;
    }
}

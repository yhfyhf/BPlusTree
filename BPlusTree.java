import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map.Entry;

/**
 * BPlusTree Class Assumptions: 1. No duplicate keys inserted 2. Order D:
 * D<=number of keys in a node <=2*D 3. All keys are non-negative
 * TODO: Rename to BPlusTree
 */
public class BPlusTree<K extends Comparable<K>, T> {

    public Node<K,T> root;
    public static final int D = 2;

    /**
     * TODO Search the value for a specific key
     *
     * @param key
     * @return value
     */
    public T search(K key) {
        return search(root, key);
    }

    /**
     * Helper method for search, which search recursively from root.
     *
     * @param root
     * @param key
     * @return
     */
    private T search(Node<K, T> root, K key) {
        // recursively
        if (root.isLeafNode) {
            for (int i = 0; i < root.keys.size(); i++) {
                if (key.compareTo(root.keys.get(i)) == 0) {
                    return ((LeafNode<K, T>) root).values.get(i);
                }
            }
            return null;
        }

        IndexNode<K, T> indexNodeRoot = (IndexNode<K, T>) root;
        int numKeys = indexNodeRoot.keys.size();
        if (key.compareTo(root.keys.get(0)) < 0) {
            return search(indexNodeRoot.children.get(0), key);
        } else if (key.compareTo(root.keys.get(numKeys - 1)) >= 0) {
            return search(indexNodeRoot.children.get(numKeys), key);
        } else {
            for (int i = 0; i < numKeys; i++) {
                if (key.compareTo(indexNodeRoot.keys.get(i + 1)) < 0) {
                    return search(indexNodeRoot.children.get(i + 1), key);
                }
            }
        }

        return null;
    }

    /**
     * Another implementation of search, which search iteratively.
     *
     * @param key
     * @return
     */
    public T search2(K key) {
        Node<K, T> node = root;
        while (!node.isLeafNode) {
            if (key.compareTo(node.keys.get(node.keys.size() - 1)) >= 0) {
                node = ((IndexNode<K, T>) node).children.get(node.keys.size());
                continue;
            }
            for (int i = 0; i < node.keys.size(); i++) {
                if (key.compareTo(node.keys.get(i)) < 0) {
                    node = ((IndexNode<K, T>) node).children.get(i);
                    break;
                }
            }
        }
        LeafNode<K, T> leafNode = (LeafNode<K, T>) node;
        for (int i = 0; i < leafNode.keys.size(); i++) {
            if (leafNode.keys.get(i).compareTo(key) == 0) {
                return leafNode.values.get(i);
            }
        }
        return null;
    }

    /**
     * TODO Insert a key/value pair into the BPlusTree
     *
     * @param key
     * @param value
     */
    public void insert(K key, T value) {
        if (root == null) {
            root = new LeafNode<K, T>(key, value);
        } else {
            Entry<K, Node<K, T>> overflow = insert(root, key, value);
            if (overflow != null) {
                // overflow node becomes new root
                root = new IndexNode<K, T>(overflow.getKey(), root, overflow.getValue());
            }
        }

    }

    /**
     * Insert key and value into node.
     *
     * First recursively insert key into node and its children,
     * and return the overflow entry. If there is overflow from children
     * nodes, split the node.
     *
     * @param node
     * @param key
     * @param value
     * @return
     */
    private Entry<K, Node<K, T>> insert(Node<K, T> node, K key, T value) {
        Entry<K, Node<K, T>> overflow = null; // overflow is from bottom
        int numKeys = node.keys.size();
        if (node.isLeafNode) {
            LeafNode<K, T> leaf = (LeafNode<K, T>) node;
            leaf.insertSorted(key, value);
            if (leaf.isOverflowed()) {
                return splitLeafNode(leaf);
            }
            return null;
        } else {    // node is IndexNode, insert downward recursively
            IndexNode<K, T> index = (IndexNode<K, T>) node;
            if (key.compareTo(index.keys.get(0)) < 0) {
                overflow = insert(index.children.get(0), key, value);
            } else if (key.compareTo(index.keys.get(numKeys - 1)) >= 0) {
                overflow = insert(index.children.get(numKeys), key, value);
            } else {
                for (int i = 0; i < numKeys; i++) {
                    if (key.compareTo(index.keys.get(i + 1)) < 0) {
                        overflow = insert(index.children.get(i + 1), key, value);
                        break;
                    }
                }
            }
        }

        if (overflow != null) {
            IndexNode<K, T> index = (IndexNode<K, T>) node;
            K overflowKey = overflow.getKey();
            int insertAt = numKeys;      // insert position in parent node
            if (overflowKey.compareTo(index.keys.get(0)) < 0) {
                insertAt = 0;
            } else if (overflowKey.compareTo(index.keys.get(numKeys - 1)) >= 0) {
                insertAt = numKeys;
            } else {
                for (int i = 0; i < numKeys; i++) {
                    if (overflowKey.compareTo(index.keys.get(i)) >= 0 &&
                            overflowKey.compareTo(index.keys.get(i + 1)) < 0) {
                        insertAt = i + 1;
                    }
                }
            }

            index.insertSorted(overflow, insertAt);
            if (index.isOverflowed()) {
                Entry<K, Node<K, T>> rightIndex = splitIndexNode(index);
                return rightIndex;
            }
            return null;
        }

        return overflow;
    }

    /**
     * TODO Split a leaf node and return the new right node and the splitting
     * key as an Entry<slitingKey, RightNode>
     *
     * @param leaf, any other relevant data
     * @return the key/node pair as an Entry
     */
    public Entry<K, Node<K,T>> splitLeafNode(LeafNode<K,T> leaf) {
        assert leaf.isOverflowed();
        LeafNode<K, T> newLeaf = new LeafNode<>(
                new ArrayList<>(leaf.keys.subList(D, 2 * D + 1)),
                new ArrayList<>(leaf.values.subList(D, 2 * D + 1)));
        leaf.keys.subList(D, 2 * D + 1).clear();
        leaf.values.subList(D, 2 * D + 1).clear();

        newLeaf.nextLeaf = leaf.nextLeaf;
        if (newLeaf.nextLeaf != null) {
            newLeaf.nextLeaf.previousLeaf = newLeaf;
        }
        leaf.nextLeaf = newLeaf;
        newLeaf.previousLeaf = leaf;
        return new AbstractMap.SimpleEntry<>(newLeaf.keys.get(0), newLeaf);
    }

    /**
     * TODO split an indexNode and return the new right node and the splitting
     * key as an Entry<slitingKey, RightNode>
     *
     * @param index, any other relevant data
     * @return new key/node pair as an Entry
     */
    public Entry<K, Node<K,T>> splitIndexNode(IndexNode<K,T> index) {
        assert index.isOverflowed();
        IndexNode<K, T> newIndex = new IndexNode<>(
                new ArrayList<>(index.keys.subList(D + 1, 2 * D + 1)),
                new ArrayList<>(index.children.subList(D + 1, 2 * D + 2))
        );
        Entry<K, Node<K, T>> ret = new AbstractMap.SimpleEntry<>(index.keys.get(D), newIndex);
        index.keys.subList(D, 2 * D + 1).clear();
        index.children.subList(D + 1, 2 * D + 2).clear();
        return ret;
    }

    /**
     * TODO Delete a key/value pair from this B+Tree
     *
     * @param key
     */
    public void delete(K key) {
        int deleteAt = delete(root, key, null);
        if (deleteAt != -1) {
            root.keys.remove(deleteAt);
        }
        if (root.keys.isEmpty() && !root.isLeafNode){    // 这儿改过
            root = ((IndexNode<K, T>) root).children.get(0);
        }
    }

    /**
     * Helper method for delete.
     *
     * @param node
     * @param key
     */
    private int delete(Node<K, T> node, K key, IndexNode<K, T> parent) {
        int numKeys = node.keys.size();
        int parentIndex = -1;
        if (node != root) {
            parentIndex = parent.children.indexOf(node);
        }
        int deleteAt = -1;

        if (node.isLeafNode) {
            LeafNode<K, T> leaf = (LeafNode<K, T>) node;
            for (int i = 0; i < numKeys; i++) {
                if (leaf.keys.get(i).compareTo(key) == 0) {
                    leaf.keys.remove(i);
                    leaf.values.remove(i);
                    break;
                }
            }

            if (leaf.isUnderflowed() && leaf != root) {
                if (parentIndex < parent.children.size() - 1) {
                    LeafNode<K, T> right = (LeafNode<K, T>) parent.children.get(parentIndex + 1);
                    return handleLeafNodeUnderflow(leaf, right, parent);
                } else {
                    LeafNode<K, T> left = (LeafNode<K, T>) parent.children.get(parentIndex - 1);
                    return handleLeafNodeUnderflow(left, leaf, parent);
                }
            } else {

                return -1;
                // 这儿要写吗？

            }
        } else {
            // node is an IndexNode
            IndexNode<K, T> index = (IndexNode<K, T>) node;
            if (key.compareTo(index.keys.get(0)) < 0) {
                deleteAt = delete(index.children.get(0), key, index);
            } else if (key.compareTo(index.keys.get(numKeys - 1)) >= 0) {
                deleteAt = delete(index.children.get(numKeys), key, index);
            } else {
                for (int i = 0; i < numKeys; i++) {
                    if (key.compareTo(index.keys.get(i + 1)) < 0) {
                        deleteAt = delete(index.children.get(i + 1), key, index);
                        break;
                    }
                }
            }

            if (deleteAt != -1) {
                if (node == root) {
                    return parentIndex;   // 这个应该就是-1
                }

                index.keys.remove(parentIndex);

                if (index.isUnderflowed()) {
                    if (parentIndex < parent.children.size() - 1) {
                        IndexNode<K, T> right = (IndexNode<K, T>) parent.children.get(parentIndex + 1);
                        return handleIndexNodeUnderflow(index, right, parent);
                    } else {
                        IndexNode<K, T> left = (IndexNode<K, T>) parent.children.get(parentIndex - 1);
                        return handleIndexNodeUnderflow(left, index, parent);
                    }
                }
            }
            return -1;
        }

    }

    /**
     * TODO Handle LeafNode Underflow (merge or redistribution)
     *
     * @param left
     *            : the smaller node
     * @param right
     *            : the bigger node
     * @param parent
     *            : their parent index node
     * @return the splitkey position in parent if merged so that parent can
     *         delete the splitkey later on. -1 otherwise
     */
    public int handleLeafNodeUnderflow(LeafNode<K,T> left, LeafNode<K,T> right,
                                       IndexNode<K,T> parent) {
        assert left.isUnderflowed() || right.isUnderflowed();
        assert left.nextLeaf == right;
        assert left == right.previousLeaf;

        int parentIndex = parent.children.indexOf(right); // index of children refers to right
        if (left.keys.size() + right.keys.size() < 2 * D) {
            // merge
            left.keys.addAll(right.keys);
            left.values.addAll(right.values);

            left.nextLeaf = right.nextLeaf;
            if (left.nextLeaf != null) {
                left.nextLeaf.previousLeaf = left;
            }

            parent.keys.remove(parentIndex - 1);
            parent.children.remove(parentIndex);
            return parentIndex;
        } else {
            // redistribution
            if (left.isUnderflowed()) {
                left.insertSorted(right.keys.remove(0), right.values.remove(0));
            } else {
                right.insertSorted(left.keys.remove(left.keys.size() - 1),
                        left.values.remove(left.values.size() - 1));
            }
            parent.keys.set(parentIndex - 1, parent.children.get(parentIndex).keys.get(0));
            return -1;
        }
    }

    /**
     * TODO Handle IndexNode Underflow (merge or redistribution)
     *
     * @param left
     *            : the smaller node
     * @param right
     *            : the bigger node
     * @param parent
     *            : their parent index node
     * @return the splitkey position in parent if merged so that parent can
     *         delete the splitkey later on. -1 otherwise
     */
    public int handleIndexNodeUnderflow(IndexNode<K,T> left,
                                        IndexNode<K,T> right, IndexNode<K,T> parent) {
        assert left.isUnderflowed() || right.isUnderflowed();

        int parentIndex = parent.children.indexOf(left);   // index of the splitting key in parent node
        assert parent.children.get(parentIndex + 1) == right;

        if (left.keys.size() + right.keys.size() < 2 * D) {
            // merge
            left.keys.add(parent.keys.remove(parentIndex));
            left.keys.addAll(right.keys);
            left.children.addAll(right.children);
            parent.children.remove(parentIndex + 1);
            return parentIndex;
        } else {
            // redistribute
            if (left.isUnderflowed()) {
                left.keys.add(parent.keys.remove(parentIndex));
                parent.keys.add(parentIndex, right.keys.remove(0));
                left.children.add(right.children.remove(0));
            } else {
                right.keys.add(0, parent.keys.get(parentIndex));
                parent.keys.set(parent.keys.size() - 1, left.keys.remove(left.keys.size() - 1));
                right.children.add(0, left.children.get(left.children.size() - 1));
            }
            return -1;
        }
    }

}

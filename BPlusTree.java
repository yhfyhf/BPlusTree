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
        return -1;

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
    public int handleIndexNodeUnderflow(IndexNode<K,T> leftIndex,
            IndexNode<K,T> rightIndex, IndexNode<K,T> parent) {
        return -1;
    }

}

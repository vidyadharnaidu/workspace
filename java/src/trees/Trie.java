package trees;

import java.util.*;

/**
 * Trie (prefix tree): insert, search, startsWith, count by prefix, autocomplete, shortest unique prefix.
 *
 * TRICK: each node = Map<Character, Node> children + boolean isWord (+ int count of words passing through).
 *        Everything is "walk the prefix char by char".
 */
public class Trie {

    static class Node {
        final Map<Character, Node> children = new TreeMap<>(); // TreeMap -> autocomplete comes out sorted
        boolean isWord;
        int count; // how many inserted words pass through this node
    }

    private final Node root = new Node();

    void insert(String word) {
        Node node = root;
        for (char c : word.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new Node());
            node.count++;
        }
        node.isWord = true;
    }

    private Node walk(String prefix) {
        Node node = root;
        for (char c : prefix.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return null;
        }
        return node;
    }

    boolean search(String word) {
        Node node = walk(word);
        return node != null && node.isWord;
    }

    boolean startsWith(String prefix) {
        return walk(prefix) != null;
    }

    int countWithPrefix(String prefix) {
        Node node = walk(prefix);
        return node == null ? 0 : node.count;
    }

    List<String> autocomplete(String prefix) {
        List<String> res = new ArrayList<>();
        Node node = walk(prefix);
        if (node != null) collect(node, new StringBuilder(prefix), res);
        return res;
    }

    private void collect(Node node, StringBuilder path, List<String> res) {
        if (node.isWord) res.add(path.toString());
        for (Map.Entry<Character, Node> e : node.children.entrySet()) {
            path.append(e.getKey());
            collect(e.getValue(), path, res);
            path.deleteCharAt(path.length() - 1); // backtrack
        }
    }

    // Shortest unique prefix: stop at the first node only ONE word passes through
    String shortestUniquePrefix(String word) {
        Node node = root;
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            node = node.children.get(c);
            sb.append(c);
            if (node.count == 1) break;
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        Trie trie = new Trie();
        List.of("apple", "app", "application", "apt", "banana", "band").forEach(trie::insert);

        System.out.println("search app       " + trie.search("app"));            // true
        System.out.println("search appl      " + trie.search("appl"));           // false
        System.out.println("startsWith appl  " + trie.startsWith("appl"));       // true
        System.out.println("count 'ap'       " + trie.countWithPrefix("ap"));    // 4
        System.out.println("autocomplete app " + trie.autocomplete("app"));      // [app, apple, application]
        System.out.println("autocomplete ban " + trie.autocomplete("ban"));      // [banana, band]

        Trie t2 = new Trie();
        List<String> words = List.of("zebra", "dog", "duck", "dove");
        words.forEach(t2::insert);
        words.forEach(w -> System.out.println("unique prefix    " + w + " -> " + t2.shortestUniquePrefix(w)));
        // zebra -> z, dog -> dog, duck -> du, dove -> dov
    }
}

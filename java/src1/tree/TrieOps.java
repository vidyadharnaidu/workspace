import java.util.*;

/**
 * PATTERN: a 26-way tree. One node = one prefix.
 *   children[26] + a boolean "a word ends here".
 * Walk the chars, create missing links on insert, follow links on search.
 */
public class TrieOps {

    static class Node {
        Node[] kids = new Node[26];
        boolean word;
        String full;                                              // handy for wordSearch2
    }

    // HOOK: walk the chars, creating nodes as you go, flag the last one.
    static class Trie {
        final Node root = new Node();

        void insert(String w) {
            Node cur = root;
            for (char c : w.toCharArray()) {
                if (cur.kids[c - 'a'] == null) cur.kids[c - 'a'] = new Node();
                cur = cur.kids[c - 'a'];
            }
            cur.word = true;
            cur.full = w;
        }

        boolean search(String w) {
            Node n = find(w);
            return n != null && n.word;
        }

        boolean startsWith(String p) { return find(p) != null; }

        Node find(String s) {
            Node cur = root;
            for (char c : s.toCharArray()) {
                cur = cur.kids[c - 'a'];
                if (cur == null) return null;
            }
            return cur;
        }
    }

    // HOOK: same trie, but '.' forks into all 26 children - so search becomes a DFS.
    static class WordDictionary {
        final Node root = new Node();

        void addWord(String w) {
            Node cur = root;
            for (char c : w.toCharArray()) {
                if (cur.kids[c - 'a'] == null) cur.kids[c - 'a'] = new Node();
                cur = cur.kids[c - 'a'];
            }
            cur.word = true;
        }

        boolean search(String w) { return dfs(w, 0, root); }

        boolean dfs(String w, int i, Node n) {
            if (n == null) return false;
            if (i == w.length()) return n.word;
            char c = w.charAt(i);
            if (c != '.') return dfs(w, i + 1, n.kids[c - 'a']);
            for (Node k : n.kids) if (dfs(w, i + 1, k)) return true;
            return false;
        }
    }

    // HOOK: put the WORDS in a trie, then DFS the board once carrying the trie node.
    static List<String> findWords(char[][] board, String[] words) {
        Trie trie = new Trie();
        for (String w : words) trie.insert(w);
        List<String> res = new ArrayList<>();
        for (int r = 0; r < board.length; r++)
            for (int c = 0; c < board[0].length; c++)
                explore(board, r, c, trie.root, res);
        return res;
    }

    static void explore(char[][] b, int r, int c, Node n, List<String> res) {
        if (r < 0 || r >= b.length || c < 0 || c >= b[0].length) return;
        char ch = b[r][c];
        if (ch == '#') return;                                    // already on the path
        Node next = n.kids[ch - 'a'];
        if (next == null) return;                                 // no word has this prefix
        if (next.word) { res.add(next.full); next.word = false; } // de-dupe by unflagging
        b[r][c] = '#';
        explore(b, r + 1, c, next, res);
        explore(b, r - 1, c, next, res);
        explore(b, r, c + 1, next, res);
        explore(b, r, c - 1, next, res);
        b[r][c] = ch;                                             // backtrack
    }

    public static void main(String[] args) {
        Trie t = new Trie();
        t.insert("apple");
        System.out.println("search apple      = " + t.search("apple"));
        System.out.println("search app        = " + t.search("app"));
        System.out.println("startsWith app    = " + t.startsWith("app"));

        WordDictionary wd = new WordDictionary();
        wd.addWord("bad");
        wd.addWord("dad");
        wd.addWord("mad");
        System.out.println("search pad        = " + wd.search("pad"));
        System.out.println("search .ad        = " + wd.search(".ad"));
        System.out.println("search b..        = " + wd.search("b.."));

        char[][] board = {
                {'o', 'a', 'a', 'n'},
                {'e', 't', 'a', 'e'},
                {'i', 'h', 'k', 'r'},
                {'i', 'f', 'l', 'v'}};
        System.out.println("wordSearch2       = " + findWords(board, new String[]{"oath", "pea", "eat", "rain"}));
    }
}

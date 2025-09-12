package com.dksd.dvim.mapping.trie;

import java.util.function.Function;

/**
 * A Trie data structure for efficient string operations. This implementation supports inserting,
 * finding, and deleting words along with associated functions.
 */
public class Trie {
    private TrieNode root = new TrieNode();

    /**
     * Inserts a word into the trie and associates it with a function.
     * If the word already exists, adds another function to the existing node.
     *
     * @param desc      description associated with the function
     * @param rightFunc the function to associate with this word
     * @return the TrieNode corresponding to the inserted word
     */
    public TrieNode insert(String left, String desc, Function<String, String> rightFunc) {
        TrieNode current = root;

        for (char l: left.toCharArray()) {
            current = current.getChildren().computeIfAbsent(l, c -> new TrieNode());
        }
        current.setIsCompleteWord(true);
        current.setLeftWord(left);
        current.setDescription(desc);
        current.addFunction(desc, rightFunc);
        return current;
    }

    /**
     * Finds a word in the trie.
     *
     * @param word the word to find; must not be {@code null}
     * @return the TrieNode if the word is found, otherwise {@code null}
     */
    public TrieNode find(String word) {
        TrieNode current = root;
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            TrieNode node = current.getChildren().get(ch);
            if (node == null) {
                return null;
            }
            current = node;
        }
        return current;
    }

    /**
     * Deletes a word from the trie. If the word does not exist, the trie remains unchanged.
     *
     * @param word the word to delete; must not be {@code null}
     */
    public void delete(String word) {
        delete(root, word, 0);
    }

    private boolean delete(TrieNode current, String word, int index) {
        if (index == word.length()) {
            if (!current.isCompleteWord()) {
                return false;
            }
            current.setIsCompleteWord(false);
            return current.getChildren().isEmpty();
        }
        char ch = word.charAt(index);
        TrieNode node = current.getChildren().get(ch);
        if (node == null) {
            return false;
        }
        boolean shouldDeleteCurrentNode = delete(node, word, index + 1) && !node.isCompleteWord();

        if (shouldDeleteCurrentNode) {
            current.getChildren().remove(ch);
            return current.getChildren().isEmpty();
        }
        return false;
    }
}
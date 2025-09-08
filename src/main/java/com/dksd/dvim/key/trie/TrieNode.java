package com.dksd.dvim.key.trie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TrieNode {
    private Map<Character, TrieNode> children = new HashMap<>();
    private String content;
    private boolean isWord;
    private List<Function<String, String>> functions = Collections.synchronizedList(new ArrayList<>());
    private List<String> descriptions = new ArrayList<>();
    private boolean hideMap;

    public Map<Character, TrieNode> getChildren() {
        return children;
    }

    public void setChildren(Map<Character, TrieNode> children) {
        this.children = children;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isWord() {
        return isWord;
    }

    public void setWord(boolean word) {
        isWord = word;
    }

    public Function<String, String> getLastFunc() {
        return functions.getLast();
    }

    public void addFunction(String desc, Function<String, String> remapFunc) {
        descriptions.add(desc);
        functions.add(remapFunc);
    }

    public void setHideMapping(boolean hideMap) {
        this.hideMap = hideMap;
    }
}
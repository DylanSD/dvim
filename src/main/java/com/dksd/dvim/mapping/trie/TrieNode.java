package com.dksd.dvim.mapping.trie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class TrieNode {
    //TODO can delay creating mem until needed.
    private Map<Character, TrieNode> children = new ConcurrentHashMap<>();
    private String content;
    private boolean isCompleteWord;
    private List<Function<String, String>> functions = Collections.synchronizedList(new ArrayList<>());
    private boolean hideMap;
    private String left;
    private String desc;

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

    public boolean isCompleteWord() {
        return isCompleteWord;
    }

    public void setIsCompleteWord(boolean isCompleteWord) {
        this.isCompleteWord = isCompleteWord;
    }

    public Function<String, String> getLastFunc() {
        return functions.getLast();
    }

    public Function<String, String> removeLastFunc() {
        if (functions.size() > 1) {
            return functions.removeLast();
        }
        return functions.getLast();
    }

    public void addFunction(String desc, Function<String, String> remapFunc) {
        functions.add(remapFunc);
    }

    public void setHideMapping(boolean hideMap) {
        this.hideMap = hideMap;
    }

    public void setLeftWord(String left) {
        this.left = left;
    }

    public void setDescription(String desc) {
        this.desc = desc;
    }

    public String getLeft() {
        return left;
    }

    public void setCompleteWord(boolean completeWord) {
        isCompleteWord = completeWord;
    }

    public List<Function<String, String>> getFunctions() {
        return functions;
    }

    public void setFunctions(List<Function<String, String>> functions) {
        this.functions = functions;
    }

    public boolean isHideMap() {
        return hideMap;
    }

    public void setHideMap(boolean hideMap) {
        this.hideMap = hideMap;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

}
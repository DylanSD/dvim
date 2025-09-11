package com.dksd.dvim.mapping.trie;

import com.dksd.dvim.view.VimMode;
import com.googlecode.lanterna.input.KeyStroke;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class TrieMapManager {

    private final Map<KeyStroke, String> keyStrokeToStringMapping = new ConcurrentHashMap<>();
    private final Map<VimMode, Trie> mappings = new ConcurrentHashMap<>();
    private Function<String, String> prevFunctionRun = null;

    public void mapRecursively(List<TrieNode> nodes, int depth, VimMode vimMode, String keyStrokes) {
        if (keyStrokes == null || depth > 10) {
            return;
        }
        TrieNode foundNode = mappings.get(vimMode).find(keyStrokes);
        if (foundNode == null) {
            return;
        }
        nodes.add(foundNode);
        if (foundNode.isWord()) {
            String funcResult = foundNode.getLastFunc().apply(keyStrokes);
            if (!".".equals(keyStrokes)) {
                prevFunctionRun = foundNode.getLastFunc();
            }
            mapRecursively(nodes, depth + 1, vimMode, funcResult);
        }
    }

    public void reMap(List<VimMode> vimModes, String left, String desc, Function<String, String> remapFunc, boolean hideMapping) {
        for (VimMode vimMode : vimModes) {
//            if (!mappings.containsKey(vimMode)) {
//                putKeyMap(vimModes, left, desc, remapFunc, hideMapping);
//            }
            TrieNode node = mappings.get(vimMode).find(left);
            if (node != null) {
                if (!node.getLastFunc().equals(remapFunc)) {
                    node.addFunction(desc, remapFunc);
                }
            }
        }
    }

    public List<TrieNode> putKeyMap(List<VimMode> vimModes,
                                    String left,
                                    String description,
                                    Function<String, String> rightFunc) {
        return putKeyMap(vimModes, left, description, rightFunc, false);
    }

    public List<TrieNode> putKeyMap(List<VimMode> vimModes,
                                    String left,
                                    String description,
                                    Function<String, String> rightFunc,
                                    boolean systemMap) {
        return putKeyMap(vimModes, List.of(left), description, rightFunc, systemMap);
    }

    public TrieNode putKeyMap(VimMode vimMode,
                              String left,
                              String description,
                              Function<String, String> rightFunc) {
        return putKeyMap(vimMode, left, description, rightFunc, false);
    }

    public List<TrieNode> putKeyMap(List<VimMode> vimModes,
                                    List<String> lefts,
                                    String description,
                                    Function<String, String> rightFunc) {
        return putKeyMap(vimModes, lefts, description, rightFunc, false);
    }

    /**
     * @param vimMode
     * @param left
     * @param description
     * @param rightFunc
     * @param hideMap
     */
    public TrieNode putKeyMap(VimMode vimMode,
                              String left,
                              String description,
                              Function<String, String> rightFunc,
                              boolean hideMap) {
        mappings.computeIfAbsent(vimMode, k -> new Trie());
        TrieNode node = mappings.get(vimMode).insert(left, description, rightFunc);
        node.setHideMapping(hideMap);
        return node;
    }

    public List<TrieNode> putKeyMap(VimMode vimMode,
                                    List<String> lefts,
                                    String description,
                                    Function<String, String> rightFunc,
                                    boolean systemMap) {
        return putKeyMap(List.of(vimMode), lefts, description, rightFunc, systemMap);
    }

    public List<TrieNode> putKeyMap(VimMode vimMode,
                                    List<String> lefts,
                                    String description,
                                    Function<String, String> rightFunc) {
        return putKeyMap(List.of(vimMode), lefts, description, rightFunc, false);
    }

    public List<TrieNode> putKeyMap(List<VimMode> vimModes,
                                    List<String> lefts,
                                    String description,
                                    Function<String, String> rightFunc,
                                    boolean systemMap) {
        List<TrieNode> retNodes = new ArrayList<>();
        for (VimMode vimMode : vimModes) {
            for (String left : lefts) {
                putKeyMap(vimMode, left, description, rightFunc, systemMap);
            }
        }
        return retNodes;
    }

    public Function<String, String> getPrevFunctionRun() {
        return prevFunctionRun;
    }

    public String toVim(List<KeyStroke> keyStrokes) {
        StringBuffer sb = new StringBuffer();
        for (KeyStroke keyStroke : keyStrokes) {
            sb.append(keyStrokeToStringMapping.get(keyStroke));
        }
        String ret = sb.toString();
        return ret;
    }

    public void addStrokeMapping(KeyStroke keyStroke, String chr) {
        keyStrokeToStringMapping.put(keyStroke, chr);
    }

    public void mapWords(List<TrieNode> foundNodes, VimMode vimMode, String cont) {
        if (!mappings.containsKey(vimMode)) {
            System.out.println("Could not find the mapping/complete: " + cont);
            return;
        }
        TrieNode foundNode = mappings.get(vimMode).find(cont);
        if (foundNode == null) {
            return;
        }
        if (foundNode.isWord()) {
            foundNodes.add(foundNode);
        }
    }
}

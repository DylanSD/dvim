package com.dksd.dvim.mapping.trie;

import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.engine.VimEng;
import com.dksd.dvim.mapping.VimKey;
import com.dksd.dvim.view.VimMode;
import com.googlecode.lanterna.input.KeyStroke;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class TrieMapManager {

    private final Map<KeyStroke, String> keyStrokeToStringMapping = new ConcurrentHashMap<>();
    private final Map<VimMode, Trie> mappings = new ConcurrentHashMap<>();
    private Set<TrieNode> remappedNodes = new HashSet<>();
    private List<TrieNode> prevExecFunctionNodes = new ArrayList<>();
    private Map<List<VimKey>, String> cachedToVim = new ConcurrentHashMap<>();

    public TrieNode mapRecursively(Buf statusBuf, VimMode vimMode, List<VimKey> keyStrokes) {
        if (keyStrokes == null) {
            return null;
        }
        String keyStrokesStr = toVim(keyStrokes);
        statusBuf.updateStatusBuffer(vimMode, keyStrokesStr);
        TrieNode foundNode = mappings.get(vimMode).find(keyStrokesStr);
        if (foundNode == null) {
            return null;
        }
        if (foundNode.isCompleteWord()) {
            String funcResult = foundNode.getLastFunc().apply(keyStrokesStr);
            if (!".".equals(keyStrokesStr)) {
                prevExecFunctionNodes.addFirst(foundNode);
            }
            //mapRecursively(foundNodesResponse, depth + 1, vimMode, funcResult);
        }
        return foundNode;
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
                    remappedNodes.add(node);
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

    public List<TrieNode> getPrevFunctionRuns() {
        return prevExecFunctionNodes;
    }

    public String toVim(List<VimKey> keyStrokes) {
        if (cachedToVim.containsKey(keyStrokes)) {
            return cachedToVim.get(keyStrokes);
        }
        StringBuffer sb = new StringBuffer();
        for (VimKey keyStroke : keyStrokes) {
            sb.append(keyStrokeToStringMapping.get(keyStroke.getKeyStroke()));
        }
        String ret = sb.toString();
        cachedToVim.put(keyStrokes, ret);
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
        if (foundNode.isCompleteWord()) {
            foundNodes.add(foundNode);
        }
    }

    public void removeRemappings() {
        for (TrieNode remappedNode : remappedNodes) {
            remappedNode.removeLastFunc();
        }
    }
}

package com.dksd.dvim.mapping;

import com.dksd.dvim.mapping.trie.TrieMapManager;
import com.dksd.dvim.mapping.trie.TrieNode;
import com.dksd.dvim.view.VimMode;
import com.googlecode.lanterna.input.KeyStroke;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class KeyMappingMatcherTest {

    private KeyMappingMatcher matcher;

    @BeforeEach
    void setUp() {
        TrieMapManager trieMapManager = new TrieMapManager();
        matcher = new KeyMappingMatcher(trieMapManager);
        trieMapManager.putKeyMap(VimMode.COMMAND, "a", "first", s -> "first");
        trieMapManager.putKeyMap(VimMode.COMMAND, "<leader>b", "second", s -> "second");
        trieMapManager.putKeyMap(VimMode.COMMAND, "<leader>bc", "third", s -> "third");

    }

    @Test
    void matchExactMapping() throws ExecutionException, InterruptedException {
        KeyStroke aStroke = new KeyStroke('a', false, false, false);
        KeyStroke leader = new KeyStroke(' ', false, false, false);
        KeyStroke bStroke = new KeyStroke('b', false, false, false);
        KeyStroke cStroke = new KeyStroke('c', false, false, false);
        matcher.getTrieMapManager().addStrokeMapping(aStroke, "a");
        matcher.getTrieMapManager().addStrokeMapping(leader, "<leader>");
        matcher.getTrieMapManager().addStrokeMapping(bStroke, "b");
        matcher.getTrieMapManager().addStrokeMapping(cStroke, "c");
        CompletableFuture<TrieNode> future = matcher.match(VimMode.COMMAND, aStroke);
        future = matcher.match(VimMode.COMMAND, leader);
        future = matcher.match(VimMode.COMMAND, bStroke);
        future = matcher.match(VimMode.COMMAND, cStroke);
        TrieNode result = future.get();
        assertEquals("<leader>bc", result.getLeft(), "Should match the '<leader>bc' mapping");

        //fnodes = matcher.match(VimMode.COMMAND, new KeyStroke('a', false, false, false));
        //assertEquals("action1", result, "Should match the 'abc' mapping");
        //fnodes = matcher.match(VimMode.COMMAND, new KeyStroke('a', false, false, false));

        //Thread.sleep(150); // depends on your matcher’s timeout

    }
}

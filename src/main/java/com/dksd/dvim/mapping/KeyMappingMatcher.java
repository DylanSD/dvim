package com.dksd.dvim.mapping;

import com.dksd.dvim.engine.VimEng;
import com.dksd.dvim.mapping.trie.TrieMapManager;
import com.dksd.dvim.mapping.trie.TrieNode;
import com.dksd.dvim.view.VimMode;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class KeyMappingMatcher {

    private final TrieMapManager trieMapManager;
    private final LinkedBlockingQueue<VimKey> keyStrokes = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingTask;

    public KeyMappingMatcher(TrieMapManager trieMapManager) {
        this.trieMapManager = trieMapManager;
    }

    // Every match call will return the future that completes after 50ms
    public CompletableFuture<TrieNode> match(VimEng vimEng, VimMode vimMode, KeyStroke key) {
        keyStrokes.add(new VimKey(vimMode, key));
        CompletableFuture<TrieNode> resultFuture = new CompletableFuture<>();

        if (pendingTask != null && !pendingTask.isDone()) {
            pendingTask.cancel(false);
        }

        if (key.getKeyType().equals(KeyType.Character) &&
                keyStrokes.peek() != null &&
                keyStrokes.peek().getKeyStroke().getCharacter() == ' ') {
            pendingTask = scheduler.schedule(() -> {
                try {
                    executeMapping(vimEng, vimMode, keyStrokes, resultFuture);
                } catch (Exception e) {
                    resultFuture.completeExceptionally(e);
                }
            }, 50, TimeUnit.MILLISECONDS);
        } else {
            executeMapping(vimEng, vimMode, keyStrokes, resultFuture);
        }
        return resultFuture;
    }

    private void executeMapping(VimEng vimEng,
                                VimMode vimMode,
                                LinkedBlockingQueue<VimKey> keyStrokes,
                                CompletableFuture<TrieNode> future) {
        List<VimKey> keys = new ArrayList<>(keyStrokes.size());
        keyStrokes.drainTo(keys);
        future.complete(trieMapManager.mapRecursively(vimEng, vimMode, keys));
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    public TrieMapManager getTrieMapManager() {
        return trieMapManager;
    }
}

package com.dksd.dvim.mapping;

import com.dksd.dvim.mapping.trie.TrieMapManager;
import com.dksd.dvim.mapping.trie.TrieNode;
import com.dksd.dvim.view.VimMode;
import com.googlecode.lanterna.input.KeyStroke;

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
    public CompletableFuture<TrieNode> match(VimMode vimMode, KeyStroke key) {
        keyStrokes.add(new VimKey(vimMode, key));

        // Cancel any previously scheduled match
        if (pendingTask != null && !pendingTask.isDone()) {
            pendingTask.cancel(false);
        }

        // Prepare a new future
        CompletableFuture<TrieNode> resultFuture = new CompletableFuture<>();

        // Schedule the task exactly 50ms later
        pendingTask = scheduler.schedule(() -> {
            try {
                List<VimKey> keys = new ArrayList<>(keyStrokes.size());
                keyStrokes.drainTo(keys);
                TrieNode foundNode =
                        trieMapManager.mapRecursively(vimMode, keys);
                resultFuture.complete(foundNode);
            } catch (Exception e) {
                resultFuture.completeExceptionally(e);
            }
        }, 50, TimeUnit.MILLISECONDS);

        return resultFuture;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    public List<KeyStroke> getKeyStrokesAsList() {
        return keyStrokes.stream().map(VimKey::getKeyStroke).toList();
    }

    public TrieMapManager getTrieMapManager() {
        return trieMapManager;
    }
}

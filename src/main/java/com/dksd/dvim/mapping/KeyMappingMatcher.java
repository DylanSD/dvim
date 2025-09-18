package com.dksd.dvim.mapping;

import com.dksd.dvim.mapping.trie.TrieMapManager;
import com.dksd.dvim.mapping.trie.TrieNode;
import com.dksd.dvim.view.View;
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
    public CompletableFuture<TrieNode> match(View view, VimMode vimMode, KeyStroke key) {
        keyStrokes.add(new VimKey(vimMode, key));
        CompletableFuture<TrieNode> resultFuture = new CompletableFuture<>();

        if (pendingTask != null && !pendingTask.isDone()) {
            pendingTask.cancel(false);
        }

        if (VimMode.COMMAND.equals(vimMode) &&
                key.getKeyType().equals(KeyType.Character) &&
                keyStrokes.peek() != null &&
                keyStrokes.peek().getKeyStroke().getCharacter() == ' ') {
            pendingTask = scheduler.schedule(() -> {
                try {
                    executeMapping(view, vimMode, keyStrokes, resultFuture);
                } catch (Exception e) {
                    resultFuture.completeExceptionally(e);
                    e.printStackTrace();
                }
            }, 250, TimeUnit.MILLISECONDS);
        } else {
            executeMapping(view, vimMode, keyStrokes, resultFuture);
        }
        return resultFuture;
    }

    private void executeMapping(View view,
                                VimMode vimMode,
                                LinkedBlockingQueue<VimKey> keyStrokes,
                                CompletableFuture<TrieNode> future) {
        List<VimKey> keys = new ArrayList<>(keyStrokes.size());
        keyStrokes.drainTo(keys);
        future.complete(trieMapManager.mapRecursively(view, vimMode, keys));
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    public TrieMapManager getTrieMapManager() {
        return trieMapManager;
    }
}

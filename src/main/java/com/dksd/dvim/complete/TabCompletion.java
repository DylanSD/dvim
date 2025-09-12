package com.dksd.dvim.complete;

import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.engine.VimEng;
import com.dksd.dvim.event.EventType;
import com.dksd.dvim.event.VimListener;
import com.dksd.dvim.mapping.trie.TrieMapManager;
import com.dksd.dvim.mapping.trie.TrieNode;
import com.dksd.dvim.view.Line;
import com.dksd.dvim.view.View;
import com.dksd.dvim.view.VimMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.dksd.dvim.complete.Telescope.moveArrowInResults;

public class TabCompletion {

    private static final long TIMEOUT = 1000;
    private final CompletableFuture<Line> resultFuture;
    private final ExecutorService executorService;
    private final TrieMapManager tabCompleteTrie = new TrieMapManager();
    public static final String ROW_INDICATOR = " -->";
    private VimListener vimListener;
    private Consumer<Line> consumer;

    public TabCompletion(ExecutorService executorService) {
        this.executorService = executorService;
        resultFuture = new CompletableFuture<>();
    }

    public CompletableFuture<Line> handleTabComplete(List<String> options, VimEng vimEng, Consumer<Line> consumer) {
        this.consumer = consumer;
        int r = vimEng.getRow();
        int c = vimEng.getCol();
        Buf activeBuf = vimEng.getActiveBuf();
        vimEng.getView().setTabComplete(vimEng.getCurrentLine());
        //Also call function, move to tab complete logic
        Buf tabBuf = vimEng.getView().getBufferByName(View.TAB_BUFFER);
        tabBuf.setLinesListStr(options);
        moveArrowInResults(tabBuf, 0, ROW_INDICATOR);

        TrieMapManager tm = vimEng.getKeyMaps().getTrieManager();
        tm.reMap(List.of(VimMode.INSERT, VimMode.COMMAND),
                "<up>",
                "move selection up",
                is -> {
                    moveArrowInResults(tabBuf, -1, ROW_INDICATOR);
                    return null;
                },
                true);

        // <Down> – move selection down
        tm.reMap(List.of(VimMode.INSERT, VimMode.COMMAND), "<down>", "move selection down",
                is -> {
                    moveArrowInResults(tabBuf, 1, ROW_INDICATOR);
                    return null;
                }, true);
        tm.reMap(List.of(VimMode.INSERT, VimMode.COMMAND), "<enter>", "accept selection",
                is -> {
                    Line selected = tabBuf.getCurrentLine();
                    resultFuture.complete(selected);
                    return null;
                }, true);
        tm.reMap(List.of(VimMode.INSERT, VimMode.COMMAND), "<esc>", "accept selection",
                is -> {
                    revertTabComplete(vimEng, tm);
                    return null;
                }, true);
        vimListener = vimEng.getView().addListener(vimEvent -> {
            if (vimEvent.getBufNo() == activeBuf.getBufNo() && EventType.BUF_CHANGE.equals(vimEvent.getEventType())) {
                String cont = vimEng.getView().getBuffer(activeBuf.getBufNo()).getLine(r).getWord(c);
                List<TrieNode> foundNodes = new ArrayList<>();
                tabCompleteTrie.mapWords(foundNodes, vimEng.getVimMode(), cont);
                List<Line> suggestedLines = new ArrayList<>();
                for (int i = 0; i < foundNodes.size(); i++) {
                    TrieNode tn = foundNodes.get(i);
                    suggestedLines.add(new Line(i, tn.getContent(), null));
                }
                tabBuf.setLines(suggestedLines);
            }
        });
        awaitResult();
        return resultFuture;
    }

    private void awaitResult() {
        executorService.submit(() -> {
            try {
                Line lineResult = resultFuture.get(TIMEOUT, TimeUnit.SECONDS);
                if (lineResult != null) {
                    System.out.println("Tab complete result: " + lineResult);
                    consumer.accept(lineResult);
                }
            } catch (Exception e) {
                // Timeout, cancellation or any other problem – just log
                e.printStackTrace();
            }
        });
    }

    private void revertTabComplete(VimEng vimEng, TrieMapManager tm) {
        vimEng.getView().setTabComplete(null);
        vimEng.getView().removeListener(vimListener);
        resultFuture.cancel(true);
        tm.removeRemappings();
    }
}

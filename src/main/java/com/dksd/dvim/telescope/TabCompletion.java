package com.dksd.dvim.telescope;

import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.engine.VimEng;
import com.dksd.dvim.event.EventType;
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

import static com.dksd.dvim.telescope.Telescope.moveArrowInResults;

public class TabCompletion {

    private static final long TIMEOUT = 1000;
    private CompletableFuture<Line> resultFuture;
    private final ExecutorService executorService;


    public TabCompletion(ExecutorService executorService) {
        this.executorService = executorService;
        resultFuture = new CompletableFuture<>();
    }

    public CompletableFuture<Line> handleTabComplete(VimEng vimEng, TrieMapManager tm) {
        int r = vimEng.getRow();
        int c = vimEng.getCol();
        int bufNo = vimEng.getActiveBuf().getBufNo();
        vimEng.getView().setTabComplete(vimEng.getCurrentLine());
        //Also call function, move to tab complete logic
        Buf tabBuf = vimEng.getView().getBufferByName(View.TAB_BUFFER);
        tm.reMap(List.of(VimMode.INSERT, VimMode.COMMAND),
                "<up>",
                "move selection up",
                is -> {
                    moveArrowInResults(tabBuf, -1);
                    return null;
                },
                true);

        // <Down> – move selection down
        tm.reMap(List.of(VimMode.INSERT, VimMode.COMMAND), "<down>", "move selection down",
                is -> {
                    moveArrowInResults(tabBuf, 1);
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
                    vimEng.getView().setTabComplete(null);
                    //TODO revert
                    return null;
                }, true);
        vimEng.getView().addListener(vimEvent -> {
            if (vimEvent.getBufNo() == bufNo && EventType.BUF_CHANGE.equals(vimEvent.getEventType())) {
                String cont = vimEng.getView().getBuffer(bufNo).getLine(r).getWord(c);
                List<TrieNode> foundNodes = new ArrayList<>();
                vimEng.getView().getTabTrie().mapWords(foundNodes, cont);
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
                    System.out.println("Telescope result: " + lineResult);
                    //Figure out how to merge the results.
                    //Nothing to do really.
                    //Actually we should set the result into the buffer
                    //revertTelescopeView(vimEng, currView, telescopeView);
                    //consumer.accept(lineResult);
                }
            } catch (Exception e) {
                // Timeout, cancellation or any other problem – just log
                e.printStackTrace();
                //revertTelescopeView(vimEng, currView, telescopeView);
            }
        });
    }

}

package com.dksd.dvim.complete;

import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.engine.VimEng;
import com.dksd.dvim.event.EventType;
import com.dksd.dvim.event.VimEvent;
import com.dksd.dvim.mapping.trie.TrieMapManager;
import com.dksd.dvim.view.Line;
import com.dksd.dvim.view.View;
import com.dksd.dvim.view.VimMode;
import de.gesundkrank.fzf4j.matchers.FuzzyMatcherV1;
import de.gesundkrank.fzf4j.models.OrderBy;
import de.gesundkrank.fzf4j.models.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Telescope – a tiny, self‑contained UI that shows a list of options,
 * lets the user filter them with fuzzy‑matching and pick one with <Enter>.
 *
 * Example usage:
 *
 *   Telescope telescope = Telescope.builder(vimEng, options, line -> {
 *           // …do something with the selected line…
 *       })
 *       .timeout(60, TimeUnit.SECONDS)               // optional
 *       .matcher(new FuzzyMatcherV1(options, OrderBy.SCORE, true, false)) // optional
 *       .onEsc(() -> System.out.println("User aborted")) // optional
 *       .build();
 *
 *   telescope.start();   // opens the UI and blocks until a line is chosen
 */
public final class Telescope<T, R> {

    public static final String ROW_INDICATOR = ">";
    /* --------------------------------------------------------------- *
     *  Required state (set by the builder)                             *
     * --------------------------------------------------------------- */
    private final VimEng vimEng;
    private List<T> options;
    private Consumer<R> resultConsumer;
    private Function<T, String> optionToStrFunc;
    private Function<Telescope<T, R>, R> onEnterFunc;
    private TrieMapManager trieMapManager;
    private ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    /* --------------------------------------------------------------- *
     *  Optional customisation (builder defaults)                        *
     * --------------------------------------------------------------- */
    private long timeout = 30;
    private TimeUnit timeoutUnit = TimeUnit.SECONDS;
    private FuzzyMatcherV1 matcher;               // can be null → created internally

    /* --------------------------------------------------------------- *
     *  Internal plumbing – not exposed to the user                      *
     * --------------------------------------------------------------- */
    private CompletableFuture<R> resultFuture;
    private View currView;
    private View telescopeView;
    private Buf inputBuf;
    private Buf resultsBuf;
    private int inputBufNo;
    private int resultsBufNo;

    public Telescope(VimEng vimEng) {
        this.vimEng = vimEng;
    }

    public Telescope(VimEng vimEng,
                     List<T> options,
                     Consumer<R> resultConsumer,
                     Function<Telescope<T, R>, R> onEnterFunc,
                     TrieMapManager trieMapManager,
                     long timeout,
                     TimeUnit timeoutUnit) {
        this.vimEng = vimEng;
        this.options = options;
        this.resultConsumer = resultConsumer;
        this.onEnterFunc = onEnterFunc;
        this.trieMapManager = trieMapManager;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }



    /* --------------------------------------------------------------- *
     *  Public entry point                                               *
     * --------------------------------------------------------------- */
    /**
     * Starts the telescope UI.  The call returns immediately – the
     * heavy lifting runs in the background thread pool that the
     * surrounding plugin already owns.
     */
    public void start() {
        resultFuture = new CompletableFuture<>();

        List<String> optionStrs = options.stream().map(obj -> optionToStrFunc.apply(obj)).toList();

        // 1️⃣  Initialise views and buffers
        initViewsAndBuffers();

        // 2️⃣  Populate the results buffer with the raw options
        resultsBuf.setLines(Line.convert(optionStrs));


        // 4️⃣  Set up fuzzy matcher & buffer‑change listener
        registerBufChangeListener(getFuzzyMatcher(optionStrs));

        // 5️⃣  Register the default key‑maps and any extra ones supplied by the caller
        registerDefaultKeyMaps();

        // 6️⃣  Wait for a result (or timeout) and forward it to the consumer
        awaitResult();
    }

    private FuzzyMatcherV1 getFuzzyMatcher(List<String> optionsStr) {
        return (matcher != null) ? matcher
                : new FuzzyMatcherV1(optionsStr, OrderBy.SCORE, true, false);
    }

    /* --------------------------------------------------------------- *
     *  Step 1 – view / buffer preparation                               *
     * --------------------------------------------------------------- */
    private void initViewsAndBuffers() {
        currView = vimEng.getView(VimEng.START_VIEW);
        telescopeView = vimEng.getView(VimEng.TELESCOPE_VIEW);
        telescopeView.reset();

        vimEng.setView(telescopeView);
        vimEng.setVimMode(VimMode.INSERT);

        inputBufNo   = telescopeView.getBufNoByName(View.SIDE_BUFFER);
        resultsBufNo = telescopeView.getBufNoByName(View.MAIN_BUFFER);

        inputBuf   = telescopeView.getBuffer(inputBufNo);
        resultsBuf = telescopeView.getBuffer(resultsBufNo);

        // 3️⃣  Initialise UI state (arrow on first line, focus input)
        telescopeView.setActiveBuf(inputBufNo);
        moveArrowInResults(resultsBuf, 0, ROW_INDICATOR);
    }

    /* --------------------------------------------------------------- *
     *  Step 2 – listener that reacts to user typing in the input buf   *
     * --------------------------------------------------------------- */
    private void registerBufChangeListener(FuzzyMatcherV1 fuzzyMatcher) {
        telescopeView.addListener(vimEvent ->
                handleBufChangeEvent(vimEvent,
                        inputBufNo,
                        inputBuf,
                        fuzzyMatcher,
                        resultsBuf));
    }

    /* --------------------------------------------------------------- *
     *  Step 3 – default key‑maps (<Esc>, <Up>, <Down>, <Enter>)        *
     * --------------------------------------------------------------- */
    private void registerDefaultKeyMaps() {
        trieMapManager.reMap(List.of(VimMode.COMMAND), "<esc>", "escape telescope",
                is -> {
                    revertTelescopeView(vimEng, currView, telescopeView, trieMapManager);
                    return null;
                }, true);

        // <Up> – move selection up
        trieMapManager.reMap(List.of(VimMode.INSERT, VimMode.COMMAND),
                "<up>",
                "move selection up",
                is -> {
                    moveArrowInResults(resultsBuf, -1, ROW_INDICATOR);
                    return null;
                },
                true);

        // <Down> – move selection down
        trieMapManager.reMap(List.of(VimMode.INSERT, VimMode.COMMAND), "<down>", "move selection down",
                is -> {
                    moveArrowInResults(resultsBuf, 1, ROW_INDICATOR);
                    return null;
                }, true);

        // <Enter> – confirm current selection
        trieMapManager.reMap(List.of(VimMode.INSERT, VimMode.COMMAND), "<enter>", "accept selection",
                is -> {
                    resultFuture.complete(onEnterFunc.apply(this));
                    return null;
                }, true);
//        vKeyMaps.reMap(List.of(VimMode.COMMAND), "d", "escape telescope",
//                is -> {
//                    //Delete current entry the arrow is pointing to.
//                    //currentSelected
//                    return null;
//                }, true);
    }

    /* --------------------------------------------------------------- *
     *  Step 4 – wait for the user’s choice (background thread)        *
     * --------------------------------------------------------------- */
    private void awaitResult() {
        executorService.submit(() -> {
            try {
                R lineResult = resultFuture.get(timeout, timeoutUnit);
                if (lineResult != null) {
                    System.out.println("Telescope result: " + lineResult);
                    revertTelescopeView(vimEng, currView, telescopeView, trieMapManager);
                    if (resultConsumer != null) {
                        resultConsumer.accept(lineResult);
                    }
                }
            } catch (Exception e) {
                // Timeout, cancellation or any other problem – just log
                e.printStackTrace();
                revertTelescopeView(vimEng, currView, telescopeView, trieMapManager);
            }
        });
    }

    public static void moveArrowInResults(Buf resultsBuf,
                                          int rowDelta,
                                          String rowIndicator) {
        if (resultsBuf.getCurrentLine() != null) {
            resultsBuf.getCurrentLine().setIndicatorStr(null);
            System.out.println(resultsBuf.getCurrentLine());
            resultsBuf.addToRow(rowDelta);
            resultsBuf.getCurrentLine().setIndicatorStr(rowIndicator);
            System.out.println(resultsBuf.getCurrentLine());
        }
    }

    private List<Result> handleBufChangeEvent(VimEvent vimEvent,
                                              int inputBufNo,
                                              Buf input,
                                              FuzzyMatcherV1 fuzzyMatcher,
                                              Buf results) {
        if (vimEvent.getBufNo() == inputBufNo && EventType.BUF_CHANGE.equals(vimEvent.getEventType())) {
            String currLine = input.getLine(input.getRow()).getContent();
            System.out.println("Received buf change event for buf: " + inputBufNo + " value " + currLine);
            List<Result> keptLines = fuzzyMatcher.match(currLine);
            List<Line> keptLinesForBuf = new ArrayList<>(keptLines.size());
            for (Result keptLine : keptLines) {
                keptLinesForBuf.add(Line.of(keptLine.getItemIndex(), keptLine.getText(), null));
            }
            results.setLines(keptLinesForBuf);
            moveArrowInResults(resultsBuf, 0, ROW_INDICATOR);
            return keptLines;
        }
        return Collections.emptyList();
    }

    private String revertTelescopeView(VimEng vimEng, View currView, View telescopeView, TrieMapManager tm) {
        vimEng.setView(currView);
        telescopeView.removeListeners();
        resultFuture.cancel(true);
        telescopeView.reset();
        tm.removeRemappings();
        System.out.println("reverted telescope view");
        return null;
    }

    public Buf getResultsBuf() {
        return resultsBuf;
    }

    public Buf getInputBuf() {
        return inputBuf;
    }

    public CompletableFuture<R> getResultFuture() {
        return resultFuture;
    }

    public void cancelFuture() {
        resultFuture.cancel(true);
    }

    public void setOptions(List<T> options) {
        this.options = options;
    }

    public void setConsumer(Consumer<R> resultConsumer) {
        this.resultConsumer = resultConsumer;
    }

    public void setOnEnterFunc(Function<Telescope<T, R>, R> onEnterFunc) {
        this.onEnterFunc = onEnterFunc;
    }

    public void setTreiManager(TrieMapManager tm) {
        this.trieMapManager = tm;
    }

    public Function<T, String> getOptionToStrFunc() {
        return optionToStrFunc;
    }

    public void setOptionToStrFunc(Function<T, String> optionToStrFunc) {
        this.optionToStrFunc = optionToStrFunc;
    }
}

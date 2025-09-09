package com.dksd.dvim.telescope;

import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.engine.VimEng;
import com.dksd.dvim.event.EventType;
import com.dksd.dvim.event.VimEvent;
import com.dksd.dvim.key.VKeyMaps;
import com.dksd.dvim.view.Line;
import com.dksd.dvim.view.LineIndicator;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Telescope – a tiny, self‑contained UI that shows a list of options,
 * lets the user filter them with fuzzy‑matching and pick one with <Enter>.
 *
 * The class is built with {@link Telescope.Builder}.  Only three arguments
 * are mandatory (the {@link VimEng} instance, the list of options and a
 * {@link Consumer} that receives the chosen {@link Line}).  Everything
 * else – timeout, matcher configuration, extra key‑maps, etc. – can be
 * customised through the builder.
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
public final class Telescope {

    /* --------------------------------------------------------------- *
     *  Required state (set by the builder)                             *
     * --------------------------------------------------------------- */
    private final VimEng vimEng;
    private final List<String> options;
    private final Consumer<Line> consumer;
    private final VKeyMaps vKeyMaps;
    private final ExecutorService executorService;

    /* --------------------------------------------------------------- *
     *  Optional customisation (builder defaults)                        *
     * --------------------------------------------------------------- */
    private final long timeout;
    private final TimeUnit timeoutUnit;
    private final FuzzyMatcherV1 matcher;               // can be null → created internally

    /* --------------------------------------------------------------- *
     *  Internal plumbing – not exposed to the user                      *
     * --------------------------------------------------------------- */
    private CompletableFuture<Line> resultFuture;
    private View currView;
    private View telescopeView;
    private Buf inputBuf;
    private Buf resultsBuf;
    private int inputBufNo;
    private int resultsBufNo;
    private final AtomicInteger currentSelected = new AtomicInteger(0);
    private final AtomicReference<Line> currentSelLine = new AtomicReference<>();

    /* --------------------------------------------------------------- *
     *  Private constructor – only the Builder can create instances    *
     * --------------------------------------------------------------- */
    private Telescope(Builder builder) {
        this.vimEng = Objects.requireNonNull(builder.vimEng);
        this.options = List.copyOf(Objects.requireNonNull(builder.options));
        this.consumer = Objects.requireNonNull(builder.consumer);

        this.timeout = builder.timeout;
        this.timeoutUnit = Objects.requireNonNull(builder.timeoutUnit);
        this.matcher = builder.matcher;               // may be null → lazy init later
        this.vKeyMaps = builder.vKeyMaps;
        this.executorService = builder.executorService;

        start(); //non-blocking
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

        // 1️⃣  Initialise views and buffers
        initViewsAndBuffers();

        // 4️⃣  Set up fuzzy matcher & buffer‑change listener
        registerBufChangeListener(getFuzzyMatcher());

        // 5️⃣  Register the default key‑maps and any extra ones supplied by the caller
        registerDefaultKeyMaps();

        // 6️⃣  Wait for a result (or timeout) and forward it to the consumer
        awaitResult();
    }

    private FuzzyMatcherV1 getFuzzyMatcher() {
        return (matcher != null) ? matcher
                : new FuzzyMatcherV1(options, OrderBy.SCORE, true, false);
    }

    /* --------------------------------------------------------------- *
     *  Step 1 – view / buffer preparation                               *
     * --------------------------------------------------------------- */
    private void initViewsAndBuffers() {
        currView = vimEng.getView(VimEng.START_VIEW);
        telescopeView = vimEng.getView(VimEng.TELESCOPE_VIEW);

        System.out.println("Hash of curr view: "
                + currView.getBufferByName(View.MAIN_BUFFER).hashCode());
        System.out.println("Hash of telescopeView: "
                + telescopeView.getBufferByName(View.MAIN_BUFFER).hashCode());

        vimEng.setView(telescopeView);

        inputBufNo   = telescopeView.getBufNoByName(View.SIDE_BUFFER);
        resultsBufNo = telescopeView.getBufNoByName(View.MAIN_BUFFER);

        inputBuf   = telescopeView.getBuffer(inputBufNo);
        resultsBuf = telescopeView.getBuffer(resultsBufNo);

        // 2️⃣  Populate the results buffer with the raw options
        resultsBuf.setLines(Line.convert(options));

        // 3️⃣  Initialise UI state (arrow on first line, focus input)
        telescopeView.setActiveBuf(View.SIDE_BUFFER);
        moveArrowInResults(resultsBuf, 0, currentSelected, currentSelLine);
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
        vKeyMaps.reMap(List.of(VimMode.COMMAND), "<esc>", "escape telescope",
                is -> {
                    revertTelescopeView(vimEng, currView, telescopeView);
                    return null;
                }, true);

        // <Up> – move selection up
        vKeyMaps.reMap(List.of(VimMode.INSERT, VimMode.COMMAND),
                "<up>",
                "move selection up",
                is -> {
                    moveArrowInResults(resultsBuf, -1, currentSelected, currentSelLine);
                    return null;
                },
                true);

        // <Down> – move selection down
        vKeyMaps.reMap(List.of(VimMode.INSERT, VimMode.COMMAND), "<down>", "move selection down",
                is -> {
                    moveArrowInResults(resultsBuf, 1, currentSelected, currentSelLine);
                    return null;
                }, true);

        // <Enter> – confirm current selection
        vKeyMaps.reMap(List.of(VimMode.INSERT, VimMode.COMMAND), "<enter>", "accept selection",
                is -> {
                    Line selected = currentSelLine.get();
                    resultFuture.complete(selected);
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
                Line lineResult = resultFuture.get(timeout, timeoutUnit);
                if (lineResult != null) {
                    System.out.println("Telescope result: " + lineResult);
                    revertTelescopeView(vimEng, currView, telescopeView);
                    consumer.accept(lineResult);
                }
            } catch (Exception e) {
                // Timeout, cancellation or any other problem – just log
                e.printStackTrace();
                revertTelescopeView(vimEng, currView, telescopeView);
            }
        });
    }

    /* --------------------------------------------------------------- *
     *  Builder – fluent API                                            *
     * --------------------------------------------------------------- */
    public static Builder builder(VimEng vimEng,
                                  VKeyMaps vKeyMaps) {
        return new Builder(vimEng, vKeyMaps);
    }

    public static final class Builder {
        // required
        private final VimEng vimEng;
        private List<String> options;
        private Consumer<Line> consumer;
        private final VKeyMaps vKeyMaps;
        private ExecutorService executorService;

        // optional – sensible defaults
        private long timeout = 100;                 // 100 units by default
        private TimeUnit timeoutUnit = TimeUnit.SECONDS;
        private FuzzyMatcherV1 matcher = null;      // lazy‑created if null

        private Builder(VimEng vimEng,
                        VKeyMaps vKeyMaps) {
            this.vimEng = vimEng;
            this.vKeyMaps = vKeyMaps;
        }

        /** Change the amount of time we wait for a selection before timing out. */
        public Builder timeout(long amount, TimeUnit unit) {
            this.timeout = amount;
            this.timeoutUnit = Objects.requireNonNull(unit);
            return this;
        }

        public Builder options(List<String> options) {
            this.options = options;
            return this;
        }

        public Builder consumer(Consumer<Line> consumer) {
            this.consumer = consumer;
            return this;
        }

        /** Provide a custom fuzzy matcher (otherwise a default one is built). */
        public Builder matcher(FuzzyMatcherV1 matcher) {
            this.matcher = matcher;
            return this;
        }

        /** Build the immutable {@link Telescope} instance. */
        public Telescope buildAndRun() {
            if (executorService == null) {
                executorService = Executors.newVirtualThreadPerTaskExecutor();
            }
            return new Telescope(this);
        }
    }

    /* --------------------------------------------------------------- *
     *  Helper stubs – these are the same methods you already had in   *
     *  the original code.  They are left untouched here for brevity.   *
     * --------------------------------------------------------------- */
    private void moveArrowInResults(Buf resultsBuf,
                                    int rowDelta,
                                    AtomicInteger currentSelected,
                                    AtomicReference<Line> currentSelLine) {
        LineIndicator li = findSelectedIndicator(resultsBuf);
        int newSelected = currentSelected.get() + rowDelta;
        if (resultsBuf.isRowInBounds(newSelected)) {
            if (li == null) {
                li = new LineIndicator("->", newSelected, LineIndicator.IndicatorType.GUTTER);
                resultsBuf.addIndicator(li);
            }
            li.setLineNo(newSelected);
            currentSelected.set(newSelected);
            currentSelLine.set(resultsBuf.getLine(newSelected));
        }
    }

    private LineIndicator findSelectedIndicator(Buf buf) {
        for (LineIndicator lineIndicator : buf.getLineIndicators()) {
            if (lineIndicator.getIndicatorStr().equals("->")) {
                return lineIndicator;
            }
        }
        return null;
    }

    private List<Result> handleBufChangeEvent(VimEvent vimEvent,
                                              int inputBufNo,
                                              Buf input,
                                              FuzzyMatcherV1 fuzzyMatcher,
                                              Buf results) {
        if (vimEvent.getBufNo() == inputBufNo && EventType.BUF_CHANGE.equals(vimEvent.getEventType())) {
            System.out.println("Received buf change event for buf: " + inputBufNo + " value " + input.getLine());
            List<Result> keptLines = fuzzyMatcher.match(input.getLine().getContent());
            List<Line> keptLinesForBuf = new ArrayList<>(keptLines.size());
            for (Result keptLine : keptLines) {
                keptLinesForBuf.add(Line.of(keptLine.getItemIndex(), keptLine.getText()));
            }
            results.setLines(keptLinesForBuf);
            return keptLines;
        }
        return Collections.emptyList();
    }

    private String revertTelescopeView(VimEng vimEng, View currView, View telescopeView) {
        vimEng.setView(currView);
        telescopeView.removeListeners();
        telescopeView.reset();
        System.out.println("reverted telescope view");
        return null;
    }

}

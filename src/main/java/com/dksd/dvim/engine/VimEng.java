package com.dksd.dvim.engine;

import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.event.EventType;
import com.dksd.dvim.event.VimEvent;
import com.dksd.dvim.event.VimListener;
import com.dksd.dvim.mapping.KeyMappingMatcher;
import com.dksd.dvim.mapping.trie.TrieMapManager;
import com.dksd.dvim.utils.PathHelper;
import com.dksd.dvim.utils.SFormatter;
import com.dksd.dvim.view.Line;
import com.dksd.dvim.view.View;
import com.dksd.dvim.view.VimMode;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.TerminalScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class VimEng {
    private Logger logger = LoggerFactory.getLogger(VimEng.class);
    public static final String START_VIEW = "start_view";
    public static final String TELESCOPE_VIEW = "tele_view";
    private final Map<String, View> views = new ConcurrentHashMap<>();
    private final AtomicReference<VimMode> vimMode = new AtomicReference<>(VimMode.COMMAND);
    private final AtomicReference<String> activeView = new AtomicReference<>(START_VIEW);
    public static final BlockingQueue<VimEvent> events = new LinkedBlockingQueue<>();
    private final List<VimListener> eventListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<VimEng>> backgroundTasks = new CopyOnWriteArrayList<>();
    private final ExecutorService threadPool;
    private final ScheduledExecutorService newScheduledThread = Executors.newSingleThreadScheduledExecutor();
    private TerminalScreen terminalScreen;
    private KeyMappingMatcher keyMappingMatcher;
    public static AtomicReference<String> errorMsg = new AtomicReference<>();

    public VimEng(TerminalScreen screen,
                  ExecutorService threadPool) {
        this.terminalScreen = screen;
        this.threadPool = threadPool;
    }

    public void init(TrieMapManager trieMapManager) {
        views.put(START_VIEW, new View(START_VIEW, terminalScreen));
        views.put(TELESCOPE_VIEW, new View(TELESCOPE_VIEW, terminalScreen));
        keyMappingMatcher = new KeyMappingMatcher(trieMapManager);
        addBackgroundTask(ve -> ve.getView().draw(terminalScreen));
        newScheduledThread.scheduleWithFixedDelay(() -> {
            for (Consumer<VimEng> backgroundTask : backgroundTasks) {
                backgroundTask.accept(this);
            }
        }, 10, 20, TimeUnit.MILLISECONDS);
        addListener(vimEvent -> {
            Buf statusBuf = getView().getBufferByName(View.STATUS_BUFFER);
            String ans = "";
            System.out.println("Received event " + vimEvent);
            if (vimEvent.getEventType().equals(EventType.KEY_PRESS)) {
                ans = SFormatter.format("MODE: {{status}} Keys: {{keys}}", vimMode.get().toString(), vimEvent.getValue());
                statusBuf.setLines(List.of(Line.of(0, ans, null)), 0);
            } else if (vimEvent.getEventType().equals(EventType.MODE_CHANGE)) {
                ans = SFormatter.format("MODE: {{status}}", vimMode.get().toString());
                statusBuf.setLines(List.of(Line.of(0, ans, null)), 0);
            }
        });
        threadPool.execute(() -> {
            while (true) {
                try {
                    VimEvent event = events.poll(10, TimeUnit.SECONDS);
                    if (event == null) {
                        continue;
                    }
                    for (VimListener eventListener : eventListeners) {
                        eventListener.handle(event);
                    }
                } catch (InterruptedException e) {
                    //NOOP
                }
            }
        });
    }

    public VimListener addListener(VimListener vimListener) {
        this.eventListeners.add(vimListener);
        return vimListener;
    }

    public void removeListeners(List<VimListener> vimListeners) {
        eventListeners.removeAll(vimListeners);
    }

    public void addBackgroundTask(Consumer<VimEng> task) {
        backgroundTasks.add(task);
    }

    public View getView() {
        return getView(activeView.get());
    }

    public View getView(String viewName) {
        return views.get(viewName);
    }

    public Buf getActiveBuf() {
        return getBuffer(getActiveBufNo());
    }

    public void stop() {
        threadPool.shutdownNow();
        newScheduledThread.shutdownNow();
        System.exit(0);
    }

    public void splitToNextLine() {
        getView().getActiveBuf().splitToNextLine();
    }

    public void setView(View view) {
        views.putIfAbsent(view.getName(), view);
        activeView.set(view.getName());
    }

    public void executeFunction(Buf activeBuf, String functionToExec) {
        try {
            List<String> params = getParams(functionToExec);
            if (params.isEmpty()) {
                if (activeBuf.getFilename() == null || activeBuf.isEmpty()) {
                    popupErrorMessage("No filename supplied for new buffer!", 10, TimeUnit.SECONDS);
                    return;
                }
                params.add(activeBuf.getFilename());
            }
            if (functionToExec.startsWith("w")) {
                PathHelper.writeFile(params, activeBuf.getLinesDangerous());
                System.out.println("Wrote file: " + params);
            } else if ("r".equals(functionToExec)) {
                activeBuf.setLines(PathHelper.readFile(Path.of(params.getFirst())), 0);
            }
        } catch (Exception ep) {
            ep.printStackTrace();
        }
    }

    private void popupErrorMessage(String errorMsg, int seconds, TimeUnit timeUnit) {
        getView().popupErrorMessage(errorMsg, seconds, timeUnit);
    }

    private List<String> getParams(String functionToExec) {
        if (functionToExec == null || functionToExec.isBlank()) {
            return new ArrayList<>();
        }

        List<String> tokens = new ArrayList<>(Arrays.asList(functionToExec.trim().split("\\s+")));

        if (tokens.size() <= 1) {
            return new ArrayList<>();
        }
        tokens.removeFirst();
        return tokens;
    }

    public void setVimMode(VimMode vimModeIn) {
        vimMode.set(vimModeIn);
        System.out.println("Sending vim mode change event " + vimModeIn);
        events.add(new VimEvent(getView().getName(), getActiveBuf().getBufNo(), EventType.MODE_CHANGE, vimModeIn.toString()));
    }

//    private void setCol(int col) {
//        getView().getBuffer(getView().getActiveBufNo()).setCol(col);
//    }
//
//    private void setRow(int row) {
//        getView().getBuffer(getView().getActiveBufNo()).setRow(row);
//    }

    public VimMode getVimMode() {
        return vimMode.get();
    }

    private Buf getBuffer(int bufNo) {
        return getView().getBuffer(bufNo);
    }

    private int getActiveBufNo() {
        return getView().getActiveBufNo();
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    private View addView(View newView) {
        views.put(newView.getName(), newView);
        return newView;
    }

    public List<String> copyLines(int start, int end) {
        List<String> retLines = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
            retLines.add(getBuffer(getActiveBufNo()).getLine(i).getContent());
        }
        return retLines;
    }

    private TerminalScreen getTerminalScreen() {
        return terminalScreen;
    }

    public void writeBuf(String value) {
        getView().getBuffer(getView().getActiveBufNo()).insertIntoLine(value);
    }

    public void moveCursor(int bufNo, int rowDelta, int colDelta) {
        getView().getBuffer(bufNo).addToRow(rowDelta);
        getView().getBuffer(bufNo).addToCol(colDelta);
    }

    public void moveCursorAbs(int bufNo, int rowDelta, int colDelta) {
        getView().getBuffer(bufNo).addToRow(rowDelta);
        getView().getBuffer(bufNo).addToCol(colDelta);
    }

    public void moveCursor(int rowDelta, int colDelta) {
        moveCursor(getView().getActiveBufNo(), rowDelta, colDelta);
    }

    public void deleteLines(int row) {
        getView().getBuffer(getView().getActiveBufNo()).deleteLine(row);
    }

    public void deleteInLine(int numChars) {
        getView().getBuffer(getView().getActiveBufNo()).deleteInLine(numChars);
    }

    public Line getCurrentLine() {
        return getView().getBuffer(getView().getActiveBufNo()).getCurrentLine();
    }

    public Line getLineAt(int row) {
        return getView().getBuffer(getView().getActiveBufNo()).getLine(row);
    }

    private Line getCurrentLine(View view, int bufNo) {
        return view.getBuffer(bufNo).getCurrentLine();
    }

    public int getCol() {
        return getView().getBuffer(getView().getActiveBufNo()).getCol();
    }

    public int getRow() {
        return getView().getBuffer(getView().getActiveBufNo()).getRow();
    }

    public void setLine(int row, String line) {
        getView().getBuffer(getView().getActiveBufNo()).setLine(row, line);
    }

    public void popPrevChange() {
        getView().getActiveBuf().undo();
    }

    public void cancelTelescope() {
        getView(TELESCOPE_VIEW).reset();
    }

    public KeyMappingMatcher getKeyMappingMatcher() {
        return keyMappingMatcher;
    }

    public void handleKey(KeyStroke key) {
        keyMappingMatcher.match(getView(), getVimMode(), key);
    }

    public BlockingQueue<VimEvent> getEvents() {
        return events;
    }
}

/*
<BS>           Backspace
<Tab>          Tab
<CR>           Enter
<Enter>        Enter
<Return>       Enter
<Esc>          Escape
<Space>        Space
<Up>           Up arrow
<Down>         Down arrow
<Left>         Left arrow
<Right>        Right arrow
<F1> - <F12>   Function keys 1 to 12
#1, #2..#9,#0  Function keys F1 to F9, F10
<Insert>       Insert
<Del>          Delete
<Home>         Home
<End>          End
<PageUp>       Page-Up
<PageDown>     Page-Down
<bar>          the '|' character, which otherwise needs to be escaped '\|'
     */


        /*
    Character,
    Escape,
    Backspace,
    ArrowLeft,
    ArrowRight,
    ArrowUp,
    ArrowDown,
    Insert,
    Delete,
    Home,
    End,
    PageUp,
    PageDown,
    Tab,
    ReverseTab,
    Enter,
    F1,
    F2,
    F3,
    F4,
    F5,
    F6,
    F7,
    F8,
    F9,
    F10,
    F11,
    F12,
    F13,
    F14,
    F15,
    F16,
    F17,
    F18,
    F19,
    Unknown,

         */

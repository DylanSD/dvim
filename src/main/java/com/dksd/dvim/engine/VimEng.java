package com.dksd.dvim.engine;

import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.mapping.KeyMappingMatcher;
import com.dksd.dvim.mapping.VKeyMaps;
import com.dksd.dvim.mapping.trie.TrieMapManager;
import com.dksd.dvim.utils.SFormatter;
import com.dksd.dvim.view.Line;
import com.dksd.dvim.view.View;
import com.dksd.dvim.view.VimMode;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.TerminalScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class VimEng {
    private Logger logger = LoggerFactory.getLogger(VimEng.class);
    public static final String START_VIEW = "start_view";
    public static final String TELESCOPE_VIEW = "tele_view";
    private Map<String, View> views = new ConcurrentHashMap<>();
    private AtomicReference<VimMode> vimMode = new AtomicReference<>(VimMode.COMMAND);
    private TerminalScreen terminalScreen;
    private final ExecutorService threadPool;
    private final ScheduledExecutorService newScheduledThread = Executors.newSingleThreadScheduledExecutor();
    private AtomicReference<String> activeView = new AtomicReference<>(START_VIEW);
    private CopyOnWriteArrayList<Consumer<VimEng>> backgroundTasks = new CopyOnWriteArrayList<>();
    private KeyMappingMatcher keyMappingMatcher;

    public VimEng(TerminalScreen screen,
                  ExecutorService threadPool,
                  TrieMapManager trieMapManager) {
        terminalScreen = screen;
        this.threadPool = threadPool;
        views.put(START_VIEW, new View(START_VIEW, screen, threadPool));
        views.put(TELESCOPE_VIEW, new View(TELESCOPE_VIEW, screen, threadPool));
        keyMappingMatcher = new KeyMappingMatcher(trieMapManager);
        addBackgroundTask(ve -> ve.getView().draw(terminalScreen));
        newScheduledThread.scheduleWithFixedDelay(() -> {
            for (Consumer<VimEng> backgroundTask : backgroundTasks) {
                backgroundTask.accept(this);
            }
        }, 10, 20, TimeUnit.MILLISECONDS);
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

//    public void removeLastKeyStroke() {
//        int last = keyStrokes.size() - 1;
//        while (!keyStrokes.isEmpty() && keyStrokes.get(last).getKeyType().equals(KeyType.Backspace)) {
//            keyStrokes.remove(last);
//            last = keyStrokes.size() - 1;
//        }
//    }

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
            String[] params = getParams(functionToExec);
            if (functionToExec.startsWith("w")) {
                activeBuf.writeFile(params);
            } else if ("r".equals(functionToExec)) {
                activeBuf.readFile(params[0]);
            }
        } catch (Exception ep) {
            ep.printStackTrace();
        }
    }

    private String[] getParams(String functionToExec) {
        if (functionToExec == null || functionToExec.isBlank()) {
            return new String[0]; // nothing to return
        }

        // Split on one-or-more spaces to avoid empty tokens
        String[] tokens = functionToExec.trim().split("\\s+");

        if (tokens.length <= 1) {
            return new String[0]; // no parameters
        }

        return Arrays.copyOfRange(tokens, 1, tokens.length);
    }

    public void setVimMode(VimMode vimModeIn) {
        vimMode.set(vimModeIn);
    }

    public void updateStatusBuffer(String keys) {
        String ans = SFormatter.format("MODE: {{status}} Keys: {{keys}}", vimMode.toString(), keys);
        getView().getBufferByName(View.STATUS_BUFFER).setLines(List.of(Line.of(0, ans, null)));
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

    public void replaceLine(int row, String replaceStr) {
        getView().getBuffer(getView().getActiveBufNo()).replaceLine(row, replaceStr);
    }

    private Buf getBuffer(int bufNo) {
        return getView().getBuffer(bufNo);
    }

    private int getActiveBufNo() {
        return getView().getActiveBufNo();
    }

    /*private void registerEvent(EventType eventName, Consumer<VimEvent> consumer) {
        keyListeners.computeIfAbsent(eventName, k -> new CopyOnWriteArraySet<>());
        keyListeners.get(eventName).add(consumer);
    }

    private void registerEventRemoveOn(EventType eventName,
                                             Consumer<VimEvent> consumer,
                                             EventType exitType) {
        registerEvent(eventName, consumer);
        registerEvent(exitType, s -> {
            if (keyListeners.get(eventName) != null) {
                keyListeners.get(eventName).remove(consumer);
            }
        });
    }*/

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    private View addView(View newView) {
        views.put(newView.getName(), newView);
        return newView;
    }

    public List<String> copyLines(int start, int end) {
        List<String> retLines = new ArrayList<>();
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

    public void loadFile(Buf buf, String fileName) {

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
        keyMappingMatcher.match(this, getVimMode(), key);
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

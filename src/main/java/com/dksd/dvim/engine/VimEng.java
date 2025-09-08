package com.dksd.dvim.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.dksd.dvim.view.VimMode;
import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.key.VKeyMaps;
import com.dksd.dvim.key.trie.TrieNode;
import com.dksd.dvim.utils.SFormatter;
import com.dksd.dvim.view.Line;
import com.dksd.dvim.view.View;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VimEng {
    private Logger logger = LoggerFactory.getLogger(VimEng.class);
    public static final String START_VIEW = "start_view";
    public static final String TELESCOPE_VIEW = "tele_view";
    private Map<String, View> views = new ConcurrentHashMap<>();
    private AtomicReference<VimMode> vimMode = new AtomicReference<>(VimMode.COMMAND);
    private List<KeyStroke> keyStrokes = Collections.synchronizedList(new ArrayList<>());
    private TerminalScreen terminalScreen;
    private final ExecutorService threadPool;
    private final ScheduledExecutorService newScheduledThread = Executors.newSingleThreadScheduledExecutor();

    private AtomicInteger hashDrawn = new AtomicInteger();
    private AtomicLong lastDrawn = new AtomicLong();
    private AtomicLong lastClearKeys = new AtomicLong();
    private AtomicReference<String> activeView = new AtomicReference<>(START_VIEW);

    public VimEng(TerminalScreen screen, ExecutorService threadPool) {
        terminalScreen = screen;
        this.threadPool = threadPool;
        views.put(START_VIEW, new View(START_VIEW, screen, threadPool));
        views.put(TELESCOPE_VIEW, new View(TELESCOPE_VIEW, screen, threadPool));
        newScheduledThread.scheduleWithFixedDelay(() -> {
            long st = System.currentTimeMillis();
            int hash = getView().hashCode();
            if (hash != hashDrawn.get()) {
                //getView().draw(terminalScreen, vimMode.get());
                hash = getView().hashCode();
                hashDrawn.set(hash);
                logger.info("Trigger hash key change draw: " + st);
                lastDrawn.set(st);
            }
            if (st - lastClearKeys.get() > 1000) {
                //TODO cant debug with this
                //VimEng.clearKeys();
                lastClearKeys.set(st);
            }
        }, 10, 50, TimeUnit.MILLISECONDS);
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

    public void removeLastKeyStroke() {
        int last = keyStrokes.size() - 1;
        while (!keyStrokes.isEmpty() && keyStrokes.get(last).getKeyType().equals(KeyType.Backspace)) {
            keyStrokes.remove(last);
            last = keyStrokes.size() - 1;
        }
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

    public void executeFunction(String functionToExec) {
        //TODO for example quit command
        //or some api code even.
    }

    public void clearKeys() {
        keyStrokes.clear();
    }

    public void setVimMode(VimMode vimModeIn) {
        vimMode.set(vimModeIn);
        System.out.println("Setting vimMode: " + vimModeIn);
        updateStatus();
    }

    public void updateStatus() {
        String ans = SFormatter.format("MODE: {{status}} Keys: {{keys}}", getVimMode().toString(), keyStrokes);
        getView().getBufferByName(View.STATUS_BUFFER).setLines(List.of(Line.of(0, ans)));
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

    public void setActiveBuffer(int bufNo) {
        getView().setActiveBufNo(bufNo);
    }

    public void setActiveBufByName(String name) {
        getView().setActiveBuf(name);
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

    private ExecutorService getThreadPool() {
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

    public void moveCursor(int rowDelta, int colDelta) {
        moveCursor(getView().getActiveBufNo(), rowDelta, colDelta);
    }

    public void deleteLines(int rowStart, int rowEnd) {
        getView().getBuffer(getView().getActiveBufNo()).deleteLine(rowStart, rowEnd);
    }

    public void deleteInLine(int numChars) {
        getView().getBuffer(getView().getActiveBufNo()).deleteInLine(numChars);
    }

    public Line getCurrentLine() {
        return getView().getBuffer(getView().getActiveBufNo()).getLine();
    }

    private Line getCurrentLine(View view, int bufNo) {
        return view.getBuffer(bufNo).getLine();
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

    List<TrieNode> foundNodes = new ArrayList<>();
    public void handleKey(VKeyMaps vKeyMaps, KeyStroke key) {
        if (key.getKeyType().equals(KeyType.EOF)) {
            System.exit(0);
        }
        lastClearKeys.set(System.currentTimeMillis());
        if (key.getKeyType().equals(KeyType.Escape)) {
            keyStrokes.clear();
        }

        keyStrokes.add(key);
        updateStatus();
        String vimCommands = vKeyMaps.toVim(keyStrokes);
        foundNodes.clear();
        vKeyMaps.mapRecursively(foundNodes, 0, vimMode.get(), vimCommands);
        if (foundNodes == null) {
            System.out.println("Did not find a command to run: " + vimCommands);
        } else if (!foundNodes.isEmpty() && foundNodes.getFirst().isWord()) {
            keyStrokes.clear();
            System.out.println("found and executed mapping and cleared keystrokes: " + vimCommands);
        } else {
            System.out.println("Not found a command not sure why: " + vimCommands);
        }
    }


    public void setCurrentDir(String dir) {

    }

    public void loadFile(Buf buf, String fileName) {

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

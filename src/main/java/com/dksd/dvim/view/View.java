package com.dksd.dvim.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.catppuccin.Palette;
import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.buffer.BufferMode;
import com.dksd.dvim.event.VimEvent;
import com.dksd.dvim.event.VimListener;
import com.dksd.dvim.higlight.JavaSyntaxHighlighter;
import com.googlecode.lanterna.Symbols;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.TerminalScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class View {

    private Logger logger = LoggerFactory.getLogger(View.class);

    public static final String STATUS_BUFFER = "status";
    public static final String HEADER_BUFFER = "header";
    public static final String MAIN_BUFFER = "main";
    public static final String SIDE_BUFFER = "side"; //used for text complete..and editing lists
    public static final TextColor.Indexed HIGHLIGHT_BG_COLOR = new TextColor.Indexed(237);
    public static final TextColor.Indexed HIGHLIGHT_MATCHED_POS = new TextColor.Indexed(70);
    public static final TextColor.Indexed SELECTED_ITEM_COLOR = new TextColor.Indexed(1);
    public static final int CHECK_RESIZE_INTERVAL_MS = 100;
    private final String name;
    private final Map<Integer, Buf> buffers = new ConcurrentHashMap<>();
    private final BlockingQueue<VimEvent> events = new LinkedBlockingQueue<>();
    private final List<VimListener> eventListeners = new CopyOnWriteArrayList<>();
    private AtomicInteger activeBufNo = new AtomicInteger(-1);
    private int statusBufNo = -1;
    private int headerBufNo = -1;
    private int mainBufNo = -1;
    private int sideBufNo = -1;
    private final ExecutorService executor;
    private JavaSyntaxHighlighter syntaxHighlighter = new JavaSyntaxHighlighter();

    public View(String viewName, TerminalScreen screen, ExecutorService executor) {
        this.name = viewName;
        this.executor = executor;

        Buf statusBuf = createBuf(
                STATUS_BUFFER,
                STATUS_BUFFER + ".txt",
                -1,//indicates fixed
                100,
                false,
                BufferMode.NO_LINE_NUMBERS,
                BufferMode.FIXED_HEIGHT,
                BufferMode.RIGHT_BORDER,
                BufferMode.LEFT_BORDER,
                BufferMode.ABS_POS);
        statusBufNo = statusBuf.getBufNo();

        Buf headerBuf = createBuf(
                HEADER_BUFFER,
                HEADER_BUFFER + ".txt",
                -1,
                100,
                false,
                BufferMode.UNSELECTABLE,
                BufferMode.NO_LINE_NUMBERS,
                BufferMode.FIXED_HEIGHT,
                BufferMode.LEFT_BORDER,
                BufferMode.RIGHT_BORDER,
                BufferMode.ABS_POS);
        headerBuf.addRow("1 - file | 2 - file 2");
        headerBufNo = headerBuf.getBufNo();

        Buf mainBuf = createBuf(
                MAIN_BUFFER,
                MAIN_BUFFER + ".txt",
                100,
                65,
                true,
                BufferMode.RELATIVE_HEIGHT,
                BufferMode.LEFT_BORDER,
                BufferMode.RIGHT_BORDER,
                BufferMode.TOP_BORDER);
        mainBufNo = mainBuf.getBufNo();
        mainBuf.addRow("Line 1 of the main buffer");
        mainBuf.addRow("for (int i = 0; i < 10; i++) { System.out.println('Yoyo');}");
        mainBuf.addRow("Line 3 of the main buffer");
        mainBuf.addRow("Line 4 of the main buffer");
        mainBuf.addRow("Line 5 of the main buffer");
        mainBuf.addRow("Line 6 of the main buffer");
        mainBuf.addRow("Line 7 of the main buffer");
        mainBuf.addRow("Line 8 of the main buffer");
        mainBuf.addRow("Line 9 of the main buffer");
        mainBuf.addRow("Line 10 of the main buffer");
        mainBuf.addRow("Line 11 of the main buffer");
        mainBuf.addRow("Line 6 of the main buffer");
        mainBuf.addRow("Line 7 of the main buffer");
        mainBuf.addRow("Line 8 of the main buffer");
        mainBuf.addRow("Line 9 of the main buffer");
        mainBuf.addRow("Line 10 of the main buffer");
        mainBuf.addRow("Line 11 of the main buffer");
        mainBuf.addRow("Line 11 of the main buffer");


        Buf sideBuf = createBuf(
                SIDE_BUFFER,
                SIDE_BUFFER + ".txt",
                100,
                35,
                true,
                BufferMode.RELATIVE_HEIGHT,
                BufferMode.NO_LINE_NUMBERS,
                BufferMode.LEFT_BORDER,
                BufferMode.TOP_BORDER);
        sideBufNo = sideBuf.getBufNo();
        //sideBuf.addRow("Side buf");

        headerBuf.setTopBufs(List.of(statusBuf));
        mainBuf.setTopBufs(List.of(headerBuf));
        sideBuf.setTopBufs(List.of(headerBuf));
        sideBuf.setLeftBufs(List.of(mainBuf));
        mainBuf.setRightBufs(List.of(sideBuf));

        calcScrollView(screen.getTerminalSize().getColumns(), screen.getTerminalSize().getRows());
        fitScrollView(screen.getTerminalSize().getColumns(), screen.getTerminalSize().getRows());
        /*
        int popoverBufNo = 5;
        buffers.put(popoverBufNo, new Buf(POPOVER_BUFFER, popoverBufNo,
                new ScrollView(-1, -1, -1 , -1),
                events
        ));
        setBufferPermissions(VimMode.ALL, popoverBufNo,
                BufferMode.GUTTER,
                BufferMode.POS_BELOW_CURSOR);
*/

        setActiveBufNo(mainBuf.getBufNo());
        executor.execute(() -> {
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

    public Buf createBuf(String name, String filename, int percentHeight, int percentWidth, boolean keepUndos, BufferMode...bufferModes) {
        int bufNum = buffers.size();
        Buf buf = new Buf(name,
                filename,
                bufNum,
                new ScrollView(percentHeight, percentWidth),
                events,
                keepUndos
        );
        buffers.put(bufNum, buf);
        setBufferPermissions(bufNum, bufferModes);
        return buf;
    }

    public int getActiveBufNo() {
        return activeBufNo.get();
    }

    public void setActiveBufNo(int activeBufNo) {
        this.activeBufNo.set(activeBufNo);
    }

    public Buf getBuffer(Integer bufNo) {
        return buffers.get(bufNo);
    }

    LinkedBlockingQueue<Future<?>> futures = new LinkedBlockingQueue<>();
    public void draw(TerminalScreen screen) {
        long st = System.currentTimeMillis();
        futures.clear();

        screen.doResizeIfNecessary();
        TextGraphics textGraphics = screen.newTextGraphics();

        try {
            screen.clear();
            Buf stBuf = buffers.get(statusBufNo);
            Buf hBuf = buffers.get(headerBufNo);
            Buf mbuf = buffers.get(mainBufNo);
            Buf sideBuf = buffers.get(sideBufNo);

            for (Buf buf : List.of(stBuf, hBuf, mbuf, sideBuf)) {
                drawBuffer(screen, textGraphics, buf, futures);
            }

            for (Future<?> future : futures) {
                future.get();
            }
            long ed = System.currentTimeMillis();
            //System.out.println("Time taken to render in parallel: " + (ed - st) + " screen size: " + screenWidth + ", " + screenHeight);
            //Draw popover buffer now. It should be below the current row unless no space then above.
            //drawPopoverBuf(screen, rowOffset, colOffset, screenWidth, screenHeight);
            //May be too hard...
            //What about another view but the buffer is editable? and we copy over some of the other buffer as ghost text?
            //Or what about splitting the text horizontally, I mean I like vertically personally...

            //terminal.setForegroundColor(new TextColor.RGB(mochaBaseRGB[0], mochaBaseRGB[1], mochaBaseRGB[2]));
            screen.refresh();
        } catch (Exception ep) {
            ep.printStackTrace();
        }
    }

    private void drawBuffer(TerminalScreen screen,
                            TextGraphics textGraphics,
                            Buf buf,
                            LinkedBlockingQueue<Future<?>> futures) {

        List<DispObj> dispLineInclGutters = buf.getLinesToDisplay();

        drawBorders(textGraphics, buf);

        for (int i = 0; i < dispLineInclGutters.size(); i++) {
            DispObj dispObj = dispLineInclGutters.get(i);
            Line gutter = genGutter(buf, dispObj.getLineContent());
                drawString(textGraphics,
                        gutter,
                        dispObj.getScreenCol() - 6,
                        dispObj.getScreenRow(),
                        futures);
                drawString(textGraphics,
                        dispObj.getLineContent(),
                        dispObj.getScreenCol(),
                        dispObj.getScreenRow(),
                        futures);
        }
        if (activeBufNo.get() == buf.getBufNo()) {
            DispObj cursor = buf.getDisplayCursor();
            placeCursor(screen, cursor.getScreenCol(), cursor.getScreenRow());
            /*try {
                logger.info("Char at cursor is: " + buf.getLine(buf.getRow()).getContent().charAt(buf.getCol()));
                System.out.println("Char at cursor is: " + buf.getLine(buf.getRow()).getContent().charAt(buf.getCol()));
            } catch (Exception ep) {
                //NOOP
            }*/
        }
    }

    private void drawBorders(TextGraphics textGraphics, Buf buf) {
        int gutterSize = buf.getGutterSize();
        int colst = buf.getScrollView().getColStart();
        int rowst = buf.getScrollView().getRowStart();
        int coled = buf.getScrollView().getColEnd();
        int rowed = buf.getScrollView().getRowEnd();

        if (buf.containsBufferMode(BufferMode.LEFT_BORDER)) {
            drawVertical(colst + gutterSize, rowst, rowed, textGraphics);
        }
        if (buf.containsBufferMode(BufferMode.RIGHT_BORDER)) {
            drawVertical(coled, rowst, rowed, textGraphics);
        }
        if (buf.containsBufferMode(BufferMode.TOP_BORDER)) {
            drawHorizontal(colst, coled, rowst, textGraphics);
        }
    }

    private int getGutterSize(Buf buf) {
        return buf.getGutterSize();
    }

    private void drawString(TextGraphics tg, Line line, int colOffset, int rowOffset, LinkedBlockingQueue<Future<?>> futures) {
        //syntaxHighlighter.drawHighlightedCode(tg, line.getContent(), rowOffset, colOffset, futures);
//xxx
        tg.putString(colOffset, rowOffset, line.getContent());
    }

    private void generateColors(TextGraphics textGraphics, Buf buf) {
        int[] mochaBaseRGB = Palette.MOCHA.getBase().getRGBComponents(); // [30, 30, 46]
        textGraphics.setForegroundColor(new TextColor.RGB(mochaBaseRGB[0], mochaBaseRGB[1], mochaBaseRGB[2]));
    }

    private Line genGutter(Buf buf,
                           Line line) {
        StringBuilder gutter = new StringBuilder();
        if (line.getIndicatorStr() != null) {
            gutter.append(line.getIndicatorStr());
        }
        if (!buf.containsBufferMode(BufferMode.NO_LINE_NUMBERS)) {
            String numStr = Integer.toString(line.getLineNumber());
            gutter.append(" ".repeat(Math.max(0, 5 - numStr.length() - gutter.length())));
            gutter.append(numStr);
        }
        return new Line(line.getLineNumber(), gutter.toString(), line.getIndicatorStr());
    }

    private static void placeCursor(TerminalScreen screen, int x, int y) {
        screen.setCursorPosition(new TerminalPosition(x, y));
    }

    private boolean isCursorRow(Buf buf, int row) {
        return buf.getRow() == row;
    }

    private static void drawVertical(int col, int rowStart, int rowEnd,
                                     TextGraphics textGraphics) {
        textGraphics.drawLine(col, rowStart, col, rowEnd, Symbols.SINGLE_LINE_VERTICAL);
    }

    private static void drawHorizontal(int colStart, int colEnd, int row,
                                     TextGraphics textGraphics) {
        textGraphics.drawLine(colStart, row, colEnd, row, Symbols.SINGLE_LINE_HORIZONTAL);
    }

    public static void putStr(TerminalScreen screen, String str, int x, int y) {
        if (str == null) {
            return;
        }
        screen.newTextGraphics()
            .putString(x, y, str);
    }

    private void putChar(TerminalScreen screen, char chr, int x, int y) {
        screen.setCharacter(x, y, new TextCharacter(chr));
    }

    private Buf getStatusBuf() {
        return buffers.get(statusBufNo);
    }

    public void setBufferPermissions(int bufNo, BufferMode... bufferMode) {
        for (BufferMode mode : bufferMode) {
            buffers.get(bufNo).addBufferMode(mode);
        }
    }

    public Buf getActiveBuf() {
        return getBuffer(getActiveBufNo());
    }

    @Override
    public String toString() {
        return "View{" +
            ", buffers=" + buffers +
            ", activeBufNo=" + activeBufNo.get() +
            '}';
    }

    public int getBufNoByName(String bufName) {
        for (Map.Entry<Integer, Buf> entry : buffers.entrySet()) {
            if (entry.getValue().getName().equals(bufName)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    public Buf getBufferByName(String bufName) {
        return buffers.get(getBufNoByName(bufName));
    }

    public List<Buf> getBuffers() {
        return new ArrayList<>(buffers.values());
    }

    public String getName() {
        return name;
    }

    public List<String> getBufferFilenames() {
        List<String> bufNames = new ArrayList<>();
        for (Buf buffer : getBuffers()) {
            bufNames.add(buffer.getFilename());
        }
        return bufNames;
    }
/*public void createIfNotExists(int bufno, VimMode vimMode, TextGraphics textGraphics) {
        if (buffers.containsKey(bufno)) {
            return;
        }
        textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
        textGraphics.setBackgroundColor(TextColor.ANSI.BLACK);

        textGraphics.setForegroundColor(TextColor.ANSI.DEFAULT);
        textGraphics.setBackgroundColor(TextColor.ANSI.DEFAULT);
    }*/

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        View view = (View) o;
        return activeBufNo == view.activeBufNo && statusBufNo == view.statusBufNo && headerBufNo == view.headerBufNo && Objects.equals(logger, view.logger) && Objects.equals(name, view.name) && Objects.equals(buffers, view.buffers) && Objects.equals(events, view.events) && Objects.equals(eventListeners, view.eventListeners) && Objects.equals(executor, view.executor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logger, name, buffers, events, eventListeners, activeBufNo, statusBufNo, headerBufNo, executor);
    }

    public void setActiveBuf(int bufNo) {
        setActiveBufNo(bufNo);
    }

    public BlockingQueue<VimEvent> getEvents() {
        return events;
    }

    public List<VimListener> getEventListeners() {
        return eventListeners;
    }

    public void addListener(VimListener vimListener) {
        this.eventListeners.add(vimListener);
    }

    public void removeListeners() {
        this.eventListeners.clear();
    }

    public void reset() {
        eventListeners.clear();
        buffers.get(sideBufNo).reset();
        buffers.get(mainBufNo).reset();
    }

    public void calcScrollView(int screenWidth, int screenHeight) {
        buffers.get(statusBufNo).setScrollView(0, 1, 0, screenWidth);
        buffers.get(headerBufNo).setScrollView(1, 2, 0, screenWidth);
        buffers.get(mainBufNo).setScrollView(2, screenHeight, 0, screenWidth / 2);
        buffers.get(sideBufNo).setScrollView(2, screenHeight, (screenWidth / 2) + 1, screenWidth);
    }

    public void fitScrollView(int screenWidth, int screenHeight) {
        int heightRemaining = screenHeight;
        heightRemaining -= 3;
        int mainHeight = (buffers.get(mainBufNo).getScrollView().getPercentOfScreenHeight() * heightRemaining) / 100;
        int newEndRow = buffers.get(mainBufNo).getScrollView().getRowStart() + mainHeight;
        buffers.get(mainBufNo).getScrollView().setRowEnd(newEndRow);
        buffers.get(sideBufNo).getScrollView().setRowEnd(newEndRow);

        int mainWidth = (buffers.get(mainBufNo).getScrollView().getPercentOfScreenWidth() * screenWidth) / 100;
        int newEndCol = buffers.get(mainBufNo).getScrollView().getColStart() + mainWidth;
        buffers.get(mainBufNo).getScrollView().setColEnd(newEndCol);
        buffers.get(sideBufNo).getScrollView().setColStart(newEndCol + 1);
        buffers.get(sideBufNo).getScrollView().setColEnd(screenWidth);
        buffers.get(headerBufNo).getScrollView().setColEnd(screenWidth);
        buffers.get(statusBufNo).getScrollView().setColEnd(screenWidth);
    }

    public void expandScrollView(int topD, int botD, int leftD, int rightD) {
        getActiveBuf().getScrollView().expandScrollView(getActiveBuf(), topD, botD, leftD, rightD);
    }
}

package com.dksd.dvim.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.catppuccin.Palette;
import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.buffer.BufferMode;
import com.dksd.dvim.engine.VimEng;
import com.dksd.dvim.higlight.JavaSyntaxHighlighter;
import com.dksd.dvim.internalbuf.InternalBuf;
import com.googlecode.lanterna.Symbols;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.TerminalScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.dksd.dvim.engine.VimEng.errorMsg;

public class View {

    private Logger logger = LoggerFactory.getLogger(View.class);

    public static final String STATUS_BUFFER = "status";
    public static final String HEADER_BUFFER = "header";
    public static final String SIDE_BUFFER = "side"; //used for text complete..and editing lists
    public static final String TAB_BUFFER = "tab";
    public static final TextColor.Indexed HIGHLIGHT_BG_COLOR = new TextColor.Indexed(237);
    public static final TextColor.Indexed HIGHLIGHT_MATCHED_POS = new TextColor.Indexed(70);
    public static final TextColor.Indexed SELECTED_ITEM_COLOR = new TextColor.Indexed(1);
    public static final int CHECK_RESIZE_INTERVAL_MS = 100;
    private final String name;
    private final Map<Integer, Buf> buffers = new ConcurrentHashMap<>();
    private AtomicInteger activeBufNo = new AtomicInteger(-1);
    private int statusBufNo = -1;
    private int headerBufNo = -1;
    private Buf mainBuf = null;
    private int sideBufNo = -1;
    private int tabBufNo = -1;
    private JavaSyntaxHighlighter syntaxHighlighter = new JavaSyntaxHighlighter();
    private final LinkedBlockingQueue<Future<?>> futures = new LinkedBlockingQueue<>();
    private Line tabComplete;
    private AtomicLong lastDrawn = new AtomicLong();
    private AtomicInteger lastHashDrawn = new AtomicInteger();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public View(String viewName, TerminalScreen screen) {
        this.name = viewName;

        Buf statusBuf = createBuf(
                STATUS_BUFFER,
                STATUS_BUFFER + ".txt",
                -1,//indicates fixed
                100,
                Set.of(
                BufferMode.NO_LINE_NUMBERS,
                BufferMode.SINGLE_LINE,
                BufferMode.FIXED_HEIGHT,
                BufferMode.RIGHT_BORDER,
                BufferMode.LEFT_BORDER,
                BufferMode.ABS_POS));
        statusBufNo = statusBuf.getBufNo();

        Buf headerBuf = createBuf(
                HEADER_BUFFER,
                HEADER_BUFFER + ".txt",
                -1,
                100,
                Set.of(
                BufferMode.UNSELECTABLE,
                BufferMode.NO_LINE_NUMBERS,
                        BufferMode.SINGLE_LINE,
                BufferMode.FIXED_HEIGHT,
                BufferMode.LEFT_BORDER,
                BufferMode.RIGHT_BORDER,
                BufferMode.ABS_POS));
        headerBuf.addRow("1 - file | 2 - file 2");
        headerBufNo = headerBuf.getBufNo();

        mainBuf = createBuf(
                "main",
                null,
                100,
                60,
                Set.of(
                BufferMode.RELATIVE_HEIGHT,
                BufferMode.ALLOW_UNDO,
                BufferMode.LEFT_BORDER,
                BufferMode.RIGHT_BORDER,
                BufferMode.TOP_BORDER));
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
                40,
                Set.of(
                BufferMode.RELATIVE_HEIGHT,
                BufferMode.NO_LINE_NUMBERS,
                BufferMode.NO_GUTTER,
                BufferMode.LEFT_BORDER,
                BufferMode.TOP_BORDER));
        sideBufNo = sideBuf.getBufNo();

        Buf tabBuf = createBuf(
                TAB_BUFFER,
                TAB_BUFFER + ".txt",
                50,
                70,
                Set.of(
                BufferMode.POP_OVER,
                BufferMode.NO_LINE_NUMBERS,
                BufferMode.LEFT_BORDER,
                BufferMode.RIGHT_BORDER,
                BufferMode.BOT_BORDER,
                BufferMode.TOP_BORDER));
        tabBufNo = tabBuf.getBufNo();

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

        setActiveBuf(mainBuf.getBufNo());
    }

    public Buf createBuf(String name, String filename, int percentHeight, int percentWidth, Set<BufferMode> bufferModes) {
        int bufNum = buffers.size();
        Buf buf = new Buf(name,
                filename,
                bufNum,
                new ScrollView(percentHeight, percentWidth),
                bufferModes
        );
        buffers.put(bufNum, buf);
        return buf;
    }

    public int getActiveBufNo() {
        return activeBufNo.get();
    }

    public Buf getBuffer(Integer bufNo) {
        return buffers.get(bufNo);
    }

    public void draw(TerminalScreen screen) {
        long st = System.currentTimeMillis();

        int viewHash = hashCode();
        if (lastHashDrawn.get() == viewHash) {//we're good
            //return;
        }

        futures.clear();

        screen.doResizeIfNecessary();
        TextGraphics textGraphics = screen.newTextGraphics();

        try {
            screen.clear();
            Buf stBuf = buffers.get(statusBufNo);
            Buf hBuf = buffers.get(headerBufNo);
            Buf sideBuf = buffers.get(sideBufNo);

            for (Buf buf : List.of(stBuf, hBuf, mainBuf, sideBuf)) {
                drawBuffer(screen, textGraphics, buf, futures);
            }

            if (tabComplete != null) {
                Buf tabBuf = buffers.get(tabBufNo);
                tabBuf.calcPopoverScrollView(getActiveBuf().getRow(),
                        screen.getTerminalSize().getColumns(), screen.getTerminalSize().getRows());
                textGraphics.fillRectangle(new TerminalPosition(tabBuf.getScrollView().getColStart(), tabBuf.getScrollView().getRowStart()),
                        new TerminalSize(tabBuf.getScrollView().getHeight(), tabBuf.getScrollView().getWidth()), ' ');

                drawBuffer(screen, textGraphics, tabBuf, futures);
            }

            //Draw error messages if any.
            String errMsg = errorMsg.get();
            if (errMsg != null && !errMsg.isEmpty()) {
                drawString(textGraphics,
                        new Line(0, errorMsg.get(), null),
                        50,
                        0,
                        futures);
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
            lastDrawn.set(System.currentTimeMillis());
            lastHashDrawn.set(viewHash);
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
            //System.out.println("Draw gutter: " + gutter.getIndicatorStr());
            if (!dispObj.isFolded()) {
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
        syntaxHighlighter.drawHighlightedCode(tg, line.getContent(), rowOffset, colOffset, futures);
//xxx
        //tg.putString(colOffset, rowOffset, line.getContent());
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
        List<String> bufNames = new ArrayList<>(getBuffers().size());
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
        return statusBufNo == view.statusBufNo && headerBufNo == view.headerBufNo && sideBufNo == view.sideBufNo && tabBufNo == view.tabBufNo && Objects.equals(name, view.name) && Objects.equals(buffers, view.buffers) && Objects.equals(activeBufNo, view.activeBufNo) && Objects.equals(tabComplete, view.tabComplete);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, buffers, activeBufNo, statusBufNo, headerBufNo, mainBuf, sideBufNo, tabBufNo, tabComplete);
    }

    public void reset() {
        buffers.get(sideBufNo).reset();
        mainBuf.reset();
    }

    public void calcScrollView(int screenWidth, int screenHeight) {
        buffers.get(statusBufNo).setScrollView(0, 1, 0, screenWidth);
        buffers.get(headerBufNo).setScrollView(1, 2, 0, screenWidth);
        mainBuf.setScrollView(2, screenHeight, 0, screenWidth / 2);
        buffers.get(sideBufNo).setScrollView(2, screenHeight, (screenWidth / 2) + 1, screenWidth);
    }

    public void fitScrollView(int screenWidth, int screenHeight) {
        int heightRemaining = screenHeight;
        heightRemaining -= 3;
        int mainHeight = (mainBuf.getScrollView().getPercentOfScreenHeight() * heightRemaining) / 100;
        int newEndRow = mainBuf.getScrollView().getRowStart() + mainHeight;
        mainBuf.getScrollView().setRowEnd(newEndRow);
        buffers.get(sideBufNo).getScrollView().setRowEnd(newEndRow);

        int mainWidth = (mainBuf.getScrollView().getPercentOfScreenWidth() * screenWidth) / 100;
        int newEndCol = mainBuf.getScrollView().getColStart() + mainWidth;
        mainBuf.getScrollView().setColEnd(newEndCol);
        buffers.get(sideBufNo).getScrollView().setColStart(newEndCol + 1);
        buffers.get(sideBufNo).getScrollView().setColEnd(screenWidth);
        buffers.get(headerBufNo).getScrollView().setColEnd(screenWidth);
        buffers.get(statusBufNo).getScrollView().setColEnd(screenWidth);
    }

    public void setTabComplete(Line cLine) {
        this.tabComplete = cLine;
    }

    public void setActiveBuf(int bufNo) {
        activeBufNo.set(bufNo);
    }

    public void setActiveBufByName(String bufName) {
        activeBufNo.set(getBufNoByName(bufName));
    }

    public Buf getMainBuffer() {
        return mainBuf;
    }

    public void popupErrorMessage(String error, int seconds, TimeUnit timeUnit) {
        errorMsg.set(error);
        scheduler.schedule(() -> {
            errorMsg.set("");
        }, seconds, timeUnit);
    }
}

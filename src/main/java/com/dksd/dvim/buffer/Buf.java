package com.dksd.dvim.buffer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.dksd.dvim.view.DispObj;
import com.dksd.dvim.view.VimMode;
import com.dksd.dvim.event.EventType;
import com.dksd.dvim.event.VimEvent;
import com.dksd.dvim.key.FindResult;
import com.dksd.dvim.view.Line;
import com.dksd.dvim.view.LineIndicator;
import com.dksd.dvim.view.ScrollView;
import com.dksd.dvim.view.VirtualView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Buf {

    private Logger logger = LoggerFactory.getLogger(Buf.class);
    public static final int GUTTER_SIZE = 5;
    private final String name;
    private String filename;
    private final int bufNo;
    private final ScrollView scrollView;
    private final VirtualView virtualView = new VirtualView();
    private final List<Line> lines = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger row = new AtomicInteger(0), col = new AtomicInteger(0);
    private final Map<VimMode, Set<BufferMode>> bufferModes = new ConcurrentHashMap<>();
    private final List<LineIndicator> lineIndicators = new ArrayList<>();
    private final Queue<VimEvent> eventQueue;

    public Buf(String name, int bufNo, ScrollView scrollView, Queue<VimEvent> eventQueue) {
        this.bufNo = bufNo;
        this.eventQueue = eventQueue;
        this.name = name;
        this.scrollView = scrollView;

    }

    public ScrollView getScrollView() {
        return scrollView;
    }

    public int getRow() {
        return row.get();
    }

    private void setRow(int row) {
        if (row < 0 || lines.isEmpty()) {
            this.row.set(0);
            return;
        }
        this.row.set(Math.min(row, this.lines.size() - 1));
    }

    public int getCol() {
        return col.get();
    }

    private void setCol(int col) {
        if (col < 0) {
            this.col.set(0);
            return;
        }
        if (col == 0 && virtualView.getColOffset() > 0) {
            virtualView.incrColOffset(-1);
            return;
        }
        int maxScroll = scrollView.getWidth() - 5 - 1 - 1;
        int ll = getLine().getContent().length();
        int actualCur = col + virtualView.getColOffset();
        System.out.println("Line len: " + ll + " col: " + col + " scrollEnd: " + maxScroll + " estimated curPos " +
                actualCur);
        if (col == maxScroll && actualCur < ll) {
            virtualView.incrColOffset(1);
            return;
        }
        int min = Math.min(col, maxScroll);
        if (min < 0) {
            min = 0;
        }
        if (actualCur > ll) {
            return;
        }
        this.col.set(min);
    }

    public void insertIntoLine(String str) {
        int row = getRow();
        int col = getCol();
        try {
            Line line = lines.get(row);
            StringBuilder sb = new StringBuilder(line.getContent());
            sb.insert(col, str);
            lines.set(row, Line.of(line.getLineNumber(), sb.toString()));
            setCol(col + str.length());
            eventQueue.add(new VimEvent(bufNo, EventType.BUF_CHANGE));
        } catch (Exception ep) {
            ep.printStackTrace();
        }
    }

    public void addToRow(int rowDelta) {
        int row = getRow();
        if (row + rowDelta >= 0 && row + rowDelta <= lines.size() - 1) {
            setRow(row + rowDelta);
        }
    }

    public void addToCol(int colDelta) {
        int col = getCol();
        setCol(col + colDelta);
    }

    public void deleteLine(int startRow, int endRow) {
        int row = getRow();
        for (int i = startRow; i < endRow; i++) {
            if (row > lines.size()) {
                setRow(lines.size() - 1);
            }
            if (!lines.isEmpty()) {
                lines.remove(row);
                eventQueue.add(new VimEvent(bufNo, EventType.BUF_CHANGE));
            }
        }
    }

    public void deleteInLine(int numChars) {
        int row = getRow();
        int col = getCol();
        Line line = lines.get(row);
        try {
            if (line != null && !line.isEmpty() && col + numChars <= line.length()) {
                String lStr = line.getContent().substring(0, col) + line.getContent().substring(col + numChars);
                line.setContent(lStr);
                eventQueue.add(new VimEvent(bufNo, EventType.BUF_CHANGE));
                lines.set(row, line);
            }
        } catch (Exception ep) {
            ep.printStackTrace();
            System.err.println("Line: " + line + " col: " + col);
        }
    }

    public Line getLine() {
        return getLine(getRow());
    }

    public Line getLine(int row) {
        if (lines.isEmpty()) {
            lines.add(new Line(0,""));
            setRow(0);
        }
        return lines.get(row);
    }

    public void replaceLine(int row, String replaceStr) {
        lines.get(row).setContent(replaceStr);
        eventQueue.add(new VimEvent(bufNo, EventType.BUF_CHANGE));
    }

    public void setLine(int row, String str) {
        while (lines.size() <= row) {
            lines.add(new Line(row, ""));
        }
        lines.set(row, new Line(row, str));
        eventQueue.add(new VimEvent(bufNo, EventType.BUF_CHANGE));
    }

    public void addBufferMode(VimMode vimMode, BufferMode bufferMode) {

        if (VimMode.ALL.equals(vimMode)) {
            for (VimMode value : VimMode.values()) {
                if (!VimMode.ALL.equals(value)) {
                    bufferModes.computeIfAbsent(value, k -> new HashSet<>());
                    this.bufferModes.get(value).add(bufferMode);
                }
            }
        } else {
            bufferModes.computeIfAbsent(vimMode, k -> new HashSet<>());
            this.bufferModes.get(vimMode).add(bufferMode);
        }
    }

    @Override
    public String toString() {
        return "Buf{" +
                "name=" + name +
                ", scrollView=" + scrollView +
                ", lines=" + lines +
                ", row=" + row +
                ", col=" + col +
                ", bufferMode=" + bufferModes +
                '}';
    }

    public int size() {
        return this.lines.size();
    }

    public boolean isEmpty() {
        return this.lines.isEmpty();
    }

    /*public List<String> copyLines(String register, int start, int end) {
        if (lines.isEmpty()) {
            return Collections.emptyList();
        }
        return lines.subList(start, end);
    }*/

    public void addRow(String str) {
        int size = lines.size();
        lines.add(new Line(size, str));
        setCol(str.length());
        eventQueue.add(new VimEvent(bufNo, EventType.BUF_CHANGE));
    }

    public List<Line> getLinesDangerous() {
        return lines;
    }

    public void readFile(String filenameIn) {
        filename = filenameIn;

        try {
            lines.clear();
            lines.addAll(Line.convert(Files.readAllLines(new File(filenameIn).toPath(), StandardCharsets.UTF_8)));
            eventQueue.add(new VimEvent(bufNo, EventType.BUF_CHANGE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeFile() {
        try {
            writeFile(filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeFile(String filenameOut) throws IOException {
        try (
                PrintWriter printWriter = new PrintWriter(
                        new BufferedWriter(
                                new OutputStreamWriter(
                                        Files.newOutputStream(Paths.get(filenameOut)),
                                        StandardCharsets.UTF_8
                                )
                        )
                )
        ) {
            for (Line line : lines) {
                printWriter.println(line.getContent());
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setLines(List<Line> keptLines) {
        this.lines.clear();
        this.lines.addAll(keptLines);
        eventQueue.add(new VimEvent(bufNo, EventType.BUF_CHANGE));
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Buf buf = (Buf) o;
        return row.get() == buf.row.get() && col.get() == buf.col.get() &&
                Objects.equals(logger, buf.logger) && Objects.equals(name, buf.name) &&
                Objects.equals(scrollView, buf.scrollView) &&
                Objects.equals(lines, buf.lines) &&
                Objects.equals(bufferModes, buf.bufferModes) &&
                Objects.equals(filename, buf.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logger, name, scrollView, lines, row, col,
                bufferModes,
                filename);
    }

    public void appendToLine(String str) {
        setLine(getRow(), getLine() + str);
        setCol(getLine().length());
        eventQueue.add(new VimEvent(bufNo, EventType.BUF_CHANGE));
    }


    public void addIndicator(LineIndicator lineIndicator) {
        lineIndicators.add(lineIndicator);
    }

    public List<FindResult> find(String searchTerm) {
        List<FindResult> results = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            int indo = lines.get(i).getContent().indexOf(searchTerm);
            if (indo != -1) {
                results.add(new FindResult(indo, i));
            }
        }
        return results;
    }

    public Map<VimMode, Set<BufferMode>> getBufferModes() {
        return bufferModes;
    }

    public List<LineIndicator> getLineIndicators() {
        return lineIndicators;
    }

    public boolean isRowInBounds(int rowPos) {
        return rowPos >= 0 && rowPos < lines.size();
    }

    public void splitToNextLine() {
        int col = getCol();
        int row = getRow();
        Line line = getLine();
        String restOfline = getStrAfter(line.getContent(), col);
        line.setContent(line.getContent().substring(0, col));
        insertStr(row + 1, restOfline);
        for (int i = row; i < lines.size(); i++) {
            lines.get(i).setLineNumber(i);
        }
        incrRow();
        setCol(0);
    }

    private void incrRow() {
        setRow(row.incrementAndGet());
    }

    private void insertStr(int indx, String restOfline) {
        lines.add(indx, new Line(indx, restOfline));
    }

    private String getStrAfter(String line, int col) {
        return line.substring(col);
    }

    public int getGutterSize() {
        return GUTTER_SIZE;
    }

    public int getBufNo() {
        return bufNo;
    }

    public void setScrollView(int rowStart, int rowEnd, int colStart, int colEnd) {
        getScrollView().setRowStart(rowStart);
        getScrollView().setRowEnd(rowEnd);
        getScrollView().setColStart(colStart);
        getScrollView().setColEnd(colEnd);
    }

    public void setTopBufs(List<Buf> bufs) {
        getScrollView().setTopBufs(bufs);
    }

    public void setBotBufs(List<Buf> bufs) {
        getScrollView().setBotBufs(bufs);
    }

    public void setLeftBufs(List<Buf> bufs) {
        getScrollView().setLeftBufs(bufs);
    }

    public void setRightBufs(List<Buf> bufs) {
        getScrollView().setRightBufs(bufs);
    }

    public VirtualView getVirtualView() {
        return virtualView;
    }

    public List<DispObj> getLinesToDisplay(VimMode vimMode) {
        List<DispObj> dispObjs = new ArrayList<>();
        //int width  = getScrollView().getWidth();
        int height = getScrollView().getHeight();
        for (int rowDataIndex = virtualView.getRowOffset(); rowDataIndex < virtualView.getRowOffset() + height && rowDataIndex < lines.size(); rowDataIndex++) {
            dispObjs.add(
                    new DispObj(getOnScreenRow(rowDataIndex), getOnScreenCol(),
                    virtualView.getCroppedLine(scrollView, lines.get(rowDataIndex))));
        }
        return dispObjs;
    }

    private int getOnScreenCol() {
        return scrollView.getColStart() + 5 + 1;
    }

    private int getOnScreenRow(int rowDataIndex) {
        return scrollView.getRowStart() + rowDataIndex + 1;
    }

    public DispObj getDisplayCursor() {
        return new DispObj(getOnScreenRow(row.get()), scrollView.getColStart() + 5 + 1 + col.get(),
                new Line(0, ""));
    }
}

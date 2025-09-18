package com.dksd.dvim.buffer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.dksd.dvim.engine.VimEng;
import com.dksd.dvim.internalbuf.InternalBuf;
import com.dksd.dvim.internalbuf.InternalBufFactory;
import com.dksd.dvim.utils.LinesHelper;
import com.dksd.dvim.view.DispObj;
import com.dksd.dvim.event.EventType;
import com.dksd.dvim.event.VimEvent;
import com.dksd.dvim.view.Line;
import com.dksd.dvim.view.ScrollView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Buf {

    private Logger logger = LoggerFactory.getLogger(Buf.class);
    public static final int GUTTER_SIZE = 5;
    private final String name;
    private String filename;
    private final int bufNo;
    private final ScrollView scrollView;
    private final InternalBuf lines;
    private final Set<BufferMode> bufferModes;
    private final AtomicInteger row = new AtomicInteger(0), col = new AtomicInteger(0);

    public Buf(String name, String filename, int bufNo, ScrollView scrollView) {
        this(name, filename, bufNo, scrollView, new HashSet<>());
    }

    public Buf(String name, String filename, int bufNo, ScrollView scrollView, Set<BufferMode> modes) {
        this.filename = filename;
        this.bufNo = bufNo;
        this.name = name;
        this.scrollView = scrollView;
        this.bufferModes = modes;
        lines = InternalBufFactory.create(bufferModes);
    }

    public ScrollView getScrollView() {
        return scrollView;
    }

    public int getRow() {
        return row.get();
    }

    private void setRow(int row) {
        this.row.set(Math.min(row, this.lines.size() - 1));
    }

    public int getCol() {
        return col.get();
    }

    //Col is the actual pos of the cursor in the buffer.
    //How it gets to the screen is a different question.
    private void setCol(int col) {
        if (col < 0) {
            this.col.set(0);
            return;
        }
        int ll = getCurrentLine().getContent().length();
        if (col > ll) {
            this.col.set(ll);
            return;
        }
        this.col.set(col);
    }

    public void insertIntoLine(String str) {
        try {
            if (lines.size() == 0) {
                addRow(str);
                return;
            }
            int row = getRow();
            int col = getCol();
            Line line = lines.get(row);
            StringBuilder sb = new StringBuilder(line.getContent());
            sb.insert(col, str);
            lines.set(row, Line.of(line.getLineNumber(), sb.toString(), line.getIndicatorStr()));
            setCol(col + str.length());
            VimEng.events.add(new VimEvent(null, bufNo, EventType.BUF_CHANGE_INSERT, "" + line.getLineNumber()));
            //System.out.println("Buf event: " + event);
        } catch (Exception ep) {
            ep.printStackTrace();
            System.err.println("Error when inserting into line: " + this);
        }
    }

    public void addToRow(int rowDelta) {
        int row = getRow();
        if (row + rowDelta >= 0 && row + rowDelta <= lines.size() - 1) {
            setRow(row + rowDelta);
            addToCol(0);
        }
    }

    public void addToCol(int colDelta) {
        int col = getCol();
        setCol(col + colDelta);
    }

    public void deleteLine(int row) {
        if (!isEmpty()) {
            lines.remove(row);
            VimEng.events.add(new VimEvent(null, bufNo, EventType.BUF_CHANGE_REMOVE, "" + row));
        }
        if (row >= lines.size()) {
            setRow(lines.size() - 1);
        }
        setCol(0);
    }

    public void deleteInLine(int numChars) {
        int row = getRow();
        int col = getCol();
        Line line = lines.get(row);
        try {
            if (line != null && !line.isEmpty() && col + numChars <= line.length()) {
                String lStr = line.getContent().substring(0, col) + line.getContent().substring(col + numChars);
                line.setContent(lStr);
                VimEng.events.add(new VimEvent(null, bufNo, EventType.BUF_CHANGE_REMOVE, lStr));
                lines.set(row, line);
            }
        } catch (Exception ep) {
            ep.printStackTrace();
            System.err.println("Line: " + line + " col: " + col);
        }
    }

    public Line getLine(int row) {
        return lines.get(row);
    }

    public void addBufferMode(BufferMode bufferMode) {
        bufferModes.add(bufferMode);
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
        return this.lines.size() == 0;
    }

    public void addRow(String str) {
        int size = lines.size();
        lines.set(size, new Line(size, str, null));
        setCol(str.length());
        VimEng.events.add(new VimEvent(null, bufNo, EventType.BUF_CHANGE_ADD, str));
    }

    public List<Line> getLinesDangerous() {
        return lines.getAll();
    }

    public String getName() {
        return name;
    }

    public void setLinesListStr(List<String> lines, int insertAfter) {
        setLines(LinesHelper.convertToLines(lines), insertAfter);
    }

    public void setLines(List<Line> keptLines, int insertAfter) {
        //TODO
        this.lines.setAll(keptLines);
        row.set(0);
        col.set(0);
        VimEng.events.add(new VimEvent(null, bufNo, EventType.BUF_CHANGE_ADD, "" + insertAfter));
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Buf buf = (Buf) o;
        return bufNo == buf.bufNo && Objects.equals(name, buf.name) && Objects.equals(filename, buf.filename) && Objects.equals(scrollView, buf.scrollView) && Objects.equals(lines, buf.lines) && Objects.equals(row, buf.row) && Objects.equals(col, buf.col) && Objects.equals(bufferModes, buf.bufferModes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, filename, bufNo, scrollView, lines, row, col, bufferModes);
    }

    public void splitToNextLine() {
        int col = getCol();
        int row = getRow();
        Line line = getCurrentLine();
        String restOfline = getStrAfter(line.getContent(), col);
        line.setContent(line.getContent().substring(0, col));
        lines.insert(row + 1, new Line(row + 1, restOfline, null));
        incrRow();
        setCol(0);
    }

    private void incrRow() {
        setRow(row.incrementAndGet());
    }

    private String getStrAfter(String line, int col) {
        return line.substring(col);
    }

    public int getGutterSize() {
        return (bufferModes.contains(BufferMode.NO_GUTTER)) ? 0 : GUTTER_SIZE;
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

    private int getVirtualRow(int row, int height) {
        if (lines.size() == 0) {
            return 0;
        }
        int fivePToBottom = (int) (height * 0.05);
        int stRow = Math.min(row + fivePToBottom, lines.size()) - height;
        stRow = Math.max(0, stRow);
        return stRow;
    }

    private int getVirtualCol(int row, int col, int width) {
        if (lines.size() == 0 || row >= lines.size()) {
            return 0;
        }

        int fivePToRight = (int) (width * 0.05);
        int stCol = Math.min(col + fivePToRight, lines.get(row).getContent().length()) - width;
        stCol = Math.max(0, stCol);
        return stCol;
    }

    public List<DispObj> getLinesToDisplay() {
        List<DispObj> dispObjs = new ArrayList<>(300);
        int leftBorderWidth = getBorderWidths(BufferMode.LEFT_BORDER);
        int topBorderWidth = getBorderWidths(BufferMode.TOP_BORDER);
        int width = getScrollView().getWidth() - getGutterSize() - leftBorderWidth;
        int height = getScrollView().getHeight();
        if (height > 1) height -= topBorderWidth;

        int stRow = getVirtualRow(getRow(), height);
        int stCol = getVirtualCol(getRow(), getCol(), width);

        for (int rowDataIndex = stRow; rowDataIndex < stRow + height && rowDataIndex < lines.size(); rowDataIndex++) {

            Line lineStr = lines.get(rowDataIndex);
            String str = lineStr.getContent();

            String croppedLine = "";
            try {
                croppedLine = str.substring(stCol, Math.min(str.length(), stCol + width));
            }
            catch (Exception ep) {
                ep.printStackTrace();
                //System.out.println("str to try crop: " + str);
            }
            dispObjs.add(
                    new DispObj(getOnScreenRow(rowDataIndex - stRow, topBorderWidth),
                            getOnScreenCol(0, leftBorderWidth),
                            new Line(rowDataIndex, croppedLine, lineStr.getIndicatorStr())));
        }
        return dispObjs;
    }

    public boolean containsBufferMode(BufferMode bufferMode) {
        return bufferModes.contains(bufferMode);
    }

    private int getBorderWidths(BufferMode bufferMode) {
        return containsBufferMode(bufferMode) ? 1 : 0;
    }

    private int getOnScreenCol(int colDataIndex, int leftBorderWidth) {
        return colDataIndex + scrollView.getColStart() + getGutterSize() + leftBorderWidth;
    }

    private int getOnScreenRow(int rowDataIndex, int topBorderWidth) {
        return scrollView.getRowStart() + rowDataIndex + topBorderWidth;
    }

    public DispObj getDisplayCursor() {
        int pRow = getRow() - getVirtualRow(getRow(), getScrollView().getHeight()) + scrollView.getRowStart() + 1;
        int pCol = getCol() - getVirtualCol(getRow(), getCol(), getScrollView().getWidth()) + scrollView.getColStart() + getGutterSize() + 1;

        pCol = Math.min(pCol, getScrollView().getWidth() + scrollView.getColStart() - 1);
        pRow = Math.min(pRow, getScrollView().getHeight() + scrollView.getRowStart() - 1);

        return new DispObj(pRow, pCol, new Line(0, "", null));
    }

    public void undo() {
        lines.undo();
    }

    public Line getCurrentLine() {
        return getLine(getRow());
    }

    public Line getCurrentLineIndicator() {
        return getLine(getRow());
    }

    public void reset() {
        lines.clear();
        row.set(0);
        col.set(0);
        VimEng.events.clear();
    }

    public void calcPopoverScrollView(int row, int screenWidth, int screenHeight) {
        int newWidth = scrollView.getPercentOfScreenWidth() * screenWidth / 100;
        scrollView.setRowStart(row + getGutterSize());
        scrollView.setRowEnd(screenHeight - 1);
        scrollView.setColStart((screenWidth / 2) - (newWidth / 2));
        scrollView.setColEnd((screenWidth / 2) + (newWidth / 2));
    }

    public void removeLines(int startRow, int endRow) {
        for (int i = startRow; i < endRow; i++) {
            lines.remove(i);
        }
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setLine(int row, String line) {
        lines.set(row, new Line(row, line, null));
    }
}

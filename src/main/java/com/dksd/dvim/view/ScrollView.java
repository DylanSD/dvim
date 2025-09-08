package com.dksd.dvim.view;

import com.dksd.dvim.buffer.Buf;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScrollView {
    private int rowStart = -1;
    private int colStart = -1;
    private int rowEnd = -1;
    private int colEnd = -1;
    private int percentOfScreenHeight;
    private int percentOfScreenWidth;
    private final Set<Buf> topBufs = new HashSet<>();
    private final Set<Buf> botBufs = new HashSet<>();
    private final Set<Buf> leftBufs = new HashSet<>();
    private final Set<Buf> rightBufs = new HashSet<>();

    public ScrollView(int percentOfScreenHeight,
                      int percentOfScreenWidth) {
        this.percentOfScreenHeight = percentOfScreenHeight;
        this.percentOfScreenWidth = percentOfScreenWidth;
    }

    public int getRowStart() {
        return rowStart;
    }

    public int getColStart() {
        return colStart;
    }

    public void setRowStart(int rowStart) {
        this.rowStart = rowStart;
    }

    public void setColStart(int colStart) {
        this.colStart = colStart;
    }

    public int getRowEnd() {
        return rowEnd;
    }

    public void setRowEnd(int rowEnd) {
        this.rowEnd = rowEnd;
    }

    public int getColEnd() {
        return colEnd;
    }

    public void setColEnd(int colEnd) {
        this.colEnd = colEnd;
    }

    public Set<Buf> getTopBufs() {
        return topBufs;
    }

    public Set<Buf> getBotBufs() {
        return botBufs;
    }

    public Set<Buf> getLeftBufs() {
        return leftBufs;
    }

    public Set<Buf> getRightBufs() {
        return rightBufs;
    }

    public void setLeftBufs(List<Buf> leftBufs) {
        this.leftBufs.addAll(leftBufs);
    }

    public void setRightBufs(List<Buf> rightBufs) {
        this.rightBufs.addAll(rightBufs);
    }

    public void setBotBufs(List<Buf> botBufs) {
        this.botBufs.addAll(botBufs);
    }

    public void setTopBufs(List<Buf> topBufs) {
        this.topBufs.addAll(topBufs);
    }

    public int getPercentOfScreenHeight() {
        return percentOfScreenHeight;
    }

    public void setPercentOfScreenHeight(int percentOfScreenHeight) {
        this.percentOfScreenHeight = percentOfScreenHeight;
    }

    public int getPercentOfScreenWidth() {
        return percentOfScreenWidth;
    }

    public void setPercentOfScreenWidth(int percentOfScreenWidth) {
        this.percentOfScreenWidth = percentOfScreenWidth;
    }

    @Override
    public String toString() {
        return "ScrollView{" +
                "rowStart=" + rowStart +
                ", colStart=" + colStart +
                ", rowEnd=" + rowEnd +
                ", colEnd=" + colEnd +
                '}';
    }

    public void expandScrollView(Buf buf, int topD, int botD, int leftD, int rightD) {
        if (!isFixedHeight(Set.of(buf)) && !isFixedHeight(topBufs) && !isFixedWidth(leftBufs) && !isFixedWidth(rightBufs)) {
            incrScrollView(topD, 0, 0, 0);
            for (Buf topBuf : topBufs) {
                topBuf.getScrollView().incrScrollView(0, topD, 0, 0);
            }
            for (Buf leftBuf : leftBufs) {
                leftBuf.getScrollView().incrScrollView(topD, 0, 0, 0);
            }
            for (Buf rightBuf : rightBufs) {
                rightBuf.getScrollView().incrScrollView(topD, 0, 0, 0);
            }
        }
        if (!isFixedHeight(Set.of(buf)) && !isFixedHeight(botBufs) && !isFixedWidth(leftBufs) && !isFixedWidth(rightBufs)) {
            incrScrollView(0, botD, 0, 0);
            for (Buf botBuf : botBufs) {
                botBuf.getScrollView().incrScrollView(botD, 0, 0, 0);
            }
            for (Buf leftBuf : leftBufs) {
                leftBuf.getScrollView().incrScrollView(0, botD, 0, 0);
            }
            for (Buf rightBuf : rightBufs) {
                rightBuf.getScrollView().incrScrollView(0, botD, 0, 0);
            }
        }
        //Left
        if (!isFixedWidth(Set.of(buf)) && !isFixedWidth(leftBufs)) {
            incrScrollView(0, 0, leftD, 0);
            for (Buf leftBuf : leftBufs) {
                leftBuf.getScrollView().incrScrollView(0, 0, 0, leftD);
            }
        }
        //Right
        if (!isFixedWidth(Set.of(buf)) && !isFixedWidth(rightBufs)) {
            incrScrollView(0, 0, 0, rightD);
            for (Buf rightBuf : rightBufs) {
                rightBuf.getScrollView().incrScrollView(0, 0, rightD, 0);
            }
        }
    }

    public void incrScrollView(int topD, int botD, int leftD, int rightD) {
        setColStart(Math.max(0, getColStart() + rightD));
        setColEnd(getColEnd() + rightD);
        setRowStart(getRowStart() + topD);
        setRowEnd(getRowEnd() + botD);
    }

    private boolean isFixedHeight(Set<Buf> bufs) {
        for (Buf buf : bufs) {
            if (buf.getScrollView().getPercentOfScreenHeight() == -1) {
                return true;
            }
        }
        return false;
    }

    private boolean isFixedWidth(Set<Buf> bufs) {
        for (Buf buf : bufs) {
            if (buf.getScrollView().getPercentOfScreenWidth() == -1) {
                return true;
            }
        }
        return false;
    }

    public int getHeight() {
        return rowEnd - rowStart;
    }

    public int getWidth() {
        return colEnd - colStart;
    }

}

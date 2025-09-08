package com.dksd.dvim.view;

public class VirtualView {
    private int rowOffset = 0;
    private int colOffset = 0;

    public int getRowOffset() {
        return rowOffset;
    }

    public void setRowOffset(int rowOffset) {
        this.rowOffset = rowOffset;
    }

    public int getColOffset() {
        return colOffset;
    }

    public void setColOffset(int colOffset) {
        this.colOffset = colOffset;
    }

    public String getStrSafe(String str, int start, int end) {
        if (str == null) {
            return null;
        }
        return str.substring(Math.max(start, 0), Math.min(str.length(), end));
    }

    public Line getCroppedLine(ScrollView scrollView, Line line) {
        try {
            return new Line(line.getLineNumber(),
            getStrSafe(line.getContent(), getColOffset(), getColOffset() + scrollView.getWidth() - 5 - 1));
        } catch (Exception ep) {
            ep.printStackTrace();
        }
        throw new IllegalStateException("Line cannot be null!");
    }

    public void incrColOffset(int delta) {
        colOffset += delta;
    }
}

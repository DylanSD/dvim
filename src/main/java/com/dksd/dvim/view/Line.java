package com.dksd.dvim.view;

import java.util.ArrayList;
import java.util.List;

public class Line {
    private volatile int lineNumber;
    private volatile String content;
    private volatile String ghostContent;
    private volatile String indicatorStr = null;

    public Line(int lineNumber, String content, String indicatorStr) {
        this.lineNumber = lineNumber;
        this.content = content;
        this.indicatorStr = indicatorStr;
    }

    public static Line of(int lineNumber, String string, String indicatorStr) {
        return new Line(lineNumber, string, indicatorStr);
    }

    public static List<Line> convert(List<String> lns) {
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < lns.size(); i++) {
            lines.add(Line.of(i, lns.get(i), null));
        }
        return lines;
    }

    public static List<String> convertLines(List<Line> lns) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < lns.size(); i++) {
            lines.add(lns.get(i).getContent());
        }
        return lines;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getContent() {
        return content;
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public int length() {
        return content.length();
    }

    public void setContent(String lStr) {
        this.content = lStr;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getIndicatorStr() {
        return indicatorStr;
    }

    public void setIndicatorStr(String indicatorStr) {
        this.indicatorStr = indicatorStr;
    }

    @Override
    public String toString() {
        return "Line{" +
                "lineNumber=" + lineNumber +
                ", content='" + content + '\'' +
                ", indicatorStr='" + indicatorStr + '\'' +
                '}';
    }

    public String getWord(int col) {
        for (int i = col - 1; i >= 0; i--) {
            if (' ' == getContent().charAt(i)) {
                return getContent().substring(Math.max(0, i - col), Math.min(col, getContent().length()));
            }
        }
        return null;
    }
}

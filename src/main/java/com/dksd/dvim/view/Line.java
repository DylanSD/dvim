package com.dksd.dvim.view;

import java.util.ArrayList;
import java.util.List;

public class Line {
    private int lineNumber;
    private String content;

    public Line(int lineNumber, String content) {
        this.lineNumber = lineNumber;
        this.content = content;
    }

    public static Line of(int lineNumber, String string) {
        return new Line(lineNumber, string);
    }

    public static List<Line> convert(List<String> lns) {
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < lns.size(); i++) {
            lines.add(Line.of(i, lns.get(i)));
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
}

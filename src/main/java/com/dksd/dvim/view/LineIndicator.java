package com.dksd.dvim.view;


import com.googlecode.lanterna.screen.TerminalScreen;

public class LineIndicator {
    private final String indicatorStr;
    private int lineNo;//-1 for all lines
    private final IndicatorType type;

    public LineIndicator(String indicatorStr, int lineNo, IndicatorType type) {
        this.indicatorStr = indicatorStr;
        this.lineNo = lineNo;
        this.type = type;
    }

    public void drawIndicator(TerminalScreen screen, String line, int col, int row) {
        View.putStr(screen, indicatorStr, col, row);
    }

    public String getIndicatorStr() {
        return indicatorStr;
    }

    public enum IndicatorType {
        END_OF_LINE, GUTTER
    }

    public int getLineNo() {
        return lineNo;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    public IndicatorType getType() {
        return type;
    }
}

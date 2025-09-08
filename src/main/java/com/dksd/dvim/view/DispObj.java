package com.dksd.dvim.view;

public class DispObj {

    private Line croppedLine;
    private int screenCol;
    private int screenRow;

    public DispObj(int screenRow, int screenCol, Line croppedLine) {
        this.screenRow = screenRow;
        this.screenCol = screenCol;
        this.croppedLine = croppedLine;
    }

    public Line getContent() {
        return croppedLine;
    }

    public int getScreenCol() {
        return screenCol;
    }

    public int getScreenRow() {
        return screenRow;
    }
}

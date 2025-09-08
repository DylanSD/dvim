package com.dksd.dvim.key;

public class FindResult {

    private final int col;
    private final int row;

    public FindResult(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}

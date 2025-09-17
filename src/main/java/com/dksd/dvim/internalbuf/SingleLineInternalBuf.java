package com.dksd.dvim.internalbuf;

import com.dksd.dvim.view.Line;

import java.util.List;

public class SingleLineInternalBuf implements InternalBuf {

    private final Line line = new Line(0, "", null);

    @Override
    public Line get(int row) {
        return line;
    }

    @Override
    public void set(int row, Line line) {
        this.line.setLineNumber(line.getLineNumber());
        this.line.setContent(line.getContent());
        this.line.setGhostContent(line.getGhostContent());
        this.line.setIndicatorStr(line.getIndicatorStr());
    }

    @Override
    public void setAll(List<Line> lines) {
        set(0, lines.getFirst());
    }

    @Override
    public void remove(int row) {
        clear();
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public void undo() {
        //noop
    }

    @Override
    public void clear() {
        line.setIndicatorStr(null);
        line.setContent("");
        line.setGhostContent(null);
        line.setLineNumber(0);
    }

    @Override
    public List<Line> getAll() {
        return List.of(line);
    }

    @Override
    public void insert(int row, Line line) {
        set(0, line);
    }

}

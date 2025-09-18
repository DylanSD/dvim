package com.dksd.dvim.internalbuf;

import com.dksd.dvim.view.Line;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiLineInternalBuf implements InternalBuf {

    private final List<Line> lines = Collections.synchronizedList(new ArrayList<>());

    public MultiLineInternalBuf() {
    }

    public MultiLineInternalBuf(MultiLineInternalBuf inLines) {
        lines.addAll(inLines.getAll());
    }

    @Override
    public Line get(int row) {
        if (lines.isEmpty() || row >= lines.size()) {
            return null;
        }
        return lines.get(row);
    }

    @Override
    public void set(int row, Line line) {
        if (row == 0 && lines.isEmpty() || row == lines.size()) {
            lines.add(line);
        }
        lines.set(row, line);
    }

    /**
     * Set all the lines after the insert point
     * Removes existing lines as well.
     * @param linesIn
     */
    @Override
    public void setAll(List<Line> linesIn) {
        /*for (int i = 0; i < insertAfter - lines.size(); i++) {
            insert(0, new Line(0, "", null));
        }
        removeAll(insertAfter, lines.size());*/
        lines.clear();
        lines.addAll(linesIn);
    }

    private void removeAll(int from, int to) {
        for (int i = from; i < to; i++) {
            remove(i);
        }
    }

    @Override
    public void remove(int row) {
        lines.remove(row);
    }

    @Override
    public int size() {
        return lines.size();
    }

    @Override
    public void undo() {
        //noop
    }

    @Override
    public void clear() {
        lines.clear();
    }

    @Override
    public List<Line> getAll() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    public void insert(int row, Line line) {
        lines.add(row, line);
    }
}

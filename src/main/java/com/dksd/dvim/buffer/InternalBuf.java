package com.dksd.dvim.buffer;

import com.dksd.dvim.view.Line;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class InternalBuf {

    private static final int MAX_UNDO_LEVEL = 20;
    private final boolean keepUndo;
    private final Stack<List<Line>> undoStack = new Stack<>();

    public InternalBuf(boolean keepUndo) {
        this.keepUndo = keepUndo;
        getCurrBuf();
    }

    public boolean isEmpty() {
        return getCurrBuf().isEmpty();
    }

    public int size() {
        return getCurrBuf().size();
    }

    public void remove(int row) {
        if (row >= 0 && row < getCurrBuf().size()) {
            pushAndGet().remove(row);
        }
    }

    public void set(int row, Line line) {
        if (row >= 0 && row < getCurrBuf().size()) {
            pushAndGet().set(row, line);
        }
    }

    public Line getCurrBuf(int row) {
        if (row >= 0 && row < getCurrBuf().size()) {
            return getCurrBuf().get(row);
        }
        return null; // sane default
    }

    public void clear() {
        getCurrBuf().clear();
    }

    public void addAll(List<Line> convert) {
        pushAndGet().addAll(convert);
    }

    public void add(Line line) {
        pushAndGet().add(line);
    }

    public void add(int indx, Line line) {
        if (indx < 0) {
            indx = 0;
        } else if (indx > getCurrBuf().size()) {
            indx = getCurrBuf().size(); // append to end
        }
        pushAndGet().add(indx, line);
    }

    public List<Line> getLines() {
        return getCurrBuf();
    }

    public void undo() {
        if (undoStack.size() > 1) {
            undoStack.pop();
        }
    }

    public List<Line> getCurrBuf() {
        if (undoStack.isEmpty()) {
            undoStack.push(Collections.synchronizedList(new ArrayList<>()));
        }
        return undoStack.peek();
    }

    public List<Line> pushAndGet() {
        if (this.keepUndo) {
            undoStack.push(Collections.synchronizedList(new ArrayList<>(undoStack.peek())));
            if (undoStack.size() > MAX_UNDO_LEVEL) {
                undoStack.remove(0); // drop the oldest snapshot
            }
        }
        return getCurrBuf();
    }

    public void reset() {
        undoStack.clear();
        getCurrBuf();
    }
}

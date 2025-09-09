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
    }

    public boolean isEmpty() {
        return get().isEmpty();
    }

    public int size() {
        return get().size();
    }

    public void remove(int row) {
        pushAndGet().remove(row);
    }

    public void set(int row, Line line) {
        pushAndGet().set(row, line);
    }

    public Line get(int row) {
        return get().get(row);
    }

    public void clear() {
        get().clear();
    }

    public void addAll(List<Line> convert) {
        pushAndGet().addAll(convert);
    }

    public void add(Line line) {
        pushAndGet().add(line);
    }

    public void add(int indx, Line line) {
        pushAndGet().add(indx, line);
    }

    public List<Line> getLines() {
        return get();
    }

    public void undo() {
        if (undoStack.size() > 1) {
            undoStack.pop();
        }
    }

    public List<Line> get() {
        if (undoStack.isEmpty()) {
            undoStack.push(Collections.synchronizedList(new ArrayList<>()));
        }
        return undoStack.peek();
    }

    public List<Line> pushAndGet() {
        if (this.keepUndo) {
            undoStack.push(Collections.synchronizedList(new ArrayList<>(undoStack.peek())));
            if (undoStack.size() >= MAX_UNDO_LEVEL) {
                undoStack.remove(MAX_UNDO_LEVEL - 1);
            }
        }
        return get();
    }
}

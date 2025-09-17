package com.dksd.dvim.internalbuf;

import com.dksd.dvim.view.Line;

import java.util.List;
import java.util.Stack;

public class MultiUndoInternalBuf implements InternalBuf {

    //private static final int MAX_UNDO_LEVEL = 20;
    private final Stack<MultiLineInternalBuf> undoStack = new Stack<>();


    @Override
    public Line get(int row) {
        return peek().get(row);
    }

    @Override
    public void set(int row, Line line) {
        undoStack.push(new MultiLineInternalBuf(peek())).set(row, line);
    }

    @Override
    public void setAll(List<Line> lines) {
        undoStack.push(new MultiLineInternalBuf()).setAll(lines);
    }

    @Override
    public void remove(int row) {
        peek().remove(row);
    }

    @Override
    public int size() {
        return peek().size();
    }

    private MultiLineInternalBuf peek() {
        if (undoStack.isEmpty()) {
            undoStack.push(new MultiLineInternalBuf());
        }
        return undoStack.peek();
    }

    @Override
    public void undo() {
        undoStack.pop();
    }

    @Override
    public void clear() {
        peek().clear();
    }

    @Override
    public List<Line> getAll() {
        return peek().getAll();
    }

    @Override
    public void insert(int row, Line line) {
        peek().insert(row, line);
    }
}

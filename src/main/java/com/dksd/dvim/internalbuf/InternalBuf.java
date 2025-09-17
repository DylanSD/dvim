package com.dksd.dvim.internalbuf;

import com.dksd.dvim.view.Line;

import java.util.List;

public interface InternalBuf {

    Line get(int row);

    void set(int row, Line line);

    void setAll(List<Line> lines);

    void remove(int row);

    int size();

    void undo();

    void clear();

    List<Line> getAll();

    void insert(int row, Line line);
}

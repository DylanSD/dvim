package com.dksd.dvim.internalbuf;

import com.dksd.dvim.view.Line;

import java.util.List;

public interface InternalBuf {

    Line get(int row);

    List<Line> getAll();

    void set(int row, Line line);

    void setAll(List<Line> lines);

    int size();

    void undo();

    void remove(int row);

    void clear();

    void insert(int row, Line line);
}

package com.dksd.dvim.history;

import com.dksd.dvim.buffer.Buf;

import java.util.List;


public interface Harpoon<T> {

    int getNextInt();

    void add(T value);

    T current();

    List<T> toList();

    T setCurrent(T value);
}

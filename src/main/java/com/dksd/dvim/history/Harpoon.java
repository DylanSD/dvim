package com.dksd.dvim.history;

import com.dksd.dvim.utils.RingBuffer;

import java.util.List;


public interface Harpoon<T> {

    int getNextInt();

    void add(T value);

    T current();

    List<T> toList();

    void setCurrent(T value);
}

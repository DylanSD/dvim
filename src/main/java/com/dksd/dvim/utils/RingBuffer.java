package com.dksd.dvim.utils;

import java.util.ArrayList;
import java.util.List;

public class RingBuffer<T> {
    private final List<T> items;
    private int currentIndex = -1;

    public RingBuffer(List<T> items) {
        if (items == null) {
            throw new IllegalArgumentException("Items cannot be null or empty");
        }
        this.items = items;
    }

    public List<T> list() {
        return items;
    }

    public T next() {
        currentIndex = (currentIndex + 1) % items.size();
        return items.get(currentIndex);
    }

    public T prev() {
        currentIndex = (currentIndex - 1 + items.size()) % items.size();
        return items.get(currentIndex);
    }

    public T current() {
        if (currentIndex == -1) {
            next();
        }
        return items.get(currentIndex);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public void add(T value) {
        if (!items.contains(value)) {
            this.items.add(value);
        }
    }

    public void clear() {
        this.items.clear();
    }

    public void setCurrent(T value) {
        items.add(value);
        currentIndex = items.indexOf(value);
    }
}


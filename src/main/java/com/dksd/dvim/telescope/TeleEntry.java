package com.dksd.dvim.telescope;

public class TeleEntry<T> {
    private final String displayLine;
    private final T obj;

    public TeleEntry(T obj, String displayLine) {
        this.obj = obj;
        this.displayLine = displayLine;
    }

    public String getDisplayLine() {
        return displayLine;
    }

    public T getObj() {
        return obj;
    }
}

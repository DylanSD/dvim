package com.dksd.dvim.mapping;

public class TeleOption {
    private String actual;
    private String ghostText;

    @Override
    public String toString() {
        return actual + " - " + ghostText;
    }
}

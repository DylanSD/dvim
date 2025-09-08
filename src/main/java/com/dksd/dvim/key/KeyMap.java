package com.dksd.dvim.key;

import java.util.function.Function;

import com.dksd.dvim.view.VimMode;

public class KeyMap {
    private final VimMode vimMode;
    private final String left;
    private final String description;
    private final Function<String, String> function;
    private boolean hideMap = false;

    public KeyMap(VimMode vimMode,
                  String left,
                  String description,
                  Function<String, String> rightFunc,
                  boolean systemMap) {
        this.vimMode = vimMode;
        this.left = left;
        this.description = description;
        this.function = rightFunc;
        this.hideMap = systemMap;
    }

    public String getLeft() {
        return left;
    }

    public String getDescription() {
        return description;
    }

    public Function<String, String> getFunction() {
        return function;
    }

    public boolean isSystemMap() {
        return hideMap;
    }

    @Override
    public String toString() {
        return left;
    }

    public VimMode getVimMode() {
        return vimMode;
    }
}

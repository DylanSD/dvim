package com.dksd.dvim.mapping;

import com.dksd.dvim.view.VimMode;
import com.googlecode.lanterna.input.KeyStroke;

public class VimKey {
    private KeyStroke keyStroke;
    private VimMode vimMode;

    public VimKey(VimMode vimMode, KeyStroke key) {
        this.vimMode = vimMode;
        this.keyStroke = key;
    }

    public KeyStroke getKeyStroke() {
        return keyStroke;
    }

    public void setKeyStroke(KeyStroke keyStroke) {
        this.keyStroke = keyStroke;
    }

    public VimMode getVimMode() {
        return vimMode;
    }

    public void setVimMode(VimMode vimMode) {
        this.vimMode = vimMode;
    }
}

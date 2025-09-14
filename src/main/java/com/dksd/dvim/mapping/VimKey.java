package com.dksd.dvim.mapping;

import com.dksd.dvim.view.VimMode;
import com.googlecode.lanterna.input.KeyStroke;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VimKey vimKey = (VimKey) o;
        return Objects.equals(keyStroke.getKeyType(), vimKey.keyStroke.getKeyType()) &&
                Objects.equals(keyStroke.getCharacter(), vimKey.keyStroke.getCharacter()) &&
                Objects.equals(keyStroke.isAltDown(), vimKey.keyStroke.isAltDown()) &&
                Objects.equals(keyStroke.isCtrlDown(), vimKey.keyStroke.isCtrlDown()) &&
                Objects.equals(keyStroke.isShiftDown(), vimKey.keyStroke.isShiftDown()) &&
                vimMode == vimKey.vimMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyStroke.getKeyType(), keyStroke.getCharacter(), keyStroke.isAltDown()
                ,keyStroke.isCtrlDown(), keyStroke.isShiftDown(), vimMode);
    }
}

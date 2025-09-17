package com.dksd.dvim.internalbuf;

import com.dksd.dvim.buffer.BufferMode;

import java.util.Set;

public class InternalBufFactory {
    public static InternalBuf create(Set<BufferMode> bufferModes) {
        if (bufferModes.contains(BufferMode.SINGLE_LINE)) {
            return new SingleLineInternalBuf();
        }
        if (!bufferModes.contains(BufferMode.ALLOW_UNDO)) {
            return new MultiLineInternalBuf();
        }
        return new MultiUndoInternalBuf();
    }
}

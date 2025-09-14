package com.dksd.dvim.history;

import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.utils.RingBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HarpoonBuf extends RingBuffer<Buf> implements Harpoon<Buf> {

    private final HarpoonType type;

    public HarpoonBuf() {
        super(Collections.synchronizedList(new ArrayList<>()));
        this.type = HarpoonType.BUFFERS;
    }

    public HarpoonBuf(List<Buf> items) {
        super(items);
        this.type = HarpoonType.BUFFERS;
    }

    @Override
    public List<Buf> toList() {
        return super.list();
    }
}

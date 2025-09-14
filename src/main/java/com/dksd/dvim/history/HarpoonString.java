package com.dksd.dvim.history;

import com.dksd.dvim.utils.RingBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HarpoonString extends RingBuffer<String> implements Harpoon<String> {

    private final HarpoonType type;

    public HarpoonString(HarpoonType type, List<String> items) {
        super(items);
        this.type = type;
    }

    public HarpoonString(HarpoonType type) {
        super(Collections.synchronizedList(new ArrayList<>()));
        this.type = type;
    }

    @Override
    public List<String> toList() {
        return super.list();
    }
}

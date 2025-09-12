package com.dksd.dvim.history;

import com.dksd.dvim.utils.RingBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Harpoon<T> extends RingBuffer<T> {

    private final HarpoonType type;

    public Harpoon(HarpoonType type, List<T> items) {
        super(items);
        this.type = type;
    }

    public Harpoon(HarpoonType type) {
        super(Collections.synchronizedList(new ArrayList<>()));
        this.type = type;
    }
}

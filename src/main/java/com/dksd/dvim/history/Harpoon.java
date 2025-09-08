package com.dksd.dvim.history;

import com.dksd.dvim.utils.RingBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Harpoon<T> extends RingBuffer<T> {

    private String key;

    public Harpoon(List<T> items) {
        super(items);
    }

    public Harpoon() {
        super(Collections.synchronizedList(new ArrayList<>()));
    }
}

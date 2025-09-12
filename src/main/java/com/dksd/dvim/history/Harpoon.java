package com.dksd.dvim.history;

import com.dksd.dvim.utils.RingBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Harpoon<T> extends RingBuffer<T> {

    private final String key;

    public Harpoon(String key, List<T> items) {
        super(items);
        this.key = key;
    }

    public Harpoon(String key) {
        super(Collections.synchronizedList(new ArrayList<>()));
        this.key = key;
    }
}

package com.dksd.dvim.history;

import com.dksd.dvim.utils.RingBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HarpoonListString extends RingBuffer<List<String>> implements Harpoon<List<String>> {

    private final HarpoonType type;

    public HarpoonListString(HarpoonType type, List<List<String>> items) {
        super(items);
        this.type = type;
    }

    public HarpoonListString(HarpoonType type) {
        super(Collections.synchronizedList(new ArrayList<>()));
        this.type = type;
    }

    @Override
    public List<List<String>> toList() {
        return super.list();
    }
}

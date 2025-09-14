package com.dksd.dvim.history;

import com.dksd.dvim.utils.PathHelper;
import com.dksd.dvim.utils.RingBuffer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.FileHandler;

public class HarpoonDir extends RingBuffer<Path> implements Harpoon<Path> {

    private final HarpoonType type;

    public HarpoonDir() {
        super(Collections.synchronizedList(new ArrayList<>()));
        this.type = HarpoonType.DIRS;
    }

    public HarpoonDir(List<Path> items) {
        super(items);
        this.type = HarpoonType.DIRS;
    }

    public HarpoonDir(String directory) {
        super(PathHelper.loadFilesIntoHarpoon(directory));
        this.type = HarpoonType.DIRS;
    }

    @Override
    public List<Path> toList() {
        return super.list();
    }
}

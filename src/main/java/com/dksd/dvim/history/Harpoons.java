package com.dksd.dvim.history;

import com.dksd.dvim.buffer.Buf;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Harpoons {

    Harpoon<List<String>> clipboard = new Harpoon<>(HarpoonType.CLIPBOARD);
    Harpoon<String> prompts = new Harpoon<>(HarpoonType.CLIPBOARD);
    Harpoon<String> keyMappings = new Harpoon<>(HarpoonType.KEYS);
    Harpoon<Buf> buffers = new Harpoon<>(HarpoonType.BUFFERS);
    Harpoon<Path> dirs = new Harpoon<>(HarpoonType.DIRS);
    Harpoon<Path> files = new Harpoon<>(HarpoonType.FILES);

    public Harpoon<List<String>> getClipboard() {
        return clipboard;
    }

    public Harpoon<String> getPrompt() {
        return prompts;
    }

    public Harpoon<String> getKeyMappings() {
        return keyMappings;
    }

    public Harpoon<Buf> getBuffers() {
        return buffers;
    }

    public Harpoon<Path> getDirs() {
        return dirs;
    }

    public Harpoon<Path> getFiles() {
        return files;
    }

    public List<String> getList() {
        return Arrays.stream(HarpoonType.values()).map(Enum::toString).toList();
    }
}

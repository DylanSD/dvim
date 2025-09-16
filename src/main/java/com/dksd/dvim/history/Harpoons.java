package com.dksd.dvim.history;

import com.dksd.dvim.buffer.Buf;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Harpoons {

    Harpoon<List<String>> clipboard = new HarpoonListString(HarpoonType.CLIPBOARD);
    Harpoon<String> prompts = new HarpoonString(HarpoonType.CLIPBOARD);
    Harpoon<String> keyMappings = new HarpoonString(HarpoonType.KEYS);
    Harpoon<Buf> buffers = new HarpoonBuf();
    Harpoon<Path> dirs = new HarpoonDir();
    Harpoon<Path> files = new HarpoonDir();
    Harpoon<Buf> todoProjects = new HarpoonBuf();

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

    public Harpoon<Buf> getTodoProjects() {
        return todoProjects;
    }
}

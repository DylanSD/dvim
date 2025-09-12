package com.dksd.dvim.history;

import com.dksd.dvim.view.Line;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Harpoons {

    public static final String CLIPBOARD = "h_clips";
    public static final String DIRS = "h_dirs";
    public static final String FILES = "h_files";
    public static final String KEYS = "h_key_mappings";
    public static final String BUFFERS = "h_buffers";
    public static final String TERMINAL = "h_terminal";
    public static final String PROMPTS = "h_prompts";

    private final Map<String, Harpoon<String>> harpoons = new ConcurrentHashMap<>();

    public void add(String harpoon, String val) {
        harpoons.computeIfAbsent(harpoon, k -> new Harpoon<>(harpoon));
        harpoons.get(harpoon).add(val);
    }

    public void addAll(String harpoon, List<String> vals) {
        for (String val : vals) {
            add(harpoon, val);
        }
    }

    public Harpoon<String> get(String harpoon) {
        return harpoons.get(harpoon);
    }

    public List<String> getList() {
        return harpoons.keySet().stream().toList();
    }

}

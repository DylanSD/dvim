package com.dksd.dvim.key;

public class MapResult {
    private String commands;
    private boolean foundMapping;

    public String getCommands() {
        return commands;
    }

    public void setFoundMapping(boolean found) {
        foundMapping = found;
    }

    public boolean isFoundMapping() {
        return foundMapping;
    }

    @Override
    public String toString() {
        return "MapResult{" +
                "commands='" + commands + '\'' +
                ", foundMapping=" + foundMapping +
                '}';
    }
}

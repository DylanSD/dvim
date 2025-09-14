package com.dksd.dvim.model;

public enum ModelName {

    MERCURY("mercury"), MERCURY_CODER("mercury-coder");

    private final String modelName;

    ModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
}

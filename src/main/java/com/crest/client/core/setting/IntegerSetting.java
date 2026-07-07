package com.crest.client.core.setting;

import com.crest.client.core.ConfigManager;

public class IntegerSetting extends Setting<Integer> {
    private final int min;
    private final int max;

    public IntegerSetting(String name, int min, int max, int defaultValue) {
        this(name, "", min, max, defaultValue);
    }

    public IntegerSetting(String name, String description, int min, int max, int defaultValue) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
    }

    public int getMin() { return min; }
    public int getMax() { return max; }

    @Override
    public void set(Integer value) {
        super.set(Math.max(min, Math.min(max, value)));
    }

    @Override
    public void load(ConfigManager config, String moduleId) {
        if (config.has(moduleId, getName())) {
            set(config.getInt(moduleId, getName()));
        }
    }

    @Override
    public void save(ConfigManager config, String moduleId) {
        config.set(moduleId, getName(), get());
    }
}

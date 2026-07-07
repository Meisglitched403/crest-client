package com.crest.client.core.setting;

import com.crest.client.core.ConfigManager;

public class FloatSetting extends Setting<Float> {
    private final float min;
    private final float max;

    public FloatSetting(String name, float min, float max, float defaultValue) {
        this(name, "", min, max, defaultValue);
    }

    public FloatSetting(String name, String description, float min, float max, float defaultValue) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
    }

    public float getMin() { return min; }
    public float getMax() { return max; }

    @Override
    public void set(Float value) {
        super.set(Math.max(min, Math.min(max, value)));
    }

    @Override
    public void load(ConfigManager config, String moduleId) {
        if (config.has(moduleId, getName())) {
            set(config.getFloat(moduleId, getName()));
        }
    }

    @Override
    public void save(ConfigManager config, String moduleId) {
        config.set(moduleId, getName(), get());
    }
}

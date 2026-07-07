package com.crest.client.core.setting;

import com.crest.client.core.ConfigManager;

public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, int defaultColor) {
        this(name, "", defaultColor);
    }

    public ColorSetting(String name, String description, int defaultColor) {
        super(name, description, defaultColor);
    }

    public int getR() { return (get() >> 16) & 0xFF; }
    public int getG() { return (get() >> 8) & 0xFF; }
    public int getB() { return get() & 0xFF; }
    public int getA() { return (get() >> 24) & 0xFF; }
    public int getRGB() { return get() & 0x00FFFFFF; }

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

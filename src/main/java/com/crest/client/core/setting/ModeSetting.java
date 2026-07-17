package com.crest.client.core.setting;

import com.crest.client.core.ConfigManager;

public class ModeSetting extends Setting<Integer> {
    private final String[] modes;

    public ModeSetting(String name, String[] modes, int defaultIndex) {
        this(name, "", modes, defaultIndex);
    }

    public ModeSetting(String name, String description, String[] modes, int defaultIndex) {
        super(name, description, defaultIndex);
        this.modes = modes;
    }

    public String[] getModes() { return modes; }
    public String getMode() { return modes[get()]; }

    public void setMode(String mode) {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(mode)) { set(i); return; }
        }
    }

    public void cycle() {
        set((get() + 1) % modes.length);
    }

    public void cycleBack() {
        set((get() - 1 + modes.length) % modes.length);
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

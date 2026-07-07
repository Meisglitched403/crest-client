package com.crest.client.core.setting;

import com.crest.client.core.ConfigManager;

public class BooleanSetting extends Setting<Boolean> {
    private final Runnable onActivate;
    private final Runnable onDeactivate;

    public BooleanSetting(String name, boolean defaultValue) {
        this(name, "", defaultValue, null, null);
    }

    public BooleanSetting(String name, String description, boolean defaultValue) {
        this(name, description, defaultValue, null, null);
    }

    public BooleanSetting(String name, String description, boolean defaultValue, Runnable onActivate, Runnable onDeactivate) {
        super(name, description, defaultValue);
        this.onActivate = onActivate;
        this.onDeactivate = onDeactivate;
    }

    @Override
    public void set(Boolean value) {
        boolean old = get();
        super.set(value);
        if (value && !old && onActivate != null) onActivate.run();
        else if (!value && old && onDeactivate != null) onDeactivate.run();
    }

    public void toggle() {
        set(!get());
    }

    @Override
    public void load(ConfigManager config, String moduleId) {
        if (config.has(moduleId, getName())) {
            set(config.getBoolean(moduleId, getName()));
        }
    }

    @Override
    public void save(ConfigManager config, String moduleId) {
        config.set(moduleId, getName(), get());
    }
}

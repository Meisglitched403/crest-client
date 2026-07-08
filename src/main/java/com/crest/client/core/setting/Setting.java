package com.crest.client.core.setting;

import com.crest.client.core.ConfigManager;
import com.crest.client.core.CrestModules;

public abstract class Setting<T> {
    private final String name;
    private final String description;
    private final T defaultValue;
    private T value;
    private boolean visible = true;
    private String moduleId;

    public Setting(String name, T defaultValue) {
        this(name, "", defaultValue);
    }

    public Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public T get() { return value; }
    public T getDefault() { return defaultValue; }

    public void bindModule(String moduleId) {
        this.moduleId = moduleId;
    }

    public void set(T value) {
        this.value = value;
        onChange();
    }

    public void reset() { set(defaultValue); }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public abstract void load(ConfigManager config, String moduleId);
    public abstract void save(ConfigManager config, String moduleId);

    protected void onChange() {
        if (moduleId != null) {
            CrestModules.getConfigManager().set(moduleId, getName(), get());
        }
    }
}

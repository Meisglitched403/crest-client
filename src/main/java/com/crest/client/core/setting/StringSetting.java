package com.crest.client.core.setting;

import com.crest.client.core.ConfigManager;

public class StringSetting extends Setting<String> {
    public StringSetting(String name, String defaultValue) {
        this(name, "", defaultValue);
    }

    public StringSetting(String name, String description, String defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public void load(ConfigManager config, String moduleId) {
        if (config.has(moduleId, getName())) {
            set(config.getString(moduleId, getName()));
        }
    }

    @Override
    public void save(ConfigManager config, String moduleId) {
        config.set(moduleId, getName(), get());
    }
}

package com.crest.client.core.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingGroup {
    private final String name;
    private final List<Setting<?>> settings;
    private boolean expanded;

    public SettingGroup(String name, boolean expandedByDefault, Setting<?>... settings) {
        this.name = name;
        this.settings = new ArrayList<>(Arrays.asList(settings));
        this.expanded = expandedByDefault;
    }

    public SettingGroup(String name, Setting<?>... settings) {
        this(name, true, settings);
    }

    public String getName() { return name; }
    public List<Setting<?>> getSettings() { return settings; }
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    public void toggleExpanded() { this.expanded = !this.expanded; }

    public void addSetting(Setting<?> setting) {
        settings.add(setting);
    }
}

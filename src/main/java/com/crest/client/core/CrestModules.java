package com.crest.client.core;

import com.crest.client.core.event.EventBus;
import com.crest.client.core.event.ModuleToggleEvent;
import com.crest.client.core.setting.Setting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CrestModules {
    private static final Map<String, CrestModule> modules = new LinkedHashMap<>();
    private static final Map<String, Boolean> enabledOverrides = new java.util.HashMap<>();
    private static final EventBus eventBus = new EventBus();
    private static final ConfigManager configManager = new ConfigManager();

    public static void init() {
        configManager.load();
    }

    public static void register(CrestModule module) {
        modules.put(module.getId(), module);
        module.onInitialize();

        for (Setting<?> setting : module.getSettings()) {
            setting.load(configManager, module.getId());
        }

        if (module instanceof HudModule hud) {
            var pos = HudSettings.getPosition(module.getId(), hud.getX(), hud.getY());
            hud.setX(pos.x);
            hud.setY(pos.y);
        }
        module.loadSettings();
        if (configManager.has(module.getId(), "_enabled")) {
            if (!configManager.getBoolean(module.getId(), "_enabled")) {
                enabledOverrides.put(module.getId(), false);
            }
        } else if (!module.isEnabled()) {
            enabledOverrides.put(module.getId(), false);
        }
    }

    public static EventBus getEventBus() { return eventBus; }
    public static ConfigManager getConfigManager() { return configManager; }

    public static CrestModule get(String id) {
        return modules.get(id);
    }

    public static Map<String, CrestModule> getAll() {
        return modules;
    }

    public static boolean isEnabled(String id) {
        return enabledOverrides.getOrDefault(id, true);
    }

    public static void setEnabled(String id, boolean enabled) {
        enabledOverrides.put(id, enabled);
        configManager.set(id, "_enabled", enabled);
        CrestModule m = modules.get(id);
        if (m != null) {
            if (enabled) m.onEnable();
            else m.onDisable();
            eventBus.post(new ModuleToggleEvent(m, enabled));
        }
    }

    public static void toggle(String id) {
        setEnabled(id, !isEnabled(id));
    }

    public static List<CrestModule> getByCategory(String category) {
        return modules.values().stream()
            .filter(m -> category.equals(m.getCategory()))
            .collect(Collectors.toList());
    }

    public static List<String> getCategories() {
        return modules.values().stream()
            .map(CrestModule::getCategory)
            .distinct()
            .collect(Collectors.toList());
    }
}

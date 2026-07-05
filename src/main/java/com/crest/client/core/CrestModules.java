package com.crest.client.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CrestModules {
    private static final Map<String, CrestModule> modules = new LinkedHashMap<>();
    private static final Map<String, Boolean> enabledOverrides = new java.util.HashMap<>();

    public static void register(CrestModule module) {
        modules.put(module.getId(), module);
        module.onInitialize();
        if (module instanceof HudModule hud) {
            var pos = HudSettings.getPosition(module.getId(), hud.getX(), hud.getY());
            hud.setX(pos.x);
            hud.setY(pos.y);
        }
        module.loadSettings();
        if (!module.isEnabled()) {
            enabledOverrides.put(module.getId(), false);
        }
    }

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
        CrestModule m = modules.get(id);
        if (m != null) {
            if (enabled) m.onEnable();
            else m.onDisable();
        }
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

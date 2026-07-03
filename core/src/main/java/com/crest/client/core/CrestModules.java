package com.crest.client.core;

import java.util.LinkedHashMap;
import java.util.Map;

public class CrestModules {
    private static final Map<String, CrestModule> modules = new LinkedHashMap<>();

    public static void register(CrestModule module) {
        modules.put(module.getId(), module);
        module.onInitialize();
    }

    public static CrestModule get(String id) {
        return modules.get(id);
    }

    public static Map<String, CrestModule> getAll() {
        return modules;
    }
}

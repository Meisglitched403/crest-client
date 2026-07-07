package com.crest.client.core;

import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.Setting;

import java.util.List;

public class EntityCullingModule implements CrestModule {
    private final IntegerSetting maxDistance = new IntegerSetting(
        "Max Distance", 16, 256, 64
    );

    @Override public String getId() { return "entity_culling"; }
    @Override public String getName() { return "Entity Culling"; }
    @Override public String getDescription() { return "Skips rendering entities beyond a configurable distance"; }
    @Override public String getCategory() { return "Performance"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(maxDistance);
    }

    public static int getMaxDistance() {
        CrestModule m = CrestModules.get("entity_culling");
        return m instanceof EntityCullingModule e ? e.maxDistance.get() : 64;
    }

    public static void setMaxDistance(int dist) {
        CrestModule m = CrestModules.get("entity_culling");
        if (m instanceof EntityCullingModule e) {
            e.maxDistance.set(dist);
            CrestModules.getConfigManager().markDirty();
        }
    }
}

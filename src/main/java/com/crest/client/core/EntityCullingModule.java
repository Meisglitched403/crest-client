package com.crest.client.core;

public class EntityCullingModule implements CrestModule {
    private static int maxDistance = 64;

    @Override public String getId() { return "entity_culling"; }
    @Override public String getName() { return "Entity Culling"; }
    @Override public String getDescription() { return "Skips rendering entities beyond a configurable distance"; }
    @Override public String getCategory() { return "Performance"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public void loadSettings() {
        maxDistance = HudSettings.getInt("entity_culling", "maxDistance", 64);
    }

    public static int getMaxDistance() {
        return maxDistance;
    }

    public static void setMaxDistance(int dist) {
        maxDistance = Math.max(16, Math.min(256, dist));
        HudSettings.setInt("entity_culling", "maxDistance", maxDistance);
        HudSettings.save();
    }
}

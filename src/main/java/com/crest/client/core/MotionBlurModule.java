package com.crest.client.core;

import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.Setting;

import java.util.List;

public class MotionBlurModule implements CrestModule {
    public static final int DEFAULT_STRENGTH = 40;

    private final IntegerSetting strength = new IntegerSetting(
        "Strength", 1, 100, DEFAULT_STRENGTH
    );

    @Override
    public String getId() { return "motion_blur"; }

    @Override
    public String getName() { return "Motion Blur"; }

    @Override
    public String getDescription() { return "Frame ghosting motion blur via GPU pipeline"; }

    @Override
    public String getCategory() { return "Visual"; }

    @Override
    public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(strength);
    }

    public static int getStrength() {
        CrestModule m = CrestModules.get("motion_blur");
        return m instanceof MotionBlurModule e ? e.strength.get() : DEFAULT_STRENGTH;
    }

    public static void setStrength(int value) {
        CrestModule m = CrestModules.get("motion_blur");
        if (m instanceof MotionBlurModule e) {
            e.strength.set(value);
            CrestModules.getConfigManager().markDirty();
        }
    }
}

package com.crest.client.core;

import com.crest.client.core.setting.*;

import java.util.List;

public class AppleSkinModule implements CrestModule {
    private final BooleanSetting saturationOverlay = new BooleanSetting("Saturation Overlay", true);
    private final BooleanSetting hungerPreview = new BooleanSetting("Hunger Preview", true);
    private final BooleanSetting exhaustionUnderlay = new BooleanSetting("Exhaustion Underlay", true);

    @Override public String getId() { return "appleskin"; }
    @Override public String getName() { return "AppleSkin"; }
    @Override public String getDescription() { return "Food-related HUD improvements: saturation overlay, hunger preview, exhaustion bar"; }
    @Override public String getCategory() { return "HUD"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(saturationOverlay, hungerPreview, exhaustionUnderlay);
    }

    public static boolean showSaturation() {
        var m = CrestModules.get("appleskin");
        return m instanceof AppleSkinModule a && a.saturationOverlay.get();
    }

    public static boolean showHungerPreview() {
        var m = CrestModules.get("appleskin");
        return m instanceof AppleSkinModule a && a.hungerPreview.get();
    }

    public static boolean showExhaustion() {
        var m = CrestModules.get("appleskin");
        return m instanceof AppleSkinModule a && a.exhaustionUnderlay.get();
    }
}

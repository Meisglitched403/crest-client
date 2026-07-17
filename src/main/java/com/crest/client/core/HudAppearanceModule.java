package com.crest.client.core;

import com.crest.client.core.setting.Setting;

import java.util.List;

public class HudAppearanceModule implements CrestModule {
    @Override
    public String getId() { return "hud_appearance"; }

    @Override
    public String getName() { return "HUD Appearance"; }

    @Override
    public String getDescription() { return "Centralized background style for all HUD overlays — color, opacity, style, and corner radius."; }

    @Override
    public String getCategory() { return "HUD"; }

    @Override
    public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return HudBackground.INSTANCE.settings();
    }
}

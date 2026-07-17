package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.Setting;

import java.util.ArrayList;
import java.util.List;

public class PlayerHealthIndicatorModule implements CrestModule {
    private final BooleanSetting heartStacking = new BooleanSetting("Heart Stacking", true);
    private final IntegerSetting heartOffset = new IntegerSetting("Heart Offset", -50, 50, 0);

    @Override
    public String getId() { return "player_health_indicator"; }

    @Override
    public String getName() { return "Player Health Indicator"; }

    @Override
    public String getDescription() { return "Shows hearts above player entities. Invisible players without armor are hidden."; }

    @Override
    public String getCategory() { return "Visual"; }

    @Override
    public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>();
        s.add(heartStacking);
        s.add(heartOffset);
        return s;
    }

    public static boolean isHeartStackingEnabled() {
        CrestModule mod = CrestModules.get("player_health_indicator");
        if (mod instanceof PlayerHealthIndicatorModule m) return m.heartStacking.get();
        return true;
    }

    public static int getHeartOffset() {
        CrestModule mod = CrestModules.get("player_health_indicator");
        if (mod instanceof PlayerHealthIndicatorModule m) return m.heartOffset.get();
        return 0;
    }
}

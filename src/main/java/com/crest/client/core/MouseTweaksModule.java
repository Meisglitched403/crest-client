package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;
import java.util.List;

public class MouseTweaksModule implements CrestModule {
    private final BooleanSetting enabled = new BooleanSetting("Enabled", true);
    private final BooleanSetting rmbTweak = new BooleanSetting("RMB Tweak", true);
    private final BooleanSetting wheelTweak = new BooleanSetting("Wheel Tweak", true);
    private final ModeSetting scrollDirection = new ModeSetting(
        "Wheel Scroll Direction",
        new String[]{"Normal", "Inverted", "Position-Aware"},
        0
    );

    @Override public String getId() { return "mouse_tweaks"; }
    @Override public String getName() { return "Mouse Tweaks"; }
    @Override public String getDescription() { return "Enhances inventory management with mouse drag and scroll tweaks."; }
    @Override public String getCategory() { return "Utility"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, rmbTweak, wheelTweak, scrollDirection);
    }

    public static boolean isRmbTweakEnabled() {
        var m = CrestModules.get("mouse_tweaks");
        if (!(m instanceof MouseTweaksModule mod)) return true;
        return mod.enabled.get() && mod.rmbTweak.get();
    }

    public static boolean isWheelTweakEnabled() {
        var m = CrestModules.get("mouse_tweaks");
        if (!(m instanceof MouseTweaksModule mod)) return true;
        return mod.enabled.get() && mod.wheelTweak.get();
    }

    public static int getScrollDirection() {
        var m = CrestModules.get("mouse_tweaks");
        if (!(m instanceof MouseTweaksModule mod)) return 0;
        return mod.scrollDirection.get();
    }
}

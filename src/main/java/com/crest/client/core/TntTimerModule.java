package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.ColorSetting;
import com.crest.client.core.setting.FloatSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.Setting;
import com.crest.client.ui.ColorUtil;

import java.util.List;

public class TntTimerModule implements CrestModule {
    private final BooleanSetting enabled = new BooleanSetting("Enabled", true);
    private final ColorSetting textColor = new ColorSetting("Text Color", 0xFFFF5555);
    private final BooleanSetting decimals = new BooleanSetting("Show Decimals", true);
    private final FloatSetting offset = new FloatSetting("Height Offset", -2.0f, 2.0f, 0.0f);
    private final BooleanSetting bgEnabled = new BooleanSetting("BG Enabled", true);
    private final ColorSetting bgColor = new ColorSetting("BG Color", 0x000000);
    private final IntegerSetting bgOpacity = new IntegerSetting("BG Opacity", 0, 255, 100);

    @Override public String getId() { return "tnt_timer"; }
    @Override public String getName() { return "TNT Timer"; }
    @Override public String getDescription() { return "Shows a countdown timer above ignited TNT."; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, textColor, decimals, offset, bgEnabled, bgColor, bgOpacity);
    }

    public static int getTextColor() {
        var m = CrestModules.get("tnt_timer");
        if (!(m instanceof TntTimerModule mod)) return 0xFFFF5555;
        return mod.textColor.get() | 0xFF000000;
    }

    public static boolean showDecimals() {
        var m = CrestModules.get("tnt_timer");
        if (!(m instanceof TntTimerModule mod)) return true;
        return mod.decimals.get();
    }

    public static float getOffset() {
        var m = CrestModules.get("tnt_timer");
        if (!(m instanceof TntTimerModule mod)) return 0.0f;
        return mod.offset.get();
    }

    public static boolean isBgEnabled() {
        var m = CrestModules.get("tnt_timer");
        if (!(m instanceof TntTimerModule mod)) return true;
        return mod.bgEnabled.get();
    }

    public static int getBgColor() {
        var m = CrestModules.get("tnt_timer");
        if (!(m instanceof TntTimerModule mod)) return 0x000000;
        return mod.bgColor.get();
    }

    public static int getBgOpacity() {
        var m = CrestModules.get("tnt_timer");
        if (!(m instanceof TntTimerModule mod)) return 100;
        return mod.bgOpacity.get();
    }
}

package com.crest.client.core;

import com.crest.client.core.setting.*;

import java.util.List;

public class ShieldStatusModule implements CrestModule {
    private final ColorSetting blockingColor = new ColorSetting("Blocking Color", 0x8000FF00);
    private final ColorSetting cooldownColor = new ColorSetting("Cooldown Color", 0x80FF0000);

    @Override public String getId() { return "shield_status"; }
    @Override public String getName() { return "Shield Status"; }
    @Override public String getDescription() { return "Tints shields green when blocking, red when on cooldown"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(blockingColor, cooldownColor);
    }

    public static int getBlockingColor() {
        ShieldStatusModule m = getModule();
        return m != null ? m.blockingColor.get() : 0x8000FF00;
    }
    public static int getCooldownColor() {
        ShieldStatusModule m = getModule();
        return m != null ? m.cooldownColor.get() : 0x80FF0000;
    }

    private static ShieldStatusModule getModule() {
        CrestModule m = CrestModules.get("shield_status");
        return m instanceof ShieldStatusModule ssm ? ssm : null;
    }
}

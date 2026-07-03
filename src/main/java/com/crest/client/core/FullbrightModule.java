package com.crest.client.core;

import net.minecraft.client.Minecraft;

public class FullbrightModule implements CrestModule {
    private int gammaLevel = 100;
    private double previousGamma;

    @Override
    public String getId() { return "fullbright"; }
    @Override
    public String getName() { return "Fullbright"; }
    @Override
    public String getDescription() { return "Brightens dark areas by maxing gamma"; }
    @Override
    public String getCategory() { return "Visual"; }
    @Override
    public boolean isEnabled() { return false; }

    @Override
    public void onInitialize() {}

    public int getGammaLevel() { return gammaLevel; }

    public void adjustGamma(int delta) {
        gammaLevel = Math.max(0, Math.min(100, gammaLevel + delta));
        if (CrestModules.isEnabled("fullbright")) {
            Minecraft mc = Minecraft.getInstance();
            mc.options.gamma().set(gammaLevel / 100.0);
        }
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        previousGamma = mc.options.gamma().get();
        mc.options.gamma().set(gammaLevel / 100.0);
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.gamma().set(previousGamma);
    }
}

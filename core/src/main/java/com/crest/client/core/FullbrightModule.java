package com.crest.client.core;

import net.minecraft.client.Minecraft;

public class FullbrightModule implements CrestModule {
    private static final double MAX_GAMMA = 10.0;
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

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        previousGamma = mc.options.gamma().get();
        mc.options.gamma().set(MAX_GAMMA);
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.gamma().set(previousGamma);
    }
}

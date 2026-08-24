package com.crest.client.core;

import net.minecraft.client.Minecraft;

public class FullbrightModule implements CrestModule {
    private static final double FULLBRIGHT_GAMMA = 10.0;
    private double savedGamma = -1;

    @Override
    public String getId() { return "fullbright"; }
    @Override
    public String getName() { return "Fullbright"; }
    @Override
    public String getDescription() { return "Raises the brightness gamma so caves and night are fully visible."; }
    @Override
    public String getCategory() { return "Visual"; }
    @Override
    public boolean isEnabled() { return false; }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            savedGamma = mc.options.gamma().get();
            mc.options.gamma().set(FULLBRIGHT_GAMMA);
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null && savedGamma >= 0) {
            mc.options.gamma().set(savedGamma);
            savedGamma = -1;
        }
    }
}

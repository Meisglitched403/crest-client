package com.crest.client.core;

import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.KeybindSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public class FullbrightModule implements CrestModule {
    private final IntegerSetting gammaLevel = new IntegerSetting(
        "Gamma Level", 0, 100, 100
    );
    private final KeybindSetting toggleKey = new KeybindSetting(
        "Toggle Key", org.lwjgl.glfw.GLFW.GLFW_KEY_B
    );
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
    public List<Setting<?>> getSettings() {
        return List.of(gammaLevel, toggleKey);
    }

    public int getGammaLevel() { return gammaLevel.get(); }

    public void adjustGamma(int delta) {
        gammaLevel.set(Math.max(0, Math.min(100, gammaLevel.get() + delta)));
        if (CrestModules.isEnabled("fullbright")) {
            Minecraft mc = Minecraft.getInstance();
            mc.options.gamma().set(gammaLevel.get() / 100.0);
        }
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        previousGamma = mc.options.gamma().get();
        mc.options.gamma().set(gammaLevel.get() / 100.0);
        if (mc.player != null) {
            mc.player.sendOverlayMessage(Component.literal("Gamma: " + gammaLevel.get() + "%"));
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.gamma().set(previousGamma);
        if (mc.player != null) {
            mc.player.sendOverlayMessage(Component.literal("Gamma: OFF"));
        }
    }
}

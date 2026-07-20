package com.crest.client.core;

import com.crest.client.core.setting.ColorSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: Combo Counter HUD. Shows the current hit-combo and the session max,
 * fed by ComboTracker (MinecraftAttackMixin).
 */
public class ComboModule extends HudModule {
    private static final int PAD = 2;
    private static final int LINE_H = 10;

    private final ColorSetting color = new ColorSetting("Text Color", 0xFFFFAA00);
    private final ColorSetting maxColor = new ColorSetting("Max Color", 0xFFAAAAAA);

    public ComboModule() {
        super(-1, 120);
    }

    @Override public String getId() { return "combo"; }
    @Override public String getName() { return "Combo Counter"; }
    @Override public String getDescription() { return "Shows your current and max hit-combo."; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>();
        s.add(color);
        s.add(maxColor);
        return s;
    }

    @Override
    public int getWidth() {
        return Math.max(Minecraft.getInstance().font.width("Combo: 0"),
                Minecraft.getInstance().font.width("Max: 0")) + PAD * 2;
    }

    @Override
    public int getHeight() {
        return LINE_H * 2 + PAD * 2;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        int boxW = getRenderWidth();
        int boxH = getHeight();
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - boxW : x;
        int ry = y;
        HudBackground.draw(g, rx, ry, boxW, boxH);

        int cy = ry + PAD;
        g.text(mc.font, Component.literal("Combo: " + ComboTracker.combo()), rx + PAD, cy, color.get());
        cy += LINE_H;
        g.text(mc.font, Component.literal("Max: " + ComboTracker.maxCombo()), rx + PAD, cy, maxColor.get());
    }
}

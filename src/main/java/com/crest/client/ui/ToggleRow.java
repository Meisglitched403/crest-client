package com.crest.client.ui;

import com.crest.client.core.CrestModules;
import com.crest.client.core.setting.BooleanSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class ToggleRow implements Widget {
    private final BooleanSetting setting;
    private final Animated anim = new Animated(0f, 12f);

    public ToggleRow(BooleanSetting setting) {
        this.setting = setting;
        anim.setImmediate(setting.get() ? 1f : 0f);
    }

    @Override
    public int getHeight() { return H; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        anim.set(setting.get() ? 1f : 0f);
        anim.tick(delta);
        int labelW = font.width(setting.getName()) + 4;
        int controlX = x + labelW + 4;
        int controlW = w - labelW - 8;
        if (controlW < ToggleSwitch.W) controlW = ToggleSwitch.W;
        g.text(font, Component.literal(setting.getName()), x + 2, y + 4, Theme.ON_SURFACE_VARIANT);
        ToggleSwitch.render(g, controlX + controlW - ToggleSwitch.W, y + 2, setting.get(), anim.get());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) { setting.toggle(); CrestModules.getConfigManager().markDirty(); return true; }
        return false;
    }
}

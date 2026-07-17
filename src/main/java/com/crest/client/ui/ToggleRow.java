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
        g.text(font, Component.literal(setting.getName()), x + 2, y + 4, Theme.ON_SURFACE_VARIANT);

        String state = setting.get() ? "ON" : "OFF";
        int stateW = font.width(state) + 8;
        int toggleX = x + w - stateW - ToggleSwitch.W - 8;
        boolean stateHover = mx >= toggleX && mx <= toggleX + stateW && my >= y && my <= y + H;
        int stateColor = ColorUtil.lerpARGB(Theme.MUTED_FOREGROUND, Theme.TEXT_ON, anim.get());
        if (stateHover) stateColor = ColorUtil.lerpARGB(stateColor, 0xFFFFFFFF, 0.2f);
        g.text(font, Component.literal(state), toggleX + 4, y + 4, stateColor);

        ToggleSwitch.render(g, toggleX + stateW + 4, y + 3, setting.get(), anim.get());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) { setting.toggle(); CrestModules.getConfigManager().markDirty(); return true; }
        return false;
    }
}

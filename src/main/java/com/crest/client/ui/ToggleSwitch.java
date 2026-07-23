package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ToggleSwitch implements Widget {
    public static final int W = 44;
    public static final int H = 24;

    private boolean on;
    private final Consumer<Boolean> onChange;
    private final Animated anim = new Animated(0f, 12f);
    private int lastX, lastY;

    public ToggleSwitch(boolean initial, Consumer<Boolean> onChange) {
        this.on = initial;
        this.onChange = onChange;
        anim.setImmediate(on ? 1f : 0f);
    }

    public boolean isOn() { return on; }
    public void setOn(boolean on) {
        this.on = on;
        anim.set(on ? 1f : 0f);
    }

    @Override
    public int getHeight() { return H; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        lastX = x; lastY = y;
        anim.set(on ? 1f : 0f);
        anim.tick(delta);
        renderStatic(g, x, y, W, on, anim.get());
    }

    /** Static drawing helper for legacy callers that pass animation amount directly. */
    public static void render(GuiGraphicsExtractor g, int x, int y, boolean on, float animAmt) {
        renderStatic(g, x, y, W, on, animAmt);
    }

    private static void renderStatic(GuiGraphicsExtractor g, int x, int y, int w, boolean on, float t) {
        int trackColor = ColorUtil.lerpARGB(0x1AFFFFFF, Theme.getAnimatedAccent(), t);
        g.fillGradient(x, y, x + w, y + H, trackColor, ColorUtil.withAlpha(trackColor, 80));
        Panel.drawHollowRect(g, x, y, w, H, Theme.BORDER_LIGHT);

        int knobMinX = x + 3;
        int knobMaxX = x + w - 19;
        int knobX = (int) Anim.lerp(knobMinX, knobMaxX, t);
        int knobColor = ColorUtil.lerpARGB(Theme.MUTED_FOREGROUND, Theme.PRIMARY, t);
        g.fill(knobX, y + 3, knobX + 16, y + H - 3, knobColor);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && mx >= lastX && mx <= lastX + W && my >= lastY && my <= lastY + H) {
            on = !on;
            anim.set(on ? 1f : 0f);
            if (onChange != null) onChange.accept(on);
            UiSounds.click();
            return true;
        }
        return false;
    }
}

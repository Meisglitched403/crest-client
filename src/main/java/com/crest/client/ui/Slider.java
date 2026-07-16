package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class Slider implements Widget {
    private final float min, max;
    private float value;
    private final Consumer<Float> onChange;
    private int lastBarX, lastBarW;
    public boolean dragging;

    public Slider(float min, float max, float initial, Consumer<Float> onChange) {
        this.min = min;
        this.max = max;
        this.value = initial;
        this.onChange = onChange;
    }

    public float getValue() { return value; }
    public void setValue(float v) { value = Anim.clamp(v, min, max); }

    @Override
    public int getHeight() { return H; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        lastBarX = x;
        lastBarW = w;
        float frac = (max > min) ? (value - min) / (max - min) : 0f;
        frac = Anim.clamp(frac, 0, 1);

        int barY = y + 7;
        g.fill(x, barY, x + w, barY + 4, ColorUtil.withAlpha(Theme.BG_BASE, 220));
        int fillW = (int) (frac * w);
        g.fill(x, barY, x + fillW, barY + 4, Theme.getAnimatedAccent());
        if (dragging) {
            g.fill(x + fillW - 2, barY - 2, x + fillW + 2, barY + 6, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        dragging = true;
        updateValue(mx);
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my) {
        if (!dragging) return false;
        updateValue(mx);
        return true;
    }

    public void stopDrag() { dragging = false; }

    private void updateValue(double mx) {
        if (max <= min || lastBarW <= 0) return;
        float rel = (float) ((mx - lastBarX) / lastBarW);
        rel = Anim.clamp(rel, 0, 1);
        value = min + rel * (max - min);
        if (onChange != null) onChange.accept(value);
    }
}

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

        int trackH = 8;
        int barY = y + (H - trackH) / 2;
        // Track (full width)
        g.fill(x, barY, x + w, barY + trackH, ColorUtil.withAlpha(Theme.MUTED, 220));
        // Fill (accent) up to the knob
        int fillW = Math.max(8, (int) (frac * w));
        g.fill(x, barY, x + fillW, barY + trackH, Theme.getAnimatedAccent());
        // Handle (8x8) centered on the fill end
        int hx = (int) (frac * w) - 4;
        int hy = y + (H - 8) / 2;
        g.fill(x + hx, hy, x + hx + 8, hy + 8, Theme.FOREGROUND);
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

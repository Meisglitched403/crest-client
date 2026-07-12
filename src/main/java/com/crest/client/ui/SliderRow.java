package com.crest.client.ui;

import com.crest.client.core.CrestModules;
import com.crest.client.core.setting.FloatSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class SliderRow implements Widget {
    private final Setting<?> setting;
    private final float min, max;
    private int lastBarX, lastBarW;
    public boolean dragging;

    public SliderRow(Setting<?> setting) {
        this.setting = setting;
        if (setting instanceof IntegerSetting is) { min = is.getMin(); max = is.getMax(); }
        else if (setting instanceof FloatSetting fs) { min = fs.getMin(); max = fs.getMax(); }
        else { min = 0; max = 1; }
    }

    @Override
    public int getHeight() { return H; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        int labelW = font.width(setting.getName()) + 4;
        lastBarX = x + labelW + 4;
        int barMaxW = w - labelW - 8;
        lastBarW = Math.min(barMaxW - 34, 110);
        if (lastBarW < 20) lastBarW = 20;

        float val = getVal();
        float frac = (max > min) ? (val - min) / (max - min) : 0f;
        frac = Anim.clamp(frac, 0, 1);

        if (dragging) {
            int barY = y + 7;
            g.fill(lastBarX, barY, lastBarX + lastBarW, barY + 4, ColorUtil.withAlpha(Theme.BG_BASE, 220));
            int fillW = (int) (frac * lastBarW);
            g.fill(lastBarX, barY, lastBarX + fillW, barY + 4, Theme.getAnimatedAccent());
            g.fill(lastBarX + fillW - 2, barY - 2, lastBarX + fillW + 2, barY + 6, 0xFFFFFFFF);
        }

        String label = (setting instanceof FloatSetting) ? String.format("%.1f", val) : String.valueOf((int) val);
        g.text(font, Component.literal(setting.getName()), x + 2, y + 4, Theme.TEXT_DIM);
        g.text(font, Component.literal(label), lastBarX + lastBarW + 6, y + 3, Theme.TEXT);
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
        float val = min + rel * (max - min);
        if (setting instanceof IntegerSetting is) is.set((int) Math.round(val));
        else if (setting instanceof FloatSetting fs) fs.set(val);
        CrestModules.getConfigManager().markDirty();
    }

    private float getVal() {
        if (setting instanceof IntegerSetting is) return is.get();
        if (setting instanceof FloatSetting fs) return fs.get();
        return 0;
    }
}

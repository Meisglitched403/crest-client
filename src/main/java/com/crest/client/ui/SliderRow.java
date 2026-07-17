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
    private final Slider slider;
    private int labelW;

    public SliderRow(Setting<?> setting) {
        this.setting = setting;
        float min, max;
        if (setting instanceof IntegerSetting is) { min = is.getMin(); max = is.getMax(); }
        else if (setting instanceof FloatSetting fs) { min = fs.getMin(); max = fs.getMax(); }
        else { min = 0; max = 1; }
        float val = (setting instanceof IntegerSetting is) ? is.get()
            : (setting instanceof FloatSetting fs) ? fs.get() : 0;
        slider = new Slider(min, max, val, this::onChange);
    }

    private void onChange(float v) {
        if (setting instanceof IntegerSetting is) is.set(Math.round(v));
        else if (setting instanceof FloatSetting fs) fs.set(v);
        CrestModules.getConfigManager().markDirty();
    }

    @Override
    public int getHeight() { return H; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        slider.setValue((setting instanceof IntegerSetting is) ? is.get()
            : (setting instanceof FloatSetting fs) ? fs.get() : 0);
        labelW = font.width(setting.getName()) + 4;

        g.text(font, Component.literal(setting.getName()), x + 2, y + 4, Theme.ON_SURFACE_VARIANT);

        String valLabel = (setting instanceof FloatSetting)
            ? String.format("%.1f", slider.getValue())
            : String.valueOf(Math.round(slider.getValue()));
        int valW = font.width(valLabel) + 8;

        int barMaxW = w - labelW - 8;
        int barW = Math.max(Math.min(barMaxW - valW - 8, 140), 40);
        int barX = x + labelW + 4;
        int valX = barX + barW + 6;

        slider.render(g, font, barX, y, barW, mx, my, delta);
        g.fill(valX, y + 2, valX + valW, y + H - 2, ColorUtil.withAlpha(Theme.SURFACE_VARIANT, 120));
        g.centeredText(font, Component.literal(valLabel), valX + valW / 2, y + 4, Theme.ON_SURFACE);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        slider.dragging = true;
        slider.mouseClicked(mx, my, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my) { return slider.mouseDragged(mx, my); }

    public void stopDrag() { slider.stopDrag(); }
}

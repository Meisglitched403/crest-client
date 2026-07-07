package com.crest.client.core;

import com.crest.client.core.setting.ColorSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.List;

public class CrosshairModule implements CrestModule {
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath("crest-client", "crosshair");

    private final ModeSetting style = new ModeSetting("Style", new String[]{"Dot", "Cross", "Circle"}, 0);
    private final ColorSetting color = new ColorSetting("Color", 0xFFFFFFFF);
    private final IntegerSetting size = new IntegerSetting("Size", 1, 20, 4);
    private final IntegerSetting gap = new IntegerSetting("Gap", 0, 10, 2);
    private final IntegerSetting thickness = new IntegerSetting("Thickness", 1, 6, 2);

    @Override
    public String getId() { return "crosshair"; }
    @Override
    public String getName() { return "Custom Crosshair"; }
    @Override
    public String getDescription() { return "Dot / Cross / Circle crosshair with color and size control"; }

    @Override
    public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(style, color, size, gap, thickness);
    }

    @Override
    public void onInitialize() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CROSSHAIR,
            LAYER,
            this::render
        );
    }

    private void render(GuiGraphicsExtractor g, DeltaTracker d) {
        if (!CrestModules.isEnabled("crosshair")) return;
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int cx = w / 2;
        int cy = h / 2;

        switch (style.get()) {
            case 0 -> renderDot(g, cx, cy);
            case 1 -> renderCross(g, cx, cy);
            case 2 -> renderCircle(g, cx, cy);
        }
    }

    private void renderDot(GuiGraphicsExtractor gfx, int cx, int cy) {
        int r = Math.max(size.get() / 2, 1);
        gfx.fill(cx - r, cy - r, cx + r + 1, cy + r + 1, color.get());
    }

    private void renderCross(GuiGraphicsExtractor gfx, int cx, int cy) {
        int t = thickness.get();
        int half = t / 2;
        int s = size.get();
        int gapVal = gap.get();
        int c = color.get();
        gfx.fill(cx - half, cy - gapVal - s, cx - half + t, cy - gapVal, c);
        gfx.fill(cx - half, cy + gapVal, cx - half + t, cy + gapVal + s, c);
        gfx.fill(cx - gapVal - s, cy - half, cx - gapVal, cy - half + t, c);
        gfx.fill(cx + gapVal, cy - half, cx + gapVal + s, cy - half + t, c);
    }

    private void renderCircle(GuiGraphicsExtractor gfx, int cx, int cy) {
        int r = Math.max(size.get(), 1);
        int c = color.get();
        for (int row = -r; row <= r; row++) {
            int halfW = (int) Math.sqrt(r * r - row * row);
            gfx.fill(cx - halfW, cy + row, cx + halfW + 1, cy + row + 1, c);
        }
    }
}

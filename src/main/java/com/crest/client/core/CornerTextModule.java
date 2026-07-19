package com.crest.client.core;

import com.crest.client.core.HudBackground;
import com.crest.client.core.setting.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CornerTextModule implements CrestModule {
    private final StringSetting text = new StringSetting("Text", "Crest Client");
    private final ModeSetting corner = new ModeSetting("Corner",
        new String[]{"Top Left", "Top Right", "Bottom Left", "Bottom Right"}, 3);
    private final ColorSetting color = new ColorSetting("Text Color", 0xFFFFFFFF);
    private final BooleanSetting shadow = new BooleanSetting("Text Shadow", true);
    private final BooleanSetting background = new BooleanSetting("Background", false);
    private final IntegerSetting offsetX = new IntegerSetting("Offset X", 0, 50, 4);
    private final IntegerSetting offsetY = new IntegerSetting("Offset Y", 0, 50, 4);
    private final FloatSetting scale = new FloatSetting("Scale", 0.5f, 3.0f, 1.0f);

    @Override public String getId() { return "corner_text"; }
    @Override public String getName() { return "Corner Text"; }
    @Override public String getDescription() { return "Shows customizable text in the corner of container screens"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(text, corner, color, shadow, background, offsetX, offsetY, scale);
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        return new CornerTextConfigScreen(this, parent);
    }

    public static String getText() {
        CornerTextModule m = getModule();
        return m != null ? m.text.get() : "Crest Client";
    }
    public static String getCorner() {
        CornerTextModule m = getModule();
        return m != null ? m.corner.getMode() : "Bottom Right";
    }
    public static int getColor() {
        CornerTextModule m = getModule();
        return m != null ? m.color.get() : 0xFFFFFFFF;
    }
    public static boolean hasShadow() {
        CornerTextModule m = getModule();
        return m != null && m.shadow.get();
    }
    public static boolean isBackgroundEnabled() {
        CornerTextModule m = getModule();
        return m != null && m.background.get();
    }
    public static int getOffsetX() {
        CornerTextModule m = getModule();
        return m != null ? m.offsetX.get() : 4;
    }
    public static int getOffsetY() {
        CornerTextModule m = getModule();
        return m != null ? m.offsetY.get() : 4;
    }

    private static CornerTextModule getModule() {
        CrestModule m = CrestModules.get("corner_text");
        return m instanceof CornerTextModule ctm ? ctm : null;
    }

    public static void draw(GuiGraphicsExtractor g) {
        if (!CrestModules.isEnabled("corner_text")) return;

        String t = getText();
        if (t == null || t.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int tw = font.width(t);
        int lh = font.lineHeight;
        int col = getColor();
        int offX = getOffsetX();
        int offY = getOffsetY();
        float s = getScale();

        int guiW = g.guiWidth();
        int guiH = g.guiHeight();

        int rx, ry;
        switch (getCorner()) {
            case "Top Left" -> { rx = offX; ry = offY; }
            case "Top Right" -> { rx = guiW - tw - offX; ry = offY; }
            case "Bottom Left" -> { rx = offX; ry = guiH - lh - offY; }
            default -> { rx = guiW - tw - offX; ry = guiH - lh - offY; }
        }

        g.pose().pushMatrix();
        g.pose().translate(rx, ry);
        g.pose().scale(s);
        g.pose().translate(-rx, -ry);

        if (isBackgroundEnabled()) {
            HudBackground.draw(g, rx - 2, ry - 2, tw + 8, lh + 8);
        }
        if (hasShadow()) {
            g.text(font, Component.literal(t), rx + 1, ry + 1, (col & 0x00FFFFFF) | 0x80000000);
        }
        g.text(font, Component.literal(t), rx, ry, col);

        g.pose().popMatrix();
    }

    public static float getScale() {
        CornerTextModule m = getModule();
        return m != null ? m.scale.get() : 1.0f;
    }
}

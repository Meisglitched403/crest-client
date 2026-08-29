package com.crest.client.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Toggle-sneak behaviour + indicator. */
public final class ToggleSneakService {
    private static volatile boolean enabled = false;

    private ToggleSneakService() {}

    public static boolean isEnabled() { return enabled; }

    public static void toggle(Minecraft mc) {
        enabled = !enabled;
        if (mc.player != null) {
            mc.player.setShiftKeyDown(enabled);
        }
    }

    public static void tickKeybindOverrides(Minecraft mc, ModConfig cfg) {}

    public static void renderIndicator(GuiGraphicsExtractor g, ModConfig cfg, int width, int height) {
        if (!enabled) return;
        int color = 0xFF55FFFF;
        String label = "Sneak";
        g.fill(width - 60, height - 24, width - 4, height - 10, (120 << 24) | 0x000000);
        g.text(Minecraft.getInstance().font, Component.literal(label), width - 56, height - 21, color);
    }
}

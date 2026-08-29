package com.crest.client.waypoints;

import net.minecraft.client.Minecraft;

/** UI scale helpers. */
public final class UiScale {
    private UiScale() {}

    public static double scaleFactor(Minecraft mc) {
        return mc.getWindow() != null ? mc.getWindow().getGuiScale() : 1.0;
    }

    public static int scale(int value, Minecraft mc) {
        return (int) Math.round(value * scaleFactor(mc));
    }
}

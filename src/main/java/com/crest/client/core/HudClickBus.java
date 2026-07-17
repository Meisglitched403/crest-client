package com.crest.client.core;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: Dispatches mouse clicks to ClickableHud modules whose current bounds
 * contain the click point. Driven from MouseInputMixin once per detected edge.
 */
public final class HudClickBus {
    private static final List<ClickableHud> handlers = new ArrayList<>();

    public static void register(ClickableHud h) { handlers.add(h); }

    public static void dispatch(int mouseX, int mouseY, int button) {
        Minecraft mc = Minecraft.getInstance();
        for (ClickableHud h : handlers) {
            int x = h.hudX(), y = h.hudY(), w = h.hudW(), hh = h.hudH();
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + hh) {
                h.onHudClick(mouseX, mouseY, button);
                return; // topmost wins (first registered)
            }
        }
    }
}

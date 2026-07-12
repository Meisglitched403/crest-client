package com.crest.client.core;

import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationToast {
    private static final List<Toast> active = new ArrayList<>();
    private static final int DURATION_MS = 3000;
    private static final int W = 220;
    private static final int H = 28;

    private record Toast(String text, long endTime, int color) {}

    public static void show(String text) { show(text, 0xFF44FF88); }
    public static void show(String text, int accentColor) {
        active.add(new Toast(text, System.currentTimeMillis() + DURATION_MS, accentColor));
    }

    public static void render(GuiGraphicsExtractor g, int screenW, int screenH) {
        long now = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();

        Iterator<Toast> it = active.iterator();
        int yOff = 8;
        while (it.hasNext()) {
            Toast t = it.next();
            if (now > t.endTime) { it.remove(); continue; }
            long remaining = t.endTime - now;
            float fade = Math.min(1f, remaining / 300f);

            int x = screenW - W - 8;
            int y = yOff;

            int alpha = (int) (200 * fade);
            Panel.draw(g, x, y, W, H, (alpha << 24) | 0x141428);

            g.fill(x, y, x + 3, y + H, t.color);
            g.text(mc.font, Component.literal(t.text), x + 12, y + (H - 8) / 2, 0xFFFFFFFF);

            yOff += H + 6;
        }
    }
}

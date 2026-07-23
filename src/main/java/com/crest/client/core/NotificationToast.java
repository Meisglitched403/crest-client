package com.crest.client.core;

import com.crest.client.ui.Anim;
import com.crest.client.ui.ColorUtil;
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
    private static final int DURATION_MS = 2500;
    private static final int FADE_MS = 300;
    private static final int W = 220;
    private static final int H = 28;

    private static record Toast(String text, long endTime, long startTime, int color) {}

    public static void show(String text) { show(text, 0xFF44FF88); }
    public static void show(String text, int accentColor) {
        active.add(new Toast(text, System.currentTimeMillis() + DURATION_MS, System.currentTimeMillis(), accentColor));
    }

    public static void render(GuiGraphicsExtractor g, int screenW, int screenH) {
        long now = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;

        Iterator<Toast> it = active.iterator();
        int yOff = 8;
        while (it.hasNext()) {
            Toast t = it.next();
            if (now > t.endTime + FADE_MS) { it.remove(); continue; }

            long elapsed = now - t.startTime;
            long remaining = t.endTime - now;
            boolean fadingOut = now > t.endTime;

            float opacity;
            if (elapsed < FADE_MS) {
                opacity = (float) elapsed / FADE_MS;
            } else if (fadingOut) {
                opacity = (float) (t.endTime + FADE_MS - now) / FADE_MS;
            } else {
                opacity = 1f;
            }
            opacity = Anim.clamp(opacity, 0f, 1f);

            float slideIn = 1f - (float) Math.exp(-elapsed / 150f);
            int slideOffset = (int) ((1f - slideIn) * 40);

            int x = screenW - W - 8 + slideOffset;
            int y = yOff;

            int alpha = (int) (220 * opacity);
            Panel.draw(g, x, y, W, H, ColorUtil.withAlpha(0xFF141428, alpha));
            g.fill(x, y, x + 3, y + H, t.color);
            g.text(mc.font, Component.literal(t.text), x + 12, y + (H - 8) / 2, ColorUtil.withAlpha(0xFFFFFFFF, (int) (255 * opacity)));

            yOff += H + 6;
        }
    }
}

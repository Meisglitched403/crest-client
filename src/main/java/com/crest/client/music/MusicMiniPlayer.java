package com.crest.client.music;

import com.crest.client.ui.Animated;
import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;
import com.crest.client.ui.UiSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/**
 * Compact reusable music player widget. Hosts (CrestMenu, pause screen mixin) draw it
 * via {@link #render} and forward clicks via {@link #mouseClicked}. The seek bar supports
 * continuous drag by polling the mouse button during render, so hosts do not need to wire
 * mouseDragged/mouseReleased.
 */
public class MusicMiniPlayer {
    private static final int BAR_H = 4;
    private static final int BTN_W = 34;
    private static final int BTN_H = 24;

    private final Animated progressAnim = new Animated(0f, 12f);
    private final Map<String, Animated> hover = new HashMap<>();
    private final Map<String, Boolean> wasHover = new HashMap<>();

    private boolean dragging;
    private Screen from;

    // Geometry mirrored from the last rendered frame (used by input handlers).
    private int barX, barW, barY;
    private int titleX, titleY, titleW, titleH;
    private int prevX, prevY, playX, playY, nextX, nextY;
    private int openX, openY, openW, openH;

    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, int mx, int my, float delta) {
        from = Minecraft.getInstance().screen;
        int accent = Theme.getAnimatedAccent();
        int pad = 10;

        Panel.draw(g, x, y, w, h, ColorUtil.withAlpha(Theme.GLASS_BG, 235));
        Panel.drawHollowRect(g, x, y, w, h, Theme.BORDER_LIGHT);

        MusicPlayer player = MusicModule.getPlayer();
        if (!drawEmpty(g, font, player, x, y, w, h, pad, accent, mx, my, delta)) {
            drawTrack(g, font, player, x, y, w, h, pad, accent, mx, my, delta);
        }

        if (!leftDown()) dragging = false;
    }

    private boolean drawEmpty(GuiGraphicsExtractor g, Font font, MusicPlayer player, int x, int y, int w, int h, int pad, int accent, int mx, int my, float delta) {
        if (player.hasTrack()) return false;

        int tx = x + pad;
        g.text(font, Component.literal("♫  Music"), tx, y + pad, Theme.MUTED_FOREGROUND);
        g.text(font, Component.literal("No track playing"), tx, y + pad + font.lineHeight + 4, Theme.TEXT_DIM);
        if (!player.isBackendAvailable()) {
            g.text(font, Component.literal("⚠ No audio device"), tx, y + pad + font.lineHeight * 2 + 4, Theme.DESTRUCTIVE);
        }

        int bw = 106, bh = 28;
        int bx = x + w - pad - bw;
        int by = y + (h - bh) / 2;
        openX = bx; openY = by; openW = bw; openH = bh;
        boolean oh = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        float hv = hoverValue("open", oh, delta);
        int base = ColorUtil.withAlpha(Theme.PRIMARY, 190);
        int hov = ColorUtil.withAlpha(accent, 220);
        Panel.draw(g, bx, by, bw, bh, ColorUtil.lerpARGB(base, hov, hv));
        Panel.drawHollowRect(g, bx, by, bw, bh, oh ? accent : Theme.BORDER_LIGHT);
        g.centeredText(font, Component.literal("Open Music ♫"), bx + bw / 2, by + (bh - 8) / 2,
            oh ? 0xFFFFFFFF : Theme.PRIMARY_FOREGROUND);
        return true;
    }

    private void drawTrack(GuiGraphicsExtractor g, Font font, MusicPlayer player, int x, int y, int w, int h, int pad, int accent, int mx, int my, float delta) {
        var info = player.getCurrentTrack().getInfo();
        String title = sanitize(info.title);
        String author = sanitize(info.author);
        if (title.isEmpty()) title = "Unknown";
        if (author.isEmpty()) author = "Unknown";
        String label = "♫  " + title + " — " + author;

        int tx = x + pad;
        int avail = w - pad * 2;
        titleX = tx; titleY = y + pad - 2; titleW = avail; titleH = font.lineHeight + 4;
        boolean th = mx >= titleX && mx <= titleX + titleW && my >= titleY && my <= titleY + titleH;
        if (th) g.fill(titleX, titleY, titleX + titleW, titleY + titleH, ColorUtil.withAlpha(accent, 40));
        String disp = font.plainSubstrByWidth(label, Math.max(0, avail - 10));
        g.text(font, Component.literal(disp), titleX, titleY + 2, th ? accent : Theme.TEXT);
        g.text(font, Component.literal("»"), x + w - pad - 6, titleY + 2, th ? accent : ColorUtil.withAlpha(Theme.MUTED_FOREGROUND, 160));

        int gap = 8;
        int totalBtns = BTN_W * 3 + gap * 2;
        int startX = x + (w - totalBtns) / 2;
        int btnY = y + h - 52;
        prevX = startX; prevY = btnY;
        playX = startX + BTN_W + gap; playY = btnY;
        nextX = playX + BTN_W + gap; nextY = btnY;

        drawBtn(g, font, accent, "prev", prevX, prevY, "⏮", false, mx, my, delta);
        drawBtn(g, font, accent, "play", playX, playY, player.isPaused() ? "▶" : "⏸", !player.isPaused(), mx, my, delta);
        drawBtn(g, font, accent, "next", nextX, nextY, "⏭", false, mx, my, delta);

        long pos = player.getPosition();
        long dur = player.getDuration();
        progressAnim.set(dur > 0 ? (float) pos / (float) dur : 0f);
        progressAnim.tick(delta);
        float prog = Mth.clamp(progressAnim.get(), 0f, 1f);

        barX = tx;
        barW = avail;
        barY = y + h - 28;
        boolean hp = mx >= barX - 2 && mx <= barX + barW + 2 && my >= barY - 4 && my <= barY + BAR_H + 4;
        g.fill(barX, barY, barX + barW, barY + BAR_H, Theme.MUTED);
        int fillW = Math.max(BAR_H, (int) (barW * prog));
        g.fill(barX, barY, barX + fillW, barY + BAR_H, accent);
        int kx = barX + (int) (barW * prog) - BAR_H / 2;
        g.fill(kx, barY - 1, kx + BAR_H, barY + BAR_H + 1,
            hp ? Theme.FOREGROUND : ColorUtil.withAlpha(Theme.FOREGROUND, 200));

        g.text(font, Component.literal(formatTime(pos)), barX, barY + BAR_H + 3, Theme.TEXT_FAINT);
        String done = formatTime(dur);
        g.text(font, Component.literal(done), barX + barW - font.width(done), barY + BAR_H + 3, Theme.TEXT_FAINT);

        if (dragging) seekFrom(mx);
    }

    private void drawBtn(GuiGraphicsExtractor g, Font font, int accent, String key, int x, int y, String label, boolean active, int mx, int my, float delta) {
        boolean hover = mx >= x && mx <= x + BTN_W && my >= y && my <= y + BTN_H;
        float hv = hoverValue(key, hover, delta);
        int base = active ? ColorUtil.withAlpha(Theme.PRIMARY, 190) : ColorUtil.withAlpha(Theme.SURFACE_VARIANT, 200);
        int hov = ColorUtil.withAlpha(Theme.BG_HOVER, 230);
        Panel.draw(g, x, y, BTN_W, BTN_H, ColorUtil.lerpARGB(base, hov, hv));
        int border = active ? accent : ColorUtil.lerpARGB(Theme.BORDER_LIGHT, accent, hv);
        Panel.drawHollowRect(g, x, y, BTN_W, BTN_H, active ? ColorUtil.withAlpha(border, 190) : ColorUtil.withAlpha(border, hover ? 200 : 90));
        int fg = active ? Theme.PRIMARY_FOREGROUND : (hover ? Theme.ON_SURFACE : Theme.ON_SURFACE_VARIANT);
        g.centeredText(font, Component.literal(label), x + BTN_W / 2, y + (BTN_H - 8) / 2, fg);
    }

    public boolean mouseClicked(double mx, double my) {
        MusicPlayer player = MusicModule.getPlayer();
        if (player.hasTrack()) {
            if (inside(mx, my, titleX, titleY, titleW, titleH)) { UiSounds.click(); openScreen(player); return true; }
            if (inside(mx, my, prevX, prevY, BTN_W, BTN_H)) { UiSounds.click(); player.previous(); return true; }
            if (inside(mx, my, playX, playY, BTN_W, BTN_H)) { UiSounds.click(); player.togglePause(); return true; }
            if (inside(mx, my, nextX, nextY, BTN_W, BTN_H)) { UiSounds.click(); player.next(); return true; }
            if (barW > 0 && mx >= barX - 2 && mx <= barX + barW + 2 && my >= barY - 4 && my <= barY + BAR_H + 4) {
                UiSounds.click();
                seekFrom(mx);
                dragging = true;
                return true;
            }
            return false;
        }
        if (inside(mx, my, openX, openY, openW, openH)) { UiSounds.click(); openScreen(player); return true; }
        return false;
    }

    private void openScreen(MusicPlayer player) {
        Minecraft.getInstance().setScreen(new MusicScreen(player, from));
    }

    private void seekFrom(double mx) {
        MusicPlayer player = MusicModule.getPlayer();
        if (barW <= 0 || !player.hasTrack()) return;
        float pct = Mth.clamp((float) ((mx - barX) / (double) barW), 0f, 1f);
        player.seek((long) (player.getDuration() * pct));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private float hoverValue(String key, boolean h, float delta) {
        Animated a = hover.computeIfAbsent(key, k -> new Animated(0f, 16f));
        Boolean prev = wasHover.get(key);
        if (h && (prev == null || !prev)) UiSounds.hover();
        wasHover.put(key, h);
        a.set(h ? 1f : 0f);
        a.tick(delta);
        return a.get();
    }

    private static boolean leftDown() {
        try {
            long handle = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(handle, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT)
                == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String formatTime(long ms) {
        long sec = ms / 1000;
        if (sec >= 3600) return String.format("%d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        var sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 32 && c != '\t' && c != '\n' && c != '\r') continue;
            sb.append(c);
        }
        return sb.toString();
    }
}
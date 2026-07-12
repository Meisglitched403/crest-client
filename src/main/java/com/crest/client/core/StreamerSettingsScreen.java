package com.crest.client.core;

import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;
import com.crest.client.ui.ToggleSwitch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class StreamerSettingsScreen extends Screen {
    private final Screen parent;
    private final StreamerModule module;
    private String urlText;
    private boolean urlFocused;
    private int cursorBlink;
    private int bitrate;
    private boolean audioEnabled;
    private int audioBitrate;
    private boolean streaming;
    private int panelW = 340;
    private int panelH = 320;
    private int panelX, panelY;

    private boolean draggingBitrate, draggingAudioBitrate;
    private static final int SLIDER_W = 200;
    private static final int SLIDER_H = 6;
    private static final int BTN_R = 6;

    public StreamerSettingsScreen(Screen parent) {
        super(Component.literal("Streamer Settings"));
        this.parent = parent;
        this.module = (StreamerModule) CrestModules.get("streamer");
        this.urlText = module.getStreamUrl();
        this.bitrate = module.getBitrate();
        this.audioEnabled = true;
        this.audioBitrate = 64;
        this.streaming = Streamer.isStreaming();
    }

    @Override
    protected void init() {
        panelW = Math.min(340, width - 40);
        panelH = 320;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme.tick(delta);
        int cx = panelX + panelW / 2;
        int left = panelX + 20;
        int right = panelX + panelW - 20;
        int cw = right - left;

        // overlay
        g.fill(0, 0, width, height, Theme.OVERLAY);
        Panel.drawGlass(g, panelX, panelY, panelW, panelH, ColorUtil.withAlpha(Theme.BG_PANEL, 240), Theme.getAnimatedAccent());

        // title
        g.centeredText(font, Component.literal("Streamer Settings"), cx, panelY + 14, Theme.TEXT);

        // back button
        String back = "< Back";
        boolean backHover = mx >= left && mx <= left + font.width(back) + 12 && my >= panelY + 6 && my <= panelY + 22;
        Panel.draw(g, left, panelY + 6, font.width(back) + 12, 16, ColorUtil.withAlpha(backHover ? Theme.BG_HOVER : 0, 200));
        g.text(font, Component.literal(back), left + 6, panelY + 8, Theme.TEXT_DIM);

        int sy = panelY + 36;

        // URL label + input
        g.text(font, Component.literal("RTMP URL"), left, sy, Theme.TEXT_DIM);
        int urlY = sy + 12;
        int urlH = 20;
        boolean urlHover = mx >= left && mx <= right && my >= urlY && my <= urlY + urlH;
        Panel.draw(g, left, urlY, cw, urlH, ColorUtil.withAlpha(urlFocused || urlHover ? Theme.BG_HOVER : Theme.BG_SURFACE, 220));
        String display = urlText.isEmpty() ? "rtmp://..." : urlText;
        int textColor = urlText.isEmpty() ? Theme.TEXT_FAINT : Theme.TEXT;
        g.text(font, Component.literal(display), left + 4, urlY + 5, textColor);
        if (urlFocused && (cursorBlink / 10) % 2 == 0) {
            int tx = left + 4 + font.width(urlText.substring(0, Math.min(urlText.length(), urlText.length())));
            g.fill(tx, urlY + 3, tx + 1, urlY + urlH - 3, Theme.getAnimatedAccent());
        }
        cursorBlink++;

        sy = urlY + urlH + 16;

        // video bitrate slider
        renderSlider(g, left, sy, cw, "Video Bitrate", bitrate, 500, 50000, "kbps", mx, my);
        sy += 40;

        // audio toggle
        g.text(font, Component.literal("Audio"), left, sy, Theme.TEXT_DIM);
        ToggleSwitch.render(g, right - ToggleSwitch.W, sy - 2, audioEnabled, audioEnabled ? 1f : 0f);
        sy += 22;

        // audio bitrate
        if (audioEnabled) {
            renderSlider(g, left, sy, cw, "Audio Bitrate", audioBitrate, 32, 320, "kbps", mx, my);
            sy += 40;
        }

        sy += 10;

        // start/stop button
        boolean streamingActive = Streamer.isStreaming();
        String streamLabel = streamingActive ? "Stop Streaming" : "Start Streaming";
        int btnW = Math.min(cw, 200);
        int btnX = cx - btnW / 2;
        int btnH = 30;
        boolean btnHover = mx >= btnX && mx <= btnX + btnW && my >= sy && my <= sy + btnH;
        int btnColor = streamingActive ? 0xFFFF4444 : Theme.getAnimatedAccent();
        Panel.draw(g, btnX, sy, btnW, btnH, ColorUtil.withAlpha(btnHover ? ColorUtil.lerpARGB(btnColor, 0xFFFFFFFF, 0.15f) : btnColor, 220));
        g.centeredText(font, Component.literal(streamLabel), cx, sy + (btnH - 8) / 2, 0xFFFFFFFF);

        sy += btnH + 8;
        g.centeredText(font, Component.literal("F8 to toggle"), cx, sy, Theme.TEXT_FAINT);
    }

    private void renderSlider(GuiGraphicsExtractor g, int x, int y, int cw, String label, int val, int min, int max, String unit, int mx, int my) {
        g.text(font, Component.literal(label), x, y, Theme.TEXT_DIM);
        String valStr = val + " " + unit;
        g.text(font, Component.literal(valStr), x + cw - font.width(valStr), y, Theme.TEXT);
        float frac = (max > min) ? (float)(val - min) / (max - min) : 0f;
        frac = Math.min(Math.max(frac, 0), 1);
        int barY = y + 14;
        g.fill(x, barY, x + cw, barY + SLIDER_H, ColorUtil.withAlpha(Theme.BG_BASE, 220));
        int fillW = (int)(frac * cw);
        g.fill(x, barY, x + fillW, barY + SLIDER_H, Theme.getAnimatedAccent());
        g.fill(x + fillW - 2, barY - 2, x + fillW + 2, barY + SLIDER_H + 2, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int btn = event.buttonInfo().input();
        int left = panelX + 20;
        int right = panelX + panelW - 20;
        int cw = right - left;

        // back
        String back = "< Back";
        if (mx >= left && mx <= left + font.width(back) + 12 && my >= panelY + 6 && my <= panelY + 22) {
            minecraft.setScreen(parent);
            return true;
        }

        // url focus
        int urlY = panelY + 48;
        int urlH = 20;
        urlFocused = mx >= left && mx <= right && my >= urlY && my <= urlY + urlH;
        cursorBlink = 0;

        int sy = urlY + urlH + 16;

        // video bitrate
        if (btn == 0 && my >= sy + 14 && my <= sy + 14 + SLIDER_H + 8) {
            draggingBitrate = true;
            updateBitrate(mx, left);
            return true;
        }
        sy += 40;

        // audio toggle
        if (btn == 0 && mx >= right - ToggleSwitch.W && mx <= right && my >= sy - 2 && my <= sy + 16) {
            audioEnabled = !audioEnabled;
            return true;
        }
        sy += 22;

        if (audioEnabled) {
            if (btn == 0 && my >= sy + 14 && my <= sy + 14 + SLIDER_H + 8) {
                draggingAudioBitrate = true;
                updateAudioBitrate(mx, left);
                return true;
            }
            sy += 40;
        }

        sy += 10;

        // start/stop button
        int btnW = Math.min(cw, 200);
        int btnX = (panelX + panelW / 2) - btnW / 2;
        int btnH = 30;
        if (btn == 0 && mx >= btnX && mx <= btnX + btnW && my >= sy && my <= sy + btnH) {
            toggleStreaming();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mx = event.x();
        int left = panelX + 20;
        if (draggingBitrate) { updateBitrate(mx, left); return true; }
        if (draggingAudioBitrate) { updateAudioBitrate(mx, left); return true; }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingBitrate = false;
        draggingAudioBitrate = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!urlFocused) return super.charTyped(event);
        int cp = event.codepoint();
        if (cp >= 32 && cp < 127) {
            urlText += event.codepointAsString();
            module.setStreamUrl(urlText);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == 256) { minecraft.setScreen(parent); return true; }
        if (urlFocused) {
            if (key == 259 && !urlText.isEmpty()) { // backspace
                urlText = urlText.substring(0, urlText.length() - 1);
                module.setStreamUrl(urlText);
                return true;
            }
            if (key == 257 || key == 335) { // enter
                urlFocused = false;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private void updateBitrate(double mx, int left) {
        int cw = panelX + panelW - 20 - left;
        float frac = (float)((mx - left) / cw);
        frac = Math.min(Math.max(frac, 0), 1);
        bitrate = Math.round(500 + frac * 49500);
        module.setBitrate(bitrate);
    }

    private void updateAudioBitrate(double mx, int left) {
        int cw = panelX + panelW - 20 - left;
        float frac = (float)((mx - left) / cw);
        frac = Math.min(Math.max(frac, 0), 1);
        audioBitrate = Math.round(32 + frac * 288);
    }

    private void toggleStreaming() {
        if (Streamer.isStreaming()) {
            Streamer.stop();
            streaming = false;
            NotificationToast.show("Stream stopped");
        } else {
            Minecraft mc = Minecraft.getInstance();
            int w = mc.getWindow().getWidth();
            int h = mc.getWindow().getHeight();
            if (w <= 0 || h <= 0) return;
            if (urlText == null || urlText.isEmpty() || urlText.equals("rtmp://live.twitch.tv/app/")) return;
            if (!Streamer.isUrlAllowed(urlText)) return;
            Streamer.start(urlText, 60, w, h, bitrate);
            streaming = true;
            NotificationToast.show("Stream started");
        }
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
    @Override
    public boolean isPauseScreen() { return false; }
}

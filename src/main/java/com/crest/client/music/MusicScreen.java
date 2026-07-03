package com.crest.client.music;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.glfwGetClipboardString;

public class MusicScreen extends Screen {
    private final MusicPlayer player;

    private StringBuilder urlText = new StringBuilder();
    private int urlCursor;
    private boolean urlFocused = true;

    private String statusText = "";
    private int statusTimer;

    private boolean draggingVolume;
    private boolean draggingProgress;

    public MusicScreen(MusicPlayer player) {
        super(Component.literal("Crest Music"));
        this.player = player;
    }

    private int margin() { return Math.max(8, width / 14); }
    private int gap() { return 4; }
    private int fieldH() { return Math.max(18, height / 30); }
    private int loadW() { return 50; }
    private int fieldY() { return height / 20 + 20; }

    private int trackLabelY() { return fieldY() + fieldH() + 14; }
    private int trackInfoY() { return trackLabelY() + 12; }
    private int barY() { return trackInfoY() + 12; }
    private int barH() { return 6; }
    private int timeY() { return barY() + barH() + 1; }

    private int controlY() { return Math.max(timeY() + 14, height * 3 / 5); }
    private int btnH() { return Math.max(18, height / 30); }
    private int btnW() { return Math.max(64, (width - margin() * 2 - gap() * 3) / 3); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int m = margin();
        int fh = fieldH();
        int gap = gap();
        int bw = btnW();
        int bh = btnH();

        g.fill(0, 0, width, height, 0xCC000000);

        int y = 6;
        g.centeredText(font, getTitle(), width / 2, y, 0xFFFFFFFF);
        g.text(font, Component.literal("X"), width - 12, y, 0xFF888888);

        y = fieldY();
        int fieldW = width - m * 2 - gap - loadW();
        int fieldR = m + fieldW;
        int lx = fieldR + gap;

        g.fill(m, y, fieldR, y + fh, 0xAA444444);
        String display = urlText.isEmpty() ? "Paste YouTube/SoundCloud URL..." : urlText.toString();
        g.text(font, Component.literal(display), m + 4, y + 4,
            urlText.isEmpty() ? 0xFF888888 : 0xFFFFFFFF);

        if (urlFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = m + 4 + font.width(urlText.substring(0, Math.min(urlCursor, urlText.length())));
            g.fill(cx, y + 3, cx + 1, y + fh - 3, 0xFFFFFFFF);
        }

        boolean hoverLoad = mx >= lx && mx <= lx + loadW() && my >= y && my <= y + fh;
        g.fill(lx, y, lx + loadW(), y + fh, hoverLoad ? 0xFF555555 : 0xFF333333);
        g.centeredText(font, Component.literal("Load"), lx + loadW() / 2, y + 4, 0xFFFFFFFF);

        y = trackLabelY();
        g.text(font, Component.literal("Now Playing"), m, y, 0xFFAAAAAA);

        y = trackInfoY();
        if (player.hasTrack()) {
            var info = player.getCurrentTrack().getInfo();
            String title = info.title != null ? info.title : "Unknown";
            String author = info.author != null ? info.author : "Unknown";
            String label = title + " - " + author;
            int maxW = width - m * 2;
            if (font.width(label) > maxW) {
                label = font.plainSubstrByWidth(label, maxW - 6) + "...";
            }
            g.text(font, Component.literal(label), m, y, 0xFFFFFFFF);

            int by = barY();
            int bh2 = barH();
            long pos = player.getPosition();
            long dur = player.getDuration();
            float progress = dur > 0 ? (float) pos / dur : 0;
            int barW = width - m * 2;
            g.fill(m, by, m + barW, by + bh2, 0xFF444444);
            int fillW = (int) (barW * progress);
            g.fill(m, by, m + fillW, by + bh2, 0xFF55FF55);

            g.text(font, Component.literal(formatTime(pos) + " / " + formatTime(dur)), m, timeY(), 0xFFAAAAAA);

            int cy = controlY();

            boolean hoverPlay = mx >= m && mx <= m + bw && my >= cy && my <= cy + bh;
            g.fill(m, cy, m + bw, cy + bh, hoverPlay ? 0xFF555555 : 0xFF333333);
            String playLabel = player.isPaused() ? "PLAY" : "PAUSE";
            g.centeredText(font, Component.literal(playLabel), m + bw / 2, cy + 4, 0xFFFFFFFF);

            int sx = m + bw + gap;
            boolean hoverStop = mx >= sx && mx <= sx + bw && my >= cy && my <= cy + bh;
            g.fill(sx, cy, sx + bw, cy + bh, hoverStop ? 0xFF555555 : 0xFF333333);
            g.centeredText(font, Component.literal("STOP"), sx + bw / 2, cy + 4, 0xFFFFFFFF);

            int vx = sx + bw + gap;
            int volLabelW = font.width("Vol") + 4;
            int volPctW = font.width("100%") + 4;
            int vsW = width - vx - m - volPctW;
            if (vsW < 40) vsW = 40;
            int sy2 = cy + (bh - 6) / 2;

            g.text(font, Component.literal("Vol"), vx, sy2 - 1, 0xFFAAAAAA);
            vx += volLabelW;
            int vlw = vsW - volLabelW;
            if (vlw < 20) vlw = 20;

            g.fill(vx, sy2, vx + vlw, sy2 + 6, 0xFF444444);
            int vf = (int) (vlw * (player.getSliderVolume() / 100f));
            g.fill(vx, sy2, vx + vf, sy2 + 6, 0xFF55AAFF);
            g.text(font, Component.literal((int) player.getSliderVolume() + "%"), vx + vlw + 4, sy2 - 1, 0xFFAAAAAA);

        } else {
            g.text(font, Component.literal("No track loaded"), m, y, 0xFF888888);
        }

        if (statusTimer > 0 && !statusText.isEmpty()) {
            g.text(font, Component.literal(statusText), m, height - 20, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!urlFocused) return false;
        int cp = event.codepoint();
        if (cp >= 32 && cp < 127) {
            String ch = event.codepointAsString();
            urlText.insert(Math.min(urlCursor, urlText.length()), ch);
            urlCursor = Math.min(urlCursor + 1, urlText.length());
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (urlFocused) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                loadUrl();
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (urlCursor > 0 && urlText.length() > 0) {
                    int idx = Math.min(urlCursor, urlText.length()) - 1;
                    urlText.deleteCharAt(idx);
                    urlCursor = Math.max(0, urlCursor - 1);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_DELETE) {
                if (urlCursor < urlText.length()) {
                    urlText.deleteCharAt(urlCursor);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_LEFT) {
                urlCursor = Math.max(0, urlCursor - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_RIGHT) {
                urlCursor = Math.min(urlText.length(), urlCursor + 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_HOME) {
                urlCursor = 0;
                return true;
            }
            if (key == GLFW.GLFW_KEY_END) {
                urlCursor = urlText.length();
                return true;
            }
            if (key == GLFW.GLFW_KEY_V && (event.modifiers() & 2) != 0) {
                long window = minecraft.getWindow().handle();
                String clip = glfwGetClipboardString(window);
                if (clip != null) {
                    urlText.insert(Math.min(urlCursor, urlText.length()), clip);
                    urlCursor = Math.min(urlCursor + clip.length(), urlText.length());
                }
                return true;
            }
        }

        if (key == GLFW.GLFW_KEY_SPACE) {
            if (player.hasTrack()) {
                player.togglePause();
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int btn = event.buttonInfo().input();
        double mx = event.x();
        double my = event.y();

        if (btn == 0) {
            int m = margin();
            int fh = fieldH();
            int gap = gap();
            int bw = btnW();
            int bh = btnH();

            int fy = fieldY();
            int fw = width - m * 2 - gap - loadW();
            int fr = m + fw;
            int lx = fr + gap;

            if (mx >= m && mx <= fr && my >= fy && my <= fy + fh) {
                urlFocused = true;
                int clickX = (int) mx - m - 4;
                urlCursor = font.plainSubstrByWidth(urlText.toString(), clickX).length();
                return true;
            }

            if (mx >= lx && mx <= lx + loadW() && my >= fy && my <= fy + fh) {
                loadUrl();
                return true;
            }

            urlFocused = false;

            if (player.hasTrack()) {
                int cy = controlY();
                int by = barY();

                if (mx >= m && mx <= m + bw && my >= cy && my <= cy + bh) {
                    player.togglePause();
                    return true;
                }

                int sx = m + bw + gap;
                if (mx >= sx && mx <= sx + bw && my >= cy && my <= cy + bh) {
                    player.stop();
                    setStatus("Stopped");
                    return true;
                }

                int barW = width - m * 2;
                int bh2 = barH();
                if (mx >= m && mx <= m + barW && my >= by && my <= by + bh2) {
                    float pct = (float) (mx - m) / barW;
                    long seekPos = (long) (player.getDuration() * pct);
                    player.seek(seekPos);
                    draggingProgress = true;
                    return true;
                }

                int volLabelW = font.width("Vol") + 4;
                int volPctW = font.width("100%") + 4;
                int vx = sx + bw + gap + volLabelW;
                int vsW = width - vx - m - volPctW;
                if (vsW < 40) vsW = 40;
                int vlw = vsW - volLabelW;
                if (vlw < 20) vlw = 20;
                int sy2 = cy + (bh - 6) / 2;

                if (mx >= vx && mx <= vx + vlw && my >= sy2 && my <= sy2 + 6) {
                    float pct = (float) (mx - vx) / vlw;
                    player.setSliderVolume(pct * 100);
                    draggingVolume = true;
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mx = event.x();
        double my = event.y();

        if (draggingVolume && player.hasTrack()) {
            int m = margin();
            int gap = gap();
            int bw = btnW();
            int volLabelW = font.width("Vol") + 4;
            int volPctW = font.width("100%") + 4;
            int sx = m + bw + gap;
            int vx = sx + bw + gap + volLabelW;
            int vsW = width - vx - m - volPctW;
            if (vsW < 40) vsW = 40;
            int vlw = vsW - volLabelW;
            if (vlw < 20) vlw = 20;
            float pct = (float) ((mx - vx) / vlw);
            player.setSliderVolume(Mth.clamp(pct * 100, 0, 100));
            return true;
        }

        if (draggingProgress && player.hasTrack()) {
            int m = margin();
            int barW = width - m * 2;
            float pct = (float) ((mx - m) / barW);
            long seekPos = (long) (player.getDuration() * Mth.clamp(pct, 0, 1));
            player.seek(seekPos);
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingVolume = false;
        draggingProgress = false;
        return super.mouseReleased(event);
    }

    @Override
    public void tick() {
        super.tick();
        if (statusTimer > 0) statusTimer--;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void loadUrl() {
        String url = urlText.toString().trim();
        if (url.isEmpty()) return;
        setStatus("Loading...");
        player.loadAndPlay(url);
    }

    public void setStatus(String msg) {
        statusText = msg;
        statusTimer = 100;
    }

    private static String formatTime(long ms) {
        long sec = ms / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }
}

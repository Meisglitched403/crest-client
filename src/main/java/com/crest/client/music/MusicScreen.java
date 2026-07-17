package com.crest.client.music;

import com.crest.client.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwGetClipboardString;

public class MusicScreen extends Screen {
    private static final int BTN_H = 22;
    private static final int BTN_GAP = 4;
    private static final int BAR_H = 4;

    private final MusicPlayer player;
    private StringBuilder urlText = new StringBuilder();
    private int urlCursor;
    private boolean urlFocused = true;

    private List<AudioTrack> searchResults;
    private boolean searchPending;
    private int resultScroll;
    private int selectedResult = -1;

    private String statusText = "";
    private int statusTimer;
    private boolean statusIsError;

    private boolean draggingVolume;
    private boolean draggingProgress;

    private final Animated openAnim = new Animated(0f, 10f);

    public MusicScreen(MusicPlayer player) {
        super(Component.literal("Crest Music"));
        this.player = player;
        openAnim.setImmediate(0f);
        openAnim.set(1f);
    }

    private int margin() { return Math.max(Spacing.S3, width / 20); }
    private int availW() { return width - margin() * 2; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme.tick(delta);
        openAnim.tick(delta);
        float open = openAnim.get();
        if (open < 0.01) return;

        g.fill(0, 0, width, height, ColorUtil.withAlpha(0xCC000000, (int) (220 * open)));

        int wy = (int) ((1 - open) * -20);
        g.pose().pushMatrix();
        g.pose().translate(0, wy);

        int m = margin();
        int aw = availW();
        int accent = Theme.getAnimatedAccent();

        // Main panel
        Panel.drawElevated(g, m, Spacing.S2, aw, height - Spacing.S4, ColorUtil.withAlpha(Theme.BG_PANEL, 220), Theme.ELEVATION_2);
        g.fill(m + 2, Spacing.S2, m + aw - 2, Spacing.S2 + 2, ColorUtil.withAlpha(accent, 120));

        // Title + close
        g.centeredText(font, getTitle(), width / 2, Spacing.S2 + 8, Theme.FOREGROUND);
        boolean closeHover = mx >= width - 18 && mx <= width - 6 && my >= Spacing.S2 + 4 && my <= Spacing.S2 + 16;
        g.text(font, Component.literal("X"), width - 14, Spacing.S2 + 4, closeHover ? Theme.DESTRUCTIVE : Theme.MUTED_FOREGROUND);

        int y = Spacing.S6 + Spacing.S1;

        // Search bar
        y = renderSearchBar(g, mx, my, m, aw, y);

        // Results area
        y = renderResults(g, mx, my, m, aw, y, accent);

        // Now playing + controls
        renderNowPlaying(g, mx, my, m, aw, y, accent);

        // Status bar
        if (statusTimer > 0 && !statusText.isEmpty() && !searchPending) {
            int sc = statusIsError ? Theme.DESTRUCTIVE : Theme.MUTED_FOREGROUND;
            g.text(font, Component.literal(statusText), m + Spacing.S1, height - Spacing.S4 - font.lineHeight, sc);
        }

        g.pose().popMatrix();
    }

    private int renderSearchBar(GuiGraphicsExtractor g, int mx, int my, int m, int aw, int y) {
        int fh = 18;
        int loadW = 36;
        int fieldW = aw - BTN_GAP - loadW - Spacing.S2 * 2;
        int fr = m + Spacing.S1 + fieldW;
        int lx = fr + BTN_GAP;

        Panel.draw(g, m + Spacing.S1, y, fieldW, fh,
            ColorUtil.withAlpha(urlFocused ? Theme.PRIMARY_CONTAINER : Theme.BG_SURFACE, 200));
        String placeholder = "Search or paste URL...";
        String display = urlText.isEmpty() ? placeholder : urlText.toString();
        int textColor = urlText.isEmpty() ? Theme.TEXT_FAINT : Theme.TEXT;
        g.text(font, Component.literal(display), m + Spacing.S1 + 4, y + 4, textColor);

        if (urlFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = m + Spacing.S1 + 4 + font.width(urlText.substring(0, Math.min(urlCursor, urlText.length())));
            g.fill(cx, y + 3, cx + 1, y + fh - 3, Theme.PRIMARY);
        }

        boolean hoverLoad = mx >= lx && mx <= lx + loadW && my >= y && my <= y + fh;
        Panel.draw(g, lx, y, loadW, fh, ColorUtil.withAlpha(hoverLoad ? Theme.BG_HOVER : Theme.SURFACE_VARIANT, 220));
        g.centeredText(font, Component.literal("Go"), lx + loadW / 2, y + 3,
            hoverLoad ? Theme.ON_SURFACE : Theme.ON_SURFACE_VARIANT);

        return y + fh + Spacing.S2;
    }

    private int renderResults(GuiGraphicsExtractor g, int mx, int my, int m, int aw, int y, int accent) {
        if (searchPending) {
            String dots = ".".repeat((int) ((System.currentTimeMillis() / 400) % 4));
            g.text(font, Component.literal("Searching" + dots), m + Spacing.S1, y, accent);
            return y + Spacing.S3;
        }

        if (searchResults == null || searchResults.isEmpty()) return y;

        int rowH = 18;
        int maxRows = Math.min(searchResults.size(), Math.max(3, (height - y - 140) / rowH));
        int rh = maxRows * rowH + Spacing.S2 + 14;

        Panel.draw(g, m + Spacing.S1, y, aw - Spacing.S2, rh,
            ColorUtil.withAlpha(Theme.SURFACE, 150));

        String header = "Results (" + searchResults.size() + ")";
        g.text(font, Component.literal(header), m + Spacing.S2, y + Spacing.S1, accent);
        g.fill(m + Spacing.S1, y + Spacing.S2 + 12, m + aw - Spacing.S2, y + Spacing.S2 + 13,
            ColorUtil.withAlpha(Theme.BORDER_LIGHT, 60));

        int ry = y + 18;
        int visible = Math.min(maxRows, searchResults.size() - resultScroll);
        int maxTextW = aw - 68;

        for (int i = 0; i < visible; i++) {
            int idx = i + resultScroll;
            if (idx >= searchResults.size()) break;
            AudioTrack track = searchResults.get(idx);
            boolean ho = my >= ry && my <= ry + rowH && mx >= m + Spacing.S1 && mx <= m + aw - Spacing.S2;
            boolean se = idx == selectedResult;

            if (ho || se) {
                g.fill(m + Spacing.S2, ry, m + aw - Spacing.S2, ry + rowH,
                    ColorUtil.withAlpha(ho ? Theme.BG_HOVER : Theme.BG_SELECT, 100));
            }

            g.text(font, Component.literal("\u25B6"), m + Spacing.S2 + 4, ry + 3, ho ? accent : Theme.MUTED_FOREGROUND);

            String text = sanitizeMeta(track.getInfo().title);
            if (track.getInfo().author != null && !track.getInfo().author.equals("Unknown")) {
                text += " - " + sanitizeMeta(track.getInfo().author);
            }
            if (font.width(text) > maxTextW) {
                text = font.plainSubstrByWidth(text, maxTextW - 4) + "...";
            }
            g.text(font, Component.literal(text), m + Spacing.S2 + 18, ry + 3, ho ? Theme.TEXT : Theme.TEXT_DIM);

            String dur = formatTime(track.getDuration());
            g.text(font, Component.literal(dur), m + aw - Spacing.S2 - font.width(dur) - 4, ry + 3, Theme.TEXT_FAINT);
            ry += rowH;
        }

        if (searchResults.size() > maxRows) {
            String info = (resultScroll + 1) + "-" + Math.min(resultScroll + maxRows, searchResults.size())
                + "/" + searchResults.size();
            g.text(font, Component.literal(info), m + aw - Spacing.S2 - font.width(info), y + rh - 12, Theme.TEXT_FAINT);
        }

        return y + rh + Spacing.S2;
    }

    private void renderNowPlaying(GuiGraphicsExtractor g, int mx, int my, int m, int aw, int y, int accent) {
        Panel.draw(g, m + Spacing.S1, y, aw - Spacing.S2, Math.max(100, height - y - Spacing.S6),
            ColorUtil.withAlpha(Theme.SURFACE, 100));

        g.text(font, Component.literal("NOW PLAYING"), m + Spacing.S2, y + Spacing.S1, Theme.MUTED_FOREGROUND);

        int ny = y + Spacing.S3 + font.lineHeight;
        if (player.hasTrack()) {
            var info = player.getCurrentTrack().getInfo();
            String title = info.title != null ? sanitizeMeta(info.title) : "Unknown";
            String author = info.author != null ? sanitizeMeta(info.author) : "Unknown";
            String label = title + " \u2014 " + author;
            if (font.width(label) > aw - Spacing.S4) {
                label = font.plainSubstrByWidth(label, aw - Spacing.S4 - 6) + "...";
            }
            g.text(font, Component.literal("♫ " + label), m + Spacing.S2, ny, Theme.TEXT);

            int by = ny + Spacing.S2 + 2;
            long pos = player.getPosition();
            long dur = player.getDuration();
            float progress = dur > 0 ? (float) pos / dur : 0;

            boolean hp = mx >= m + Spacing.S2 && mx <= m + aw - Spacing.S2 && my >= by && my <= by + BAR_H;
            g.fill(m + Spacing.S2, by, m + aw - Spacing.S2, by + BAR_H, 0xFF333355);
            int fillW = (int) ((aw - Spacing.S4) * progress);
            if (fillW > 0) {
                g.fill(m + Spacing.S2, by, m + Spacing.S2 + fillW, by + BAR_H, hp ? accent : 0xFF55FF55);
            }
            g.text(font, Component.literal(formatTime(pos) + " / " + formatTime(dur)), m + Spacing.S2, by + BAR_H + 2, Theme.TEXT_FAINT);

            int cy = by + BAR_H + Spacing.S3;
            nowPlayingBtnY = cy;
            int btnW = Math.min(90, (aw - Spacing.S8 - BTN_GAP) / 2);

            renderButton(g, mx, my, m + Spacing.S2, cy, btnW, player.isPaused() ? "\u25B6 PLAY" : "\u23F8 PAUSE");
            renderButton(g, mx, my, m + Spacing.S2 + btnW + BTN_GAP, cy, btnW, "\u25A0 STOP");

            int volY = cy + BTN_H + BTN_GAP;
            renderVolumeSlider(g, mx, my, m + Spacing.S2, volY, aw - Spacing.S4, accent);
        } else {
            g.text(font, Component.literal("No track loaded"), m + Spacing.S2, ny, Theme.TEXT_DIM);
        }
    }

    private void renderButton(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, String label) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + BTN_H;
        Panel.draw(g, x, y, w, BTN_H, ColorUtil.withAlpha(hover ? Theme.BG_HOVER : Theme.SURFACE_VARIANT, 220));
        g.centeredText(font, Component.literal(label), x + w / 2, y + 4, hover ? Theme.ON_SURFACE : Theme.ON_SURFACE_VARIANT);
    }

    private int volumeSliderX;
    private int volumeSliderW;
    private int nowPlayingBtnY;

    private void renderVolumeSlider(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, int accent) {
        int labelW = font.width("Volume") + 6;
        int pctW = font.width(" 100%") + 4;
        int vsW = w - labelW - pctW;
        if (vsW < 40) vsW = 40;
        int sy = y + (BTN_H - BAR_H) / 2;

        g.text(font, Component.literal("Volume"), x, y + 4, Theme.MUTED_FOREGROUND);

        int barX = x + labelW + 2;
        volumeSliderX = barX;
        volumeSliderW = vsW;

        float vol = player.getSliderVolume();
        int vf = (int) (vsW * (vol / 100f));
        boolean hoverVol = mx >= barX && mx <= barX + vsW && my >= sy && my <= sy + BAR_H;

        g.fill(barX, sy, barX + vsW, sy + BAR_H, 0xFF333355);
        if (vf > 0) {
            g.fill(barX, sy, barX + vf, sy + BAR_H, hoverVol ? 0xFF77CCFF : accent);
        }

        String pct = (int) vol + "%";
        g.text(font, Component.literal(pct), barX + vsW + 4, y + 4, hoverVol ? Theme.TEXT : Theme.TEXT_DIM);
    }

    // --- Input ---

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!urlFocused) return false;
        int cp = event.codepoint();
        if (cp >= 32 && cp < 127) {
            urlText.insert(Math.min(urlCursor, urlText.length()), String.valueOf((char) cp));
            urlCursor = Math.min(urlCursor + 1, urlText.length());
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }

        if (urlFocused) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { goAction(); return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (urlCursor > 0 && urlText.length() > 0) { urlText.deleteCharAt(urlCursor - 1); urlCursor--; }
                return true;
            }
            if (key == GLFW.GLFW_KEY_DELETE) { if (urlCursor < urlText.length()) { urlText.deleteCharAt(urlCursor); } return true; }
            if (key == GLFW.GLFW_KEY_LEFT && urlCursor > 0) { urlCursor--; return true; }
            if (key == GLFW.GLFW_KEY_RIGHT && urlCursor < urlText.length()) { urlCursor++; return true; }
            if (key == GLFW.GLFW_KEY_HOME) { urlCursor = 0; return true; }
            if (key == GLFW.GLFW_KEY_END) { urlCursor = urlText.length(); return true; }
            if (key == GLFW.GLFW_KEY_V && (event.modifiers() & 2) != 0) {
                String clip = glfwGetClipboardString(minecraft.getWindow().handle());
                if (clip != null) {
                    clip = sanitizeInput(clip);
                    if (!clip.isEmpty()) { urlText.insert(urlCursor, clip); urlCursor += clip.length(); }
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_DOWN && hasResults() && !searchResults.isEmpty()) {
                urlFocused = false; selectedResult = 0; return true;
            }
            return true;
        }

        if (hasResults()) {
            if (key == GLFW.GLFW_KEY_UP) {
                if (selectedResult <= 0) { urlFocused = true; selectedResult = -1; return true; }
                selectedResult--; if (selectedResult < resultScroll) resultScroll = selectedResult; return true;
            }
            if (key == GLFW.GLFW_KEY_DOWN) {
                if (selectedResult < 0) selectedResult = 0;
                else if (selectedResult < searchResults.size() - 1) selectedResult++;
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (selectedResult >= 0 && selectedResult < searchResults.size()) { playResult(selectedResult); return true; }
            }
        }

        if (key == GLFW.GLFW_KEY_SPACE) {
            urlFocused = false; selectedResult = -1;
            if (player.hasTrack()) { player.togglePause(); return true; }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int btn = event.buttonInfo().input();
        if (btn != 0) return super.mouseClicked(event, doubleClick);

        double mx = event.x();
        double my = event.y();
        int m = margin();
        int aw = availW();
        int fh = 18;
        int loadW = 36;

        // Close button
        if (mx >= width - 18 && mx <= width - 6 && my >= Spacing.S2 + 4 && my <= Spacing.S2 + 16) {
            onClose();
            return true;
        }

        // Search field
        int sy = Spacing.S6 + Spacing.S1;
        int fieldW = aw - BTN_GAP - loadW - Spacing.S2 * 2;
        int fr = m + Spacing.S1 + fieldW;
        int lx = fr + BTN_GAP;

        if (mx >= m + Spacing.S1 && mx <= fr && my >= sy && my <= sy + fh) {
            urlFocused = true; selectedResult = -1;
            urlCursor = font.plainSubstrByWidth(urlText.toString(), Math.max(0, (int) mx - m - Spacing.S1 - 4)).length();
            return true;
        }
        if (mx >= lx && mx <= lx + loadW && my >= sy && my <= sy + fh) { goAction(); return true; }
        urlFocused = false;

        // Results
        if (hasResults()) {
            int rowH = 18;
            int maxRows = Math.min(searchResults.size(), Math.max(3, (height - sy - fh - 140) / rowH));
            int ry = sy + fh + Spacing.S2 + 18;
            for (int i = 0; i < maxRows && i + resultScroll < searchResults.size(); i++) {
                int idx = i + resultScroll;
                if (mx >= m + Spacing.S1 && mx <= m + aw - Spacing.S2 && my >= ry && my <= ry + rowH) {
                    playResult(idx); return true;
                }
                ry += rowH;
            }
        }

        if (player.hasTrack()) {
            int npY = nowPlayingBtnY;
            if (npY == 0) return super.mouseClicked(event, doubleClick);
            int btnW = Math.min(90, (aw - Spacing.S8 - BTN_GAP) / 2);

            if (mx >= m + Spacing.S2 && mx <= m + Spacing.S2 + btnW && my >= npY && my <= npY + BTN_H) {
                player.togglePause(); return true;
            }
            if (mx >= m + Spacing.S2 + btnW + BTN_GAP && mx <= m + Spacing.S2 + btnW * 2 + BTN_GAP && my >= npY && my <= npY + BTN_H) {
                player.stop(); setStatus("Stopped"); return true;
            }

            int progressBarY = npY - Spacing.S3 - 2;
            if (mx >= m + Spacing.S2 && mx <= m + aw - Spacing.S2 && my >= progressBarY && my <= progressBarY + BAR_H) {
                float pct = (float) Math.max(0, Math.min(1, (mx - m - Spacing.S2) / (aw - Spacing.S4)));
                player.seek((long) (player.getDuration() * pct));
                draggingProgress = true;
                return true;
            }

            int volY = npY + BTN_H + BTN_GAP;
            int labelW = font.width("Volume") + 6;
            int barX = m + Spacing.S2 + labelW + 2;
            int vsW = aw - Spacing.S4 - labelW - font.width(" 100%") - 4;
            if (vsW < 40) vsW = 40;
            int volBarY = volY + (BTN_H - BAR_H) / 2;
            if (mx >= barX && mx <= barX + vsW && my >= volBarY && my <= volBarY + BAR_H) {
                float pct = (float) ((mx - barX) / Math.max(1, vsW));
                player.setSliderVolume(Mth.clamp(pct * 100, 0, 100));
                draggingVolume = true;
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mx = event.x();
        if (draggingVolume && player.hasTrack() && volumeSliderW > 0) {
            float pct = (float) ((mx - volumeSliderX) / volumeSliderW);
            player.setSliderVolume(Mth.clamp(pct * 100, 0, 100));
            return true;
        }
        if (draggingProgress && player.hasTrack()) {
            int m = margin();
            int aw = availW();
            float pct = (float) ((mx - m - Spacing.S2) / (aw - Spacing.S4));
            player.seek((long) (player.getDuration() * Mth.clamp(pct, 0, 1)));
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
    public void onClose() { minecraft.setScreen(null); }
    @Override
    public boolean isPauseScreen() { return false; }

    private boolean hasResults() { return searchResults != null && !searchResults.isEmpty(); }

    private void goAction() {
        String input = urlText.toString().trim();
        if (input.isEmpty()) return;

        searchResults = null; resultScroll = 0; selectedResult = -1;

        if (input.contains("://")) {
            setStatus("Loading...");
            player.loadAndPlay(input);
        } else {
            searchPending = true;
            setStatus("Searching...");
            player.search(input, new MusicPlayer.SearchCallback() {
                @Override
                public void onResults(List<AudioTrack> tracks) {
                    Minecraft.getInstance().execute(() -> {
                        searchResults = tracks;
                        searchPending = false;
                        setStatus("Found " + tracks.size() + " result" + (tracks.size() != 1 ? "s" : ""));
                    });
                }
                @Override
                public void onError(String msg) {
                    Minecraft.getInstance().execute(() -> {
                        searchPending = false;
                        statusIsError = true;
                        setStatus(msg);
                    });
                }
            });
        }
    }

    private void playResult(int index) {
        if (index < 0 || index >= searchResults.size()) return;
        AudioTrack track = searchResults.get(index);
        selectedResult = index;
        setStatus("\u25B6  Loading: " + track.getInfo().title);
        player.loadAndPlay(track.getInfo().uri);
    }

    public void setStatus(String msg) { statusText = msg; statusTimer = 80; }

    private static String formatTime(long ms) {
        long sec = ms / 1000;
        if (sec >= 3600) return String.format("%d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    private static String sanitizeInput(String s) {
        if (s == null) return "";
        var sb = new StringBuilder(Math.min(s.length(), 4096));
        for (int i = 0; i < s.length() && sb.length() < 4096; i++) {
            char c = s.charAt(i);
            if (c == '\t' || c == ' ') sb.append(c);
            else if (c >= 32 && c < 127) sb.append(c);
        }
        return sb.toString().trim();
    }

    private static String sanitizeMeta(String s) {
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

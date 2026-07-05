package com.crest.client.music;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwGetClipboardString;

public class MusicScreen extends Screen {
    private static final int TEXT_DIM = 0xFF888888;
    private static final int TEXT_ACCENT = 0xFF55AAFF;

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

    private int fieldBottom() { return fieldY() + fieldH(); }

    private boolean hasResults() { return searchResults != null && !searchResults.isEmpty(); }

    private int resultsAreaY() { return fieldBottom() + 6; }

    private int resultsAreaH() {
        if (!hasResults()) return 0;
        int headerH = 12;
        int rowH = 18;
        int pad = 4;
        int totalH = headerH + searchResults.size() * rowH + pad;
        return Math.min(totalH, height / 3);
    }

    private int resultsBottom() { return resultsAreaY() + resultsAreaH(); }

    private int resultsVisibleRows() {
        if (!hasResults()) return 0;
        int availH = resultsAreaH() - 12;
        return Math.min(availH / 18, searchResults.size() - resultScroll);
    }

    // Everything below results shifts down by results area height
    private int trackLabelY() { return hasResults() ? resultsBottom() + 8 : fieldBottom() + 14; }
    private int trackInfoY() { return trackLabelY() + 12; }
    private int barY() { return trackInfoY() + 12; }
    private int barH() { return 6; }
    private int timeY() { return barY() + barH() + 1; }

    private int controlY() {
        int base = Math.max(timeY() + 14, height * 3 / 5);
        if (hasResults()) {
            base = Math.max(base, resultsBottom() + 8);
        }
        return base;
    }

    private int btnH() { return Math.max(18, height / 30); }
    private int btnW() { return Math.max(64, (width - margin() * 2 - gap() * 3) / 3); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int m = margin();
        int fh = fieldH();
        int gap = gap();
        int bw = btnW();
        int bh = btnH();
        int availW = width - m * 2;

        g.fill(0, 0, width, height, 0xCC000000);

        int y = 6;
        g.centeredText(font, getTitle(), width / 2, y, 0xFFFFFFFF);
        g.text(font, Component.literal("X"), width - 12, y, TEXT_DIM);

        // --- URL / Search input ---
        y = fieldY();
        int fieldW = availW - gap - loadW();
        int fieldR = m + fieldW;
        int lx = fieldR + gap;

        g.fill(m, y, fieldR, y + fh, urlFocused ? 0xAA555555 : 0xAA444444);
        String display = urlText.isEmpty() ? "Search or paste SoundCloud URL..." : urlText.toString();
        g.text(font, Component.literal(display), m + 4, y + 4,
            urlText.isEmpty() ? TEXT_DIM : 0xFFFFFFFF);

        if (urlFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = m + 4 + font.width(urlText.substring(0, Math.min(urlCursor, urlText.length())));
            g.fill(cx, y + 3, cx + 1, y + fh - 3, 0xFFFFFFFF);
        }

        boolean hoverGo = mx >= lx && mx <= lx + loadW() && my >= y && my <= y + fh;
        g.fill(lx, y, lx + loadW(), y + fh, hoverGo ? 0xFF555555 : 0xFF333333);
        g.centeredText(font, Component.literal("Go"), lx + loadW() / 2, y + 4, 0xFFFFFFFF);

        // --- Search results ---
        if (hasResults()) {
            int ry = resultsAreaY();
            int rh = resultsAreaH();

            String header = "SoundCloud Results (" + searchResults.size() + ")";
            g.text(font, Component.literal(header), m, ry, TEXT_ACCENT);

            int rowY = ry + 14;
            int visible = resultsVisibleRows();
            int maxTextW = availW - 64;

            for (int i = 0; i < visible; i++) {
                int idx = i + resultScroll;
                if (idx >= searchResults.size()) break;
                AudioTrack track = searchResults.get(idx);

                boolean hovered = my >= rowY && my <= rowY + 18 && mx >= m && mx <= m + availW;
                if (hovered) {
                    g.fill(m, rowY, m + availW, rowY + 18, 0x44333366);
                }
                if (idx == selectedResult && !hovered) {
                    g.fill(m, rowY, m + availW, rowY + 18, 0x22224444);
                }

                String text = track.getInfo().title;
                if (track.getInfo().author != null && !track.getInfo().author.equals("Unknown")) {
                    text += " - " + track.getInfo().author;
                }
                if (font.width(text) > maxTextW) {
                    text = font.plainSubstrByWidth(text, maxTextW - 4) + "...";
                }

                int txtColor = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;
                g.text(font, Component.literal(text), m + 4, rowY + 3, txtColor);

                String dur = formatTime(track.getDuration());
                g.text(font, Component.literal(dur), m + availW - font.width(dur) - 4, rowY + 3, TEXT_DIM);

                rowY += 18;
            }

            int totalInArea = (rh - 14) / 18;
            if (searchResults.size() > totalInArea) {
                String scrollInfo = (resultScroll + 1) + "-" + Math.min(resultScroll + totalInArea, searchResults.size()) + "/" + searchResults.size();
                g.text(font, Component.literal(scrollInfo), width - m - font.width(scrollInfo), rowY + 2, TEXT_DIM);
            }
        }

        if (searchPending) {
            g.text(font, Component.literal("Searching..."), m, height - 20, TEXT_ACCENT);
        }

        // --- Now Playing ---
        y = trackLabelY();
        g.text(font, Component.literal("Now Playing"), m, y, TEXT_DIM);

        y = trackInfoY();
        if (player.hasTrack()) {
            var info = player.getCurrentTrack().getInfo();
            String title = info.title != null ? info.title : "Unknown";
            String author = info.author != null ? info.author : "Unknown";
            String label = title + " - " + author;
            int maxW = availW;
            if (font.width(label) > maxW) {
                label = font.plainSubstrByWidth(label, maxW - 6) + "...";
            }
            g.text(font, Component.literal(label), m, y, 0xFFFFFFFF);

            int by = barY();
            int bh2 = barH();
            long pos = player.getPosition();
            long dur = player.getDuration();
            float progress = dur > 0 ? (float) pos / dur : 0;
            int barW = availW;
            g.fill(m, by, m + barW, by + bh2, 0xFF444444);
            int fillW = (int) (barW * progress);
            g.fill(m, by, m + fillW, by + bh2, 0xFF55FF55);

            g.text(font, Component.literal(formatTime(pos) + " / " + formatTime(dur)), m, timeY(), TEXT_DIM);

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
            int volPctW = font.width(" 100%") + 4;
            int vsW = availW - (vx - m) - volPctW;
            if (vsW < 40) vsW = 40;
            int sy2 = cy + (bh - 6) / 2;

            g.text(font, Component.literal("Vol"), vx, sy2 - 1, TEXT_DIM);
            vx += volLabelW;
            int vlw = vsW - volLabelW;
            if (vlw < 20) vlw = 20;

            g.fill(vx, sy2, vx + vlw, sy2 + 6, 0xFF444444);
            int vf = (int) (vlw * (player.getSliderVolume() / 100f));
            g.fill(vx, sy2, vx + vf, sy2 + 6, TEXT_ACCENT);
            g.text(font, Component.literal((int) player.getSliderVolume() + "%"), vx + vlw + 4, sy2 - 1, TEXT_DIM);

        } else {
            g.text(font, Component.literal("No track loaded"), m, y, TEXT_DIM);
        }

        if (statusTimer > 0 && !statusText.isEmpty() && !searchPending) {
            g.text(font, Component.literal(statusText), m, height - 20, TEXT_DIM);
        }
    }

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

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (urlFocused) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                goAction();
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
        } else if (hasResults()) {
            if (key == GLFW.GLFW_KEY_UP) {
                if (selectedResult <= 0) {
                    urlFocused = true;
                    return true;
                }
                selectedResult--;
                if (selectedResult < resultScroll) resultScroll = selectedResult;
                return true;
            }
            if (key == GLFW.GLFW_KEY_DOWN) {
                if (selectedResult < 0) {
                    selectedResult = 0;
                } else if (selectedResult < searchResults.size() - 1) {
                    selectedResult++;
                    int visible = resultsVisibleRows();
                    if (selectedResult >= resultScroll + visible) {
                        resultScroll = selectedResult - visible + 1;
                    }
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (selectedResult >= 0 && selectedResult < searchResults.size()) {
                    playResult(selectedResult);
                    return true;
                }
            }
        }

        if (key == GLFW.GLFW_KEY_SPACE) {
            urlFocused = false;
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
            int availW = width - m * 2;

            int fy = fieldY();
            int fw = availW - gap - loadW();
            int fr = m + fw;
            int lx = fr + gap;

            // URL input field click
            if (mx >= m && mx <= fr && my >= fy && my <= fy + fh) {
                urlFocused = true;
                int clickX = (int) mx - m - 4;
                urlCursor = font.plainSubstrByWidth(urlText.toString(), clickX).length();
                return true;
            }

            // Go button click
            if (mx >= lx && mx <= lx + loadW() && my >= fy && my <= fy + fh) {
                goAction();
                return true;
            }

            // Click somewhere else = unfocus URL
            urlFocused = false;

            // Search result click
            if (hasResults()) {
                int ry = resultsAreaY() + 14;
                int visible = resultsVisibleRows();
                for (int i = 0; i < visible; i++) {
                    int idx = i + resultScroll;
                    if (idx >= searchResults.size()) break;
                    if (mx >= m && mx <= m + availW && my >= ry && my <= ry + 18) {
                        playResult(idx);
                        return true;
                    }
                    ry += 18;
                }
            }

            // Controls
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

                int barW = availW;
                int bh2 = barH();
                if (mx >= m && mx <= m + barW && my >= by && my <= by + bh2) {
                    float pct = (float) (mx - m) / barW;
                    long seekPos = (long) (player.getDuration() * pct);
                    player.seek(seekPos);
                    draggingProgress = true;
                    return true;
                }

                int volLabelW = font.width("Vol") + 4;
                int volPctW = font.width(" 100%") + 4;
                int vx = sx + bw + gap + volLabelW;
                int vsW = availW - (vx - m) - volPctW;
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
        int m = margin();

        if (draggingVolume && player.hasTrack()) {
            int gap = gap();
            int bw = btnW();
            int volLabelW = font.width("Vol") + 4;
            int volPctW = font.width(" 100%") + 4;
            int sx = m + bw + gap;
            int vx = sx + bw + gap + volLabelW;
            int availW = width - m * 2;
            int vsW = availW - (vx - m) - volPctW;
            if (vsW < 40) vsW = 40;
            int vlw = vsW - volLabelW;
            if (vlw < 20) vlw = 20;
            float pct = (float) ((mx - vx) / vlw);
            player.setSliderVolume(Mth.clamp(pct * 100, 0, 100));
            return true;
        }

        if (draggingProgress && player.hasTrack()) {
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

    private void goAction() {
        String input = urlText.toString().trim();
        if (input.isEmpty()) return;

        if (input.contains("://")) {
            searchResults = null;
            resultScroll = 0;
            selectedResult = -1;
            setStatus("Loading...");
            player.loadAndPlay(input);
        } else {
            searchPending = true;
            searchResults = null;
            resultScroll = 0;
            selectedResult = -1;
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
                        setStatus(msg);
                    });
                }
            });
        }
    }

    private void playResult(int index) {
        if (index < 0 || index >= searchResults.size()) return;
        AudioTrack track = searchResults.get(index);
        setStatus("Loading: " + track.getInfo().title);
        player.loadAndPlay(track.getInfo().uri);
    }

    public void setStatus(String msg) {
        statusText = msg;
        statusTimer = 100;
    }

    private static String formatTime(long ms) {
        long sec = ms / 1000;
        if (sec >= 3600) {
            return String.format("%d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
        }
        return String.format("%d:%02d", sec / 60, sec % 60);
    }
}

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
    private static final int GAP = 4;
    private static final int FG = 0xFFFFFFFF;
    private static final int DIM = 0xFF888888;
    private static final int LABEL = 0xFFAAAAAA;
    private static final int ACCENT = 0xFF55AAFF;
    private static final int GREEN = 0xFF55FF55;
    private static final int BG_SURFACE = 0xCC111122;
    private static final int BG_INPUT = 0xAA333344;
    private static final int BG_INPUT_FOCUSED = 0xAA444455;
    private static final int BG_BTN = 0xFF333344;
    private static final int BG_BTN_HOVER = 0xFF555566;
    private static final int BG_RESULT = 0x44333366;
    private static final int BG_RESULT_SELECTED = 0x22224444;
    private static final int BG_PROGRESS = 0xFF444444;

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
    private boolean hoverLoad;
    private int hoverResult = -1;

    public MusicScreen(MusicPlayer player) {
        super(Component.literal("Crest Music"));
        this.player = player;
    }

    private int margin() { return Math.max(12, width / 16); }
    private int fieldH() { return Math.max(20, height / 28); }
    private int loadW() { return 50; }
    private int fieldY() { return height / 20 + 24; }
    private int fieldBottom() { return fieldY() + fieldH(); }

    private boolean hasResults() { return searchResults != null && !searchResults.isEmpty(); }

    private int resultsAreaY() { return fieldBottom() + 8; }

    private int resultsAreaH() {
        if (!hasResults()) return 0;
        int h = 14 + searchResults.size() * 20 + 6;
        return Math.min(h, Math.max(height / 4, height / 5));
    }

    private int resultsBottom() { return resultsAreaY() + resultsAreaH(); }

    private int resultsVisible() {
        if (!hasResults()) return 0;
        int avail = resultsAreaH() - 14;
        if (avail < 20) return 0;
        int maxRows = avail / 20;
        return Math.min(maxRows, searchResults.size() - resultScroll);
    }

    private int trackLabelY() { return (hasResults() ? resultsBottom() + 10 : fieldBottom() + 16); }
    private int trackInfoY() { return trackLabelY() + 14; }
    private int barY() { return trackInfoY() + 14; }
    private int barH() { return 6; }
    private int timeY() { return barY() + barH() + 2; }

    private int controlY() {
        int base = Math.max(timeY() + 16, height * 3 / 5 + 8);
        if (hasResults()) {
            base = Math.max(base, resultsBottom() + 10);
        }
        return base;
    }

    private int btnH() { return Math.max(20, height / 28); }
    private int btnW() { return Math.max(72, (width - margin() * 2 - GAP * 3) / 3); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int m = margin();
        int fh = fieldH();
        int bw = btnW();
        int bh = btnH();
        int availW = width - m * 2;

        g.fill(0, 0, width, height, 0xCC000000);

        int y = 10;
        g.centeredText(font, getTitle(), width / 2, y, FG);
        g.text(font, Component.literal("X"), width - 14, y, DIM);

        y = fieldY();
        int fieldW = availW - GAP - loadW();
        int fr = m + fieldW;
        int lx = fr + GAP;

        g.fill(m, y, fr, y + fh, urlFocused ? BG_INPUT_FOCUSED : BG_INPUT);
        String placeholder = "Search SoundCloud or paste URL...";
        String display = urlText.isEmpty() ? placeholder : urlText.toString();
        int textColor = urlText.isEmpty() ? DIM : FG;
        g.text(font, Component.literal(display), m + 4, y + 4, textColor);

        if (urlFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = m + 4 + font.width(urlText.substring(0, Math.min(urlCursor, urlText.length())));
            g.fill(cx, y + 3, cx + 1, y + fh - 3, FG);
        }

        hoverLoad = mx >= lx && mx <= lx + loadW() && my >= y && my <= y + fh;
        g.fill(lx, y, lx + loadW(), y + fh, hoverLoad ? BG_BTN_HOVER : BG_BTN);
        g.centeredText(font, Component.literal("Go"), lx + loadW() / 2, y + 4,
            hoverLoad ? FG : DIM);

        int bottomStatusY = height - 20;

        if (searchPending) {
            String dots = ".".repeat((int) ((System.currentTimeMillis() / 400) % 4));
            g.text(font, Component.literal("Searching" + dots), m, bottomStatusY, ACCENT);
        }

        if (hasResults()) {
            int ry = resultsAreaY();
            int rh = resultsAreaH();

            String header = "Results (" + searchResults.size() + ")";
            g.text(font, Component.literal(header), m, ry, ACCENT);

            g.fill(m, ry + 14, m + availW, ry + 14 + 1, 0x33FFFFFF);

            int rowY = ry + 18;
            int visible = resultsVisible();
            int maxTextW = availW - 68;

            for (int i = 0; i < visible; i++) {
                int idx = i + resultScroll;
                if (idx >= searchResults.size()) break;
                AudioTrack track = searchResults.get(idx);

                boolean ho = my >= rowY && my <= rowY + 20 && mx >= m && mx <= m + availW;
                boolean se = idx == selectedResult;

                if (ho) {
                    g.fill(m, rowY, m + availW, rowY + 20, BG_RESULT);
                    hoverResult = idx;
                } else if (se) {
                    g.fill(m, rowY, m + availW, rowY + 20, BG_RESULT_SELECTED);
                }

                if (ho) {
                    g.text(font, Component.literal("\u25B6"), m + 4, rowY + 3, ACCENT);
                } else {
                    g.text(font, Component.literal("\u25B6"), m + 4, rowY + 3, DIM);
                }

                String text = track.getInfo().title;
                if (track.getInfo().author != null && !track.getInfo().author.equals("Unknown")) {
                    text += " - " + track.getInfo().author;
                }
                if (font.width(text) > maxTextW) {
                    text = font.plainSubstrByWidth(text, maxTextW - 4) + "...";
                }

                g.text(font, Component.literal(text), m + 18, rowY + 4, ho ? FG : 0xFFCCCCCC);

                String dur = formatTime(track.getDuration());
                g.text(font, Component.literal(dur), m + availW - font.width(dur) - 4, rowY + 4, DIM);

                rowY += 20;
            }

            int totalArea = (rh - 14) / 20;
            if (searchResults.size() > totalArea) {
                String info = (resultScroll + 1) + "-" + Math.min(resultScroll + totalArea, searchResults.size())
                    + "/" + searchResults.size();
                g.text(font, Component.literal(info), width - m - font.width(info), rowY + 2, DIM);
            }
        }

        y = trackLabelY();
        g.text(font, Component.literal("NOW PLAYING"), m, y, LABEL);

        y = trackInfoY();
        if (player.hasTrack()) {
            var info = player.getCurrentTrack().getInfo();
            String title = info.title != null ? info.title : "Unknown";
            String author = info.author != null ? info.author : "Unknown";
            String label = title + " \u2014 " + author;
            if (font.width(label) > availW) {
                label = font.plainSubstrByWidth(label, availW - 6) + "...";
            }

            g.text(font, Component.literal("♫ ").append(Component.literal(label)), m, y, FG);

            int by = barY();
            long pos = player.getPosition();
            long dur = player.getDuration();
            float progress = dur > 0 ? (float) pos / dur : 0;

            boolean hp = mx >= m && mx <= m + availW && my >= by && my <= by + barH();

            g.fill(m, by, m + availW, by + barH(), BG_PROGRESS);
            int fillW = (int) (availW * progress);
            if (fillW > 0) {
                g.fill(m, by, m + fillW, by + barH(), hp ? ACCENT : GREEN);
            }

            g.text(font, Component.literal(formatTime(pos) + " / " + formatTime(dur)), m, timeY(), DIM);

            int cy = controlY();

            boolean hoverPlay = mx >= m && mx <= m + bw && my >= cy && my <= cy + bh;
            g.fill(m, cy, m + bw, cy + bh, hoverPlay ? BG_BTN_HOVER : BG_BTN);
            String playLabel = player.isPaused() ? "\u25B6  PLAY" : "\u23F8  PAUSE";
            g.centeredText(font, Component.literal(playLabel), m + bw / 2, cy + 4, hoverPlay ? FG : DIM);

            int sx = m + bw + GAP;
            boolean hoverStop = mx >= sx && mx <= sx + bw && my >= cy && my <= cy + bh;
            g.fill(sx, cy, sx + bw, cy + bh, hoverStop ? BG_BTN_HOVER : BG_BTN);
            g.centeredText(font, Component.literal("\u25A0  STOP"), sx + bw / 2, cy + 4, hoverStop ? FG : DIM);

            renderVolumeSlider(g, mx, my, m, sx, bw, cy, bh, availW);

        } else {
            g.text(font, Component.literal("No track loaded"), m, y, DIM);

            if (searchPending) {
                String dots = ".".repeat((int) ((System.currentTimeMillis() / 400) % 4));
                g.text(font, Component.literal("Searching" + dots), m, y + 16, ACCENT);
            }
        }

        if (statusTimer > 0 && !statusText.isEmpty() && !searchPending) {
            int sc = statusIsError ? 0xFFFF5555 : DIM;
            g.text(font, Component.literal(statusText), m, bottomStatusY, sc);
        }
    }

    private int volumeSliderX;
    private int volumeSliderW;

    private void renderVolumeSlider(GuiGraphicsExtractor g, int mx, int my, int m, int sx, int bw, int cy, int bh, int availW) {
        int vx = sx + bw + GAP;
        int volLabelW = font.width("Vol") + 6;
        int volPctW = font.width(" 100%") + 4;
        int vsW = availW - (vx - m) - volPctW;
        if (vsW < 50) vsW = 50;
        int sy = cy + (bh - 6) / 2;

        g.text(font, Component.literal("Vol"), vx, sy - 1, LABEL);
        vx += volLabelW;
        int vlw = vsW - volLabelW;
        if (vlw < 24) vlw = 24;

        volumeSliderX = vx;
        volumeSliderW = vlw;

        float vol = player.getSliderVolume();
        int vf = (int) (vlw * (vol / 100f));

        boolean hoverVol = mx >= vx && mx <= vx + vlw && my >= sy && my <= sy + 6;

        g.fill(vx, sy, vx + vlw, sy + 6, BG_PROGRESS);
        if (vf > 0) {
            g.fill(vx, sy, vx + vf, sy + 6, hoverVol ? 0xFF77CCFF : ACCENT);
        }

        String pct = (int) vol + "%";
        g.text(font, Component.literal(pct), vx + vlw + 4, sy - 1,
            hoverVol ? FG : DIM);

        if (hoverVol) {
            int dotX = vx + vf;
            g.fill(dotX - 2, sy - 2, dotX + 2, sy + 8, FG);
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
                    urlText.deleteCharAt(urlCursor - 1);
                    urlCursor--;
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_DELETE) {
                if (urlCursor < urlText.length()) {
                    urlText.deleteCharAt(urlCursor);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_LEFT && urlCursor > 0) {
                urlCursor--;
                return true;
            }
            if (key == GLFW.GLFW_KEY_RIGHT && urlCursor < urlText.length()) {
                urlCursor++;
                return true;
            }
            if (key == GLFW.GLFW_KEY_HOME) { urlCursor = 0; return true; }
            if (key == GLFW.GLFW_KEY_END) { urlCursor = urlText.length(); return true; }
            if (key == GLFW.GLFW_KEY_V && (event.modifiers() & 2) != 0) {
                String clip = glfwGetClipboardString(minecraft.getWindow().handle());
                if (clip != null) {
                    urlText.insert(urlCursor, clip);
                    urlCursor += clip.length();
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_DOWN) {
                if (hasResults() && !searchResults.isEmpty()) {
                    urlFocused = false;
                    selectedResult = 0;
                    return true;
                }
            }
            return true;
        }

        if (hasResults()) {
            if (key == GLFW.GLFW_KEY_UP) {
                if (selectedResult <= 0) {
                    urlFocused = true;
                    selectedResult = -1;
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
                    int vis = resultsVisible();
                    if (selectedResult >= resultScroll + vis) {
                        resultScroll = selectedResult - vis + 1;
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
            selectedResult = -1;
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

        if (btn != 0) return super.mouseClicked(event, doubleClick);

        int m = margin();
        int fh = fieldH();
        int bw = btnW();
        int bh = btnH();
        int availW = width - m * 2;

        int fy = fieldY();
        int fw = availW - GAP - loadW();
        int fr = m + fw;
        int lx = fr + GAP;

        if (mx >= m && mx <= fr && my >= fy && my <= fy + fh) {
            urlFocused = true;
            selectedResult = -1;
            int cx = (int) mx - m - 4;
            urlCursor = font.plainSubstrByWidth(urlText.toString(), Math.max(0, cx)).length();
            return true;
        }

        if (mx >= lx && mx <= lx + loadW() && my >= fy && my <= fy + fh) {
            goAction();
            return true;
        }

        urlFocused = false;

        if (hasResults()) {
            int ry = resultsAreaY() + 18;
            int visible = resultsVisible();
            for (int i = 0; i < visible; i++) {
                int idx = i + resultScroll;
                if (idx >= searchResults.size()) break;
                if (mx >= m && mx <= m + availW && my >= ry && my <= ry + 20) {
                    playResult(idx);
                    return true;
                }
                ry += 20;
            }
        }

        if (player.hasTrack()) {
            int cy = controlY();
            int by = barY();

            if (mx >= m && mx <= m + bw && my >= cy && my <= cy + bh) {
                player.togglePause();
                return true;
            }

            int sx = m + bw + GAP;
            if (mx >= sx && mx <= sx + bw && my >= cy && my <= cy + bh) {
                player.stop();
                setStatus("\u25A0 Stopped");
                return true;
            }

            if (mx >= m && mx <= m + availW && my >= by && my <= by + barH()) {
                float pct = (float) Math.max(0, Math.min(1, (mx - m) / availW));
                long pos = (long) (player.getDuration() * pct);
                player.seek(pos);
                draggingProgress = true;
                return true;
            }

            int volLabelW = font.width("Vol") + 6;
            int volPctW = font.width(" 100%") + 4;
            int sxv = m + bw + GAP;
            int vx = sxv + bw + GAP + volLabelW;
            int vsW = availW - (vx - m) - volPctW;
            if (vsW < 50) vsW = 50;
            int vlw = vsW - volLabelW;
            if (vlw < 24) vlw = 24;
            int sy = cy + (bh - 6) / 2;

            if (mx >= vx && mx <= vx + vlw && my >= sy && my <= sy + 6) {
                float pct = (float) ((mx - vx) / Math.max(1, vlw));
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
        double my = event.y();
        int m = margin();

        if (draggingVolume && player.hasTrack()) {
            if (volumeSliderW > 0) {
                float pct = (float) ((mx - volumeSliderX) / volumeSliderW);
                player.setSliderVolume(Mth.clamp(pct * 100, 0, 100));
            }
            return true;
        }

        if (draggingProgress && player.hasTrack()) {
            float pct = (float) ((mx - m) / (width - m * 2));
            long pos = (long) (player.getDuration() * Mth.clamp(pct, 0, 1));
            player.seek(pos);
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

        searchResults = null;
        resultScroll = 0;
        selectedResult = -1;
        hoverResult = -1;

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
        String label = track.getInfo().title;
        setStatus("\u25B6  Loading: " + label);
        player.loadAndPlay(track.getInfo().uri);
    }

    public void setStatus(String msg) {
        statusText = msg;
        statusTimer = 80;
    }

    private static String formatTime(long ms) {
        long sec = ms / 1000;
        if (sec >= 3600) {
            return String.format("%d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
        }
        return String.format("%d:%02d", sec / 60, sec % 60);
    }
}

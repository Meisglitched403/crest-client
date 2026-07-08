package com.crest.client.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

/**
 * Centralized UI theme. Replaces the scattered per-screen color literals and
 * provides an animated accent that the whole UI can reference.
 */
public final class Theme {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("crest-theme.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Palette (ARGB)
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFF9AA0B5;
    public static final int TEXT_FAINT = 0xFF6B7088;
    public static final int BG_BASE = 0xCC0A0A1A;
    public static final int BG_PANEL = 0xCC141428;
    public static final int BG_HOVER = 0x44343A66;
    public static final int BG_SELECT = 0x66202655;
    public static final int BG_SURFACE = 0xCC1A1A33;
    public static final int BORDER = 0x55FFFFFF;
    public static final int TEXT_ON = 0xFF5BFF8F;
    public static final int TEXT_OFF = 0xFFFF5B6E;
    public static final int OVERLAY = 0x800A0A1A;

    private static int accent = 0xFF5555FF;
    private static final Animated accentHue = new Animated(0f, 0.6f);
    private static long animStart = System.currentTimeMillis();

    private static float accentPulse;

    public static int getAccent() {
        return accent;
    }

    public static void setAccent(int c) {
        accent = c;
        save();
    }

    /** Animated accent that gently pulses between the base accent and a shifted hue. */
    public static int getAnimatedAccent() {
        int shifted = ColorUtil.hueShift(accent, accentHue.get());
        return ColorUtil.lerpARGB(accent, shifted, 0.35f * accentPulse);
    }

    public static int getAccentHover() {
        return ColorUtil.lerpARGB(accent, 0xFFFFFFFF, 0.18f);
    }

    /** Call once per frame from the active screen's render to advance animations. */
    public static void tick(float dt) {
        float elapsed = (System.currentTimeMillis() - animStart) / 1000f;
        accentHue.set((elapsed * 0.05f) % 1.0f);
        accentHue.tick(dt);
        accentPulse = (float) (0.5f + 0.5f * Math.sin(elapsed * 1.5f));
    }

    public static void load() {
        try (FileReader r = new FileReader(PATH.toFile())) {
            AccentData data = GSON.fromJson(r, AccentData.class);
            if (data != null && data.accent != 0) accent = data.accent;
        } catch (Exception ignored) {}
    }

    public static void save() {
        try (FileWriter w = new FileWriter(PATH.toFile())) {
            GSON.toJson(new AccentData(accent), w);
        } catch (Exception ignored) {}
    }

    private static final class AccentData {
        int accent;
        AccentData(int accent) { this.accent = accent; }
    }
}

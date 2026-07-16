package com.crest.client.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

public final class Theme {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("crest-theme.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // --- CSS-variable-style named palette ---
    public static final int BACKGROUND = 0xDD111128;
    public static final int FOREGROUND = 0xFFF5F5FF;

    public static final int CARD = 0xDD151530;
    public static final int CARD_FOREGROUND = 0xFFF5F5FF;

    public static final int POPOVER = 0xDD1E1E42;
    public static final int POPOVER_FOREGROUND = 0xFF9AA0B5;

    public static final int PRIMARY = 0xFF5555FF;
    public static final int PRIMARY_FOREGROUND = 0xFFFFFFFF;
    public static final int PRIMARY_CONTAINER = 0x882A2A80;

    public static final int SECONDARY = 0xFF9AA0B5;
    public static final int SECONDARY_FOREGROUND = 0xFFFFFFFF;

    public static final int MUTED = 0xDD1E1E42;
    public static final int MUTED_FOREGROUND = 0xFF6B7088;

    public static final int ACCENT = PRIMARY;
    public static final int ACCENT_FOREGROUND = PRIMARY_FOREGROUND;

    public static final int DESTRUCTIVE = 0xFFFF5B6E;
    public static final int DESTRUCTIVE_FOREGROUND = 0xFFFFFFFF;

    public static final int BORDER = 0x55FFFFFF;
    public static final int INPUT = 0x55FFFFFF;
    public static final int RING = 0x885555FF;

    public static final int RADIUS = 6;

    // --- Legacy semantic aliases (keep for backward compat) ---
    public static final int ON_PRIMARY = PRIMARY_FOREGROUND;
    public static final int ON_SECONDARY = SECONDARY_FOREGROUND;
    public static final int SURFACE = CARD;
    public static final int ON_SURFACE = FOREGROUND;
    public static final int SURFACE_VARIANT = POPOVER;
    public static final int ON_SURFACE_VARIANT = POPOVER_FOREGROUND;
    public static final int SURFACE_CONTAINER = BACKGROUND;
    public static final int SURFACE_TINT = 0x800A0A22;
    public static final int OUTLINE = BORDER;
    public static final int ERROR = DESTRUCTIVE;

    // --- Legacy semantic aliases for UI states ---
    public static final int TEXT = FOREGROUND;
    public static final int TEXT_DIM = POPOVER_FOREGROUND;
    public static final int TEXT_FAINT = MUTED_FOREGROUND;
    public static final int TEXT_ON = 0xFF5BFF8F;
    public static final int TEXT_OFF = ERROR;

    public static final int BG_BASE = BACKGROUND;
    public static final int BG_PANEL = CARD;
    public static final int BG_HOVER = 0x55404077;
    public static final int BG_SELECT = 0x772E2E88;
    public static final int BG_SURFACE = POPOVER;
    public static final int OVERLAY = SURFACE_TINT;

    // --- Glassmorphic ---
    public static final int GLASS_BG = 0x80141218;
    public static final int BORDER_LIGHT = 0x20FFFFFF;
    public static final int CARD_GRADIENT_TOP = 0x15FFFFFF;
    public static final int CARD_GRADIENT_BOT = 0x02FFFFFF;
    public static final int SIDEBAR_BG = 0x33000000;

    // --- Elevation (pixels) ---
    public static final int ELEVATION_0 = 0;
    public static final int ELEVATION_1 = 2;
    public static final int ELEVATION_2 = 4;
    public static final int ELEVATION_3 = 8;
    public static final int ELEVATION_4 = 12;
    public static final int ELEVATION_5 = 16;

    private static int accent = PRIMARY;
    private static final Animated accentHue = new Animated(0f, 0.6f);
    private static long animStart = System.currentTimeMillis();
    private static float accentPulse;

    public static int getAccent() { return accent; }
    public static void setAccent(int c) { accent = c; save(); }

    public static int getAnimatedAccent() {
        int shifted = ColorUtil.hueShift(accent, accentHue.get());
        return ColorUtil.lerpARGB(accent, shifted, 0.35f * accentPulse);
    }

    public static int getAccentHover() {
        return ColorUtil.lerpARGB(accent, 0xFFFFFFFF, 0.18f);
    }

    public static int getAccentDim() {
        return ColorUtil.withAlpha(accent, 100);
    }

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

    private static final class AccentData { int accent; AccentData(int accent) { this.accent = accent; } }
}

package com.crest.client.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Central theming for Crest Client.
 *
 * The user-editable state lives in {@link ThemeData} (persisted to crest-theme.json).
 * The static {@code Theme.*} constants are kept in sync with the active data via
 * {@link #sync()}, so existing call sites (e.g. {@code Theme.FOREGROUND}) continue to
 * work and automatically reflect the loaded theme.
 */
public final class Theme {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("crest-theme.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ThemeData data = ThemePresets.DARK.clone();
    private static ThemeData pending = data.clone();
    private static final List<ThemeChangeListener> listeners = new ArrayList<>();

    public interface ThemeChangeListener {
        void onThemeChanged();
    }

    // --- CSS-variable-style named palette (synced from active data) ---
    public static int BACKGROUND;
    public static int FOREGROUND;

    public static int CARD;
    public static int CARD_FOREGROUND;

    public static int POPOVER;
    public static int POPOVER_FOREGROUND;

    public static int PRIMARY;
    public static int PRIMARY_FOREGROUND = 0xFFFFFFFF;
    public static int PRIMARY_CONTAINER;

    public static int SECONDARY;
    public static int SECONDARY_FOREGROUND = 0xFFFFFFFF;

    public static int MUTED;
    public static int MUTED_FOREGROUND;

    public static int ACCENT;
    public static int ACCENT_FOREGROUND = 0xFFFFFFFF;

    public static int DESTRUCTIVE;
    public static int DESTRUCTIVE_FOREGROUND = 0xFFFFFFFF;

    public static int BORDER;
    public static int INPUT;
    public static int RING;

    public static int RADIUS;

    // --- Legacy semantic aliases (mirrors of the above) ---
    public static int ON_PRIMARY;
    public static int ON_SECONDARY;
    public static int SURFACE;
    public static int ON_SURFACE;
    public static int SURFACE_VARIANT;
    public static int ON_SURFACE_VARIANT;
    public static int SURFACE_CONTAINER;
    public static int SURFACE_TINT;
    public static int OUTLINE;
    public static int ERROR;

    // --- Legacy semantic aliases for text ---
    public static int TEXT;
    public static int TEXT_DIM;
    public static int TEXT_FAINT;
    public static int TEXT_ON = 0xFF5BFF8F;
    public static int TEXT_OFF;

    public static int BG_BASE;
    public static int BG_PANEL;
    public static int BG_HOVER = 0x55404077;
    public static int BG_SELECT = 0x772E2E88;
    public static int BG_SURFACE;
    public static int OVERLAY;

    // --- Glassmorphic ---
    public static int GLASS_BG;
    public static int BORDER_LIGHT;
    public static int CARD_GRADIENT_TOP;
    public static int CARD_GRADIENT_BOT;
    public static int SIDEBAR_BG;

    // --- Elevation (pixels) ---
    public static final int ELEVATION_0 = 0;
    public static final int ELEVATION_1 = 2;
    public static final int ELEVATION_2 = 4;
    public static final int ELEVATION_3 = 8;
    public static final int ELEVATION_4 = 12;
    public static final int ELEVATION_5 = 16;

    // --- Glass opacity (0..255) used by screen backdrops / top strips ---
    public static int glassOpacity;
    public static int topStripAlpha;

    // --- Font scale (1.0 default) ---
    public static float fontScale = 1f;

    public enum Density { COMPACT, NORMAL, COMFORTABLE }
    public static Density density = Density.NORMAL;

    public static int ROW_H() {
        return switch (density) {
            case COMPACT -> 22;
            case COMFORTABLE -> 32;
            default -> 26;
        };
    }

    // --- Accent animation ---
    private static final Animated accentHue = new Animated(0f, 0.6f);
    private static long animStart = System.currentTimeMillis();
    private static float accentPulse;

    static {
        sync();
    }

    /** Recompute all static constants from the active {@link ThemeData}. */
    public static void sync() {
        BACKGROUND = data.background;
        FOREGROUND = data.foreground;
        CARD = data.card;
        CARD_FOREGROUND = data.cardForeground;
        POPOVER = data.popover;
        POPOVER_FOREGROUND = data.popoverForeground;
        PRIMARY = data.accent;
        PRIMARY_CONTAINER = ColorUtil.withAlpha(data.accent, 136);
        SECONDARY = data.secondary;
        MUTED = data.muted;
        MUTED_FOREGROUND = data.mutedForeground;
        ACCENT = data.accent;
        DESTRUCTIVE = data.destructive;
        BORDER = data.border;
        INPUT = data.border;
        RING = ColorUtil.withAlpha(data.accent, 136);
        RADIUS = data.radius;

        ON_PRIMARY = PRIMARY_FOREGROUND;
        ON_SECONDARY = SECONDARY_FOREGROUND;
        SURFACE = CARD;
        ON_SURFACE = FOREGROUND;
        SURFACE_VARIANT = POPOVER;
        ON_SURFACE_VARIANT = POPOVER_FOREGROUND;
        SURFACE_CONTAINER = BACKGROUND;
        SURFACE_TINT = ColorUtil.withAlpha(data.background, 200);
        OUTLINE = BORDER;
        ERROR = DESTRUCTIVE;

        TEXT = FOREGROUND;
        TEXT_DIM = POPOVER_FOREGROUND;
        TEXT_FAINT = MUTED_FOREGROUND;
        TEXT_OFF = ERROR;

        BG_BASE = BACKGROUND;
        BG_PANEL = CARD;
        BG_SURFACE = POPOVER;
        OVERLAY = SURFACE_TINT;

        GLASS_BG = data.glassBg;
        BORDER_LIGHT = ColorUtil.withAlpha(0xFFFFFFFF, 32);
        CARD_GRADIENT_TOP = ColorUtil.withAlpha(0xFFFFFFFF, 21);
        CARD_GRADIENT_BOT = ColorUtil.withAlpha(0xFFFFFFFF, 2);
        SIDEBAR_BG = data.sidebarBg;

        glassOpacity = data.glassOpacity;
        topStripAlpha = (int) (160 * (data.glassOpacity / 255f));
        fontScale = data.fontScale;
        density = data.density;

        fireThemeChanged();
    }

    private static void fireThemeChanged() {
        for (ThemeChangeListener l : listeners) {
            try { l.onThemeChanged(); } catch (Exception ignored) {}
        }
    }

    public static void addListener(ThemeChangeListener l) {
        listeners.add(l);
    }

    public static void removeListener(ThemeChangeListener l) {
        listeners.remove(l);
    }

    public static int scaled(int size) {
        return (int) (size * fontScale);
    }

    public static float scaledF(float size) {
        return size * fontScale;
    }

    public static int scaledLineHeight() {
        return scaled(9);
    }

    public static int scaledTextOffset(int baseY) {
        return baseY + (scaled(9) - 9) / 2;
    }

    public static int getAccent() { return data.accent; }

    public static int getAnimatedAccent() {
        int shifted = ColorUtil.hueShift(data.accent, accentHue.get());
        return ColorUtil.lerpARGB(data.accent, shifted, 0.35f * accentPulse);
    }

    public static int getAccentHover() {
        return ColorUtil.lerpARGB(data.accent, 0xFFFFFFFF, 0.18f);
    }

    public static int getAccentDim() {
        return ColorUtil.withAlpha(data.accent, 100);
    }

    public static int hoverTint() { return ColorUtil.withAlpha(0xFFFFFFFF, 8); }

    public static void tick(float dt) {
        float elapsed = (System.currentTimeMillis() - animStart) / 1000f;
        accentHue.set((elapsed * 0.05f) % 1.0f);
        accentHue.tick(dt);
        accentPulse = (float) (0.5f + 0.5f * Math.sin(elapsed * 1.5f));
    }

    // --- Data access for the editor ---
    public static ThemeData get() { return data; }
    public static ThemeData getPending() { return pending; }
    public static void setPending(ThemeData d) { pending = d.clone(); }
    public static void commitPending() { data = pending.clone(); sync(); save(); }

    public static void apply(ThemeData d) { data = d.clone(); sync(); save(); }
    public static void reset() { apply(ThemePresets.DARK); }

    public static void load() {
        try (FileReader r = new FileReader(PATH.toFile())) {
            ThemeData loaded = GSON.fromJson(r, ThemeData.class);
            if (loaded != null) data = loaded.normalize();
        } catch (Exception ignored) {}
        pending = data.clone();
        sync();
    }

    public static void save() {
        try (FileWriter w = new FileWriter(PATH.toFile())) {
            GSON.toJson(data, w);
        } catch (Exception ignored) {}
    }
}

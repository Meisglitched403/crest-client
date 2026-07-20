package com.crest.client.ui;

/** Built-in theme presets. */
public final class ThemePresets {
    private ThemePresets() {}

    public static final ThemeData DARK;
    public static final ThemeData LIGHT;
    public static final ThemeData AMOLED;

    static {
        DARK = new ThemeData();
        DARK.accent = 0xFF5555FF;
        DARK.background = 0xDD111128;
        DARK.foreground = 0xFFF5F5FF;
        DARK.card = 0xDD151530;
        DARK.cardForeground = 0xFFF5F5FF;
        DARK.popover = 0xDD1E1E42;
        DARK.popoverForeground = 0xFF9AA0B5;
        DARK.secondary = 0xFF9AA0B5;
        DARK.muted = 0xDD1E1E42;
        DARK.mutedForeground = 0xFF6B7088;
        DARK.destructive = 0xFFFF5B6E;
        DARK.border = 0x55FFFFFF;
        DARK.glassBg = 0x80141218;
        DARK.sidebarBg = 0x33000000;
        DARK.radius = 6;
        DARK.glassOpacity = 220;
        DARK.fontScale = 1f;
        DARK.density = Theme.Density.NORMAL;
        DARK.preset = "Dark";

        LIGHT = new ThemeData();
        LIGHT.accent = 0xFF3B5BFF;
        LIGHT.background = 0xEEF1F6FF;
        LIGHT.foreground = 0xFF1A1C24;
        LIGHT.card = 0xEEFFFFFF;
        LIGHT.cardForeground = 0xFF1A1C24;
        LIGHT.popover = 0xEEF4F6FB;
        LIGHT.popoverForeground = 0xFF4A5066;
        LIGHT.secondary = 0xFF8A90A6;
        LIGHT.muted = 0xEEEDF1F8;
        LIGHT.mutedForeground = 0xFF6B7088;
        LIGHT.destructive = 0xFFE23B52;
        LIGHT.border = 0x55000000;
        LIGHT.glassBg = 0xCCE6EAF2;
        LIGHT.sidebarBg = 0x33FFFFFF;
        LIGHT.radius = 8;
        LIGHT.glassOpacity = 230;
        LIGHT.fontScale = 1f;
        LIGHT.density = Theme.Density.NORMAL;
        LIGHT.preset = "Light";

        AMOLED = new ThemeData();
        AMOLED.accent = 0xFF00E5FF;
        AMOLED.background = 0xFF000000;
        AMOLED.foreground = 0xFFEAF6FF;
        AMOLED.card = 0xFF0C0C12;
        AMOLED.cardForeground = 0xFFEAF6FF;
        AMOLED.popover = 0xFF14141C;
        AMOLED.popoverForeground = 0xFF8AA0B5;
        AMOLED.secondary = 0xFF8AA0B5;
        AMOLED.muted = 0xFF101018;
        AMOLED.mutedForeground = 0xFF5A6478;
        AMOLED.destructive = 0xFFFF3B6E;
        AMOLED.border = 0x332A2A33;
        AMOLED.glassBg = 0xCC000000;
        AMOLED.sidebarBg = 0x33000000;
        AMOLED.radius = 10;
        AMOLED.glassOpacity = 235;
        AMOLED.fontScale = 1f;
        AMOLED.density = Theme.Density.NORMAL;
        AMOLED.preset = "Amoled";
    }

    public static ThemeData fromName(String name) {
        if (name == null) return DARK.clone();
        return switch (name.toLowerCase()) {
            case "light" -> LIGHT.clone();
            case "amoled" -> AMOLED.clone();
            default -> DARK.clone();
        };
    }
}

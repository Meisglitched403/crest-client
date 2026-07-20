package com.crest.client.ui;

/** Serializable, user-editable theme state persisted to crest-theme.json. */
public final class ThemeData {
    public int accent = 0xFF5555FF;
    public int background = 0xDD111128;
    public int foreground = 0xFFF5F5FF;
    public int card = 0xDD151530;
    public int cardForeground = 0xFFF5F5FF;
    public int popover = 0xDD1E1E42;
    public int popoverForeground = 0xFF9AA0B5;
    public int secondary = 0xFF9AA0B5;
    public int muted = 0xDD1E1E42;
    public int mutedForeground = 0xFF6B7088;
    public int destructive = 0xFFFF5B6E;
    public int border = 0x55FFFFFF;
    public int glassBg = 0x80141218;
    public int sidebarBg = 0x33000000;
    public int radius = 6;
    public int glassOpacity = 220;
    public float fontScale = 1f;
    public Theme.Density density = Theme.Density.NORMAL;
    public String preset = "Dark";

    public ThemeData() {}

    public ThemeData clone() {
        ThemeData d = new ThemeData();
        d.accent = accent;
        d.background = background;
        d.foreground = foreground;
        d.card = card;
        d.cardForeground = cardForeground;
        d.popover = popover;
        d.popoverForeground = popoverForeground;
        d.secondary = secondary;
        d.muted = muted;
        d.mutedForeground = mutedForeground;
        d.destructive = destructive;
        d.border = border;
        d.glassBg = glassBg;
        d.sidebarBg = sidebarBg;
        d.radius = radius;
        d.glassOpacity = glassOpacity;
        d.fontScale = fontScale;
        d.density = density;
        d.preset = preset;
        return d;
    }

    /** Fill in any missing/garbage fields with safe defaults. */
    public ThemeData normalize() {
        if (radius < 0 || radius > 24) radius = 6;
        if (glassOpacity < 0 || glassOpacity > 255) glassOpacity = 220;
        if (fontScale < 0.7f || fontScale > 1.5f) fontScale = 1f;
        if (density == null) density = Theme.Density.NORMAL;
        if (preset == null) preset = "Custom";
        return this;
    }
}

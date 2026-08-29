package com.crest.client.waypoints;

public final class ColorUtil {
    private ColorUtil() {}

    public static int clampChannel(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static int hsvToRgb(float h, float s, float v) {
        h = ((h % 360f) + 360f) % 360f;
        s = Math.max(0f, Math.min(1f, s));
        v = Math.max(0f, Math.min(1f, v));
        float c = v * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = v - c;
        float r = 0, g = 0, b = 0;
        if (h < 60) { r = c; g = x; }
        else if (h < 120) { r = x; g = c; }
        else if (h < 180) { g = c; b = x; }
        else if (h < 240) { g = x; b = c; }
        else if (h < 300) { r = x; b = c; }
        else { r = c; b = x; }
        int ri = clampChannel(Math.round((r + m) * 255f));
        int gi = clampChannel(Math.round((g + m) * 255f));
        int bi = clampChannel(Math.round((b + m) * 255f));
        return (ri << 16) | (gi << 8) | bi;
    }

    public static float[] rgbToHsv(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;
        float h = 0f;
        if (d != 0) {
            if (max == rf) h = 60f * ((gf - bf) / d);
            else if (max == gf) h = 60f * ((bf - rf) / d + 2f);
            else h = 60f * ((rf - gf) / d + 4f);
        }
        if (h < 0) h += 360f;
        float s = max == 0 ? 0 : d / max;
        return new float[] { h, s, max };
    }

    public static int withAlpha(int rgb, int alphaPercent) {
        int a = clampChannel((int) (255f * (alphaPercent / 100f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}

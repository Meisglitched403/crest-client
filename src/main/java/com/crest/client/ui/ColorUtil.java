package com.crest.client.ui;

/** ARGB color manipulation helpers. Colors are packed 0xAARRGGBB. */
public final class ColorUtil {
    private ColorUtil() {}

    public static int rgba(int r, int g, int b, int a) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    public static int argb(int a, int r, int g, int b) {
        return rgba(r, g, b, a);
    }

    public static int getR(int c) { return (c >> 16) & 0xFF; }
    public static int getG(int c) { return (c >> 8) & 0xFF; }
    public static int getB(int c) { return c & 0xFF; }
    public static int getA(int c) { return (c >> 24) & 0xFF; }

    public static int withAlpha(int c, int a) {
        return (c & 0x00FFFFFF) | ((a & 0xFF) << 24);
    }

    public static int setAlpha(int c, int a) {
        return (c & 0x00FFFFFF) | ((a & 0xFF) << 24);
    }

    /** Linear interpolate between two packed ARGB colors (alpha lerped too). */
    public static int lerpARGB(int a, int b, float t) {
        int ar = getR(a), ag = getG(a), ab = getB(a), aa = getA(a);
        int br = getR(b), bg = getG(b), bb = getB(b), ba = getA(b);
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bch = Math.round(ab + (bb - ab) * t);
        int al = Math.round(aa + (ba - aa) * t);
        return rgba(r, g, bch, al);
    }

    /** Subtle hue rotation for animated accents. t in [0,1) loops. */
    public static int hueShift(int base, float t) {
        float h = (float) (t % 1.0f);
        if (h < 0) h += 1f;
        return hsvToInt(h, 0.7f, 1.0f, getA(base) / 255f);
    }

    public static int hsvToInt(float h, float s, float v, float a) {
        int hi = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6);
        int p = Math.round(255 * v * (1 - s));
        int q = Math.round(255 * v * (1 - f * s));
        int t2 = Math.round(255 * v * (1 - (1 - f) * s));
        int w = Math.round(255 * v);
        int r, g, b;
        switch (hi) {
            case 0: r = w; g = t2; b = p; break;
            case 1: r = q; g = w; b = p; break;
            case 2: r = p; g = w; b = t2; break;
            case 3: r = p; g = q; b = w; break;
            case 4: r = t2; g = p; b = w; break;
            default: r = w; g = p; b = q; break;
        }
        return rgba(r, g, b, Math.round(a * 255));
    }

    /** Convert HSV (h,s,v in 0..1) to packed ARGB with full alpha. */
    public static int hsvToArgb(float h, float s, float v) {
        return hsvToInt(h, s, v, 1f);
    }

    /** Shorthand for hsvToArgb. */
    public static int hsv(float h, float s, float v) {
        return hsvToInt(h, s, v, 1f);
    }

    /** Convert packed ARGB to HSV (h,s,v in 0..1). Alpha ignored. */
    public static float[] toHSV(int c) {
        int r = getR(c), g = getG(c), b = getB(c);
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        float v = max / 255f;
        float d = (max - min) / 255f;
        float s = max == 0 ? 0f : d / v;
        float h;
        if (d == 0) h = 0f;
        else if (max == r) h = ((g - b) / 255f) / d;
        else if (max == g) h = 2f + ((b - r) / 255f) / d;
        else h = 4f + ((r - g) / 255f) / d;
        h /= 6f;
        if (h < 0) h += 1f;
        return new float[]{h, s, v};
    }
}

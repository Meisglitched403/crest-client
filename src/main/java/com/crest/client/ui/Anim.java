package com.crest.client.ui;

/** Tween / easing helpers and frame-rate-independent interpolation. */
public final class Anim {
    private Anim() {}

    public static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static int lerpColor(int a, int b, float t) {
        return ColorUtil.lerpARGB(a, b, t);
    }

    /** Frame-rate-independent smoothing factor for a given per-second speed. */
    public static float smooth(float dt, float speed) {
        return 1.0f - (float) Math.exp(-speed * dt);
    }

    public static float easeOutCubic(float t) {
        t = clamp(t, 0, 1);
        float u = 1 - t;
        return 1 - u * u * u;
    }

    public static float easeInOutCubic(float t) {
        t = clamp(t, 0, 1);
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    public static float easeOutBack(float t) {
        t = clamp(t, 0, 1);
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        float u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }
}

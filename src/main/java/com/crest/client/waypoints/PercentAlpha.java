package com.crest.client.waypoints;

import java.util.function.IntSupplier;

/** Computes a 0-255 alpha from a percent (0-100). */
public final class PercentAlpha {
    private PercentAlpha() {}

    public static int alpha(int percent) {
        return Math.max(0, Math.min(255, (int) Math.round(percent / 100.0 * 255.0)));
    }

    public static int alphaFrom(IntSupplier percent) { return alpha(percent.getAsInt()); }
}

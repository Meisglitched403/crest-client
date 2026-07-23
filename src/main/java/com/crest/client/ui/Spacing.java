package com.crest.client.ui;

/** Tailwind-style spacing scale (multiples of 4px). */
public final class Spacing {
    private Spacing() {}

    public static final int S0 = 0;
    public static final int S1 = 4;
    public static final int S2 = 8;
    public static final int S3 = 12;
    public static final int S4 = 16;
    public static final int S5 = 20;
    public static final int S6 = 24;
    public static final int S7 = 28;
    public static final int S8 = 32;
    public static final int S9 = 36;
    public static final int S10 = 40;
    public static final int S11 = 44;
    public static final int S12 = 48;

    public static int scaled(int base) {
        return (int) (base * Theme.fontScale);
    }

    public static int densityAdjusted(int base) {
        float factor = switch (Theme.density) {
            case COMPACT -> 0.85f;
            case COMFORTABLE -> 1.25f;
            default -> 1.0f;
        };
        return (int) (base * factor * Theme.fontScale);
    }
}

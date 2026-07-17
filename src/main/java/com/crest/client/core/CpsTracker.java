package com.crest.client.core;

import net.minecraft.client.Minecraft;

/**
 * ponytail: Click counter fed from MouseInputMixin. Maintains a 1-second rolling
 * window per button (left=0, right=1) and decays via the client tick.
 */
public final class CpsTracker {
    private static final int[] count = new int[2];      // clicks in current window
    private static final int[] display = new int[2];    // last completed window value
    private static int tick;

    public static void onClick(int button) {
        if (button == 0 || button == 1) count[button]++;
    }

    // called from a TickEvent listener
    public static void tick() {
        tick++;
        if (tick >= 20) {
            display[0] = count[0];
            display[1] = count[1];
            count[0] = 0;
            count[1] = 0;
            tick = 0;
        }
    }

    public static int leftCps() { return display[0]; }
    public static int rightCps() { return display[1]; }
}

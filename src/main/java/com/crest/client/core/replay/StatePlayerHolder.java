package com.crest.client.core.replay;

import com.crest.client.core.StatePlayer;

public class StatePlayerHolder {
    public static volatile StatePlayer active;
    public static long startTimeMs;

    public static void start(StatePlayer sp) {
        active = sp;
        startTimeMs = System.currentTimeMillis();
        sp.reset();
    }

    public static void stop() {
        active = null;
        startTimeMs = 0;
    }

    public static boolean isPlaying() {
        return active != null;
    }

    public static long elapsedUs() {
        if (startTimeMs == 0) return 0;
        return (System.currentTimeMillis() - startTimeMs) * 1000L;
    }

    public static long durationUs() {
        if (active == null) return 0;
        return active.lastTimestampUs();
    }
}
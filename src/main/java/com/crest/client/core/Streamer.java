package com.crest.client.core;

import java.util.concurrent.atomic.AtomicBoolean;

public class Streamer {
    private static final AtomicBoolean streaming = new AtomicBoolean(false);
    private static long startTimeMs;
    private static volatile String lastError;

    public static boolean isStreaming() { return streaming.get(); }
    public static long getStartTime() { return startTimeMs; }
    public static int getCapturedFrames() { return 0; }
    public static int getDroppedFrames() { return 0; }
    public static String getLastError() { return lastError; }

    public static boolean isUrlAllowed(String url) {
        if (url == null) return false;
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return false;
        if (!trimmed.startsWith("rtmp://") && !trimmed.startsWith("rtmps://")) return false;
        if (trimmed.contains(" ") || trimmed.contains("\t") || trimmed.contains("\n")) return false;
        if (trimmed.indexOf("://") != trimmed.lastIndexOf("://")) return false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c >= 'a' && c <= 'z') continue;
            if (c >= 'A' && c <= 'Z') continue;
            if (c >= '0' && c <= '9') continue;
            if (c == '.' || c == '_' || c == '-' || c == '/' || c == ':') continue;
            return false;
        }
        return true;
    }

    public static void start(String url, int targetFps, int w, int h, int bitrate) {
        if (streaming.getAndSet(true)) return;
        lastError = null;

        try {
            FifoStreamer.startStream(url, targetFps, w, h, bitrate);
        } catch (Exception e) {
            lastError = e.getMessage();
            streaming.set(false);
            e.printStackTrace();
            return;
        }

        startTimeMs = System.currentTimeMillis();
    }

    public static void stop() {
        streaming.set(false);
        FifoStreamer.stop();
    }
}

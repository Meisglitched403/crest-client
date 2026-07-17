package com.crest.client.core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Streamer {
    private static final AtomicBoolean streaming = new AtomicBoolean(false);
    private static long startTimeMs;
    private static volatile String lastError;
    // ponytail: set true when streaming started with audio requested but we had to
    // fall back to no-audio because the audio device/backend was unavailable.
    private static volatile boolean audioFellBack;

    private static final AtomicLong totalCapturedFrames = new AtomicLong();
    private static final AtomicLong totalDroppedFrames = new AtomicLong();
    private static final AtomicLong totalEncodedFrames = new AtomicLong();
    private static volatile long lastSecondCaptured;
    private static volatile long lastSecondEncoded;

    public static boolean isStreaming() { return streaming.get(); }
    public static long getStartTime() { return startTimeMs; }
    public static String getLastError() { return lastError; }
    public static void setLastError(String e) { lastError = e; }
    public static boolean didAudioFallBack() { return audioFellBack; }
    public static void setAudioFellBack(boolean v) { audioFellBack = v; }

    public static long getCapturedFrames() { return totalCapturedFrames.get(); }
    public static long getDroppedFrames() { return totalDroppedFrames.get(); }
    public static long getEncodedFrames() { return totalEncodedFrames.get(); }

    public static void addCaptured() { totalCapturedFrames.incrementAndGet(); lastSecondCaptured = System.currentTimeMillis() / 1000; }
    public static void addDropped() { totalDroppedFrames.incrementAndGet(); }
    public static void addEncoded() { totalEncodedFrames.incrementAndGet(); lastSecondEncoded = System.currentTimeMillis() / 1000; }

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

    public static void start(String url, int targetFps, int w, int h, int bitrate,
                             String encoder, String preset, String audioDevice, double scaleFactor,
                             String recordOutput) {
        if (streaming.getAndSet(true)) return;
        lastError = null;
        audioFellBack = false;
        totalCapturedFrames.set(0);
        totalDroppedFrames.set(0);
        totalEncodedFrames.set(0);

        try {
            FifoStreamer.startStream(url, targetFps, w, h, bitrate, encoder, preset, audioDevice, scaleFactor, recordOutput);
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

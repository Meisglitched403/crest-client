package com.crest.client.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// ponytail: wf-recorder doesn't stream RTMP directly, so pipe to ffmpeg.
// Upgrade when wf-recorder adds --muxer support for direct RTMP output.
public class Streamer {
    private static final AtomicBoolean streaming = new AtomicBoolean(false);
    private static long startTimeMs;
    private static int capturedFrames;

    private static Process wfProcess, ffProcess;

    public static boolean isStreaming() { return streaming.get(); }
    public static long getStartTime() { return startTimeMs; }
    public static int getCapturedFrames() { return capturedFrames; }
    public static int getDroppedFrames() { return 0; }

    public static boolean isUrlAllowed(String url) {
        if (url == null) return false;
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return false;
        if (!trimmed.startsWith("rtmp://") && !trimmed.startsWith("rtmps://")) return false;
        if (trimmed.contains(" ") || trimmed.contains("\t") || trimmed.contains("\n")) return false;
        if (trimmed.indexOf("://") != trimmed.lastIndexOf("://")) return false;
        if (trimmed.matches(".*[^A-Za-z0-9.\\-_/:].*")) return false;
        return true;
    }

    public static void start(String url, int targetFps, int w, int h) {
        if (streaming.getAndSet(true)) return;
        capturedFrames = 0;

        try {
            ProcessBuilder wfpb = new ProcessBuilder(
                "wf-recorder",
                "--codec", "libx264",
                "--framerate", String.valueOf(targetFps),
                "-g", w + "x" + h + "+0+0",
                "--audio", "default",
                "-o", "-"
            );
            wfpb.redirectError(ProcessBuilder.Redirect.INHERIT);
            wfProcess = wfpb.start();

            ProcessBuilder ffpb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", "pipe:0",
                "-c:v", "copy",
                "-c:a", "aac",
                "-b:a", "128k",
                "-f", "flv", url
            );
            ffpb.redirectError(ProcessBuilder.Redirect.INHERIT);
            ffProcess = ffpb.start();
            new Thread(() -> {
                try { wfProcess.getInputStream().transferTo(ffProcess.getOutputStream()); } catch (Exception ignored) {}
            }).start();

            startTimeMs = System.currentTimeMillis();
        } catch (Exception e) {
            streaming.set(false);
            e.printStackTrace();
        }
    }

    public static void stop() {
        streaming.set(false);
        kill(ffProcess);
        kill(wfProcess);
        wfProcess = null;
        ffProcess = null;
    }

    private static void kill(Process p) {
        if (p == null || !p.isAlive()) return;
        p.destroy();
        try { p.waitFor(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (p.isAlive()) p.destroyForcibly();
    }
}
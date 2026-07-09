package com.crest.client.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Streamer {
    private static final AtomicBoolean streaming = new AtomicBoolean(false);
    private static long startTimeMs;
    private static int capturedFrames;
    private static volatile String lastError;

    private static Process wfProcess, ffProcess;

    public static boolean isStreaming() { return streaming.get(); }
    public static long getStartTime() { return startTimeMs; }
    public static int getCapturedFrames() { return capturedFrames; }
    public static int getDroppedFrames() { return 0; }
    public static String getLastError() { return lastError; }

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
        lastError = null;

        try {
            ProcessBuilder wfpb = new ProcessBuilder(
                "wf-recorder",
                "--codec", "rawvideo",
                "--framerate", String.valueOf(targetFps),
                "-g", w + "x" + h + "+0+0",
                "--audio", "default",
                "-o", "-"
            );
            wfpb.redirectError(ProcessBuilder.Redirect.PIPE);
            wfProcess = wfpb.start();

            ProcessBuilder ffpb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-f", "rawvideo",
                "-pix_fmt", "rgba",
                "-s", w + "x" + h,
                "-r", String.valueOf(targetFps),
                "-i", "pipe:0",
                "-f", "pulse",
                "-i", "default",
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-crf", "28",
                "-tune", "zerolatency",
                "-c:a", "aac",
                "-b:a", "128k",
                "-f", "flv", url
            );
            ffpb.redirectError(ProcessBuilder.Redirect.PIPE);
            ffProcess = ffpb.start();

            new Thread(() -> {
                try { wfProcess.getInputStream().transferTo(ffProcess.getOutputStream()); } catch (Exception ignored) {}
            }).start();

            captureWfErrors(wfProcess);
            captureFfErrors(ffProcess);

            startTimeMs = System.currentTimeMillis();
        } catch (Exception e) {
            lastError = e.getMessage();
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

    private static void captureWfErrors(Process p) {
        new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("error") || line.contains("Error") || line.contains("fail")) {
                        lastError = "wf-recorder: " + line;
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private static void captureFfErrors(Process p) {
        new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("error") || line.contains("Error")) {
                        lastError = "ffmpeg: " + line;
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }
}
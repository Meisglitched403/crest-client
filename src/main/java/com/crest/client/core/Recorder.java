package com.crest.client.core;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Recorder {
    private static final AtomicBoolean recording = new AtomicBoolean(false);
    private static long startTimeMs;
    private static Process wfProcess;
    private static volatile String currentFilePath;

    public static boolean isRecording() { return recording.get(); }
    public static long getStartTime() { return startTimeMs; }
    public static String getCurrentFilePath() { return currentFilePath; }

    public static void start(int fps) {
        if (recording.getAndSet(true)) return;

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w <= 0 || h <= 0) { recording.set(false); return; }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        File homeDir = new File(System.getProperty("user.home", "."));
        File videosDir = new File(homeDir, "Videos");
        if (!videosDir.exists()) videosDir.mkdirs();
        File outFile = new File(videosDir, "crest-recording-" + timestamp + ".mkv");

        try {
            currentFilePath = outFile.getCanonicalPath();
        } catch (Exception e) {
            currentFilePath = outFile.getAbsolutePath();
        }

        ProcessBuilder pb = new ProcessBuilder(
            "wf-recorder",
            "-f", currentFilePath,
            "-g", w + "x" + h + "+0+0",
            "--codec", "libx264",
            "--framerate", String.valueOf(fps),
            "--audio", "default"
        );
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.inheritIO();
        try {
            wfProcess = pb.start();
        } catch (Exception e) {
            e.printStackTrace();
            recording.set(false);
        }

        startTimeMs = System.currentTimeMillis();
        StateRecorder.start(currentFilePath);
    }

    public static void stop() {
        recording.set(false);
        StateRecorder.stop();
        if (wfProcess != null && wfProcess.isAlive()) {
            wfProcess.destroy();
            try { wfProcess.waitFor(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            if (wfProcess.isAlive()) wfProcess.destroyForcibly();
        }
        wfProcess = null;
    }
}
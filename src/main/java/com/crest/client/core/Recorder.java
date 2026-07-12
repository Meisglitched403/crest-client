package com.crest.client.core;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Recorder {
    private static final AtomicBoolean recording = new AtomicBoolean(false);
    private static long startTimeMs;
    private static volatile String currentFilePath;
    private static Process recorderProcess;
    private static boolean recordAudio = true;

    public static boolean isRecording() { return recording.get(); }
    public static long getStartTime() { return startTimeMs; }
    public static String getCurrentFilePath() { return currentFilePath; }
    public static void setRecordAudio(boolean enabled) { recordAudio = enabled; }

    public static void start(int fps) {
        if (recording.getAndSet(true)) return;

        String monitor = getFocusedMonitor();
        if (monitor == null) {
            recording.set(false);
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        File homeDir = new File(System.getProperty("user.home", "."));
        File videosDir = new File(homeDir, "Videos");
        if (!videosDir.exists()) videosDir.mkdirs();
        File outFile = new File(videosDir, "crest-recording-" + timestamp + ".mp4");

        try {
            currentFilePath = outFile.getCanonicalPath();
        } catch (Exception e) {
            currentFilePath = outFile.getAbsolutePath();
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("gpu-screen-recorder");
        cmd.add("-w"); cmd.add(monitor);
        cmd.add("-f"); cmd.add(String.valueOf(fps));
        cmd.add("-fm"); cmd.add("cfr");
        cmd.add("-k"); cmd.add("auto");
        cmd.add("-o"); cmd.add(currentFilePath);
        cmd.add("-c"); cmd.add("mp4");
        if (recordAudio) {
            cmd.add("-a"); cmd.add("default_output");
            cmd.add("-ac"); cmd.add("aac");
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            recorderProcess = pb.start();
        } catch (Exception e) {
            e.printStackTrace();
            recording.set(false);
            return;
        }

        startTimeMs = System.currentTimeMillis();
        StateRecorder.start(currentFilePath);
    }

    public static void stop() {
        recording.set(false);
        StateRecorder.stop();

        if (recorderProcess != null && recorderProcess.isAlive()) {
            long pid = recorderProcess.pid();
            try {
                Runtime.getRuntime().exec(new String[]{"kill", "-INT", String.valueOf(pid)});
                if (!recorderProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    recorderProcess.destroyForcibly();
                    recorderProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                }
            } catch (Exception ignored) {
                recorderProcess.destroyForcibly();
            }
            recorderProcess = null;
        }
    }

    private static String getFocusedMonitor() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "hyprctl monitors -j | jq -r '.[] | select(.focused == true) | .name'"});
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String name = r.readLine();
                if (name != null && !name.isEmpty()) return name;
            }
        } catch (Exception ignored) {}
        // fallback: first monitor from gpu-screen-recorder list
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"gpu-screen-recorder", "--list-monitors"});
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                if (line != null) {
                    int sep = line.indexOf('|');
                    if (sep > 0) return line.substring(0, sep);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}

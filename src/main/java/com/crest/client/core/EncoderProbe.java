package com.crest.client.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class EncoderProbe {
    private static final String[][] ENCODERS = {
        {"h264_nvenc",     "NVIDIA NVENC H.264"},
        {"hevc_nvenc",     "NVIDIA NVENC HEVC"},
        {"h264_amf",       "AMD AMF H.264"},
        {"hevc_amf",       "AMD AMF HEVC"},
        {"h264_videotoolbox", "Apple VideoToolbox H.264"},
        {"hevc_videotoolbox", "Apple VideoToolbox HEVC"},
        {"h264_vaapi",     "Linux VAAPI H.264"},
        {"hevc_vaapi",     "Linux VAAPI HEVC"},
        {"h264_qsv",       "Intel QSV H.264"},
        {"hevc_qsv",       "Intel QSV HEVC"},
        {"libx264",        "Software x264"},
        {"libx265",        "Software x265"},
    };

    private static final String[] PRESETS_SW = {"ultrafast", "superfast", "veryfast", "faster", "fast",
        "medium", "slow", "slower", "veryslow", "placebo"};
    private static final String[] PRESETS_HW = {"fast", "medium", "slow"};

    private static volatile String bestEncoder = "libx264";
    private static volatile String[] encoderList = {"libx264"};

    public static void probe() {
        List<String> found = new ArrayList<>();
        try {
            Process p = new ProcessBuilder("ffmpeg", "-hide_banner", "-encoders").start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    for (String[] enc : ENCODERS) {
                        if (line.contains(" " + enc[0] + " ")) {
                            if (!found.contains(enc[0])) found.add(enc[0]);
                        }
                    }
                }
            }
            p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        if (found.isEmpty()) found.add("libx264");

        encoderList = found.toArray(new String[0]);

        // Detect a usable VAAPI render device so we can hardware-encode on Intel/AMD Linux.
        if (found.contains("h264_vaapi") || found.contains("hevc_vaapi")) {
            for (String dev : new String[]{"/dev/dri/renderD128", "/dev/dri/renderD129", "/dev/dri/card0"}) {
                if (new java.io.File(dev).exists()) { vaapiDevice = dev; break; }
            }
        }

        // pick best: nvenc > amf > videotoolbox > qsv > libx264
        for (String[] enc : ENCODERS) {
            if (found.contains(enc[0])) {
                bestEncoder = enc[0];
                break;
            }
        }
    }

    public static String getBestEncoder() { return bestEncoder; }
    public static String[] getAvailableEncoders() { return encoderList; }

    public static String[] getPresets(String encoder) {
        if (encoder.startsWith("libx") || encoder.startsWith("software") || encoder.equals("libx264") || encoder.equals("libx265")) {
            return PRESETS_SW;
        }
        return PRESETS_HW;
    }

    public static int defaultPresetIndex(String encoder) {
        if (encoder.startsWith("libx")) return 0; // ultrafast
        return 1; // medium for HW
    }

    public static boolean isVaapi(String encoder) {
        return encoder.endsWith("_vaapi");
    }

    // Path to a VAAPI render device, or null if unavailable.
    private static String vaapiDevice;

    public static String getVaapiDevice() { return vaapiDevice; }

    public static boolean hasVaapiDevice() { return vaapiDevice != null; }

    // --- Audio backend probing ---

    private static String audioBackend = "pulse";
    private static String[] audioDevices = {"default"};

    static {
        probeAudio();
        probeVaapi();
    }

    private static void probeVaapi() {
        // Only probe if VAAPI encoders are available
        for (String[] enc : ENCODERS) {
            if (enc[0].endsWith("_vaapi")) {
                for (String dev : new String[]{"/dev/dri/renderD128", "/dev/dri/renderD129", "/dev/dri/card0"}) {
                    if (new java.io.File(dev).exists()) { vaapiDevice = dev; return; }
                }
                break;
            }
        }
    }

    private static void probeAudio() {
        // Check for pipewire or pulse
        try {
            Process p = new ProcessBuilder("pactl", "info").start();
            if (p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS) && p.exitValue() == 0) {
                audioBackend = "pulse";
            }
        } catch (Exception ignored) {}

        try {
            Process p = new ProcessBuilder("pw-cli", "info").start();
            if (p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS) && p.exitValue() == 0) {
                audioBackend = "pipewire";
            }
        } catch (Exception ignored) {}

        // Get default sink name
        if (audioBackend.equals("pulse")) {
            try {
                Process p = new ProcessBuilder("pactl", "get-default-sink").start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line = r.readLine();
                    if (line != null && !line.isBlank()) {
                        audioDevices = new String[]{line.trim(), "default"};
                    }
                }
                p.waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        } else if (audioBackend.equals("pipewire")) {
            try {
                Process p = new ProcessBuilder("pw-cli", "list-objects", "Node").start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (line.contains("node.name")) {
                            String name = line.split("=")[1].trim().replace("\"", "");
                            audioDevices = new String[]{name, "default"};
                            break;
                        }
                    }
                }
                p.waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }
    }

    public static String getAudioBackend() { return audioBackend; }
    public static String[] getAudioDevices() { return audioDevices; }
    
    // Returns the backend that actually works with ffmpeg
    public static String getWorkingAudioBackend() {
        // Try pipewire first, but fall back to pulse if pipewire format not supported
        if (audioBackend.equals("pipewire")) {
            // Check if ffmpeg supports the pipewire input format
            try {
                Process p = new ProcessBuilder("ffmpeg", "-hide_banner", "-formats").start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (line.contains("pipewire")) {
                            return "pipewire";
                        }
                    }
                }
                p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {}
            // Fall back to pulse
            return "pulse";
        }
        return audioBackend;
    }
}

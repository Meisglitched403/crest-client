package com.crest.client.core;

import com.crest.client.core.event.TickEvent;
import com.crest.client.core.setting.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class StreamerModule implements CrestModule {
    private final StringSetting rtmpUrl = new StringSetting("RTMP URL",
        "Twitch: rtmp://live.twitch.tv/app/STREAM_KEY YouTube: rtmp://a.rtmp.youtube.com/live2/STREAM_KEY",
        "rtmp://live.twitch.tv/app/");
    private final IntegerSetting bitrate = new IntegerSetting("Bitrate (kbps)", 500, 50000, 12000);
    private final IntegerSetting fps = new IntegerSetting("FPS", 15, 120, 60);
    private final ModeSetting scale = new ModeSetting("Resolution Scale",
        new String[]{"0.25x", "0.5x", "0.75x", "1.0x"}, 3);
    private final ModeSetting encoder = new ModeSetting("Encoder",
        "Probed at startup; shows available encoders", new String[]{"libx264"}, 0);
    private final ModeSetting encoderPreset = new ModeSetting("Encoder Preset",
        new String[]{"fast", "medium", "slow"}, 1);
    private final BooleanSetting audioEnabled = new BooleanSetting("Stream Audio", true);
    private final StringSetting audioDevice = new StringSetting("Audio Device",
        "PulseAudio sink name or 'default'. Leave empty for default.", "default");
    private final BooleanSetting recordWhileStreaming = new BooleanSetting("Record While Streaming", false);
    private final KeybindSetting streamKey = new KeybindSetting("Stream Toggle", GLFW.GLFW_KEY_F8);

    @Override
    public String getId() { return "streamer"; }
    @Override
    public String getName() { return "Live Streamer"; }
    @Override
    public String getDescription() { return "Stream gameplay to Twitch/YouTube via RTMP"; }
    @Override
    public String getCategory() { return "Misc"; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(rtmpUrl, bitrate, fps, scale, encoder, encoderPreset, audioEnabled, audioDevice, recordWhileStreaming, streamKey);
    }

    @Override
    public void onInitialize() {
        EncoderProbe.probe();
        String[] available = EncoderProbe.getAvailableEncoders();

        java.lang.reflect.Field modesField;
        try {
            modesField = ModeSetting.class.getDeclaredField("modes");
            modesField.setAccessible(true);
            modesField.set(encoder, available);
        } catch (Exception ignored) {}
        encoder.set(0); // best encoder is first

        String[] presets = EncoderProbe.getPresets(available[0]);
        try {
            modesField = ModeSetting.class.getDeclaredField("modes");
            modesField.setAccessible(true);
            modesField.set(encoderPreset, presets);
        } catch (Exception ignored) {}
        encoderPreset.set(EncoderProbe.defaultPresetIndex(available[0]));

        CrestModules.getEventBus().subscribe(TickEvent.class, this::onTick);
    }

    private void onTick(TickEvent event) {
        if (streamKey.wasPressed()) {
            toggleStreaming();
        }
    }

    public String getStreamUrl() { return rtmpUrl.get(); }
    public void setStreamUrl(String url) { rtmpUrl.set(url); }
    public int getBitrate() { return bitrate.get(); }
    public void setBitrate(int value) { bitrate.set(value); }
    public int getFps() { return fps.get(); }
    public double getScaleFactor() {
        String s = scale.getMode();
        return Double.parseDouble(s.substring(0, s.length() - 1));
    }
    public String getEncoder() { return encoder.getMode(); }
    public String getEncoderPreset() { return encoderPreset.getMode(); }
    public boolean isAudioEnabled() { return audioEnabled.get(); }
    public String getAudioDevice() { return audioDevice.get(); }
    public boolean isRecordWhileStreaming() { return recordWhileStreaming.get(); }

    public void toggleStreaming() {
        if (Streamer.isStreaming()) {
            Streamer.stop();
        } else {
            Minecraft mc = Minecraft.getInstance();
            int w = mc.getWindow().getWidth();
            int h = mc.getWindow().getHeight();
            if (w <= 0 || h <= 0) return;
            String url = rtmpUrl.get();
            if (url == null || url.isEmpty() || url.equals("rtmp://live.twitch.tv/app/")) return;
            if (!Streamer.isUrlAllowed(url)) return;
            String recPath = null;
            if (recordWhileStreaming.get()) {
                String dir = System.getProperty("user.home") + "/Videos/";
                try { new java.io.File(dir).mkdirs(); } catch (Exception ignored) {}
                recPath = dir + "crest-stream-" + java.time.Instant.now().toString()
                    .replace(":", "-").substring(0, 19) + ".mkv";
            }
Streamer.start(url, fps.get(), w, h, bitrate.get(),
    encoder.getMode(), encoderPreset.getMode(),
    audioEnabled.get() ? EncoderProbe.getAudioDevices()[0] : "none",
    getScaleFactor(), recPath);
        }
    }

    @Override
    public void onEnable() {}
    @Override
    public void onDisable() {
        if (Streamer.isStreaming()) Streamer.stop();
    }
}

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
    private final IntegerSetting bitrate = new IntegerSetting("Bitrate (kbps)", 500, 20000, 3500);
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
return List.of(rtmpUrl, bitrate, streamKey);
}

    @Override
    public void onInitialize() {
        CrestModules.getEventBus().subscribe(TickEvent.class, this::onTick);
    }

    private void onTick(TickEvent event) {
        if (streamKey.wasPressed()) {
            toggleStreaming();
        }
    }

    public String getStreamUrl() { return rtmpUrl.get(); }
    public void setStreamUrl(String url) { rtmpUrl.set(url); }

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
            Streamer.start(url, 30, w, h);
        }
    }

    @Override
    public void onEnable() {}
    @Override
    public void onDisable() {
        if (Streamer.isStreaming()) Streamer.stop();
    }
}

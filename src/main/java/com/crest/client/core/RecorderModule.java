package com.crest.client.core;

import com.crest.client.core.event.TickEvent;
import com.crest.client.core.setting.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class RecorderModule implements CrestModule {
    private final IntegerSetting fps = new IntegerSetting("FPS", 15, 120, 30);
    private final KeybindSetting recordKey = new KeybindSetting("Record Key", GLFW.GLFW_KEY_F6);
    private final BooleanSetting audioEnabled = new BooleanSetting("Record Audio", true);

    @Override
    public String getId() { return "recorder"; }
    @Override
    public String getName() { return "Screen Recorder"; }
    @Override
    public String getDescription() { return "Record gameplay via gpu-screen-recorder"; }
    @Override
    public String getCategory() { return "Misc"; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(fps, audioEnabled, recordKey);
    }

    @Override
    public void onInitialize() {
        CrestModules.getEventBus().subscribe(TickEvent.class, this::onTick);
    }

    private void onTick(TickEvent event) {
        Minecraft mc = event.getClient();
        if (mc.screen != null) return;

        if (recordKey.wasPressed()) {
            toggleRecording();
        }
    }

    public void toggleRecording() {
        if (Recorder.isRecording()) {
            Recorder.stop();
        } else {
            Recorder.setRecordAudio(audioEnabled.get());
            Recorder.start(fps.get());
        }
    }

    @Override
    public void onEnable() {}
    @Override
    public void onDisable() {
        if (Recorder.isRecording()) Recorder.stop();
    }
}

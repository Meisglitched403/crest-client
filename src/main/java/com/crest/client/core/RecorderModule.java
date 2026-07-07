package com.crest.client.core;

import com.crest.client.core.event.TickEvent;
import com.crest.client.core.setting.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class RecorderModule implements CrestModule {
    private final IntegerSetting fps = new IntegerSetting("FPS", 15, 120, 30);
    private final KeybindSetting recordKey = new KeybindSetting("Record Key", GLFW.GLFW_KEY_F6);
    private final KeybindSetting playerKey = new KeybindSetting("Player Key", GLFW.GLFW_KEY_F7);

    @Override
    public String getId() { return "recorder"; }
    @Override
    public String getName() { return "Screen Recorder"; }
    @Override
    public String getDescription() { return "Record gameplay to .crest format"; }
    @Override
    public String getCategory() { return "Misc"; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(fps, recordKey, playerKey);
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
        if (playerKey.wasPressed()) {
            openPlayer();
        }
    }

    public void toggleRecording() {
        if (Recorder.isRecording()) {
            Recorder.stop();
        } else {
            Recorder.start(fps.get());
        }
    }

    private void openPlayer() {
        String home = System.getProperty("user.home", ".");
        File dir = new File(home, "Videos");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".crest"));
        if (files == null || files.length == 0) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        Minecraft.getInstance().setScreen(new RecorderPlayer(files[0].getAbsolutePath()));
    }

    @Override
    public void onEnable() {}
    @Override
    public void onDisable() {
        if (Recorder.isRecording()) Recorder.stop();
    }
}

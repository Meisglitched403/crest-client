package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.KeybindSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: Stopwatch HUD. Toggle/reset via keybind; displays elapsed time.
 * Pure HUD + keybind, no Minecraft hooks.
 */
public class StopwatchModule extends HudModule {
    private static final int PAD = 2;
    private static final int LINE_H = 10;

    private final KeybindSetting toggleKey = new KeybindSetting("Toggle Key", GLFW.GLFW_KEY_H);
    private final BooleanSetting showMillis = new BooleanSetting("Show Milliseconds", true);

    private long startNanos = -1;
    private long frozenNanos = -1;
    private boolean running;

    public StopwatchModule() {
        super(4, 120);
    }

    @Override public String getId() { return "stopwatch"; }
    @Override public String getName() { return "Stopwatch"; }
    @Override public String getDescription() { return "Counts elapsed time; toggle/reset with a keybind."; }
    @Override public boolean isEnabled() { return false; }
    @Override public String getCategory() { return "Utility"; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>();
        s.add(toggleKey);
        s.add(showMillis);
        return s;
    }

    @Override
    public void onInitialize() {}

    private void onPress() {
        if (!running) {
            if (startNanos < 0 || frozenNanos >= 0) {
                startNanos = System.nanoTime() - (frozenNanos >= 0 ? frozenNanos : 0);
                frozenNanos = -1;
            }
            running = true;
        } else {
            frozenNanos = System.nanoTime() - startNanos;
            running = false;
        }
    }

    @Override
    public int getWidth() {
        return Minecraft.getInstance().font.width("00:00.000") + PAD * 2;
    }

    @Override
    public int getHeight() {
        return LINE_H + PAD * 2;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        if (toggleKey.wasPressed()) onPress();

        long elapsed = elapsedNanos();
        String txt = format(elapsed);

        int boxW = getRenderWidth();
        int boxH = getHeight();
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - boxW : x;
        int ry = y;
        HudBackground.draw(g, rx, ry, boxW, boxH);

        int color = running ? 0xFFFFFFFF : 0xFFAAAAAA;
        g.text(mc.font, Component.literal(txt), rx + PAD, ry + PAD, color);
    }

    private long elapsedNanos() {
        if (startNanos < 0) return 0;
        if (running) return System.nanoTime() - startNanos;
        return frozenNanos >= 0 ? frozenNanos : 0;
    }

    private String format(long nanos) {
        long totalMs = nanos / 1_000_000;
        long ms = totalMs % 1000;
        long totalSec = totalMs / 1000;
        long s = totalSec % 60;
        long m = totalSec / 60;
        if (showMillis.get()) {
            return String.format("%02d:%02d.%03d", m, s, ms);
        }
        return String.format("%02d:%02d", m, s);
    }
}

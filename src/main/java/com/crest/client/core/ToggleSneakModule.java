package com.crest.client.core;

import com.crest.client.core.event.TickEvent;
import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: Toggle Sneak. Lets sneak work as a hold (vanilla) or a toggle.
 * In toggle mode a sneak-key press flips the sneaking state; we drive
 * LocalPlayer.setShiftKeyDown ourselves and suppress the vanilla hold handling.
 */
public class ToggleSneakModule implements CrestModule {
    private final ModeSetting mode = new ModeSetting("Mode", new String[]{"Hold", "Toggle"}, 0);
    private final BooleanSetting startSneaking = new BooleanSetting("Start Sneaking", false);

    private boolean sneaking;
    private boolean prevKey;

    public ToggleSneakModule() {
        sneaking = startSneaking.get();
    }

    @Override public String getId() { return "toggle_sneak"; }
    @Override public String getName() { return "Toggle Sneak"; }
    @Override public String getDescription() { return "Use sneak as hold or toggle."; }
    @Override public String getCategory() { return "Utility"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>();
        s.add(mode);
        s.add(startSneaking);
        return s;
    }

    @Override
    public void onInitialize() {
        CrestModules.getEventBus().subscribe(TickEvent.class, this::onTick);
    }

    public static boolean isToggleMode() {
        var m = CrestModules.get("toggle_sneak");
        return m instanceof ToggleSneakModule mod && CrestModules.isEnabled("toggle_sneak")
                && mod.mode.get() == 1;
    }

    public static boolean isSneaking() {
        var m = CrestModules.get("toggle_sneak");
        return m instanceof ToggleSneakModule mod && mod.sneaking;
    }

    private void onTick(TickEvent e) {
        if (!CrestModules.isEnabled("toggle_sneak")) {
            prevKey = false;
            return;
        }
        Minecraft mc = e.getClient();
        if (mc.player == null) {
            prevKey = false;
            return;
        }
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0) return;
        boolean key = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean pressed = key && !prevKey;
        prevKey = key;

        if (mode.get() == 1) { // Toggle
            if (pressed) {
                sneaking = !sneaking;
            }
            mc.player.setShiftKeyDown(sneaking);
        }
        // Hold mode: leave shift entirely to vanilla.
    }
}

package com.crest.client.waypoints;

import com.crest.client.core.KeybindManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** Holds wWaypoints keybind codes and registers them with Crest's KeybindManager. */
public final class ModKeybindings {
    public int createWaypoint = GLFW.GLFW_KEY_B;
    public int openWaypointMenu = GLFW.GLFW_KEY_N;
    public int openWaypointSettings = GLFW.GLFW_KEY_G;
    public int toggleSneak = GLFW.GLFW_KEY_Z;
    public int hideWaypoints = GLFW.GLFW_KEY_H;
    public int cycleFolder = GLFW.GLFW_KEY_F;
    public int clearUnlocked = GLFW.GLFW_KEY_K;
    public int signGuiPopupToggle = GLFW.GLFW_KEY_J;
    public final Map<String, Integer> presetCreateKeys = new HashMap<>();

    public static ModKeybindings createDefault() { return new ModKeybindings(); }

    public void registerActions(WaypointManager mgr, ModConfig cfg,
                                 Runnable openMenu, Runnable openSettings) {
        KeybindManager.registerAction(createWaypoint, () -> {
            Minecraft mc = Minecraft.getInstance();
            QuickWaypointController.createHere(mc, mgr, cfg, cfg.activeLabelPresetId);
        });
        KeybindManager.registerAction(openWaypointMenu, openMenu);
        KeybindManager.registerAction(openWaypointSettings, openSettings);
        KeybindManager.registerAction(toggleSneak, () -> ToggleSneakService.toggle(Minecraft.getInstance()));
        KeybindManager.registerAction(hideWaypoints, () -> {
            int[] thresholds = cfg.hideWaypointCycleThresholds != null ? cfg.hideWaypointCycleThresholds : new int[] { 0 };
            cfg.hideThresholdIndex = HideWaypointCycle.nextIndex(thresholds, cfg.hideThresholdIndex);
            cfg.activeHideThreshold = HideWaypointCycle.activeThreshold(thresholds, cfg.hideThresholdIndex);
        });
        KeybindManager.registerAction(cycleFolder, () -> FolderNavigationKeybindService.cycle(mgr, cfg));
        KeybindManager.registerAction(clearUnlocked, () -> mgr.clearUnlocked());
        KeybindManager.registerAction(signGuiPopupToggle, () -> {});
    }
}

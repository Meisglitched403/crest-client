package com.crest.client.core;

import com.crest.client.core.setting.KeybindSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeybindManager {
    private static final Map<Integer, Boolean> wasDown = new HashMap<>();
    private static final Map<Integer, Runnable> actionKeybinds = new HashMap<>();
    private static int clickGuiKey = GLFW.GLFW_KEY_GRAVE_ACCENT;

    // Cached list of (GLFW key, module id) pairs — rebuilt only on register.
    private static final List<KeybindEntry> keybindEntries = new ArrayList<>();
    private static boolean entriesDirty = true;

    private record KeybindEntry(int key, String moduleId) {}

    public static void setClickGuiKey(int key) {
        clickGuiKey = key;
    }

    public static int getClickGuiKey() { return clickGuiKey; }

    public static void registerAction(int key, Runnable action) {
        actionKeybinds.put(key, action);
    }

    public static void unregisterAction(int key) {
        actionKeybinds.remove(key);
    }

    public static void markDirty() {
        entriesDirty = true;
    }

    public static void processTick() {
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0) return;

        rebuildCache();

        Minecraft mc = Minecraft.getInstance();
        boolean inWorld = mc.screen == null;

        if (inWorld) {
            for (KeybindEntry e : keybindEntries) {
                checkKey(e.key, () -> CrestModules.toggle(e.moduleId));
            }
        }

        checkKey(clickGuiKey, () -> {
            if (mc.screen instanceof CrestMenu) {
                mc.screen.onClose();
            } else if (mc.screen == null) {
                mc.setScreen(new CrestMenu());
            }
        });

        // Action keybinds (open screens, toggles) must not run while the user is
        // typing into a text field, otherwise keys like M get eaten instead of
        // inserted into the focused input. We still track the key's up/down edge
        // every tick so the action can't fire retroactively when focus leaves.
        boolean typing = mc.screen instanceof ChatScreen
            || (mc.screen instanceof TextCapturingScreen && ((TextCapturingScreen) mc.screen).isCapturingText());
        for (var entry : actionKeybinds.entrySet()) {
            int key = entry.getKey();
            boolean pressed = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            boolean prev = wasDown.getOrDefault(key, false);
            if (pressed && !prev && !typing) {
                entry.getValue().run();
            }
            wasDown.put(key, pressed);
        }
    }

    private static void rebuildCache() {
        if (!entriesDirty) return;
        keybindEntries.clear();
        Map<Integer, String> seen = new HashMap<>();
        for (CrestModule mod : CrestModules.getAll().values()) {
            if (mod.selfHandlesKeybinds()) continue;
            for (Setting<?> s : mod.getSettings()) {
                if (s instanceof KeybindSetting ks && ks.get() != GLFW.GLFW_KEY_UNKNOWN) {
                    int key = ks.get();
                    String prev = seen.put(key, mod.getId());
                    if (prev != null && !prev.equals(mod.getId())) {
                        String name = "key#" + key;
                        String warn = "[Crest] Keybind conflict on '" + name + "' (" + prev + " / " + mod.getId() + ")";
                        System.err.println(warn);
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(
                                    net.minecraft.network.chat.Component.literal(warn));
                        }
                    }
                    keybindEntries.add(new KeybindEntry(key, mod.getId()));
                }
            }
        }
        entriesDirty = false;
    }

    private static void checkKey(int key, Runnable action) {
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0) return;
        boolean pressed = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
        boolean prev = wasDown.getOrDefault(key, false);
        if (pressed && !prev) {
            action.run();
        }
        wasDown.put(key, pressed);
    }
}
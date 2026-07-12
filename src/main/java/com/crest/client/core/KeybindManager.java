package com.crest.client.core;

import com.crest.client.core.setting.KeybindSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.Minecraft;
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

        for (KeybindEntry e : keybindEntries) {
            checkKey(e.key, () -> CrestModules.toggle(e.moduleId));
        }

        checkKey(clickGuiKey, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof CrestMenu) {
                mc.screen.onClose();
            } else if (mc.screen == null) {
                mc.setScreen(new CrestMenu());
            }
        });

        for (var entry : actionKeybinds.entrySet()) {
            checkKey(entry.getKey(), entry.getValue());
        }
    }

    private static void rebuildCache() {
        if (!entriesDirty) return;
        keybindEntries.clear();
        for (CrestModule mod : CrestModules.getAll().values()) {
            for (Setting<?> s : mod.getSettings()) {
                if (s instanceof KeybindSetting ks && ks.get() != GLFW.GLFW_KEY_UNKNOWN) {
                    keybindEntries.add(new KeybindEntry(ks.get(), mod.getId()));
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
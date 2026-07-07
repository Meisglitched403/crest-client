package com.crest.client.core;

import com.crest.client.core.setting.KeybindSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

public class KeybindManager {
    private static final Map<Integer, Boolean> wasDown = new HashMap<>();
    private static final Map<Integer, Runnable> actionKeybinds = new HashMap<>();
    private static int clickGuiKey = GLFW.GLFW_KEY_GRAVE_ACCENT;

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

    public static void processTick() {
        long window = glfwGetCurrentContext();
        if (window == 0) return;

        // Process module toggle keybinds from KeybindSetting in each module's settings
        for (CrestModule mod : CrestModules.getAll().values()) {
            for (Setting<?> s : mod.getSettings()) {
                if (s instanceof KeybindSetting ks) {
                    checkKey(ks.get(), () -> CrestModules.toggle(mod.getId()));
                }
            }
        }

        // Process ClickGUI key
        checkKey(clickGuiKey, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof CrestClickGui) {
                mc.screen.onClose();
            } else if (mc.screen == null) {
                mc.setScreen(new CrestClickGui());
            }
        });

        // Process registered action keybinds
        for (Map.Entry<Integer, Runnable> entry : actionKeybinds.entrySet()) {
            checkKey(entry.getKey(), entry.getValue());
        }
    }

    private static void checkKey(int key, Runnable action) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return;
        long window = glfwGetCurrentContext();
        if (window == 0) return;
        boolean pressed = glfwGetKey(window, key) == GLFW.GLFW_PRESS;
        boolean prev = wasDown.getOrDefault(key, false);
        if (pressed && !prev) {
            action.run();
        }
        wasDown.put(key, pressed);
    }
}

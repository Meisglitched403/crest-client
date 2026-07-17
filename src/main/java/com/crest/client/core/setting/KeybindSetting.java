package com.crest.client.core.setting;

import com.crest.client.core.ConfigManager;
import org.lwjgl.glfw.GLFW;

public class KeybindSetting extends Setting<Integer> {
    private boolean wasDown;

    public KeybindSetting(String name, int defaultKey) {
        this(name, "", defaultKey);
    }

    public KeybindSetting(String name, String description, int defaultKey) {
        super(name, description, defaultKey);
    }

    public String getKeyName() {
        int key = get();
        if (key == GLFW.GLFW_KEY_UNKNOWN || key < 0) return "None";
        String name = GLFW.glfwGetKeyName(key, 0);
        return name != null ? name.toUpperCase() : "KEY_" + key;
    }

    public boolean isPressed() {
        int key = get();
        if (key == GLFW.GLFW_KEY_UNKNOWN || key < 0) return false;
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0) return false;
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    public boolean wasPressed() {
        boolean pressed = isPressed();
        boolean result = pressed && !wasDown;
        wasDown = pressed;
        return result;
    }

    @Override
    public void load(ConfigManager config, String moduleId) {
        if (config.has(moduleId, getName())) {
            set(config.getInt(moduleId, getName()));
        }
    }

    @Override
    public void save(ConfigManager config, String moduleId) {
        config.set(moduleId, getName(), get());
    }
}

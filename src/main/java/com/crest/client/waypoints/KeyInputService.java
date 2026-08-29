package com.crest.client.waypoints;

import org.lwjgl.glfw.GLFW;

/** Key name / resolution helpers (GLFW-based, matching Crest's KeybindManager). */
public final class KeyInputService {
    private KeyInputService() {}

    public static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "NONE";
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name == null) name = GLFW.glfwGetKeyName(GLFW.GLFW_KEY_UNKNOWN, 0);
        return name != null ? name.toUpperCase() : ("KEY_" + key);
    }

    public static int resolve(int keyCode) { return keyCode; }
}

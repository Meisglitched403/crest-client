package com.crest.client.core;

import com.crest.client.core.setting.FloatSetting;
import com.crest.client.core.setting.KeybindSetting;
import com.crest.client.core.setting.Setting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWScrollCallbackI;

import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

public class ZoomModule implements CrestModule {
    public static final double SMOOTH_SPEED = 0.15;
    public static final double DEFAULT_ZOOM_MULTIPLIER = 0.25;

    public static double currentFov;
    public static double targetFov;
    public static double originalFov;
    public static boolean initialized;

    private static boolean wasKeyDown;
    private static double zoomMultiplier = DEFAULT_ZOOM_MULTIPLIER;
    private static long cachedWindow;
    private static GLFWScrollCallbackI prevScrollCallback;

    private final FloatSetting multiplierSetting = new FloatSetting(
        "Zoom Multiplier", 0.05f, 1.0f, (float) DEFAULT_ZOOM_MULTIPLIER
    );
    private final KeybindSetting zoomKeySetting = new KeybindSetting(
        "Zoom Key", GLFW.GLFW_KEY_Z
    );

    @Override
    public String getId() { return "zoom"; }
    @Override
    public String getName() { return "Zoom"; }
    @Override
    public String getDescription() { return "Smooth zoom on Z key; scroll to adjust"; }
    @Override
    public String getCategory() { return "Visual"; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(multiplierSetting, zoomKeySetting);
    }

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!CrestModules.isEnabled("zoom")) return;

            long window = glfwGetCurrentContext();
            if (window == 0) return;

            if (!initialized) {
                originalFov = client.options.fov().get();
                zoomMultiplier = multiplierSetting.get();
                currentFov = originalFov;
                targetFov = originalFov;
                cachedWindow = window;
                initialized = true;

                prevScrollCallback = GLFW.glfwSetScrollCallback(window, (w, xOffset, yOffset) -> {
                    if (CrestModules.isEnabled("zoom") && wasKeyDown) {
                        zoomMultiplier = Math.max(0.05, Math.min(1.0, zoomMultiplier - yOffset * 0.05));
                        targetFov = Math.max(1.0, originalFov * zoomMultiplier);
                        multiplierSetting.set((float) zoomMultiplier);
                        CrestModules.getConfigManager().markDirty();
                    }
                    if (prevScrollCallback != null) {
                        prevScrollCallback.invoke(w, xOffset, yOffset);
                    }
                });
            }

            int zoomKey = zoomKeySetting.get();
            boolean isDown = glfwGetKey(window, zoomKey) == GLFW.GLFW_PRESS;
            if (isDown && !wasKeyDown) targetFov = Math.max(1.0, originalFov * zoomMultiplier);
            else if (!isDown && wasKeyDown) targetFov = originalFov;
            wasKeyDown = isDown;
        });
    }

    public static boolean isZooming() {
        return wasKeyDown;
    }

    @Override
    public void onDisable() {
        if (initialized) {
            currentFov = originalFov;
            targetFov = originalFov;
            wasKeyDown = false;
            if (cachedWindow != 0 && prevScrollCallback != null) {
                GLFW.glfwSetScrollCallback(cachedWindow, prevScrollCallback);
                prevScrollCallback = null;
            }
        }
    }
}

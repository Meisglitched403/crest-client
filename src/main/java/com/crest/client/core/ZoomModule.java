package com.crest.client.core;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWScrollCallbackI;

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
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!CrestModules.isEnabled("zoom")) return;

            long window = glfwGetCurrentContext();
            if (window == 0) return;

            if (!initialized) {
                originalFov = client.options.fov().get();
                zoomMultiplier = HudSettings.getInt("zoom", "multiplier", (int) (DEFAULT_ZOOM_MULTIPLIER * 100)) / 100.0;
                currentFov = originalFov;
                targetFov = originalFov;
                cachedWindow = window;
                initialized = true;

                prevScrollCallback = GLFW.glfwSetScrollCallback(window, (w, xOffset, yOffset) -> {
                    if (CrestModules.isEnabled("zoom") && wasKeyDown) {
                        zoomMultiplier = Math.max(0.05, Math.min(1.0, zoomMultiplier - yOffset * 0.05));
                        targetFov = Math.max(1.0, originalFov * zoomMultiplier);
                        HudSettings.setInt("zoom", "multiplier", (int) (zoomMultiplier * 100));
                    }
                    if (prevScrollCallback != null) {
                        prevScrollCallback.invoke(w, xOffset, yOffset);
                    }
                });
            }

            boolean isDown = glfwGetKey(window, GLFW.GLFW_KEY_Z) == GLFW.GLFW_PRESS;
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

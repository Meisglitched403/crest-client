package com.crest.client.core;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

public class ZoomModule implements CrestModule {
    public static final double ZOOM_FOV = 30.0;
    public static final double SMOOTH_SPEED = 0.15;

    // Static state read by GameRendererMixin every frame
    public static double currentFov;
    public static double targetFov;
    public static double originalFov;
    public static boolean initialized;

    private static boolean wasKeyDown;

    @Override
    public String getId() { return "zoom"; }
    @Override
    public String getName() { return "Zoom"; }
    @Override
    public String getDescription() { return "Smooth zoom on Z key"; }
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
                currentFov = originalFov;
                targetFov = originalFov;
                initialized = true;
            }

            boolean isDown = glfwGetKey(window, GLFW.GLFW_KEY_Z) == GLFW.GLFW_PRESS;
            if (isDown && !wasKeyDown) targetFov = ZOOM_FOV;
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
        }
    }
}

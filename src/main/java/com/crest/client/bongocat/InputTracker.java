package com.crest.client.bongocat;

import com.crest.client.core.CrestModules;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

/**
 * ponytail: Polls GLFW for the keys the user cares about (WASD + Shift + Ctrl)
 * and the mouse cursor position. The right paw follows the cursor, so we track
 * its position (gui-scaled) and whether it moved recently.
 */
public class InputTracker {
    private static InputTracker instance;

    // Only WASD + Shift + Ctrl drive the left paw (user request).
    private static final int[] TRACKED_KEYS = {
        GLFW_KEY_W, GLFW_KEY_A, GLFW_KEY_S, GLFW_KEY_D,
        GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT, GLFW_KEY_LEFT_CONTROL
    };

    private final boolean[] keyStates = new boolean[512];
    private boolean initialized;

    // cursor (gui scaled) + movement tracking
    private int cursorX, cursorY;
    private int lastCursorX, lastCursorY;
    private long lastMoveMs;

    public static InputTracker getInstance() {
        if (instance == null) instance = new InputTracker();
        return instance;
    }

    public boolean tryInit() {
        if (initialized) return true;
        initialized = true;
        return true;
    }

    public void update() {
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0) return;

        Minecraft mc = Minecraft.getInstance();
        for (int k : TRACKED_KEYS) {
            keyStates[k] = GLFW.glfwGetKey(window, k) == GLFW_PRESS;
        }

        if (mc.getWindow() != null) {
            java.nio.DoubleBuffer bx = org.lwjgl.BufferUtils.createDoubleBuffer(1);
            java.nio.DoubleBuffer by = org.lwjgl.BufferUtils.createDoubleBuffer(1);
            GLFW.glfwGetCursorPos(window, bx, by);
            double px = bx.get(0);
            double py = by.get(0);
            int scale = mc.getWindow().getGuiScale();
            lastCursorX = cursorX; lastCursorY = cursorY;
            cursorX = (int) (px / scale);
            cursorY = (int) (py / scale);
            if (Math.abs(cursorX - lastCursorX) > 1 || Math.abs(cursorY - lastCursorY) > 1) {
                lastMoveMs = System.currentTimeMillis();
            }
        }
    }

    public boolean anyWasdOrMod() {
        for (int k : TRACKED_KEYS) if (keyStates[k]) return true;
        return false;
    }

    public boolean mouseMovedRecently() {
        return System.currentTimeMillis() - lastMoveMs < 250;
    }

    public int getCursorX() { return cursorX; }
    public int getCursorY() { return cursorY; }
}

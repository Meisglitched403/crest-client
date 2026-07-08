package com.crest.client.bongocat;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_3;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_5;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_6;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_7;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_8;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_9;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_C;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_G;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Q;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_T;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_V;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_X;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Z;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;

public class InputTracker {
    private static InputTracker instance;

    private final SpringPaw leftPaw = new SpringPaw();
    private final SpringPaw rightPaw = new SpringPaw();
    // Only the keys actually rendered/needed are tracked, to avoid scanning the
    // entire GLFW key range every frame (privacy + efficiency).
    private static final int[] TRACKED_KEYS = {
        GLFW_KEY_1, GLFW_KEY_2, GLFW_KEY_3, GLFW_KEY_4, GLFW_KEY_5, GLFW_KEY_6,
        GLFW_KEY_7, GLFW_KEY_8, GLFW_KEY_9,
        GLFW_KEY_Q, GLFW_KEY_W, GLFW_KEY_E, GLFW_KEY_R, GLFW_KEY_T,
        GLFW_KEY_A, GLFW_KEY_S, GLFW_KEY_D, GLFW_KEY_F, GLFW_KEY_G,
        GLFW_KEY_Z, GLFW_KEY_X, GLFW_KEY_C, GLFW_KEY_V, GLFW_KEY_B,
        GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT, GLFW_KEY_LEFT_CONTROL, GLFW_KEY_SPACE
    };
    // Keys considered "left hand" for paw animation.
    private static final boolean[] IS_LEFT_KEY = new boolean[512];

    static {
        int[] left = {
            GLFW_KEY_1, GLFW_KEY_2, GLFW_KEY_3, GLFW_KEY_4, GLFW_KEY_5, GLFW_KEY_6,
            GLFW_KEY_Q, GLFW_KEY_W, GLFW_KEY_E, GLFW_KEY_R, GLFW_KEY_T,
            GLFW_KEY_A, GLFW_KEY_S, GLFW_KEY_D, GLFW_KEY_F, GLFW_KEY_G,
            GLFW_KEY_Z, GLFW_KEY_X, GLFW_KEY_C, GLFW_KEY_V, GLFW_KEY_B
        };
        for (int k : left) IS_LEFT_KEY[k] = true;
    }

    private final boolean[] keyStates = new boolean[512];
    private boolean lmb, rmb;
    private long window;
    private long lastFrameTime;
    private boolean initialized;

    public static InputTracker getInstance() {
        if (instance == null) instance = new InputTracker();
        return instance;
    }

    public boolean tryInit() {
        if (initialized) return true;
        window = glfwGetCurrentContext();
        if (window == 0) return false;
        lastFrameTime = System.currentTimeMillis();
        initialized = true;
        return true;
    }

    public void update() {
        if (!initialized) return;

        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastFrameTime) / 1000.0f, 0.05f);
        lastFrameTime = now;

        for (int k : TRACKED_KEYS) {
            keyStates[k] = glfwGetKey(window, k) == GLFW_PRESS;
        }

        lmb = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        rmb = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS;

        boolean leftHeld = lmb || anyLeftKeyPressed();
        boolean rightHeld = rmb || anyRightKeyPressed();

        leftPaw.setPressed(leftHeld);
        rightPaw.setPressed(rightHeld);

        leftPaw.update(dt);
        rightPaw.update(dt);
    }

    private boolean anyLeftKeyPressed() {
        for (int k : TRACKED_KEYS) {
            if (keyStates[k] && IS_LEFT_KEY[k]) return true;
        }
        return false;
    }

    private boolean anyRightKeyPressed() {
        for (int k : TRACKED_KEYS) {
            if (keyStates[k] && !IS_LEFT_KEY[k]) return true;
        }
        return false;
    }

    public boolean[] getKeyStates() {
        // Return a defensive copy so callers cannot mutate internal state.
        return keyStates.clone();
    }

    public SpringPaw getLeftPaw() {
        return leftPaw;
    }

    public SpringPaw getRightPaw() {
        return rightPaw;
    }
}

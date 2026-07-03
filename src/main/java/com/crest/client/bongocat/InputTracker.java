package com.crest.client.bongocat;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_0;
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
import static org.lwjgl.glfw.GLFW.GLFW_KEY_H;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_I;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_J;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_K;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_L;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_M;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_N;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_O;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_P;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Q;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_T;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_U;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_V;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_X;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Y;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Z;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_2;
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
    private final boolean[] keyStates = new boolean[GLFW_KEY_LAST];
    private long window;
    private long lastFrameTime;
    private boolean initialized;

    private static final boolean[] IS_LEFT_KEY = new boolean[GLFW_KEY_LAST];

    static {
        IS_LEFT_KEY[GLFW_KEY_1] = true;  IS_LEFT_KEY[GLFW_KEY_2] = true;
        IS_LEFT_KEY[GLFW_KEY_3] = true;  IS_LEFT_KEY[GLFW_KEY_4] = true;
        IS_LEFT_KEY[GLFW_KEY_5] = true;  IS_LEFT_KEY[GLFW_KEY_6] = true;
        IS_LEFT_KEY[GLFW_KEY_Q] = true;  IS_LEFT_KEY[GLFW_KEY_W] = true;
        IS_LEFT_KEY[GLFW_KEY_E] = true;  IS_LEFT_KEY[GLFW_KEY_R] = true;
        IS_LEFT_KEY[GLFW_KEY_T] = true;
        IS_LEFT_KEY[GLFW_KEY_A] = true;  IS_LEFT_KEY[GLFW_KEY_S] = true;
        IS_LEFT_KEY[GLFW_KEY_D] = true;  IS_LEFT_KEY[GLFW_KEY_F] = true;
        IS_LEFT_KEY[GLFW_KEY_G] = true;
        IS_LEFT_KEY[GLFW_KEY_Z] = true;  IS_LEFT_KEY[GLFW_KEY_X] = true;
        IS_LEFT_KEY[GLFW_KEY_C] = true;  IS_LEFT_KEY[GLFW_KEY_V] = true;
        IS_LEFT_KEY[GLFW_KEY_B] = true;
    }

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

        for (int i = 0; i < GLFW_KEY_LAST; i++) {
            keyStates[i] = glfwGetKey(window, i) == GLFW_PRESS;
        }

        boolean lmb = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        boolean rmb = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS;

        boolean leftHeld = lmb || anyLeftKeyPressed();
        boolean rightHeld = rmb || anyRightKeyPressed();

        leftPaw.setPressed(leftHeld);
        rightPaw.setPressed(rightHeld);

        leftPaw.update(dt);
        rightPaw.update(dt);
    }

    private boolean anyLeftKeyPressed() {
        for (int i = 0; i < GLFW_KEY_LAST; i++) {
            if (keyStates[i] && i != GLFW_KEY_RIGHT_SHIFT && IS_LEFT_KEY[i]) return true;
        }
        return false;
    }

    private boolean anyRightKeyPressed() {
        for (int i = 0; i < GLFW_KEY_LAST; i++) {
            if (keyStates[i] && i != GLFW_KEY_RIGHT_SHIFT && !IS_LEFT_KEY[i]) return true;
        }
        return false;
    }

    public boolean[] getKeyStates() {
        return keyStates;
    }

    public SpringPaw getLeftPaw() {
        return leftPaw;
    }

    public SpringPaw getRightPaw() {
        return rightPaw;
    }
}

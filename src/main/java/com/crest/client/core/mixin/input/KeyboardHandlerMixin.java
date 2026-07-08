package com.crest.client.core.mixin.input;

import com.crest.client.core.StateRecorder;
import net.minecraft.client.KeyboardHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// ponytail: keys polled via GLFW since KeyboardHandler refactored to lambdas in
// this MC version. Upgrade to proper event callback when MC version stabilises.
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    private static boolean[] prevKeys = new boolean[256];

    @Inject(method = "tick", at = @At("HEAD"))
    private void crest$pollKeys(CallbackInfo ci) {
        if (!StateRecorder.isActive()) return;
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0) return;

        for (int k = 32; k < 256; k++) {
            boolean pressed = GLFW.glfwGetKey(window, k) == GLFW.GLFW_PRESS;
            if (pressed != prevKeys[k]) {
                prevKeys[k] = pressed;
                StateRecorder.onKey(k, 0, pressed ? 1 : 0, 0);
            }
        }
    }
}
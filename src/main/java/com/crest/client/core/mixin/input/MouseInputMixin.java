package com.crest.client.core.mixin.input;

import com.crest.client.core.StateRecorder;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseInputMixin {
    private static int prevButtons;

    @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"))
    private void crest$pollMouse(CallbackInfo ci) {
        if (!StateRecorder.isActive()) return;
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0) return;

        int buttons = 0;
        for (int b = GLFW.GLFW_MOUSE_BUTTON_1; b <= GLFW.GLFW_MOUSE_BUTTON_LAST; b++) {
            if (GLFW.glfwGetMouseButton(window, b) == GLFW.GLFW_PRESS) buttons |= (1 << b);
        }
        int changed = prevButtons ^ buttons;
        if (changed != 0) {
            for (int b = 0; b <= GLFW.GLFW_MOUSE_BUTTON_LAST; b++) {
                if ((changed & (1 << b)) != 0) {
                    int action = (buttons & (1 << b)) != 0 ? 1 : 0;
                    StateRecorder.onMouseButton(b, action, 0);
                }
            }
            prevButtons = buttons;
        }
    }
}
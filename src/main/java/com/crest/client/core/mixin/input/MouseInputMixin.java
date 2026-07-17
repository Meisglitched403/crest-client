package com.crest.client.core.mixin.input;

import com.crest.client.core.CpsTracker;
import com.crest.client.core.HudClickBus;
import com.crest.client.core.StateRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(MouseHandler.class)
public class MouseInputMixin {
    private static int prevButtons;
    private static Field xposField, yposField;

    @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"))
    private void crest$pollMouse(CallbackInfo ci) {
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
                    boolean pressed = action == 1;
                    if (pressed) {
                        CpsTracker.onClick(b);
                        if (b == GLFW.GLFW_MOUSE_BUTTON_1) dispatchHudClick();
                    }
                    if (StateRecorder.isActive()) StateRecorder.onMouseButton(b, action, 0);
                }
            }
            prevButtons = buttons;
        }
    }

    private static void dispatchHudClick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null || mc.screen != null) return; // only in-world, no screen open
        try {
            if (xposField == null) {
                xposField = MouseHandler.class.getDeclaredField("xpos");
                yposField = MouseHandler.class.getDeclaredField("ypos");
                xposField.setAccessible(true);
                yposField.setAccessible(true);
            }
            double px = xposField.getDouble(mc.mouseHandler);
            double py = yposField.getDouble(mc.mouseHandler);
            int scale = mc.getWindow().getGuiScale();
            int mx = (int) (px / scale);
            int my = (int) (py / scale);
            HudClickBus.dispatch(mx, my, GLFW.GLFW_MOUSE_BUTTON_1);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}

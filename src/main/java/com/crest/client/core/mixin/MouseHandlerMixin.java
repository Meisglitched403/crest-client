package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.ZoomModule;
import com.crest.client.music.PauseScreenMusicWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    /**
     * PauseScreen does not override mouseClicked (it inherits the GuiEventListener
     * default), so it cannot be mixin-targeted directly. Intercept the press before
     * it reaches Screen#mouseClicked and hand clicks inside the mini player card to it.
     */
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void crest$pauseMiniClick(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS || button.button() != 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof PauseScreen)) return;
        double mx = mc.mouseHandler.getScaledXPos(mc.getWindow());
        double my = mc.mouseHandler.getScaledYPos(mc.getWindow());
        if (!PauseScreenMusicWidget.inside(mx, my)) return;
        if (PauseScreenMusicWidget.handleClick(mx, my)) ci.cancel();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (!CrestModules.isEnabled("zoom") || !ZoomModule.isActive()) return;
        if (!ZoomModule.isScrollZoomEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;
        ZoomModule.addScrollTier((int) Math.signum(vertical));
        ci.cancel();
    }

    @ModifyArg(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 0)
    private double modifyTurnYaw(double yaw) {
        if (!CrestModules.isEnabled("zoom") || !ZoomModule.isActive()) return yaw;
        return yaw * ZoomModule.getSensitivity();
    }

    @ModifyArg(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 1)
    private double modifyTurnPitch(double pitch) {
        if (!CrestModules.isEnabled("zoom") || !ZoomModule.isActive()) return pitch;
        return pitch * ZoomModule.getSensitivity();
    }
}

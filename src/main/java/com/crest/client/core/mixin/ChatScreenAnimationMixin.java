package com.crest.client.core.mixin;

import com.crest.client.core.ChatAnimationModule;
import com.crest.client.core.Easing;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenAnimationMixin {
    @Unique private int crest$openTick;
    @Unique private boolean crest$pushed;

    @Inject(method = "init", at = @At("HEAD"))
    private void crest$onInit(CallbackInfo ci) {
        crest$openTick = Minecraft.getInstance().gui.getGuiTicks();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void crest$beforeRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        crest$pushed = false;
        if (!ChatAnimationModule.isOn()) return;
        if (!ChatAnimationModule.messagesAnimated()) return;
        DeltaTracker tracker = Minecraft.getInstance().getDeltaTracker();
        float elapsedMs = ((Minecraft.getInstance().gui.getGuiTicks() - crest$openTick)
                + tracker.getGameTimeDeltaPartialTick(false)) * 50f;
        int fadeTime = ChatAnimationModule.getFadeTime();
        if (elapsedMs >= fadeTime) return;
        float t = Math.min(1f, elapsedMs / fadeTime);
        float displacement = (1 - Easing.apply(ChatAnimationModule.getEasing(), ChatAnimationModule.getEasingMode(), t)) * 20;
        if (displacement > 0.5f) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(0, displacement);
            crest$pushed = true;
        }
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void crest$afterRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (crest$pushed) {
            graphics.pose().popMatrix();
        }
    }
}

package com.crest.client.core.mixin;

import com.crest.client.core.ChatAnimationModule;
import com.crest.client.core.CrestModules;
import com.crest.client.core.Easing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenAnimationMixin {
    @Unique private long crest$openTime;

    @Inject(method = "init", at = @At("HEAD"))
    private void crest$onInit(CallbackInfo ci) {
        crest$openTime = System.currentTimeMillis();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void crest$beforeRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!CrestModules.isEnabled("chat_animation")) return;
        long elapsed = System.currentTimeMillis() - crest$openTime;
        int fadeTime = ChatAnimationModule.getFadeTime();
        if (elapsed >= fadeTime) return;
        float t = Math.min(1f, (float) elapsed / fadeTime);
        float displacement = (1 - Easing.apply(ChatAnimationModule.getEasing(), t)) * (graphics.guiHeight() / 4f);
        if (displacement > 0.5f) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(0, displacement);
        }
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void crest$afterRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!CrestModules.isEnabled("chat_animation")) return;
        long elapsed = System.currentTimeMillis() - crest$openTime;
        int fadeTime = ChatAnimationModule.getFadeTime();
        if (elapsed >= fadeTime) return;
        float t = Math.min(1f, (float) elapsed / fadeTime);
        float displacement = (1 - Easing.apply(ChatAnimationModule.getEasing(), t)) * (graphics.guiHeight() / 4f);
        if (displacement > 0.5f) {
            graphics.pose().popMatrix();
        }
    }
}

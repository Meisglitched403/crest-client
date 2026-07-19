package com.crest.client.core.mixin;

import com.crest.client.core.ChatAnimationModule;
import com.crest.client.core.CrestModules;
import com.crest.client.core.Easing;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatComponentAnimationMixin {
    @Unique private long crest$lastMessageTime;

    @Inject(method = "addMessage", at = @At("TAIL"))
    private void crest$onAddMessage(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        crest$lastMessageTime = System.currentTimeMillis();
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At("HEAD"))
    private void crest$beforeRender(ChatComponent.ChatGraphicsAccess access, int width, int height, ChatComponent.DisplayMode mode, CallbackInfo ci) {
        if (!CrestModules.isEnabled("chat_animation")) return;
        if (!ChatAnimationModule.messagesAnimated()) return;
        long elapsed = System.currentTimeMillis() - crest$lastMessageTime;
        int fadeTime = ChatAnimationModule.getFadeTime();
        if (elapsed >= fadeTime) return;
        float t = Math.min(1f, (float) elapsed / fadeTime);
        float displacement = (1 - Easing.apply(ChatAnimationModule.getEasing(), ChatAnimationModule.getEasingMode(), t)) * 9;
        if (displacement > 0.5f) {
            access.updatePose(m -> m.translate(0, displacement));
        }
    }
}

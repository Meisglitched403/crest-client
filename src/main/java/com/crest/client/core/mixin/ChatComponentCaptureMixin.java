package com.crest.client.core.mixin;

import com.crest.client.core.ChatHeadsHelper;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ChatComponent.ChatGraphicsAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentCaptureMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"))
    private static void crest$captureGuiGraphics(CallbackInfo ci, @Local(argsOnly = true) GuiGraphicsExtractor graphics) {
        ChatHeadsHelper.guiGraphics = graphics;
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At("HEAD"))
    private static void crest$captureChatGraphicsAccess(CallbackInfo ci, @Local(argsOnly = true) ChatGraphicsAccess access) {
        ChatHeadsHelper.chatGraphicsAccess = access;
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At("RETURN"))
    private static void crest$clearGraphics(CallbackInfo ci) {
        ChatHeadsHelper.guiGraphics = null;
        ChatHeadsHelper.chatGraphicsAccess = null;
    }

    @Inject(method = "captureClickableText", at = @At("HEAD"))
    private static void crest$noGraphics(CallbackInfo ci) {
        ChatHeadsHelper.guiGraphics = null;
        ChatHeadsHelper.chatGraphicsAccess = null;
    }
}

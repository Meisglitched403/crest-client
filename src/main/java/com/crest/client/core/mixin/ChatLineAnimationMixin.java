package com.crest.client.core.mixin;

import com.crest.client.core.ChatAnimationModule;
import com.crest.client.core.Easing;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$1", priority = 2000)
public abstract class ChatLineAnimationMixin {
    @Shadow @Final private int val$entryHeight;

    @ModifyArgs(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;handleMessage(IFLnet/minecraft/util/FormattedCharSequence;)Z"))
    private void crest$animateMessage(Args args, @Local(argsOnly = true) GuiMessage.Line line) {
        apply(args, line, 0, 0, 1);
    }

    @ModifyArgs(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;handleTag(IIIIFLnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V"))
    private void crest$animateTag(Args args, @Local(argsOnly = true) GuiMessage.Line line) {
        apply(args, line, 1, 3, 4);
    }

    @ModifyArgs(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;handleTagIcon(IIZLnet/minecraft/client/multiplayer/chat/GuiMessageTag;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag$Icon;)V"))
    private void crest$animateTagIcon(Args args, @Local(argsOnly = true) GuiMessage.Line line) {
        apply(args, line, 1, 1, -1);
    }

    @Unique
    private void apply(Args args, GuiMessage.Line line, int yIndex1, int yIndex2, int opacityIndex) {
        float progress = ChatAnimationModule.lineProgress(line);
        
        // Early exit for completed animations - no calculations needed
        if (progress >= 1f) return;
        
        boolean slide = ChatAnimationModule.messagesAnimated();
        boolean fade = ChatAnimationModule.opacityAnimated();
        if (!slide && !fade) return;

        // Calculate easing once for both slide and fade
        float eased = Easing.apply(ChatAnimationModule.getEasing(), ChatAnimationModule.getEasingMode(), progress);

        if (slide) {
            // Use floating-point displacement to preserve sub-pixel precision
            // Only round at the very end to avoid stair-stepping artifacts
            float dyFloat = (1f - eased) * val$entryHeight;
            int dy = Math.round(dyFloat);
            
            if (dy != 0) {
                args.set(yIndex1, args.<Integer>get(yIndex1) + dy);
                if (yIndex2 != yIndex1) args.set(yIndex2, args.<Integer>get(yIndex2) + dy);
            }
        }
        
        if (fade && opacityIndex >= 0) {
            args.set(opacityIndex, args.<Float>get(opacityIndex) * eased);
        }
    }
}

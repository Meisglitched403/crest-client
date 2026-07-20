package com.crest.client.core.mixin;

import com.crest.client.core.ChatHeadsHelper;
import com.crest.client.core.ChatHeadsModule;
import com.crest.client.core.mixininterface.ChatHeadOwnable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$1")
public abstract class ChatHeadsInnerMixin {

    @ModifyArgs(
            method = "accept",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;handleMessage(IFLnet/minecraft/util/FormattedCharSequence;)Z"
            )
    )
    private void crest$renderHeadAndShiftText(Args args, @Local(argsOnly = true) GuiMessage.Line line,
            @Share("chatOffset") LocalIntRef chatOffset) {
        if (!ChatHeadsHelper.shouldRenderHeads() || ChatHeadsHelper.guiGraphics == null) {
            chatOffset.set(0);
            return;
        }

        PlayerInfo owner = ((ChatHeadOwnable)(Object)line).crest$getOwner();
        if (owner == null) {
            chatOffset.set(0);
            return;
        }

        int headSize = ChatHeadsModule.getHeadSize();
        int offset = headSize + 2;
        chatOffset.set(offset);

        int y = args.get(0);
        ChatHeadsHelper.renderHead(ChatHeadsHelper.guiGraphics, 2, y, owner, headSize);
        if (ChatHeadsHelper.chatGraphicsAccess != null) {
            ChatHeadsHelper.chatGraphicsAccess.updatePose(p -> p.translate(offset, 0));
        }
    }

    @Inject(
            method = "accept",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;handleMessage(IFLnet/minecraft/util/FormattedCharSequence;)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void crest$unshiftText(CallbackInfo ci, @Share("chatOffset") LocalIntRef chatOffset) {
        int offset = chatOffset.get();
        if (offset > 0 && ChatHeadsHelper.chatGraphicsAccess != null) {
            ChatHeadsHelper.chatGraphicsAccess.updatePose(p -> p.translate(-offset, 0));
        }
    }
}

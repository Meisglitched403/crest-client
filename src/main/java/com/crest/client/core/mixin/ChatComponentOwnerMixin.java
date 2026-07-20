package com.crest.client.core.mixin;

import com.crest.client.core.ChatHeadsHelper;
import com.crest.client.core.ChatHeadsModule;
import com.crest.client.core.CrestModules;
import com.crest.client.core.mixininterface.ChatHeadOwnable;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.components.ChatComponent.class)
public class ChatComponentOwnerMixin {

    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"))
    private void crest$captureOwner(GuiMessage message, CallbackInfo ci) {
        if (!CrestModules.isEnabled("chat_heads")) return;
        PlayerInfo owner = ChatHeadsHelper.findOwner(message.content());
        if (owner != null) {
            ((ChatHeadOwnable)(Object)message).crest$setOwner(owner);
        }
    }
}

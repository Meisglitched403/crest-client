package com.crest.client.core.mixin;

import com.crest.client.core.ChatHeadsModule;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatListener.class)
public class ChatHeadsMixin {
    @Inject(method = "handlePlayerChatMessage", at = @At("HEAD"))
    private void crest$captureProfile(PlayerChatMessage message, GameProfile profile, ChatType.Bound bound, CallbackInfo ci) {
        ChatHeadsModule.setLastProfile(profile);
    }

    @Inject(method = "handleDisguisedChatMessage", at = @At("HEAD"))
    private void crest$clearProfileDisguised(Component component, ChatType.Bound bound, CallbackInfo ci) {
        ChatHeadsModule.setLastProfile(null);
    }

    @Inject(method = "handleSystemMessage", at = @At("HEAD"))
    private void crest$clearProfileSystem(Component component, boolean bl, CallbackInfo ci) {
        ChatHeadsModule.setLastProfile(null);
    }
}

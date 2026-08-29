package com.crest.client.core.mixin;

import com.crest.client.core.WaypointsModule;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks chat messages so coordinates can become waypoints (wWaypoints behaviour). */
@Mixin(ChatComponent.class)
public class ChatWaypointMixin {

    @Inject(method = "addPlayerMessage", at = @At("HEAD"))
    private void crest$onPlayer(Component message, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
        WaypointsModule.handleChat(message);
    }

    @Inject(method = "addServerSystemMessage", at = @At("HEAD"))
    private void crest$onServerSystem(Component message, CallbackInfo ci) {
        WaypointsModule.handleChat(message);
    }

    @Inject(method = "addClientSystemMessage", at = @At("HEAD"))
    private void crest$onClientSystem(Component message, CallbackInfo ci) {
        WaypointsModule.handleChat(message);
    }
}

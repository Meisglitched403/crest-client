package com.crest.client.core.mixin;

import com.crest.client.core.StateRecorder;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Inject(method = "channelRead0", at = @At("HEAD"))
    private void crest$onPacketIn(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        StateRecorder.onPacketIn(packet);
    }

    @Inject(method = "send", at = @At("HEAD"))
    private void crest$onPacketOut(Packet<?> packet, CallbackInfo ci) {
        StateRecorder.onPacketOut(packet);
    }
}
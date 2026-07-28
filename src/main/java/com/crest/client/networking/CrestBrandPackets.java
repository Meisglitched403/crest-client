package com.crest.client.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class CrestBrandPackets {
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(
            CrestBrandStatusS2CPacket.TYPE, CrestBrandStatusS2CPacket.STREAM_CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
            CrestBrandQueryC2SPacket.TYPE, CrestBrandQueryC2SPacket.STREAM_CODEC
        );
    }
}

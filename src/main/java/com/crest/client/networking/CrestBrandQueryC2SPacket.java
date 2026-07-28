package com.crest.client.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CrestBrandQueryC2SPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CrestBrandQueryC2SPacket> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("crest-client", "brand_query"));

    public static final StreamCodec<FriendlyByteBuf, CrestBrandQueryC2SPacket> STREAM_CODEC =
        StreamCodec.unit(new CrestBrandQueryC2SPacket());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

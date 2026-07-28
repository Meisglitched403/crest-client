package com.crest.client.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record CrestBrandStatusS2CPacket(Set<UUID> crestUsers) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CrestBrandStatusS2CPacket> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("crest-client", "brand_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrestBrandStatusS2CPacket> STREAM_CODEC =
        StreamCodec.ofMember(
            (packet, output) -> {
                output.writeVarInt(packet.crestUsers().size());
                for (UUID uuid : packet.crestUsers()) {
                    output.writeUUID(uuid);
                }
            },
            input -> {
                int size = input.readVarInt();
                Set<UUID> set = new HashSet<>(size);
                for (int i = 0; i < size; i++) {
                    set.add(input.readUUID());
                }
                return new CrestBrandStatusS2CPacket(set);
            }
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

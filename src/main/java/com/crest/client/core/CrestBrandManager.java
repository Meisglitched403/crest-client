package com.crest.client.core;

import com.crest.client.networking.CrestBrandQueryC2SPacket;
import com.crest.client.networking.CrestBrandPackets;
import com.crest.client.networking.CrestBrandStatusS2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CrestBrandManager {
    private static final Set<UUID> crestUsers = new HashSet<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        CrestBrandPackets.register();

        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> {
            if (client.player != null) {
                crestUsers.add(client.player.getUUID());
            }
            ClientPlayNetworking.send(new CrestBrandQueryC2SPacket());
        });

        ClientPlayNetworking.registerGlobalReceiver(
            CrestBrandStatusS2CPacket.TYPE,
            (payload, context) -> {
                crestUsers.clear();
                crestUsers.addAll(payload.crestUsers());
                context.player().sendSystemMessage(
                    Component.literal(
                        "[Crest] Brand status loaded: " + payload.crestUsers().size() + " crest-client users online")
                );
            }
        );
    }

    public static Set<UUID> getCrestUsers() {
        return crestUsers;
    }

    public static boolean isCrestUser(UUID uuid) {
        return crestUsers.contains(uuid);
    }

    public static boolean isInitialized() {
        return initialized;
    }
}

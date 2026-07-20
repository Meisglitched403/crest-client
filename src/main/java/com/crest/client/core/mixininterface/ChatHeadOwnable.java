package com.crest.client.core.mixininterface;

import net.minecraft.client.multiplayer.PlayerInfo;

public interface ChatHeadOwnable {
    PlayerInfo crest$getOwner();
    void crest$setOwner(PlayerInfo owner);
}

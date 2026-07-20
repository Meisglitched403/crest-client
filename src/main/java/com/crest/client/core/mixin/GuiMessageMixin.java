package com.crest.client.core.mixin;

import com.crest.client.core.mixininterface.ChatHeadOwnable;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.class)
public abstract class GuiMessageMixin implements ChatHeadOwnable {
    @Unique
    private PlayerInfo crest$owner;

    @Override
    public PlayerInfo crest$getOwner() {
        return crest$owner;
    }

    @Override
    public void crest$setOwner(PlayerInfo owner) {
        crest$owner = owner;
    }
}

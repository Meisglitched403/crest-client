package com.crest.client.core.mixin;

import com.crest.client.core.mixininterface.ChatHeadOwnable;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMessage.Line.class)
public abstract class GuiMessageLineMixin implements ChatHeadOwnable {
    @Unique
    private PlayerInfo crest$owner;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void crest$copyOwner(CallbackInfo ci) {
        crest$owner = ((ChatHeadOwnable)(Object)((GuiMessage.Line)(Object)this).parent()).crest$getOwner();
    }

    @Override
    public PlayerInfo crest$getOwner() {
        return crest$owner;
    }

    @Override
    public void crest$setOwner(PlayerInfo owner) {
        crest$owner = owner;
    }
}

package com.crest.client.core.mixin;

import com.crest.client.core.ComboTracker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MinecraftAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void crest$onLandHit(Player player, Entity entity, CallbackInfo ci) {
        ComboTracker.onHit();
    }
}

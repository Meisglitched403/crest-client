package com.crest.client.core.mixin;

import com.crest.client.core.replay.StatePlayerHolder;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * During replay re-simulation, we feed synthetic packets via ReplayTickMixin.
 * The real Connection must not also process packets or we have double-feeding.
 * This mixin skips Connection.tick() entirely while StatePlayerHolder is active.
 */
@Mixin(Connection.class)
public class ConnectionBlockMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void crest$blockDuringReplay(CallbackInfo ci) {
        if (StatePlayerHolder.isPlaying()) {
            ci.cancel();
        }
    }
}
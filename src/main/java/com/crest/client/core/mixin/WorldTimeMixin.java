package com.crest.client.core.mixin;

import com.crest.client.core.CrestModule;
import com.crest.client.core.CrestModules;
import com.crest.client.core.TimeChangerModule;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class WorldTimeMixin {
    @Inject(method = "getOverworldClockTime", at = @At("RETURN"), cancellable = true)
    private void crest$overrideTime(CallbackInfoReturnable<Long> cir) {
        if (!CrestModules.isEnabled("time_changer")) return;
        CrestModule m = CrestModules.get("time_changer");
        if (m instanceof TimeChangerModule tcm) {
            cir.setReturnValue(tcm.getOverrideTime(cir.getReturnValue()));
        }
    }
}

package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.ParticleChangerModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(
        method = "createParticle",
        at = @At("HEAD"),
        cancellable = true
    )
    private void crest$maybeDrop(ParticleOptions options, double x, double y, double z,
                                 double dx, double dy, double dz, CallbackInfoReturnable<Particle> cir) {
        if (ParticleChangerModule.shouldDrop()) {
            cir.setReturnValue(null);
        }
    }

    @Inject(
        method = "createParticle",
        at = @At("RETURN")
    )
    private void crest$applyScale(ParticleOptions options, double x, double y, double z,
                                  double dx, double dy, double dz, CallbackInfoReturnable<Particle> cir) {
        Particle p = cir.getReturnValue();
        if (p != null) ParticleChangerModule.apply(p);
    }
}

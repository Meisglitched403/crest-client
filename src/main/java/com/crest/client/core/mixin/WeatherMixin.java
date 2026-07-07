package com.crest.client.core.mixin;

import com.crest.client.core.CrestModule;
import com.crest.client.core.CrestModules;
import com.crest.client.core.WeatherChangerModule;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class WeatherMixin {
    @Inject(method = "getRainLevel", at = @At("RETURN"), cancellable = true)
    private void crest$overrideRain(float delta, CallbackInfoReturnable<Float> cir) {
        if (!CrestModules.isEnabled("weather_changer")) return;
        CrestModule m = CrestModules.get("weather_changer");
        if (m instanceof WeatherChangerModule wcm) {
            cir.setReturnValue(wcm.getRainLevel());
        }
    }

    @Inject(method = "getThunderLevel", at = @At("RETURN"), cancellable = true)
    private void crest$overrideThunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (!CrestModules.isEnabled("weather_changer")) return;
        CrestModule m = CrestModules.get("weather_changer");
        if (m instanceof WeatherChangerModule wcm) {
            cir.setReturnValue(wcm.getThunderLevel());
        }
    }
}

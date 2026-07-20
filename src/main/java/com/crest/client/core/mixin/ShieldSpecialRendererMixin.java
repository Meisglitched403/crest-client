package com.crest.client.core.mixin;

import com.crest.client.core.ShieldTintSubmitter;
import com.crest.client.core.ShieldTintSubmitterHolder;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShieldSpecialRenderer.class)
public class ShieldSpecialRendererMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void crest$capture(SpriteGetter sprites, ShieldModel model, CallbackInfo ci) {
        ShieldTintSubmitterHolder.set(new ShieldTintSubmitter(model, sprites));
    }
}

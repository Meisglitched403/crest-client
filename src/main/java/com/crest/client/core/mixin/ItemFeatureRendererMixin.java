package com.crest.client.core.mixin;

import com.crest.client.core.GlintColorizerModule;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {

    @Inject(method = "getFoilRenderType", at = @At("RETURN"), cancellable = true)
    private static void crest$recolorGlint(RenderType base, boolean bl, CallbackInfoReturnable<RenderType> cir) {
        if (!GlintColorizerModule.shouldApply()) return;
        cir.setReturnValue(GlintColorizerModule.getItemGlint(base));
    }
}

package com.crest.client.core.mixin;

import com.crest.client.core.ShieldFocusedPlayer;
import com.crest.client.core.ShieldStatusModule;
import com.crest.client.core.ShieldTintSubmitterHolder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponentMap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public abstract class ShieldLayerRenderStateMixin {

    @Shadow @Final ItemStackRenderState this$0;
    @Shadow private @Nullable SpecialModelRenderer<Object> specialRenderer;
    @Shadow private @Nullable Object argumentForSpecialRendering;
    @Shadow private ItemStackRenderState.FoilType foilType;

    @Inject(method = "submit",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/special/SpecialModelRenderer;submit(Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V"),
            cancellable = true)
    private void crest$redirectShieldSubmit(PoseStack poseStack, SubmitNodeCollector collector,
                                            int lightCoords, int overlayCoords, int outlineColor,
                                            CallbackInfo ci) {
        if (!ShieldStatusModule.isActive()) return;
        if (!ShieldSpecialRenderer.class.isInstance(this.specialRenderer)) return;

        DataComponentMap components = (this.argumentForSpecialRendering instanceof DataComponentMap)
                ? (DataComponentMap) this.argumentForSpecialRendering : null;
        boolean hasFoil = this.foilType != ItemStackRenderState.FoilType.NONE;

        ShieldTintSubmitterHolder.get().submit(null, components, poseStack, collector,
                lightCoords, overlayCoords, hasFoil, ShieldFocusedPlayer.get(this$0));
        poseStack.popPose();
        ci.cancel();
    }
}


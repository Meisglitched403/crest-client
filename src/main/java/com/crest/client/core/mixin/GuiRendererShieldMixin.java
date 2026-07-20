package com.crest.client.core.mixin;

import com.crest.client.core.ShieldFocusedPlayer;
import com.crest.client.core.ShieldStatusModule;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererShieldMixin {

    @Inject(method = "lambda$prepareItemElements$0",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiItemAtlas;getOrUpdate(Lnet/minecraft/client/renderer/item/TrackingItemStackRenderState;)Lnet/minecraft/client/gui/render/GuiItemAtlas$SlotView;"))
    private void crest$markShieldAnimated(MutableBoolean hasOversizedItems, GuiItemAtlas itemAtlas,
                                          GuiItemRenderState itemState, CallbackInfo ci) {
        if (!ShieldStatusModule.isActive()) return;
        Object identity = itemState.itemStackRenderState().getModelIdentity();
        if (identity != null && identity.toString().contains("shield")) {
            itemState.itemStackRenderState().setAnimated();
            // GUI shields always belong to the local player.
            ShieldFocusedPlayer.setLocal();
        }
    }
}

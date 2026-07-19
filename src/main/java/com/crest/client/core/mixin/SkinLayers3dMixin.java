package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.SkinLayers3dModule;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class SkinLayers3dMixin {
    @Shadow
    private boolean slim;

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void crest$onSetupAnim(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel model = (PlayerModel) (Object) this;
        boolean enabled = CrestModules.isEnabled("skin_layers_3d");

        float h = enabled ? SkinLayers3dModule.getHeadThickness() : 0f;
        float b = enabled ? SkinLayers3dModule.getBodyThickness() : 0f;
        float a = enabled ? SkinLayers3dModule.getArmsThickness() : 0f;
        float l = enabled ? SkinLayers3dModule.getLegsThickness() : 0f;

        float hatS = 1f + h * 0.12f;
        model.hat.y = 4f * (1f - hatS);
        model.hat.xScale = hatS;
        model.hat.yScale = hatS;
        model.hat.zScale = hatS;

        float jacketS = 1f + b * 0.08f;
        model.jacket.y = 0f;
        model.jacket.xScale = jacketS;
        model.jacket.yScale = 1f;
        model.jacket.zScale = jacketS;

        float armS = 1f + a * 0.08f;
        model.leftSleeve.y = 0f;
        model.leftSleeve.xScale = armS;
        model.leftSleeve.yScale = 1f;
        model.leftSleeve.zScale = armS;
        model.rightSleeve.y = 0f;
        model.rightSleeve.xScale = armS;
        model.rightSleeve.yScale = 1f;
        model.rightSleeve.zScale = armS;

        float legS = 1f + l * 0.06f;
        model.leftPants.y = 0f;
        model.leftPants.xScale = legS;
        model.leftPants.yScale = 1f;
        model.leftPants.zScale = legS;
        model.rightPants.yScale = 1f;
        model.rightPants.xScale = legS;
        model.rightPants.zScale = legS;
    }
}

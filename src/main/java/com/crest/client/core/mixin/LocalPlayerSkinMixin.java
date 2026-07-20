package com.crest.client.core.mixin;

import com.crest.client.core.SkinChanger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class LocalPlayerSkinMixin {

    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void crest$getSkin(CallbackInfoReturnable<PlayerSkin> ci) {
        PlayerSkin override = SkinChanger.getOverride();
        if (override == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == (Object) this) {
            ci.setReturnValue(override);
        }
    }
}

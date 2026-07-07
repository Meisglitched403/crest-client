package com.crest.client.core.mixin;

import com.crest.client.core.FreelookModule;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public class FreelookMixin {
    @Redirect(
        method = "turnPlayer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void crest$redirectPlayerTurn(LocalPlayer player, double deltaX, double deltaY) {
        if (FreelookModule.isActive()) {
            FreelookModule.addAngles((float) deltaX, (float) deltaY);
        } else {
            player.turn(deltaX, deltaY);
        }
    }
}

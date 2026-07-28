package com.crest.client.core.mixin;

import com.crest.client.core.ViewModelModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @ModifyReturnValue(method = "getCurrentSwingDuration", at = @At("RETURN"))
    private int viewmodel$swingDuration(int original) {
        if (!ViewModelModule.isActive()) return original;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || (Object) this != mc.player) return original;
        return ViewModelModule.isNoSwing() ? 0 : ViewModelModule.getSwingSpeed() + original;
    }

    @ModifyVariable(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V",
        at = @At("HEAD"), argsOnly = true, name = "hand")
    private InteractionHand viewmodel$swingHand(InteractionHand hand) {
        if (!ViewModelModule.isActive()) return hand;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || (Object) this != mc.player) return hand;
        return switch (ViewModelModule.getSwingMode()) {
            case 1 -> InteractionHand.MAIN_HAND;
            case 2 -> InteractionHand.OFF_HAND;
            default -> hand;
        };
    }
}

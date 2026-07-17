package com.crest.client.core.mixin;

import com.crest.client.core.ScrollableTooltipsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(net.minecraft.client.gui.screens.Screen.class)
public class ScreenTooltipMixin {

    @Inject(method = "getTooltipFromItem", at = @At("RETURN"), cancellable = true)
    private static void crest$wrapTooltip(Minecraft mc, ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> lines = cir.getReturnValue();
        if (lines == null) return;
        cir.setReturnValue(ScrollableTooltipsModule.process(lines));
    }
}

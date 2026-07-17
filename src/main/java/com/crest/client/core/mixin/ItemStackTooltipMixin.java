package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.ShulkerPreviewModule;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ItemStack.class)
public class ItemStackTooltipMixin {

    @Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
    private void crest$shulkerPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (!CrestModules.isEnabled("shulker_preview")) return;
        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty()) return;

        ItemContainerContents contents = self.get(DataComponents.CONTAINER);
        if (contents == null) return;

        NonNullList<ItemStack> list = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.copyInto(list);
        cir.setReturnValue(Optional.of(new ShulkerPreviewModule.ShulkerPreviewComponent(list)));
    }
}

package com.crest.client.core.mixin;

import com.crest.client.core.ItemPhysicsModule;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void crest$itemPhysics(CallbackInfo ci) {
        ItemPhysicsModule.apply((ItemEntity) (Object) this);
    }
}

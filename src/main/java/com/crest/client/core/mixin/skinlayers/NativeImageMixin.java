package com.crest.client.core.mixin.skinlayers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.platform.NativeImage;

import com.crest.client.skinlayers.accessor.NativeImageAccessor;

@Mixin(NativeImage.class)
public abstract class NativeImageMixin implements NativeImageAccessor {

    @Shadow
    private long pixels;

    @Override
    public boolean skinlayers$isAllocated() {
        return pixels != 0L;
    }

}

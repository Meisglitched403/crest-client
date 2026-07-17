package com.crest.client.core.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccess {
    @Invoker("setPostEffect")
    void crest$setPostEffect(Identifier id);

    @Invoker("clearPostEffect")
    void crest$clearPostEffect();
}

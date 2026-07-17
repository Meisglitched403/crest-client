package com.crest.client.core.mixin;

import com.crest.client.core.ColorSaturationModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {
    private static final Logger LOG = LoggerFactory.getLogger("Crest/ColorSaturation");

    @Accessor("postChainProjection")
    public abstract Projection crest$postChainProjection();

    @Accessor("postChainProjectionMatrixBuffer")
    public abstract ProjectionMatrixBuffer crest$postChainProjectionMatrixBuffer();

    @Inject(method = "getPostChain", at = @At("HEAD"), cancellable = true)
    private void crest$colorSaturation(Identifier id, Set<Identifier> targets, CallbackInfoReturnable<PostChain> cir) {
        if (!ColorSaturationModule.shouldApply()) return;
        if (!id.equals(ColorSaturationModule.SATURATION_ID)) return;

        try {
            TextureManager tm = Minecraft.getInstance().getTextureManager();
            float sat = ColorSaturationModule.currentSaturation();

            var input = new net.minecraft.client.renderer.PostChainConfig.TargetInput(
                "InSampler", Identifier.withDefaultNamespace("previous"), false, false);
            var pass = new net.minecraft.client.renderer.PostChainConfig.Pass(
                Identifier.withDefaultNamespace("position_tex"),
                Identifier.fromNamespaceAndPath("crest-client", "saturation"),
                List.of(input),
                Identifier.withDefaultNamespace("final"),
                Map.of("Saturation", List.of(new net.minecraft.client.renderer.UniformValue.FloatUniform(sat)))
            );
            var config = new net.minecraft.client.renderer.PostChainConfig(Map.of(), List.of(pass));

            PostChain chain = PostChain.load(config, tm, targets, PostChain.MAIN_TARGET_ID,
                crest$postChainProjection(), crest$postChainProjectionMatrixBuffer());
            cir.setReturnValue(chain);
        } catch (Exception e) {
            // Graceful: fall through to vanilla JSON-based chain if our build fails.
            LOG.warn("ColorSaturation post chain build failed, using fallback", e);
        }
    }
}


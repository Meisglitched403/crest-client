package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.MotionBlurModule;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MotionBlurMixin {
    private static GpuTexture prevFrameTexture;
    private static GpuTextureView prevFrameView;
    private static GpuTexture swapTexture;
    private static GpuTextureView swapView;
    private static int texW = -1, texH = -1;
    private static boolean captured;
    private static RenderPipeline pipeline;
    private static GpuSampler sampler;

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(CallbackInfo ci) {
        if (!CrestModules.isEnabled("motion_blur")) return;
        int strength = MotionBlurModule.getStrength();
        if (strength <= 0) return;

        try {
            doGhost(strength);
        } catch (Throwable t) {
            System.err.println("[CrestMotionBlur] ERROR: " + t);
            cleanup();
            CrestModules.setEnabled("motion_blur", false);
        }
    }

    @Unique
    private static void doGhost(int strength) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        GpuTexture mainTex = main.getColorTexture();
        if (mainTex == null) return;

        int w = main.width;
        int h = main.height;
        ensureTextures(w, h);

        GpuDevice dev = RenderSystem.getDevice();
        if (dev == null) return;
        CommandEncoder encoder = dev.createCommandEncoder();

        encoder.copyTextureToTexture(mainTex, swapTexture, 0, 0, 0, 0, 0, 0, 1);

        if (captured) {
            float alpha = Math.min(strength / 100.0f, 1.0f);

            ByteBuffer buf = ByteBuffer.allocate(16).order(ByteOrder.nativeOrder());
            buf.putFloat(1.0f); buf.putFloat(1.0f); buf.putFloat(1.0f); buf.putFloat(alpha);
            buf.flip();
            GpuBuffer uniformBuf = dev.createBuffer(() -> "BlitConfig", GpuBuffer.USAGE_UNIFORM, buf);

            try {
                RenderPass pass = encoder.createRenderPass(
                    () -> "motionBlurGhost",
                    main.getColorTextureView(),
                    OptionalInt.empty()
                );
                try {
                    pass.setPipeline(getOrCreatePipeline());
                    pass.bindTexture("InSampler", prevFrameView, getOrCreateSampler(dev));
                    pass.setUniform("BlitConfig", uniformBuf);
                    pass.draw(0, 3);
                } finally {
                    pass.close();
                }
            } finally {
                uniformBuf.close();
            }
        }

        GpuTexture tmp = prevFrameTexture;
        prevFrameTexture = swapTexture;
        swapTexture = tmp;
        GpuTextureView tmpView = prevFrameView;
        prevFrameView = swapView;
        swapView = tmpView;

        captured = true;
    }

    @Unique
    private static void ensureTextures(int width, int height) {
        if (texW == width && texH == height) return;
        GpuDevice dev = RenderSystem.getDevice();
        if (dev == null) return;

        if (prevFrameTexture != null) prevFrameTexture.close();
        if (prevFrameView != null) prevFrameView.close();
        if (swapTexture != null) swapTexture.close();
        if (swapView != null) swapView.close();

        int usage = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_SRC;
        prevFrameTexture = dev.createTexture("MotionBlurPrev", usage, TextureFormat.RGBA8, width, height, 1, 1);
        prevFrameView = dev.createTextureView(prevFrameTexture);
        swapTexture = dev.createTexture("MotionBlurSwap", usage, TextureFormat.RGBA8, width, height, 1, 1);
        swapView = dev.createTextureView(swapTexture);
        texW = width;
        texH = height;
        captured = false;
    }

    @Unique
    private static RenderPipeline getOrCreatePipeline() {
        if (pipeline == null) {
            pipeline = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                .withLocation("pipeline/motion_blur_blit")
                .withVertexShader("core/screenquad")
                .withFragmentShader("crest-client:core/motion_blur")
                .withSampler("InSampler")
                .withUniform("BlitConfig", UniformType.UNIFORM_BUFFER)
                .withColorTargetState(new ColorTargetState(
                    Optional.of(new BlendFunction(
                        SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                        SourceFactor.ZERO, DestFactor.ONE
                    )),
                    ColorTargetState.WRITE_COLOR
                ))
                .build();
        }
        return pipeline;
    }

    @Unique
    private static GpuSampler getOrCreateSampler(GpuDevice dev) {
        if (sampler == null) {
            sampler = dev.createSampler(
                AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR, FilterMode.LINEAR,
                0, OptionalDouble.empty()
            );
        }
        return sampler;
    }

    @Unique
    private static void cleanup() {
        if (prevFrameTexture != null) { prevFrameTexture.close(); prevFrameTexture = null; }
        if (prevFrameView != null) { prevFrameView.close(); prevFrameView = null; }
        if (swapTexture != null) { swapTexture.close(); swapTexture = null; }
        if (swapView != null) { swapView.close(); swapView = null; }
        if (sampler != null) { sampler.close(); sampler = null; }
        pipeline = null;
        texW = texH = -1;
        captured = false;
    }
}

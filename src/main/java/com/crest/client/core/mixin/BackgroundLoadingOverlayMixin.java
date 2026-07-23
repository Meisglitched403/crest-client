package com.crest.client.core.mixin;

import com.crest.client.core.BackgroundResourceLoaderModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LoadingOverlay.class)
public class BackgroundLoadingOverlayMixin {
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;

    @Unique private long rrls$atEndStart = -1L;
    @Unique private long rrls$createdAt = -1L;

    @Inject(method = "isPauseScreen", at = @At("HEAD"), cancellable = true)
    private void rrls$isPauseScreen(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (BackgroundResourceLoaderModule.isActive() && mc.level != null) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void rrls$onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!BackgroundResourceLoaderModule.isActive() || mc.level == null) return;

        long window = mc.getWindow().handle();
        if (window != 0 && GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            BackgroundResourceLoaderModule.setCurrentLoading(false);
            mc.setOverlay(null);
            if (mc.screen == null) {
                mc.setScreen(new PauseScreen(true));
            }
            ci.cancel();
            return;
        }

        int w = graphics.guiWidth();
        int h = graphics.guiHeight();

        long fadeTime = this.rrls$atEndStart != -1L ? this.rrls$atEndStart : this.fadeOutStart;

        float ease = 1.0F;
        if (fadeTime > -1L) {
            float f = (float)(Util.getMillis() - fadeTime) / BackgroundResourceLoaderModule.getAnimationSpeed();
            ease -= Mth.clamp(f * f, 0.0F, 1.0F);
        }

        if (ease <= 0.0F) {
            BackgroundResourceLoaderModule.setCurrentLoading(false);
            mc.setOverlay(null);
        }

        int alpha = (int)(ease * 255) << 24;

        switch (BackgroundResourceLoaderModule.getProgressStyle()) {
            case 0:
                int barY = (int) ((double) h * 0.8325);
                int barRadius = (int) (Math.min(w * 0.75, h) * 0.5);
                int bx0 = w / 2 - barRadius;
                int bx1 = w / 2 + barRadius;
                int by0 = barY - 5;
                int by1 = barY + 5;
                int barWidth = bx1 - bx0;
                int filled = (int)(barWidth * Mth.clamp(this.currentProgress, 0.0F, 1.0F));
                graphics.fill(bx0, by0, bx1, by1, alpha | 0x444444);
                graphics.fill(bx0, by0, bx0 + filled, by1, alpha | 0x00FFFFFF);
                break;
            case 1:
                String text = "Loading resources... " + (int)(this.currentProgress * 100) + "%";
                int textColor;
                if (BackgroundResourceLoaderModule.isRgbProgress()) {
                    float hue = (Util.getMillis() % 3000L) / 3000f;
                    textColor = hsbToRgb(hue, 0.8f, 1.0f) & 0x00FFFFFF | 0xFF000000;
                } else {
                    textColor = alpha | 0x00FFFFFF;
                }
                graphics.centeredText(
                    Minecraft.getInstance().font,
                    Component.literal(text),
                    w / 2, h - 20, textColor
                );
                break;
            case 2:
                break;
        }

        ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void rrls$onTick(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (BackgroundResourceLoaderModule.isActive() && mc.level != null) {
            BackgroundResourceLoaderModule.setCurrentLoading(true);

            if (this.rrls$createdAt == -1L) {
                this.rrls$createdAt = Util.getMillis();
            }

            if (this.currentProgress >= 0.99999F && this.rrls$atEndStart == -1L) {
                this.rrls$atEndStart = Util.getMillis();
            }

            if (Util.getMillis() - this.rrls$createdAt > 60000L) {
                BackgroundResourceLoaderModule.setCurrentLoading(false);
                mc.setOverlay(null);
            }
        }
    }

    @Unique
    private static int hsbToRgb(float hue, float saturation, float brightness) {
        float h = hue * 6;
        int i = (int) h;
        float f = h - i;
        float p = brightness * (1 - saturation);
        float q = brightness * (1 - saturation * f);
        float t = brightness * (1 - saturation * (1 - f));
        float r, g, b;
        switch (i % 6) {
            case 0: r = brightness; g = t; b = p; break;
            case 1: r = q; g = brightness; b = p; break;
            case 2: r = p; g = brightness; b = t; break;
            case 3: r = p; g = q; b = brightness; break;
            case 4: r = t; g = p; b = brightness; break;
            case 5: r = brightness; g = p; b = q; break;
            default: r = g = b = 0; break;
        }
        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }
}

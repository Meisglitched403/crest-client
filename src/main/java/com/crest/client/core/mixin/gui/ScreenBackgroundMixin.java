package com.crest.client.core.mixin.gui;

import com.crest.client.core.CrestModules;
import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {

    @Inject(method = "extractBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void onExtractBlurredBackground(GuiGraphicsExtractor g, CallbackInfo ci) {
        if (!CrestModules.isEnabled("ui_theme")) return;
        ci.cancel();
        int w = g.guiWidth();
        int h = g.guiHeight();
        Theme.tick(0.016f);
        g.fill(0, 0, w, h, ColorUtil.withAlpha(Theme.OVERLAY, 235));
        Panel.drawGlass(g, 0, 0, w, h, ColorUtil.withAlpha(Theme.OVERLAY, 235), Theme.getAnimatedAccent());
    }

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void onExtractTransparentBackground(GuiGraphicsExtractor g, CallbackInfo ci) {
        if (!CrestModules.isEnabled("ui_theme")) return;
        ci.cancel();
        int w = g.guiWidth();
        int h = g.guiHeight();
        Theme.tick(0.016f);
        g.fill(0, 0, w, h, ColorUtil.withAlpha(Theme.OVERLAY, 160));
        Panel.drawGlass(g, 0, 0, w, h, ColorUtil.withAlpha(Theme.OVERLAY, 160), Theme.getAnimatedAccent());
    }
}

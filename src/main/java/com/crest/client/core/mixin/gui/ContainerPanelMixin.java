package com.crest.client.core.mixin.gui;

import com.crest.client.core.CrestModules;
import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerPanelMixin {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onContainerExtract(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
        if (!CrestModules.isEnabled("ui_theme")) return;
        Theme.tick(delta);
        int accent = Theme.getAnimatedAccent();
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;
        Panel.drawGlass(g, x, y, w, h, ColorUtil.withAlpha(Theme.BG_PANEL, 150), accent);
    }
}

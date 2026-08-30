package com.crest.client.core.mixin.gui;

import com.crest.client.music.PauseScreenMusicWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class PauseScreenMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void crest$drawMiniPlayer(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        PauseScreenMusicWidget.render(g, mc.font, g.guiWidth(), g.guiHeight(), mx, my, delta);
    }
}
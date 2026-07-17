package com.crest.client.core.mixin.gui;

import com.crest.client.core.CrestMenu;
import com.crest.client.core.StreamerSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Unique private static final int BTN_W = 56;
    @Unique private static final int BTN_H = 20;

    @Unique private boolean modulesHovered;
    @Unique private boolean crestHovered;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void crest$drawButtons(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth();
        int h = g.guiHeight();

        int modulesX = w / 2 - BTN_W - 2;
        int crestX = w / 2 + 2;
        int y = h - 24;

        modulesHovered = mx >= modulesX && mx <= modulesX + BTN_W && my >= y && my <= y + BTN_H;
        crestHovered = mx >= crestX && mx <= crestX + BTN_W && my >= y && my <= y + BTN_H;

        g.fill(modulesX, y, modulesX + BTN_W, y + BTN_H, modulesHovered ? 0xFF404040 : 0xFF202020);
        g.centeredText(mc.font, Component.literal("Modules"), modulesX + BTN_W / 2, y + (BTN_H - 8) / 2,
            modulesHovered ? 0xFFFFFFA0 : 0xFFFFFFFF);

        g.fill(crestX, y, crestX + BTN_W, y + BTN_H, crestHovered ? 0xFF404040 : 0xFF202020);
        g.centeredText(mc.font, Component.literal("Crest"), crestX + BTN_W / 2, y + (BTN_H - 8) / 2,
            crestHovered ? 0xFFFFFFA0 : 0xFFFFFFFF);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void crest$onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> ci) {
        if (event.button() != 0) return;
        if (modulesHovered) {
            CrestMenu.open();
            ci.cancel();
            ci.setReturnValue(true);
        } else if (crestHovered) {
            Minecraft.getInstance().setScreen(new StreamerSettingsScreen((TitleScreen)(Object)this));
            ci.cancel();
            ci.setReturnValue(true);
        }
    }
}

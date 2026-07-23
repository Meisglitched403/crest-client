package com.crest.client.core.mixin.gui;

import com.crest.client.core.CrestMenu;
import com.crest.client.core.ResourcePackBrowserScreen;
import com.crest.client.core.StreamerSettingsScreen;
import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Theme;
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
    @Unique private boolean packsHovered;
    @Unique private boolean lastModulesHovered;
    @Unique private boolean lastCrestHovered;
    @Unique private boolean lastPacksHovered;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void crest$drawButtons(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth();
        int h = g.guiHeight();

        int modulesX = w / 2 - BTN_W * 3 / 2 - 4;
        int packsX = w / 2 - BTN_W / 2;
        int crestX = w / 2 + BTN_W / 2 + 4;
        int y = h - 24;

        modulesHovered = mx >= modulesX && mx <= modulesX + BTN_W && my >= y && my <= y + BTN_H;
        packsHovered = mx >= packsX && mx <= packsX + BTN_W && my >= y && my <= y + BTN_H;
        crestHovered = mx >= crestX && mx <= crestX + BTN_W && my >= y && my <= y + BTN_H;

        if (modulesHovered && !lastModulesHovered) com.crest.client.ui.UiSounds.hover();
        if (packsHovered && !lastPacksHovered) com.crest.client.ui.UiSounds.hover();
        if (crestHovered && !lastCrestHovered) com.crest.client.ui.UiSounds.hover();
        lastModulesHovered = modulesHovered;
        lastPacksHovered = packsHovered;
        lastCrestHovered = crestHovered;

        int btnBase = ColorUtil.lerpARGB(Theme.BACKGROUND, Theme.FOREGROUND, 0.10f);
        int btnHover = ColorUtil.lerpARGB(Theme.BACKGROUND, Theme.FOREGROUND, 0.22f);
        g.fill(modulesX, y, modulesX + BTN_W, y + BTN_H, modulesHovered ? btnHover : btnBase);
        g.centeredText(mc.font, Component.literal("Modules"), modulesX + BTN_W / 2, y + (BTN_H - 8) / 2,
            modulesHovered ? Theme.getAnimatedAccent() : Theme.FOREGROUND);

        g.fill(packsX, y, packsX + BTN_W, y + BTN_H, packsHovered ? btnHover : btnBase);
        g.centeredText(mc.font, Component.literal("Packs"), packsX + BTN_W / 2, y + (BTN_H - 8) / 2,
            packsHovered ? Theme.getAnimatedAccent() : Theme.FOREGROUND);

        g.fill(crestX, y, crestX + BTN_W, y + BTN_H, crestHovered ? btnHover : btnBase);
        g.centeredText(mc.font, Component.literal("Crest"), crestX + BTN_W / 2, y + (BTN_H - 8) / 2,
            crestHovered ? Theme.getAnimatedAccent() : Theme.FOREGROUND);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void crest$onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> ci) {
        if (event.button() != 0) return;
        if (modulesHovered) {
            com.crest.client.ui.UiSounds.click();
            CrestMenu.open();
            ci.cancel();
            ci.setReturnValue(true);
        } else if (packsHovered) {
            com.crest.client.ui.UiSounds.click();
            Minecraft.getInstance().setScreen(new ResourcePackBrowserScreen((TitleScreen)(Object)this));
            ci.cancel();
            ci.setReturnValue(true);
        } else if (crestHovered) {
            com.crest.client.ui.UiSounds.click();
            Minecraft.getInstance().setScreen(new StreamerSettingsScreen((TitleScreen)(Object)this));
            ci.cancel();
            ci.setReturnValue(true);
        }
    }
}

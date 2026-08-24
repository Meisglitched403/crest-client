package com.crest.client.core.mixin.gui;

import com.crest.client.core.CrestMenu;
import com.crest.client.core.ResourcePackBrowserScreen;
import com.crest.client.core.StreamerSettingsScreen;
import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;
import com.crest.client.ui.UiSounds;
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
    @Unique private static final int BTN_W = 88;
    @Unique private static final int BTN_H = 28;

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

        int gap = 8;
        int totalW = BTN_W * 3 + gap * 2;
        int modulesX = w / 2 - totalW / 2;
        int packsX = modulesX + BTN_W + gap;
        int crestX = packsX + BTN_W + gap;
        int y = h - 40;

        modulesHovered = mx >= modulesX && mx <= modulesX + BTN_W && my >= y && my <= y + BTN_H;
        packsHovered = mx >= packsX && mx <= packsX + BTN_W && my >= y && my <= y + BTN_H;
        crestHovered = mx >= crestX && mx <= crestX + BTN_W && my >= y && my <= y + BTN_H;

        if (modulesHovered && !lastModulesHovered) UiSounds.hover();
        if (packsHovered && !lastPacksHovered) UiSounds.hover();
        if (crestHovered && !lastCrestHovered) UiSounds.hover();
        lastModulesHovered = modulesHovered;
        lastPacksHovered = packsHovered;
        lastCrestHovered = crestHovered;

        int accent = Theme.getAnimatedAccent();
        drawButton(g, mc, "Modules", modulesX, y, modulesHovered, accent);
        drawButton(g, mc, "Packs", packsX, y, packsHovered, accent);
        drawButton(g, mc, "Crest", crestX, y, crestHovered, accent);

        String brand = "Crest";
        g.text(mc.font, Component.literal(brand), w - 16 - mc.font.width(brand), 12,
            ColorUtil.withAlpha(Theme.MUTED_FOREGROUND, 140));
        int bx = w - 16;
        g.text(mc.font, Component.literal("."), bx, 12, accent);
    }

    @Unique private void drawButton(GuiGraphicsExtractor g, Minecraft mc, String label, int x, int y, boolean hovered, int accent) {
        int fill = hovered
            ? ColorUtil.withAlpha(accent, 215)
            : ColorUtil.withAlpha(Theme.CARD, 225);
        Panel.draw(g, x, y, BTN_W, BTN_H, fill);
        Panel.drawHollowRect(g, x, y, BTN_W, BTN_H, hovered ? accent : Theme.BORDER_LIGHT);
        g.centeredText(mc.font, Component.literal(label), x + BTN_W / 2, y + (BTN_H - 8) / 2,
            hovered ? 0xFFFFFFFF : Theme.FOREGROUND);
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

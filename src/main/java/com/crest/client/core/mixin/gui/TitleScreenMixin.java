package com.crest.client.core.mixin.gui;

import com.crest.client.core.CrestMenu;
import com.crest.client.core.StreamerSettingsScreen;
import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Unique private static final int SIDEBAR_W = 44;
    @Unique private static final int BTN_W = 180;
    @Unique private static final int BTN_H = 34;
    @Unique private static final int BTN_GAP = 10;
    @Unique private static final int BTN_R = 6;

    @Unique private int crest$hoveredBtn = -1;
    @Unique private int crest$hoveredSide = -1;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void crest$render(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
        ci.cancel();
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth();
        int h = g.guiHeight();

        // background
        g.fill(0, 0, w, h, 0xFF0A0A1A);
        g.fill(0, 0, w, h, 0x22000044);

        // subtle accent glow at top
        int glowH = h / 3;
        for (int i = 0; i < glowH; i++) {
            int a = (int) (18 * (1 - (float) i / glowH));
            g.fill(0, i, w, i + 1, (a << 24) | 0x4444FF);
        }

        // sidebar
        g.fill(0, 0, SIDEBAR_W, h, 0xCC141428);
        g.fill(SIDEBAR_W, 0, SIDEBAR_W + 1, h, 0x332A2A55);

        // sidebar buttons
        String[] sideIcons = {"☰", "▶", "⚙"};
        String[] sideTips = {"Mods", "Streamer", "Theme"};
        int sideY = h / 2 - 50;
        for (int i = 0; i < sideIcons.length; i++) {
            int bx = (SIDEBAR_W - 28) / 2;
            int by = sideY + i * 40;
            boolean hover = mx >= bx && mx <= bx + 28 && my >= by && my <= by + 28;
            if (hover) crest$hoveredSide = i;
            int bg = hover ? Theme.BG_HOVER : 0x00000000;
            g.fill(bx, by, bx + 28, by + 28, bg);
            g.centeredText(mc.font, Component.literal(sideIcons[i]), bx + 14, by + 6, hover ? Theme.TEXT : Theme.TEXT_DIM);
        }

        // title
        String title = "Crest Client";
        int titleY = h / 4 - 20;
        int titleX = SIDEBAR_W + (w - SIDEBAR_W) / 2;
        // shadow
        g.centeredText(mc.font, Component.literal(title), titleX + 1, titleY + 1, 0x55000000);
        g.centeredText(mc.font, Component.literal(title), titleX, titleY, Theme.getAnimatedAccent());

        // subtitle
        String sub = "Minecraft Enhancement Client";
        g.centeredText(mc.font, Component.literal(sub), titleX, titleY + 14, Theme.TEXT_DIM);

        // version
        String ver = "v1.0.0";
        g.text(mc.font, Component.literal(ver), w - 36 - mc.font.width(ver), h - 14, Theme.TEXT_FAINT);

        // main buttons
        String[] btnLabels = {"Singleplayer", "Multiplayer", "Options", "Quit Game"};
        int btnAreaX = SIDEBAR_W + (w - SIDEBAR_W - BTN_W) / 2;
        int btnStartY = h / 2 - 10;
        crest$hoveredBtn = -1;

        for (int i = 0; i < btnLabels.length; i++) {
            int by = btnStartY + i * (BTN_H + BTN_GAP);
            boolean hover = mx >= btnAreaX && mx <= btnAreaX + BTN_W && my >= by && my <= by + BTN_H;
            if (hover) crest$hoveredBtn = i;

            int tint = hover
                ? ColorUtil.withAlpha(Theme.BG_HOVER, 220)
                : ColorUtil.withAlpha(Theme.BG_PANEL, 200);
            Panel.draw(g, btnAreaX, by, BTN_W, BTN_H, tint);

            if (hover) {
                g.fill(btnAreaX, by, btnAreaX + 3, by + BTN_H, Theme.getAnimatedAccent());
            }

            g.centeredText(mc.font, Component.literal(btnLabels[i]), btnAreaX + BTN_W / 2, by + (BTN_H - 8) / 2, Theme.TEXT);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void crest$onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> ci) {
        double mx = event.x();
        double my = event.y();
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        // sidebar
        int sideY = h / 2 - 50;
        for (int i = 0; i < 3; i++) {
            int bx = (SIDEBAR_W - 28) / 2;
            int by = sideY + i * 40;
            if (mx >= bx && mx <= bx + 28 && my >= by && my <= by + 28) {
                if (i == 0) CrestMenu.open();
                else if (i == 1) mc.setScreen(new StreamerSettingsScreen((TitleScreen)(Object)this));
                else if (i == 2) { /* theme toggle placeholder */ }
                ci.setReturnValue(true);
                ci.cancel();
                return;
            }
        }

        // main buttons
        String[] btnLabels = {"Singleplayer", "Multiplayer", "Options", "Quit Game"};
        int btnAreaX = SIDEBAR_W + (w - SIDEBAR_W - BTN_W) / 2;
        int btnStartY = h / 2 - 10;
        for (int i = 0; i < btnLabels.length; i++) {
            int by = btnStartY + i * (BTN_H + BTN_GAP);
            if (mx >= btnAreaX && mx <= btnAreaX + BTN_W && my >= by && my <= by + BTN_H) {
                TitleScreen self = (TitleScreen)(Object)this;
                if (i == 0) mc.setScreen(new SelectWorldScreen(self));
                else if (i == 1) mc.setScreen(new JoinMultiplayerScreen(self));
                else if (i == 2) mc.setScreen(new OptionsScreen(self, mc.options, false));
                else if (i == 3) mc.stop();
                ci.setReturnValue(true);
                ci.cancel();
                return;
            }
        }

        ci.cancel();
        ci.setReturnValue(false);
    }
}

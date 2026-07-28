package com.crest.client.catstrokes;

import com.crest.client.core.HudModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class CatStrokesModule extends HudModule {
    public CatStrokesModule() {
        super(-1, -1);
    }

    @Override
    public String getId() { return "bango_cat"; }

    @Override
    public String getName() { return "BangoCat"; }

    @Override
    public String getCategory() { return "HUD"; }

    @Override
    public String getDescription() { return "Bango Cat standard mode: catbg with keyboard overlays and left hand."; }

    @Override
    public int getWidth() {
        return (int) (CatStrokesTextures.TEX_W * CatStrokesConfig.getInstance().scale);
    }

    @Override
    public int getHeight() {
        return (int) (CatStrokesTextures.TEX_H * CatStrokesConfig.getInstance().scale);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        if (mc.getWindow() == null) return;
        KeyStateTracker input = KeyStateTracker.getInstance();
        if (!input.tryInit()) return;
        input.update();

        int dw = getWidth();
        int dh = getHeight();
        int bx, by;
        if (x < 0) {
            bx = (mc.getWindow().getGuiScaledWidth() - dw) / 2;
        } else {
            bx = x;
        }
        if (y < 0) {
            by = mc.getWindow().getGuiScaledHeight() - dh - 4;
        } else {
            by = y;
        }

        CatStrokesOverlay.render(g, mc, input, bx, by, CatStrokesConfig.getInstance().scale);
    }
}

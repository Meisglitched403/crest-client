package com.crest.client.bongocat;

import com.crest.client.core.CrestModule;
import com.crest.client.core.CrestModules;
import com.crest.client.core.HudModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;

/**
 * ponytail: Bongo Cat as a normal HUD module, so it is positioned/dragged via
 * the standard Crest HUD editor (no separate drag GUI). Uses the real
 * bongo-cat-obs sprites via BongoCatOverlay.
 */
public class BongoCatModule extends HudModule {

    public BongoCatModule() {
        super(-1, -1); // default bottom-center (resolved at render time)
    }

    @Override
    public String getId() { return "bongocat"; }

    @Override
    public String getName() { return "Bongo Cat"; }

    @Override
    public String getCategory() { return "HUD"; }

    @Override
    public String getDescription() { return "Real Bongo Cat: left paw on WASD/Shift/Ctrl, right paw follows the mouse."; }

    @Override
    public int getWidth() {
        return (int) (BongoCatTextures.TEX_W * BongoCatConfig.getInstance().scale);
    }

    @Override
    public int getHeight() {
        return (int) (BongoCatTextures.TEX_H * BongoCatConfig.getInstance().scale);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        if (mc.getWindow() == null) return;
        InputTracker input = InputTracker.getInstance();
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

        BongoCatOverlay.render(g, mc, input, BongoCatConfig.getInstance(), bx, by);
    }
}

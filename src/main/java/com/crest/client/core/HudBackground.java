package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.ColorSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;
import com.crest.client.ui.ColorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * ponytail: Shared, fully-customizable HUD background.
 *
 * Every HUD module instantiates one of these and exposes its settings. The
 * module computes its own (possibly right-anchored) top-left corner and box
 * size, then calls draw(g, x, y, w, h) so the background is ALWAYS aligned with
 * the content (this fixes the old per-module misalignment where some modules
 * filled using raw x,y instead of the anchored rx,ry).
 *
 * Style options:
 *   - Filled  : solid panel at the chosen color + opacity.
 *   - Border  : transparent interior with a 1px outline (minimal footprint).
 *   - Frosted : semi-opaque panel + a lighter top edge to mimic a glass look
 *               (cheap stand-in for a real world blur, which is too expensive
 *               for an in-world overlay).
 */
public class HudBackground {
    public final ColorSetting color = new ColorSetting("BG Color", 0x000000);
    public final IntegerSetting opacity = new IntegerSetting("BG Opacity", 0, 255, 0x66);
    public final ModeSetting style = new ModeSetting("BG Style",
        new String[]{"Filled", "Border", "Frosted"}, 0);
    public final BooleanSetting enabled = new BooleanSetting("BG Enabled", true);

    public List<Setting<?>> settings() {
        return List.of(enabled, color, opacity, style);
    }

    public void draw(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (!enabled.get() || w <= 0 || h <= 0) return;

        int a = opacity.get();
        int base = ColorUtil.argb(a, color.getR(), color.getG(), color.getB());

        switch (style.get()) {
            case 1 -> { // Border
                g.fill(x, y, x + w, y + 1, base);
                g.fill(x, y + h - 1, x + w, y + h, base);
                g.fill(x, y, x + 1, y + h, base);
                g.fill(x + w - 1, y, x + w, y + h, base);
            }
            case 2 -> { // Frosted
                g.fill(x, y, x + w, y + h, base);
                // lighter top edge to read as glass
                int edge = ColorUtil.argb(Math.min(255, a + 40),
                    Math.min(255, color.getR() + 60),
                    Math.min(255, color.getG() + 60),
                    Math.min(255, color.getB() + 60));
                g.fill(x, y, x + w, y + 1, edge);
                g.fill(x, y, x + 1, y + h, edge);
            }
            default -> g.fill(x, y, x + w, y + h, base); // Filled
        }
    }
}

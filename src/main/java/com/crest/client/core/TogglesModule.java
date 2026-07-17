package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: Toggles panel — an interactive HUD listing other Crest modules with
 * their on/off state. Left-click a row to toggle that module. Click dispatch is
 * wired through HudClickBus (driven by MouseInputMixin). Disabled by default.
 */
public class TogglesModule extends HudModule implements ClickableHud {
    private static final int PAD = 2;
    private static final int LINE_H = 11;
    private static final int COL_W = 120;

    private final BooleanSetting showCategory = new BooleanSetting("Show Category", true);

    private int drawX, drawY, drawW, drawH;

    public TogglesModule() {
        super(-1, 120);
    }

    @Override public String getId() { return "toggles"; }
    @Override public String getName() { return "Toggles Panel"; }
    @Override public String getDescription() { return "Clickable list of modules to toggle them in-game."; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>();
        s.add(showCategory);
        return s;
    }

    @Override
    public void onInitialize() {
        HudClickBus.register(this);
    }

    @Override
    public int getWidth() {
        int w = COL_W;
        Minecraft mc = Minecraft.getInstance();
        for (CrestModule m : CrestModules.getAll().values()) {
            if (m == this) continue;
            String label = m.getName() + (showCategory.get() ? " [" + m.getCategory() + "]" : "");
            w = Math.max(w, mc.font.width(label));
        }
        return w + PAD * 2;
    }

    @Override
    public int getHeight() {
        int n = 0;
        for (CrestModule m : CrestModules.getAll().values()) if (m != this) n++;
        return n > 0 ? n * LINE_H + PAD * 2 : LINE_H + PAD * 2;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        List<CrestModule> list = new ArrayList<>();
        for (CrestModule m : CrestModules.getAll().values()) if (m != this) list.add(m);

        drawW = getWidth();
        drawH = getHeight();
        drawX = x < 0 ? mc.getWindow().getGuiScaledWidth() - drawW : x;
        drawY = y;
        HudBackground.draw(g, drawX, drawY, drawW, drawH);

        int cy = drawY + PAD;
        for (CrestModule m : list) {
            boolean on = CrestModules.isEnabled(m.getId());
            String label = m.getName() + (showCategory.get() ? " [" + m.getCategory() + "]" : "");
            int dot = on ? 0xFF55FF55 : 0xFF888888;
            g.fill(drawX + PAD, cy + 2, drawX + PAD + 6, cy + 8, dot);
            g.text(mc.font, Component.literal(label), drawX + PAD + 10, cy, on ? 0xFFFFFFFF : 0xFFAAAAAA);
            cy += LINE_H;
        }
    }

    @Override
    public int hudX() { return drawX; }
    @Override
    public int hudY() { return drawY; }
    @Override
    public int hudW() { return drawW; }
    @Override
    public int hudH() { return drawH; }

    @Override
    public void onHudClick(int mouseX, int mouseY, int button) {
        List<CrestModule> list = new ArrayList<>();
        for (CrestModule m : CrestModules.getAll().values()) if (m != this) list.add(m);
        int idx = (mouseY - drawY - PAD) / LINE_H;
        if (idx >= 0 && idx < list.size()) {
            CrestModule m = list.get(idx);
            CrestModules.toggle(m.getId());
        }
    }
}

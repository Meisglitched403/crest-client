package com.crest.client.ui;

import com.crest.client.core.setting.SettingGroup;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class GroupHeaderRow implements Widget {
    private static final int HEADER_H = 28;
    private final SettingGroup group;
    private final Animated arrowAnim = new Animated(0f, 8f);

    public GroupHeaderRow(SettingGroup group) {
        this.group = group;
        arrowAnim.setImmediate(group.isExpanded() ? 1f : 0f);
    }

    @Override
    public int getWidth() {
        return 0;
    }

    public int getHeight() {
        return HEADER_H;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        arrowAnim.set(group.isExpanded() ? 1f : 0f);
        arrowAnim.tick(delta);

        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + HEADER_H;
        int bg = ColorUtil.withAlpha(hover ? Theme.SURFACE_VARIANT : Theme.CARD, hover ? 140 : 80);
        g.fill(x, y + 2, x + w, y + HEADER_H - 2, bg);

        String arrow = group.isExpanded() ? "\u25BC" : "\u25B6";
        int arrowX = x + 6;
        int arrowY = y + (HEADER_H - font.lineHeight) / 2;
        
        int arrowColor = ColorUtil.withAlpha(Theme.getAnimatedAccent(), 
            (int) (150 + arrowAnim.get() * 50));
        g.text(font, Component.literal(arrow), arrowX, arrowY, arrowColor);

        int nameX = arrowX + font.width(arrow) + 8;
        g.text(font, Component.literal(group.getName()), nameX, y + (HEADER_H - font.lineHeight) / 2,
            Theme.FOREGROUND);

        int count = group.getSettings().size();
        String countStr = count + " setting" + (count != 1 ? "s" : "");
        int countW = font.width(countStr);
        g.text(font, Component.literal(countStr), x + w - countW - 6, y + (HEADER_H - font.lineHeight) / 2,
            ColorUtil.withAlpha(Theme.MUTED_FOREGROUND, 150));

        if (hover) {
            g.fill(x + 2, y + 2, x + 4, y + HEADER_H - 2, Theme.getAnimatedAccent());
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            group.toggleExpanded();
            return true;
        }
        return false;
    }
}

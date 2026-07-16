package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Click-to-open vertical menu. */
public class DropdownMenu {
    public static final int ITEM_H = 28;

    public boolean open;

    private int anchorX, anchorY, anchorW;
    private int mx, my;

    public final List<Item> items = new ArrayList<>();

    public static final class Item {
        public final String label;
        public final Runnable onClick;
        public boolean checked;
        public boolean separator;

        public Item(String label, Runnable onClick) {
            this.label = label;
            this.onClick = onClick;
        }

        public Item separator() { this.separator = true; return this; }
    }

    public DropdownMenu add(String label, Runnable onClick) {
        items.add(new Item(label, onClick));
        return this;
    }

    public DropdownMenu addChecked(String label, boolean checked, Runnable onClick) {
        Item item = new Item(label, onClick);
        item.checked = checked;
        items.add(item);
        return this;
    }

    public void addSeparator() { items.add(new Item("", () -> {}).separator()); }

    public void toggle(int x, int y, int w) {
        open = !open;
        anchorX = x; anchorY = y; anchorW = w;
    }

    public void close() { open = false; }

    public void render(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        if (!open) return;
        mx = mouseX; my = mouseY;

        int menuW = Math.max(anchorW, 140);
        int menuH = items.size() * ITEM_H + Spacing.S1;
        int menuX = Math.min(anchorX, anchorX + anchorW - menuW);
        int menuY = anchorY + anchorW > 0 ? anchorY + 4 : anchorY - menuH - 2;

        Panel.drawElevated(g, menuX, menuY, menuW, menuH, ColorUtil.withAlpha(Theme.POPOVER, 240), Theme.ELEVATION_2);

        int iy = menuY + Spacing.S1 / 2;
        for (Item item : items) {
            if (item.separator) {
                Separator.draw(g, menuX + Spacing.S2, iy + Spacing.S1, menuW - Spacing.S4);
                iy += ITEM_H;
                continue;
            }
            boolean hover = mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= iy && mouseY <= iy + ITEM_H;
            if (hover) {
                g.fill(menuX + 2, iy, menuX + menuW - 2, iy + ITEM_H, ColorUtil.withAlpha(Theme.MUTED, 100));
            }
            String prefix = item.checked ? "\u2713 " : "  ";
            int fg = hover ? Theme.FOREGROUND : Theme.MUTED_FOREGROUND;
            g.text(font, Component.literal(prefix + item.label), menuX + Spacing.S2, iy + (ITEM_H - font.lineHeight) / 2 + 1, fg);
            iy += ITEM_H;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open) return false;

        int menuW = Math.max(anchorW, 140);
        int menuH = items.size() * ITEM_H + Spacing.S1;
        int menuX = Math.min(anchorX, anchorX + anchorW - menuW);
        int menuY = anchorY + anchorW > 0 ? anchorY + 4 : anchorY - menuH - 2;

        int iy = menuY + Spacing.S1 / 2;
        for (Item item : items) {
            if (item.separator) { iy += ITEM_H; continue; }
            if (mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= iy && mouseY <= iy + ITEM_H) {
                item.onClick.run();
                open = false;
                return true;
            }
            iy += ITEM_H;
        }

        // Click outside closes
        if (mouseX < menuX || mouseX > menuX + menuW || mouseY < menuY || mouseY > menuY + menuH) {
            open = false;
        }
        return false;
    }
}

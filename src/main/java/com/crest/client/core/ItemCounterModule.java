package com.crest.client.core;

import com.crest.client.core.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ItemCounterModule extends HudModule {
    public ItemCounterModule() {
        super(4, 120);
    }

    @Override public String getId() { return "item_counter"; }
    @Override public String getName() { return "Item Counter"; }
    @Override public String getDescription() { return "Shows durability of held item"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public int getWidth() { return 80; }

    @Override
    public int getHeight() { Minecraft mc = Minecraft.getInstance(); return mc.font.lineHeight + 4; }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        Player player = mc.player;
        if (player == null) return;

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return;

        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - getWidth() : x;

        String text;
        if (held.isDamageableItem()) {
            int cur = held.getMaxDamage() - held.getDamageValue();
            int max = held.getMaxDamage();
            int pct = max > 0 ? cur * 100 / max : 100;
            text = held.getHoverName().getString() + " " + cur + "/" + max + " (" + pct + "%)";
        } else {
            text = held.getHoverName().getString() + " x" + held.getCount();
        }

        int tw = mc.font.width(text);
        g.fill(rx, y, rx + Math.max(tw, getWidth()) + 4, y + mc.font.lineHeight + 4, 0x66000000);
        g.text(mc.font, Component.literal(text), rx + 2, y + 2, 0xFFFFFFFF);
    }
}

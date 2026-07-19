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

    // ponytail: cache the rendered text + Component; rebuild only when the held
    // item's name/damage/count actually changes.
    private String cachedKey;
    private String cachedText;
    private Component cachedComp;

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        Player player = mc.player;
        if (player == null) return;

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return;

        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - getRenderWidth() : x;

        String key;
        String text;
        if (held.isDamageableItem()) {
            int cur = held.getMaxDamage() - held.getDamageValue();
            int max = held.getMaxDamage();
            int pct = max > 0 ? cur * 100 / max : 100;
            key = "dmg:" + cur + ":" + max;
            text = held.getHoverName().getString() + " " + cur + "/" + max + " (" + pct + "%)";
        } else {
            key = "cnt:" + held.getCount();
            text = held.getHoverName().getString() + " x" + held.getCount();
        }

        if (!key.equals(cachedKey) || cachedComp == null) {
            cachedKey = key;
            cachedText = text;
            cachedComp = Component.literal(text);
        }

        int tw = mc.font.width(cachedText);
        HudBackground.draw(g, rx, y, Math.max(tw, getRenderWidth()) + 4, mc.font.lineHeight + 4);
        g.text(mc.font, cachedComp, rx + 2, y + 2, 0xFFFFFFFF);
    }
}

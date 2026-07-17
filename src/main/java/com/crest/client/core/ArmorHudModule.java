package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ArmorHudModule extends HudModule {
    private static final int MARGIN = 2;
    private static final int ICON_W = 16;
    private static final int BAR_W = 60;
    private static final int BAR_H = 4;
    private static final int LINE_H = 18;
    private static final int LABEL_W = 80;
    private static final int BG = 0x66000000;

    private final ModeSetting durabilityMode = new ModeSetting(
        "Durability Mode", new String[]{"Bar+%", "% only", "Num", "Bar only", "Bar+Num"}, 0
    );
    private final BooleanSetting showHeld = new BooleanSetting("Show Held Item", true);

    public ArmorHudModule() {
        super(4, 50);
    }

    @Override public String getId() { return "armor_hud"; }
    @Override public String getName() { return "Armor HUD"; }
    @Override public String getDescription() { return "Shows armor durability and held item (toggleable), with customizable background."; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>();
        s.add(durabilityMode); s.add(showHeld);
        return s;
    }

    public int getDurabilityMode() { return durabilityMode.get(); }
    public void setDurabilityMode(int mode) { durabilityMode.set(mode); }
    public void cycleDurabilityMode() {
        durabilityMode.cycle();
        CrestModules.getConfigManager().markDirty();
    }

    public String getModeLabel() { return durabilityMode.getMode(); }

    @Override
    public int getWidth() {
        return MARGIN + ICON_W + 2 + LABEL_W + BAR_W + MARGIN;
    }

    @Override
    public int getHeight() {
        int slots = 4;
        Player p = Minecraft.getInstance().player;
        if (showHeld.get() && p != null && p.getMainHandItem().isDamageableItem()) slots++;
        return MARGIN + slots * (LINE_H + MARGIN);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        Player player = mc.player;
        if (player == null) return;

        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - getWidth() : x;
        int ry = y;
        HudBackground.draw(g, rx, ry, getWidth(), getHeight());

        int cy = ry + MARGIN;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            renderSlot(g, mc, player.getItemBySlot(slot), slot.getName(), rx, cy);
            cy += LINE_H + MARGIN;
        }

        ItemStack held = player.getMainHandItem();
        if (showHeld.get() && held.isDamageableItem()) {
            renderSlot(g, mc, held, "held", rx, cy);
        }
    }

    private String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ponytail: cache per-slot text so we don't rebuild a String + Component every frame.
    private final java.util.Map<String, Component> labelCache = new java.util.HashMap<>();

    private void renderSlot(GuiGraphicsExtractor g, Minecraft mc, ItemStack stack, String label, int rx, int cy) {
        int iconX = rx + MARGIN;
        int textX = iconX + ICON_W + 2;
        int barX = rx + getWidth() - MARGIN - BAR_W;
        String display = "[" + cap(label) + "]: ";

        if (stack.isEmpty()) {
            g.text(mc.font, Component.literal(display + "--"), textX, cy + 2, 0xFF888888);
            g.fill(barX, cy + 4, barX + BAR_W, cy + 4 + BAR_H, 0x44000000);
            return;
        }

        g.item(stack, iconX, cy);

        int maxDmg = stack.getMaxDamage();
        int dmg = stack.getDamageValue();
        int cur = maxDmg - dmg;
        int pct = maxDmg > 0 ? cur * 100 / maxDmg : 100;

        int barColor = pct > 60 ? 0xFF55FF55 : pct > 30 ? 0xFFFFFF55 : 0xFFFF5555;
        int fillW = cur * BAR_W / Math.max(maxDmg, 1);

        String key = display + durabilityMode.get() + ":" + cur + ":" + maxDmg;
        Component comp = labelCache.get(key);
        if (comp == null) {
            String suffix = switch (durabilityMode.get()) {
                case 0, 4 -> pct + "%";
                default -> cur + "/" + maxDmg;
            };
            comp = Component.literal(display + suffix);
            if (labelCache.size() < 32) labelCache.put(key, comp);
        }

        int mode = durabilityMode.get();
        boolean showText = mode <= 2;        // 0,1,2 show text
        boolean showBar = mode == 0 || mode == 3 || mode == 4; // bar variants

        if (showText) g.text(mc.font, comp, textX, cy + 2, 0xFFFFFFFF);
        g.fill(barX, cy + 4, barX + BAR_W, cy + 4 + BAR_H, 0x44000000);
        if (showBar && fillW > 0) g.fill(barX, cy + 4, barX + fillW, cy + 4 + BAR_H, barColor);
    }
}

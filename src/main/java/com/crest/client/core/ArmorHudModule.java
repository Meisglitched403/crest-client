package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ArmorHudModule extends HudModule {
    private static final int MARGIN = 2;
    private static final int ICON_W = 16;
    private static final int BAR_W = 60;
    private static final int BAR_H = 4;
    private static final int LINE_H = 18;
    private static final int LABEL_W = 80;
    private static final int BG = 0x66000000;

    private int durabilityMode = 0;

    public ArmorHudModule() {
        super(4, 50);
    }

    @Override public String getId() { return "armor_hud"; }
    @Override public String getName() { return "Armor HUD"; }
    @Override public String getDescription() { return "Shows armor durability and held item"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public void loadSettings() {
        int mode = HudSettings.getInt(getId(), "durabilityMode", 0);
        if (mode != 0) setDurabilityMode(mode);
    }

    public int getDurabilityMode() { return durabilityMode; }
    public void setDurabilityMode(int mode) { this.durabilityMode = Math.max(0, Math.min(2, mode)); }
    public void cycleDurabilityMode() {
        durabilityMode = (durabilityMode + 1) % 3;
        HudSettings.setInt("armor_hud", "durabilityMode", durabilityMode);
        HudSettings.save();
    }

    public String getModeLabel() {
        return switch (durabilityMode) {
            case 0 -> "Bar+%";
            case 1 -> "% only";
            case 2 -> "Num";
            default -> "";
        };
    }

    @Override
    public int getWidth() {
        return MARGIN + ICON_W + 2 + LABEL_W + BAR_W + MARGIN;
    }

    @Override
    public int getHeight() {
        int slots = 4;
        Player p = Minecraft.getInstance().player;
        if (p != null && p.getMainHandItem().isDamageableItem()) slots++;
        return MARGIN + slots * (LINE_H + MARGIN);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        Player player = mc.player;
        if (player == null) return;

        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - getWidth() : x;
        int cy = y + MARGIN;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            renderSlot(g, mc, player.getItemBySlot(slot), slot.getName(), rx, cy);
            cy += LINE_H + MARGIN;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isDamageableItem()) {
            renderSlot(g, mc, held, "held", rx, cy);
        }
    }

    private String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void renderSlot(GuiGraphicsExtractor g, Minecraft mc, ItemStack stack, String label, int rx, int cy) {
        int iconX = rx + MARGIN;
        int textX = iconX + ICON_W + 2;
        int barX = rx + getWidth() - MARGIN - BAR_W;
        String display = "[" + cap(label) + "]: ";

        if (stack.isEmpty()) {
            g.text(mc.font, Component.literal(display + "--"), textX, cy + 2, 0x888888);
            g.fill(barX, cy + 4, barX + BAR_W, cy + 4 + BAR_H, 0x44000000);
            return;
        }

        g.item(stack, iconX, cy);

        int maxDmg = stack.getMaxDamage();
        int dmg = stack.getDamageValue();
        int cur = maxDmg - dmg;
        int pct = maxDmg > 0 ? cur * 100 / maxDmg : 100;

        switch (durabilityMode) {
            case 0 -> {
                g.text(mc.font, Component.literal(display + pct + "%"), textX, cy + 2, 0xFFFFFF);
                int barColor = pct > 60 ? 0xFF55FF55 : pct > 30 ? 0xFFFFFF55 : 0xFFFF5555;
                int fillW = cur * BAR_W / Math.max(maxDmg, 1);
                g.fill(barX, cy + 4, barX + BAR_W, cy + 4 + BAR_H, 0x44000000);
                if (fillW > 0) g.fill(barX, cy + 4, barX + fillW, cy + 4 + BAR_H, barColor);
            }
            case 1 -> g.text(mc.font, Component.literal(display + pct + "%"), textX, cy + 2, 0xFFFFFF);
            case 2 -> g.text(mc.font, Component.literal(display + cur + "/" + maxDmg), textX, cy + 2, 0xFFFFFF);
        }
    }
}

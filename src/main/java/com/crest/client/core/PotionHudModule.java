package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Collection;

public class PotionHudModule extends HudModule {
    private static final int LINE_H = 14;
    private static final int PAD = 2;
    private static final int BAR_W = 4;
    private static final int BG = 0x66000000;

    public PotionHudModule() {
        super(-1, 4);
    }

    @Override
    public String getId() { return "potion_hud"; }
    @Override
    public String getName() { return "Potion HUD"; }
    @Override
    public String getDescription() { return "Shows active potion effects with timers"; }
    @Override
    public boolean isEnabled() { return false; }

    @Override
    public int getWidth() { return 130; }

    @Override
    public int getHeight() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 12;
        int count = mc.player.getActiveEffects().size();
        return count == 0 ? 12 : count * LINE_H + PAD;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        if (mc.player == null) return;
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - getWidth() : x;
        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        if (effects.isEmpty()) {
            g.fill(rx, y, rx + getWidth(), y + getHeight(), BG);
            g.text(mc.font, Component.literal("No effects"), rx + PAD, y + PAD, 0x888888);
            return;
        }

        int i = 0;
        for (MobEffectInstance effect : effects) {
            int cy = y + PAD + i * LINE_H;
            Holder<MobEffect> holder = effect.getEffect();
            MobEffect type = holder.value();
            int color = type.getColor();
            if (color == 0) color = 0x888888;
            int argb = 0xFF000000 | (color & 0x00FFFFFF);

            g.fill(rx, cy, rx + BAR_W, cy + LINE_H, argb);
            g.fill(rx + BAR_W, cy, rx + getWidth(), cy + LINE_H, BG);

            String name = Component.translatable(effect.getDescriptionId()).getString();
            int amp = effect.getAmplifier() + 1;
            int ticks = effect.getDuration();
            int secs = ticks / 20;
            String time = secs >= 3600
                ? String.format("%d:%02d:%02d", secs / 3600, (secs % 3600) / 60, secs % 60)
                : String.format("%d:%02d", secs / 60, secs % 60);
            String label = amp > 1 ? name + " " + toRoman(amp) + " " + time : name + " " + time;

            g.text(mc.font, Component.literal(label), rx + BAR_W + PAD, cy + PAD, 0xFFFFFF);
            i++;
        }
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(n);
        };
    }
}

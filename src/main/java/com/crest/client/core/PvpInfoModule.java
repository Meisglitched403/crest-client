package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: PvpInfo HUD. When the crosshair is on a living entity, overlays its
 * health, armor, absorption, and (for players) ping. Pure HUD readout.
 */
public class PvpInfoModule extends HudModule {
    private static final int PAD = 2;
    private static final int LINE_H = 10;

    private final HudBackground bg = new HudBackground();
    private final BooleanSetting showPing = new BooleanSetting("Show Ping", true);
    private final BooleanSetting showArmor = new BooleanSetting("Show Armor", true);
    private final BooleanSetting showHealthBar = new BooleanSetting("Show Health Bar", true);

    public PvpInfoModule() {
        super(-1, 200);
    }

    @Override public String getId() { return "pvpingo"; }
    @Override public String getName() { return "PvP Info"; }
    @Override public String getDescription() { return "Shows health/armor/ping of the entity you are looking at."; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>(bg.settings());
        s.add(showPing);
        s.add(showArmor);
        s.add(showHealthBar);
        return s;
    }

    @Override
    public int getWidth() {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.font.width("Name: Unknown");
        w = Math.max(w, mc.font.width("HP: 20.0 / 20.0"));
        if (showPing.get()) w = Math.max(w, mc.font.width("Ping: 0ms"));
        if (showArmor.get()) w = Math.max(w, mc.font.width("Armor: 20"));
        return w + PAD * 2;
    }

    @Override
    public int getHeight() {
        int lines = 2;
        if (showPing.get()) lines++;
        if (showArmor.get()) lines++;
        if (showHealthBar.get()) lines++;
        return lines * LINE_H + PAD * 2;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        Entity target = mc.crosshairPickEntity;
        if (!(target instanceof LivingEntity living)) return;
        if (mc.player == null) return;

        int boxW = getWidth();
        int boxH = getHeight();
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - boxW : x;
        int ry = y;
        bg.draw(g, rx, ry, boxW, boxH);

        int cy = ry + PAD;
        g.text(mc.font, Component.literal("Name: " + living.getName().getString()), rx + PAD, cy, 0xFFFFFFFF);
        cy += LINE_H;

        float hp = living.getHealth();
        float maxHp = living.getMaxHealth();
        float absorb = living.getAbsorptionAmount();
        g.text(mc.font, Component.literal("HP: " + String.format("%.1f", hp) + " / " + String.format("%.1f", maxHp)
            + (absorb > 0 ? " (+" + String.format("%.1f", absorb) + ")" : "")), rx + PAD, cy, 0xFFFF6666);
        cy += LINE_H;

        if (showHealthBar.get()) {
            int barW = boxW - PAD * 2;
            int fillW = (int) (barW * Math.min(1.0F, hp / maxHp));
            g.fill(rx + PAD, cy, rx + PAD + barW, cy + 3, 0xFF330000);
            g.fill(rx + PAD, cy, rx + PAD + fillW, cy + 3, 0xFF55FF55);
            cy += LINE_H;
        }

        if (showArmor.get()) {
            g.text(mc.font, Component.literal("Armor: " + living.getArmorValue()), rx + PAD, cy, 0xFFAAAAAA);
            cy += LINE_H;
        }

        if (showPing.get() && living instanceof Player) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(living.getUUID());
            int ping = info != null ? info.getLatency() : -1;
            g.text(mc.font, Component.literal("Ping: " + (ping >= 0 ? ping + "ms" : "?")), rx + PAD, cy, 0xFF55CCFF);
        }
    }
}

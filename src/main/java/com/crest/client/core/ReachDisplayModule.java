package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: ReachDisplay HUD. Shows the distance from the player to the entity
 * currently under the crosshair, colored by whether that distance is within the
 * player's attack reach (via Player.isWithinAttackRange).
 */
public class ReachDisplayModule extends HudModule {
    private static final int PAD = 2;
    private static final int LINE_H = 10;

    private final HudBackground bg = new HudBackground();
    private final BooleanSetting showMax = new BooleanSetting("Show Max Reach", true);
    private final BooleanSetting showTarget = new BooleanSetting("Show Target Name", true);

    public ReachDisplayModule() {
        super(4, 150);
    }

    @Override public String getId() { return "reach"; }
    @Override public String getName() { return "Reach Display"; }
    @Override public String getDescription() { return "Shows your current attack reach to the crosshair target."; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>(bg.settings());
        s.add(showMax);
        s.add(showTarget);
        return s;
    }

    @Override
    public int getWidth() {
        return Math.max(
            Minecraft.getInstance().font.width("Reach: 0.00"),
            Minecraft.getInstance().font.width("Max: 0.00")) + PAD * 2;
    }

    @Override
    public int getHeight() {
        int lines = 1;
        if (showMax.get()) lines++;
        if (showTarget.get()) lines++;
        return lines * LINE_H + PAD * 2;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        Entity target = mc.crosshairPickEntity;
        Player player = mc.player;
        if (player == null || target == null) return;

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 tpos = target.position();
        double dist = eye.distanceTo(tpos);

        ItemStack stack = player.getMainHandItem();
        AABB box = target.getBoundingBox();
        // pad the box slightly so the boundary reads fairly
        boolean within = player.isWithinAttackRange(stack, box, dist + 0.01);

        int boxW = getWidth();
        int boxH = getHeight();
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - boxW : x;
        int ry = y;
        bg.draw(g, rx, ry, boxW, boxH);

        int cy = ry + PAD;
        int col = within ? 0xFF55FF55 : 0xFFFF5555;
        g.text(mc.font, Component.literal("Reach: " + String.format("%.2f", dist)), rx + PAD, cy, col);
        cy += LINE_H;

        if (showTarget.get()) {
            g.text(mc.font, Component.literal(target.getName().getString()), rx + PAD, cy, 0xFFCCCCCC);
            cy += LINE_H;
        }

        if (showMax.get()) {
            double maxReach = findMaxReach(player, stack, box);
            g.text(mc.font, Component.literal("Max: " + String.format("%.2f", maxReach)), rx + PAD, cy, 0xFFAAAAAA);
        }
    }

    private static double findMaxReach(Player player, ItemStack stack, AABB box) {
        // binary search the reachable distance along the box axis
        double lo = 0.0, hi = 12.0;
        for (int i = 0; i < 24; i++) {
            double mid = (lo + hi) / 2.0;
            if (player.isWithinAttackRange(stack, box, mid)) lo = mid;
            else hi = mid;
        }
        return lo;
    }
}

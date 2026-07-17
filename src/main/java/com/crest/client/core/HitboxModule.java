package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.ColorSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: Hitbox + HitColor. Draws entity bounding boxes in the world via the
 * LevelRenderer.renderLevel mixin (WorldRenderEvents was removed in this MC
 * version), so colors can be a solid color or "smart" (reachable = green,
 * out-of-range = red, teammate = blue).
 */
public class HitboxModule implements CrestModule {
    private final BooleanSetting showPlayers = new BooleanSetting("Players", true);
    private final BooleanSetting showMobs = new BooleanSetting("Mobs", true);
    private final BooleanSetting showAnimals = new BooleanSetting("Animals", true);
    private final BooleanSetting filled = new BooleanSetting("Filled", false);
    private final ColorSetting color = new ColorSetting("Color", 0xFF55FF55);
    private final ModeSetting colorMode = new ModeSetting("Color Mode",
        new String[]{"Solid", "Smart"}, 1);
    private final IntegerSetting lineWidth = new IntegerSetting("Line Width", 1, 6, 2);
    private final IntegerSetting range = new IntegerSetting("Range", 1, 200, 64);

    public HitboxModule() {}

    @Override public String getId() { return "hitbox"; }
    @Override public String getName() { return "Hitbox"; }
    @Override public String getDescription() { return "Draws entity hitboxes with smart/reach-based coloring."; }
    @Override public String getCategory() { return "Render"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>();
        s.add(showPlayers); s.add(showMobs); s.add(showAnimals);
        s.add(filled); s.add(colorMode); s.add(color); s.add(lineWidth); s.add(range);
        return s;
    }

    public void drawWorldBoxes(PoseStack ps, MultiBufferSource.BufferSource buffer, CameraRenderState cam) {
        if (!CrestModules.isEnabled("hitbox")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (cam == null) return;

        var lines = buffer.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.lines());
        var box = filled.get()
            ? buffer.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.debugFilledBox())
            : null;

        double r2 = (double) range.get() * range.get();

        ps.pushPose();
        for (Entity e : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(range.get()), e -> true)) {
            if (e == mc.player) continue;
            if (!wants(e)) continue;
            if (e.distanceToSqr(mc.player) > r2) continue;

            AABB aabb = e.getBoundingBox();
            int col = pickColor(mc, e);

            VoxelShape vs = Shapes.create(aabb);
            if (box != null) {
                ShapeRenderer.renderShape(ps, box, vs, 0, 0, 0, (50 << 24) | (col & 0x00FFFFFF), 1.0F);
            }
            ShapeRenderer.renderShape(ps, lines, vs, 0, 0, 0, (0xFF << 24) | (col & 0x00FFFFFF), lineWidth.get());
        }
        ps.popPose();
        buffer.endBatch(net.minecraft.client.renderer.rendertype.RenderTypes.lines());
        if (box != null) {
            buffer.endBatch(net.minecraft.client.renderer.rendertype.RenderTypes.debugFilledBox());
        }
    }

    private boolean wants(Entity e) {
        if (e instanceof Player) return showPlayers.get();
        if (e instanceof LivingEntity) {
            return e.getType().getCategory().isFriendly() ? showAnimals.get() : showMobs.get();
        }
        return showMobs.get();
    }

    private int pickColor(Minecraft mc, Entity e) {
        if (colorMode.get() == 0) return color.get();
        Player p = mc.player;
        if (p != null && e instanceof LivingEntity) {
            if (isTeammate(mc, p, (LivingEntity) e)) return 0xFF5599FF;
            boolean within = p.isWithinAttackRange(p.getMainHandItem(), e.getBoundingBox(),
                p.getEyePosition(1.0F).distanceTo(e.position()) + 0.01);
            return within ? 0xFF55FF55 : 0xFFFF5555;
        }
        return color.get();
    }

    private static boolean isTeammate(Minecraft mc, Player p, LivingEntity other) {
        if (!(other instanceof Player)) return false;
        if (mc.level == null) return false;
        var sb = mc.level.getScoreboard();
        String a = teamOf(sb, p.getName().getString());
        String b = teamOf(sb, other.getName().getString());
        return a != null && a.equals(b);
    }

    private static String teamOf(net.minecraft.world.scores.Scoreboard sb, String name) {
        for (var t : sb.getPlayerTeams()) {
            if (t.getPlayers().contains(name)) return t.getName();
        }
        return null;
    }
}

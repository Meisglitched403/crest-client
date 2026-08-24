package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.ColorSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;
import com.crest.client.core.setting.SettingGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public List<SettingGroup> getSettingGroups() {
        return List.of(
            new SettingGroup("Entity Types", true, 
                showPlayers, showMobs, showAnimals),
            new SettingGroup("Appearance", true,
                filled, colorMode, color, lineWidth),
            new SettingGroup("Range", true, range)
        );
    }

    public void drawWorldBoxes(PoseStack ps, MultiBufferSource.BufferSource buffer, CameraRenderState cam) {
        if (!CrestModules.isEnabled("hitbox")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (cam == null) return;

        double r2 = (double) range.get() * range.get();

        List<Object[]> shapes = new ArrayList<>();
        ps.pushPose();
        for (Entity e : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(range.get()), e -> true)) {
            if (e == mc.player) continue;
            if (!wants(e)) continue;
            if (e.distanceToSqr(mc.player) > r2) continue;
            shapes.add(new Object[]{Shapes.create(e.getBoundingBox()), pickColor(mc, e)});
        }
        ps.popPose();

        if (filled.get()) {
            VertexConsumer box = buffer.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.debugFilledBox());
            for (Object[] s : shapes) {
                ShapeRenderer.renderShape(ps, box, (VoxelShape) s[0], 0, 0, 0,
                    (50 << 24) | ((Integer) s[1] & 0x00FFFFFF), 1.0F);
            }
            buffer.endBatch(net.minecraft.client.renderer.rendertype.RenderTypes.debugFilledBox());
        }

        VertexConsumer lines = buffer.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.lines());
        for (Object[] s : shapes) {
            ShapeRenderer.renderShape(ps, lines, (VoxelShape) s[0], 0, 0, 0,
                (0xFF << 24) | ((Integer) s[1] & 0x00FFFFFF), lineWidth.get());
        }
        buffer.endBatch(net.minecraft.client.renderer.rendertype.RenderTypes.lines());
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
        var map = teamMap(mc.level.getScoreboard());
        String a = map.get(p.getName().getString());
        String b = map.get(other.getName().getString());
        return a != null && a.equals(b);
    }

    // name -> team, rebuilt at most once per second. Avoids re-scanning every
    // team's roster for every entity on every frame.
    private static final Map<String, String> teamByName = new HashMap<>();
    private static long lastTeamBuild;

    private static Map<String, String> teamMap(net.minecraft.world.scores.Scoreboard sb) {
        long now = System.currentTimeMillis();
        if (lastTeamBuild == 0 || now - lastTeamBuild > 1000) {
            teamByName.clear();
            for (var t : sb.getPlayerTeams()) {
                for (String name : t.getPlayers()) {
                    teamByName.put(name, t.getName());
                }
            }
            lastTeamBuild = now;
        }
        return teamByName;
    }
}

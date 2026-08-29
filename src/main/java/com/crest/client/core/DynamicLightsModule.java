package com.crest.client.core;

import com.crest.client.core.dynlight.LightCatalog;
import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.Setting;
import com.crest.client.core.setting.SettingGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side dynamic lights. The visible effect is a smooth, radial glow
 * overlay drawn in the world (Sodium-independent, works on any server). A real
 * light-level hook (getRawBrightness) also brightens held items / the player's
 * own view. Purely visual — no server involvement, no mob-spawning impact.
 */
public class DynamicLightsModule implements CrestModule {

    private final IntegerSetting intensity = new IntegerSetting("Intensity", 1, 10, 6);
    private final IntegerSetting range = new IntegerSetting("Range", 5, 32, 16);
    private final BooleanSetting otherPlayers = new BooleanSetting("Other Players", true);
    private final BooleanSetting mobLights = new BooleanSetting("Mob Lights", true);
    private final BooleanSetting enchantedGlow = new BooleanSetting("Enchanted Glow", true);
    private final BooleanSetting tinted = new BooleanSetting("Tinted Glows", true);

    @Override public String getId() { return "dynamic_lights"; }
    @Override public String getName() { return "Dynamic Lights"; }
    @Override public String getDescription() {
        return "Glow around light items / glowing mobs (client-side, any server).";
    }
    @Override public String getCategory() { return "Render"; }
    // On by default (module system defaults isEnabled() -> true).

    public int getIntensity() { return intensity.get(); }
    public int getRange() { return range.get(); }
    public boolean getOtherPlayers() { return otherPlayers.get(); }
    public boolean getMobLights() { return mobLights.get(); }
    public boolean getEnchantedGlow() { return enchantedGlow.get(); }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(intensity, range, otherPlayers, mobLights, enchantedGlow, tinted);
    }

    @Override
    public List<SettingGroup> getSettingGroups() {
        return List.of(
            new SettingGroup("Appearance", true, intensity, tinted, enchantedGlow),
            new SettingGroup("Sources", true, otherPlayers, mobLights),
            new SettingGroup("Range", true, range)
        );
    }

    private static final class Source {
        final Vec3 pos;
        final int level;
        final int rgb;
        Source(Vec3 pos, int level, int rgb) { this.pos = pos; this.level = level; this.rgb = rgb; }
    }

    public void renderWorld(PoseStack ps, MultiBufferSource.BufferSource buffer, CameraRenderState crs) {
        if (!CrestModules.isEnabled("dynamic_lights")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || crs == null) return;

        Vec3 cam = new Vec3(crs.pos.x, crs.pos.y, crs.pos.z);
        double k = intensity.get() / 10.0;
        double r = range.get();
        double r2 = r * r;

        List<Source> sources = new ArrayList<>();
        addHandGlow(mc.player, sources);
        if (otherPlayers.get()) {
            for (Player p : mc.level.players()) {
                if (p == mc.player) continue;
                if (mc.player.distanceToSqr(p) > r2) continue;
                addHandGlow(p, sources);
            }
        }
        if (mobLights.get()) {
            for (Entity e : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(r), x -> true)) {
                if (mc.player.distanceToSqr(e) > r2) continue;
                int level = LightCatalog.entityLevel(e.getType());
                int tint = LightCatalog.entityTint(e.getType());
                if (level == 0 && e instanceof ItemEntity ie) {
                    ItemStack it = ie.getItem();
                    level = LightCatalog.itemLevel(it, enchantedGlow.get());
                    if (level > 0) tint = LightCatalog.itemTint(it);
                }
                if (level == 0 && e.isOnFire() && !(e instanceof Player)) {
                    level = 15; tint = LightCatalog.TINT_FIRE;
                }
                if (level > 0) {
                    Vec3 c = e.getBoundingBox().getCenter();
                    int rgb = tinted.get() ? (tint & 0xFFFFFF) : (LightCatalog.TINT_WARM & 0xFFFFFF);
                    sources.add(new Source(c, level, rgb));
                }
            }
        }

        if (sources.isEmpty()) return;

        // Camera-facing billboard basis.
        Vec3 dir = new Vec3(mc.player.getX() - cam.x, mc.player.getY() - cam.y, mc.player.getZ() - cam.z);
        if (dir.lengthSqr() < 1e-6) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        Vec3 worldUp = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(worldUp).normalize();
        Vec3 up = right.cross(dir).normalize();

        VertexConsumer vc = buffer.getBuffer(RenderTypes.debugTriangleFan());
        int SIDES = 20;

        for (Source s : sources) {
            double R = 0.5 + (s.level / 15.0) * (1.3 + intensity.get() * 0.07);
            int aCore = (int) (255 * (0.18 + 0.22 * k) * (s.level / 15.0));
            if (aCore < 8) aCore = 8;
            int cr = (s.rgb >> 16) & 0xFF, cg = (s.rgb >> 8) & 0xFF, cb = s.rgb & 0xFF;

            // Camera-facing glow (opaque center -> transparent rim = smooth gradient).
            emitGradientDisc(vc, ps, s.pos, right, up, R, SIDES, cr, cg, cb, aCore, 0);
            // Crossed plane so the glow reads as volumetric from any angle.
            emitGradientDisc(vc, ps, s.pos, up, dir, R, SIDES, cr, cg, cb, aCore, 0);
            // Wide, faint outer halo for a soft ambient falloff.
            emitGradientDisc(vc, ps, s.pos, right, up, R * 1.9, SIDES, cr, cg, cb, (int) (aCore * 0.4), 0);

            // Ground pool: soft disc on the floor beneath the source.
            double gy = Math.floor(s.pos.y) + 0.06;
            double Rp = Math.min(R * 2.4, 3.0);
            emitGradientDisc(vc, ps, new Vec3(s.pos.x, gy, s.pos.z),
                new Vec3(1, 0, 0), new Vec3(0, 0, 1), Rp, SIDES, cr, cg, cb, (int) (aCore * 0.7), 0);
        }
        buffer.endBatch(RenderTypes.debugTriangleFan());
    }

    private void addHandGlow(Player p, List<Source> out) {
        Vec3 hand = new Vec3(p.getX(), p.getY() + 1.1, p.getZ());
        addStack(p.getMainHandItem(), hand, out);
        addStack(p.getOffhandItem(), hand, out);
    }

    private void addStack(ItemStack stack, Vec3 hand, List<Source> out) {
        int level = LightCatalog.itemLevel(stack, enchantedGlow.get());
        if (level <= 0) return;
        int tint = LightCatalog.itemTint(stack);
        int rgb = tinted.get() ? (tint & 0xFFFFFF) : (LightCatalog.TINT_WARM & 0xFFFFFF);
        out.add(new Source(hand, level, rgb));
    }

    /** One triangle fan with a radial gradient: opaque at the center, transparent at the rim. */
    private static void emitGradientDisc(VertexConsumer vc, PoseStack ps, Vec3 center,
                                         Vec3 ax, Vec3 ay, double radius, int sides,
                                         int r, int g, int b, int aCenter, int aRim) {
        org.joml.Matrix4f pose = ps.last().pose();
        vc.addVertex(pose, (float) center.x, (float) center.y, (float) center.z)
            .setColor(r, g, b, aCenter);
        for (int i = 0; i <= sides; i++) {
            double ang = (Math.PI * 2.0) * (i / (double) sides);
            double cx = center.x + ax.x * Math.cos(ang) * radius + ay.x * Math.sin(ang) * radius;
            double cy = center.y + ax.y * Math.cos(ang) * radius + ay.y * Math.sin(ang) * radius;
            double cz = center.z + ax.z * Math.cos(ang) * radius + ay.z * Math.sin(ang) * radius;
            vc.addVertex(pose, (float) cx, (float) cy, (float) cz)
                .setColor(r, g, b, aRim);
        }
    }
}

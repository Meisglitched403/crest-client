package com.crest.client.core;

import com.crest.client.core.setting.FloatSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.particle.Particle;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: ParticleChanger. Globally scales particles, extends/shortens their
 * lifetime, and (via density) probabilistically drops spawns. Applied by
 * ParticleEngineMixin at particle creation.
 */
public class ParticleChangerModule implements CrestModule {
    public static final ParticleChangerModule INSTANCE = new ParticleChangerModule();

    private final FloatSetting scale = new FloatSetting("Scale", 0.1F, 5.0F, 1.0F);
    private final FloatSetting lifetime = new FloatSetting("Lifetime", 0.1F, 5.0F, 1.0F);
    private final IntegerSetting density = new IntegerSetting("Density %", 0, 100, 100);

    @Override public String getId() { return "particle_changer"; }
    @Override public String getName() { return "Particle Changer"; }
    @Override public String getDescription() { return "Scales, re-times, and thins out particles globally (color tint omitted)."; }
    @Override public String getCategory() { return "Render"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return new ArrayList<>(List.of(scale, lifetime, density));
    }

    public static boolean shouldDrop() {
        if (!CrestModules.isEnabled("particle_changer")) return false;
        int d = INSTANCE.density.get();
        if (d >= 100) return false;
        return RANDOM.nextInt(100) >= d;
    }

    private static final java.util.Random RANDOM = new java.util.Random();

    public static void apply(Particle p) {
        if (!CrestModules.isEnabled("particle_changer")) return;
        float s = INSTANCE.scale.get();
        if (s != 1.0F) p.scale(s);
        float l = INSTANCE.lifetime.get();
        if (l != 1.0F) p.setLifetime(Math.max(1, (int) (p.getLifetime() * l)));
    }
}

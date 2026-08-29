package com.crest.client.waypoints;

import java.util.Random;

/** Picks a colour for new waypoints by type / random mode. */
public final class WaypointColorService {
    private static final Random RNG = new Random();

    private WaypointColorService() {}

    public static int pickNewWaypointColor(ModConfig cfg, Waypoint.Type type) {
        switch (cfg.randomColorsMode) {
            case PER_WAYPOINT -> { return ColorUtil.hsvToRgb(RNG.nextFloat() * 360f, 0.7f, 0.9f); }
            case PER_GROUP -> { return ColorUtil.hsvToRgb(RNG.nextFloat() * 360f, 0.7f, 0.9f); }
            default -> {
                ModConfig.TypeDefaults d = switch (type) {
                    case DEATH -> cfg.deathWaypointAppearance != null ? toDefaults(cfg.deathWaypointAppearance) : cfg.singleWaypointDefaults;
                    case SUPPLY_DROP -> cfg.hopliteSupplyDropAppearance != null ? toDefaults(cfg.hopliteSupplyDropAppearance) : cfg.singleWaypointDefaults;
                    case AUTO_PICK -> cfg.hopliteAutoPickAppearance != null ? toDefaults(cfg.hopliteAutoPickAppearance) : cfg.singleWaypointDefaults;
                    default -> cfg.singleWaypointDefaults;
                };
                return d.color;
            }
        }
    }

    private static ModConfig.TypeDefaults toDefaults(ModConfig.HopliteAppearance a) {
        ModConfig.TypeDefaults d = new ModConfig.TypeDefaults();
        d.color = a.color;
        return d;
    }
}

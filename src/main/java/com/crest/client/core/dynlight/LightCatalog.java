package com.crest.client.core.dynlight;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Author-made catalog of which items/entities emit light and how strong.
 * This is the only place the "what glows" knowledge lives, and it was written
 * from general Minecraft knowledge (NOT copied from any third-party mod).
 *
 * Levels mirror the usual 0-15 light scale; we map them to glow alpha/radius.
 */
public final class LightCatalog {
    private LightCatalog() {}

    // item -> light level
    private static final Map<Item, Integer> ITEM_LEVEL = new HashMap<>();
    // item -> glow tint (ARGB rgb); absent -> warm default
    private static final Map<Item, Integer> ITEM_TINT = new HashMap<>();
    // entity type -> light level
    private static final Map<EntityType<?>, Integer> ENTITY_LEVEL = new HashMap<>();
    private static final Map<EntityType<?>, Integer> ENTITY_TINT = new HashMap<>();

    public static final int TINT_WARM = 0xD9A066;
    public static final int TINT_COOL = 0xCCEEFF;
    public static final int TINT_FIRE = 0xFF9030;
    public static final int TINT_PURPLE = 0xB18CFF;
    public static final int TINT_CYAN = 0x7DE8FF;
    public static final int TINT_PALE = 0xBBCCFF;
    public static final int TINT_GREEN = 0x9CE8A0;

    static {
        put(15, TINT_WARM,
            Items.TORCH, Items.LANTERN, Items.SOUL_LANTERN,
            Items.CAMPFIRE, Items.SOUL_CAMPFIRE,
            Items.GLOWSTONE, Items.SEA_LANTERN, Items.OCHRE_FROGLIGHT,
            Items.VERDANT_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT,
            Items.JACK_O_LANTERN, Items.MAGMA_BLOCK, Items.SHROOMLIGHT,
            Items.LAVA_BUCKET, Items.FIRE_CHARGE);

        put(12, TINT_COOL,
            Items.END_ROD, Items.GLOW_INK_SAC, Items.GLOW_BERRIES,
            Items.GLOW_LICHEN, Items.SCULK_CATALYST, Items.VAULT,
            Items.PRISMARINE_CRYSTALS, Items.SEA_PICKLE,
            Items.SPECTRAL_ARROW);

        put(9, TINT_PURPLE,
            Items.BLAZE_ROD, Items.BLAZE_POWDER, Items.AMETHYST_SHARD,
            Items.CRYING_OBSIDIAN, Items.NETHER_STAR, Items.TOTEM_OF_UNDYING,
            Items.ENDER_CHEST, Items.ENCHANTING_TABLE, Items.BEACON,
            Items.GLOW_ITEM_FRAME);

        put(6, TINT_GREEN,
            Items.DRAGON_BREATH, Items.END_CRYSTAL);

        // entity lights
        ent(15, TINT_FIRE, EntityType.BLAZE, EntityType.END_CRYSTAL,
            EntityType.TNT, EntityType.MAGMA_CUBE, EntityType.GLOW_ITEM_FRAME);
        ent(12, TINT_FIRE, EntityType.FIREBALL, EntityType.SMALL_FIREBALL,
            EntityType.SPECTRAL_ARROW, EntityType.SHULKER_BULLET);
        ent(9, 0xFF6677, EntityType.FIREWORK_ROCKET);
        ent(15, TINT_CYAN, EntityType.GLOW_SQUID);
    }

    private static void put(int level, int tint, Item... items) {
        for (Item i : items) {
            ITEM_LEVEL.put(i, level);
            ITEM_TINT.put(i, tint);
        }
    }

    private static void ent(int level, int tint, EntityType<?>... types) {
        for (EntityType<?> t : types) {
            ENTITY_LEVEL.put(t, level);
            ENTITY_TINT.put(t, tint);
        }
    }

    /** Light level for a held item stack, honoring the enchanted-glow toggle. */
    public static int itemLevel(ItemStack stack, boolean enchantedGlow) {
        if (stack == null || stack.isEmpty()) return 0;
        int l = ITEM_LEVEL.getOrDefault(stack.getItem(), 0);
        if (l == 0 && enchantedGlow && stack.isEnchanted()) return 6;
        return l;
    }

    public static int itemTint(ItemStack stack) {
        return ITEM_TINT.getOrDefault(stack.getItem(), TINT_WARM);
    }

    public static int entityLevel(EntityType<?> type) {
        return ENTITY_LEVEL.getOrDefault(type, 0);
    }

    public static int entityTint(EntityType<?> type) {
        return ENTITY_TINT.getOrDefault(type, TINT_WARM);
    }
}

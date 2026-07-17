package com.crest.client.core;

import com.crest.client.core.setting.FloatSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: ItemPhysics. Makes dropped items bounce and spin a little when they
 * land, client-side visual only. Injected by ItemEntityMixin at the end of tick.
 */
public class ItemPhysicsModule implements CrestModule {
    public static final ItemPhysicsModule INSTANCE = new ItemPhysicsModule();

    private final FloatSetting bounce = new FloatSetting("Bounce", 0.0F, 0.6F, 0.25F);
    private final FloatSetting spin = new FloatSetting("Spin", 0.0F, 3.0F, 1.0F);

    @Override public String getId() { return "item_physics"; }
    @Override public String getName() { return "Item Physics"; }
    @Override public String getDescription() { return "Adds a bounce + spin to dropped items (client-side visual)."; }
    @Override public String getCategory() { return "Render"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return new ArrayList<>(List.of(bounce, spin));
    }

    public static void apply(ItemEntity e) {
        if (!CrestModules.isEnabled("item_physics")) return;
        if (!e.onGround()) return;
        var v = e.getDeltaMovement();
        if (v.y < 0.08 && v.y > -0.08) {
            float b = INSTANCE.bounce.get();
            if (b > 0) {
                e.setDeltaMovement(v.x * 0.7, b, v.z * 0.7);
            }
        }
    }
}

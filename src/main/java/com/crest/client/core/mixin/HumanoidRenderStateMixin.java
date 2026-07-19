package com.crest.client.core.mixin;

import com.crest.client.core.accessor.ShieldTintAccessor;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HumanoidRenderState.class)
public class HumanoidRenderStateMixin implements ShieldTintAccessor {
    @Unique
    private int shieldTintColor = -1;

    @Override
    public int crest$getShieldTintColor() {
        return shieldTintColor;
    }

    @Override
    public void crest$setShieldTintColor(int color) {
        this.shieldTintColor = color;
    }
}

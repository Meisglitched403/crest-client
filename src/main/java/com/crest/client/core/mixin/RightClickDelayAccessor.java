package com.crest.client.core.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface RightClickDelayAccessor {
    @Accessor("rightClickDelay")
    void setRightClickDelay(int delay);

    @Accessor("rightClickDelay")
    int getRightClickDelay();
}

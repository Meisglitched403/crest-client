package com.crest.client.core.mixin;

import com.crest.client.core.ChatTimestampModule;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Mixin(ChatComponent.class)
public class ChatTimestampMixin {

    private static final DateTimeFormatter FMT_24 = DateTimeFormatter.ofPattern("HH:mm");

    @ModifyVariable(
        method = "addMessage",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Component crest$stamp(Component contents) {
        if (!ChatTimestampModule.isOn()) return contents;

        LocalTime now = LocalTime.now();
        String stamp = "[" + now.format(FMT_24) + "] ";

        int col = ChatTimestampModule.colorArgb();
        int rgb = (col & 0xFFFFFF);
        MutableComponent prefix = Component.literal(stamp).withColor(rgb);
        return prefix.append(contents);
    }
}

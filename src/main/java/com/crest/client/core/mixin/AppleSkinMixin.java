package com.crest.client.core.mixin;

import com.crest.client.core.AppleSkinModule;
import com.crest.client.core.CrestModules;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class AppleSkinMixin {
    @Inject(method = "extractFood(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;II)V", at = @At("HEAD"))
    private void crest$beforeFood(GuiGraphicsExtractor graphics, Player player, int top, int right, CallbackInfo ci) {
        if (!CrestModules.isEnabled("appleskin")) return;
        if (!AppleSkinModule.showExhaustion()) return;
        FoodData food = player.getFoodData();
        float exhaustion = ((FoodDataAccessor)(Object)food).crest$getExhaustionLevel();
        float ratio = Math.min(1f, exhaustion / 4f);
        int width = (int)(ratio * 81);
        if (width > 0) {
            graphics.fill(right - width, top - 1, right, top + 8, 0x44FFFFFF);
        }
    }

    @Inject(method = "extractFood(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;II)V", at = @At("RETURN"))
    private void crest$afterFood(GuiGraphicsExtractor graphics, Player player, int top, int right, CallbackInfo ci) {
        if (!CrestModules.isEnabled("appleskin")) return;
        FoodData food = player.getFoodData();
        if (AppleSkinModule.showSaturation()) {
            float saturation = food.getSaturationLevel();
            if (saturation > 0) {
                for (int i = 0; i < 10; i++) {
                    float barSaturation = saturation - i * 2;
                    if (barSaturation <= 0) break;
                    int iconX = right - i * 8 - 9;
                    int fillW = (int)(Math.min(barSaturation, 2) / 2f * 9);
                    if (fillW > 0) {
                        graphics.fill(iconX + 9 - fillW, top, iconX + 9, top + 9, 0x66FF00FF);
                    }
                }
            }
        }
        if (AppleSkinModule.showHungerPreview()) {
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty()) held = player.getOffhandItem();
            if (!held.isEmpty()) {
                FoodProperties props = held.get(DataComponents.FOOD);
                if (props != null) {
                    int restore = props.nutrition();
                    int current = food.getFoodLevel();
                    int total = Math.min(20, current + restore);
                    for (int i = 0; i < 10; i++) {
                        int bar = i * 2;
                        if (bar >= total) break;
                        if (bar < current) continue;
                        int iconX = right - i * 8 - 9;
                        graphics.fill(iconX, top, iconX + 9, top + 9, 0x66FFFF00);
                    }
                }
            }
        }
    }
}

package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: ShulkerPreview. When hovering a shulker box, shows its inventory grid
 * in the tooltip. The component is produced by ItemStackMixin.getTooltipImage()
 * and rendered here; ClientTooltipComponentCallback wires the two together.
 */
public class ShulkerPreviewModule implements CrestModule {
    private final BooleanSetting showCount = new BooleanSetting("Show Count", true);
    private final IntegerSetting slotsPerRow = new IntegerSetting("Slots / Row", 1, 9, 9);

    public ShulkerPreviewModule() {}

    @Override public String getId() { return "shulker_preview"; }
    @Override public String getName() { return "Shulker Preview"; }
    @Override public String getDescription() { return "Shows a shulker box's contents in its tooltip."; }
    @Override public String getCategory() { return "Utility"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return new ArrayList<>(List.of(showCount, slotsPerRow));
    }

    @Override
    public void onInitialize() {
        ClientTooltipComponentCallback.EVENT.register(component -> {
            if (component instanceof ShulkerPreviewComponent c) {
                return new ShulkerPreviewClientComponent(c.items(), showCount.get(), slotsPerRow.get());
            }
            return null;
        });
    }

    public record ShulkerPreviewComponent(NonNullList<ItemStack> items) implements TooltipComponent {}

    public static class ShulkerPreviewClientComponent implements ClientTooltipComponent {
        private static final int SLOT = 18;
        private final NonNullList<ItemStack> items;
        private final boolean showCount;
        private final int perRow;

        public ShulkerPreviewClientComponent(NonNullList<ItemStack> items, boolean showCount, int perRow) {
            this.items = items;
            this.showCount = showCount;
            this.perRow = Math.max(1, Math.min(9, perRow));
        }

        @Override
        public int getWidth(Font font) {
            return perRow * SLOT;
        }

        @Override
        public int getHeight(Font font) {
            int rows = (int) Math.ceil((double) items.size() / perRow);
            return rows * SLOT;
        }

        @Override
        public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor g) {
            int cols = perRow;
            for (int i = 0; i < items.size(); i++) {
                int cx = x + (i % cols) * SLOT;
                int cy = y + (i / cols) * SLOT;
                g.fill(cx, cy, cx + SLOT, cy + SLOT, 0xAA000000);
                ItemStack stack = items.get(i);
                if (!stack.isEmpty()) {
                    g.item(stack, cx + 1, cy + 1);
                    if (showCount && stack.getCount() > 1) {
                        g.itemDecorations(font, stack, cx + 1, cy + 1);
                    }
                }
            }
        }
    }
}

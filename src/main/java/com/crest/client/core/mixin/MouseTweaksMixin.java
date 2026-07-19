package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.MouseTweaksModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class MouseTweaksMixin {
    @Shadow
    protected Slot hoveredSlot;
    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;
    @Shadow
    protected AbstractContainerMenu menu;
    @Shadow
    protected abstract void slotClicked(@Nullable Slot slot, int slotId, int buttonNum, ContainerInput containerInput);

    @Unique
    private boolean crest$rmbDragActive;
    @Unique
    private Slot crest$rmbLastSlot;

    // ── Slot hit testing ──────────────────────────────────────────────

    @Unique
    private boolean crest$isHoveringSlot(Slot slot, double mx, double my) {
        mx -= this.leftPos;
        my -= this.topPos;
        return mx >= (double) (slot.x - 1) && mx < (double) (slot.x + 17)
            && my >= (double) (slot.y - 1) && my < (double) (slot.y + 17);
    }

    @Unique
    @Nullable
    private Slot crest$getHoveredSlot(double mx, double my) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && crest$isHoveringSlot(slot, mx, my)) {
                return slot;
            }
        }
        return null;
    }

    // ── Wheel Tweak ───────────────────────────────────────────────────

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void crest$onMouseScrolled(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (!MouseTweaksModule.isWheelTweakEnabled()) return;
        Slot slot = crest$getHoveredSlot(x, y);
        if (slot == null) return;
        if (!slot.hasItem()) return;

        int delta = (int) Math.signum(scrollY);
        if (delta == 0) return;

        boolean push = crest$wheelShouldPush(delta, slot);
        if (push) {
            slotClicked(slot, slot.index, 0, ContainerInput.QUICK_MOVE);
        } else {
            crest$wheelPullItem(slot);
        }
        cir.setReturnValue(true);
    }

    @Unique
    private boolean crest$wheelShouldPush(int delta, Slot slot) {
        Container playerInv = Minecraft.getInstance().player.getInventory();
        boolean inPlayerInv = slot.container == playerInv;
        return switch (MouseTweaksModule.getScrollDirection()) {
            case 1 -> delta < 0;
            case 2 -> inPlayerInv ? delta < 0 : delta > 0;
            default -> delta > 0;
        };
    }

    @Unique
    private void crest$wheelPullItem(Slot hovered) {
        Container playerInv = Minecraft.getInstance().player.getInventory();
        Container hoveredContainer = hovered.container;
        ItemStack target = hovered.getItem();

        for (Slot s : this.menu.slots) {
            if (s == hovered || !s.isActive() || !s.hasItem()) continue;
            if (s.container == hoveredContainer) continue;
            if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(s.getItem(), target)) continue;
            slotClicked(s, s.index, 0, ContainerInput.QUICK_MOVE);
            return;
        }
    }

    // ── RMB Tweak ─────────────────────────────────────────────────────

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void crest$onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!MouseTweaksModule.isRmbTweakEnabled()) return;
        if (event.button() != 1) return;

        Slot slot = crest$getHoveredSlot(event.x(), event.y());
        if (slot == null) return;
        if (this.menu.getCarried().isEmpty()) return;

        crest$rmbDragActive = true;
        crest$rmbLastSlot = slot;
        slotClicked(slot, slot.index, 1, ContainerInput.PICKUP);
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void crest$onMouseDragged(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (!crest$rmbDragActive) return;

        Slot slot = crest$getHoveredSlot(event.x(), event.y());
        if (slot != null && slot != crest$rmbLastSlot) {
            crest$rmbLastSlot = slot;
            if (!this.menu.getCarried().isEmpty()) {
                slotClicked(slot, slot.index, 1, ContainerInput.PICKUP);
            } else {
                crest$rmbDragActive = false;
                crest$rmbLastSlot = null;
            }
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void crest$onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!crest$rmbDragActive) return;
        crest$rmbDragActive = false;
        crest$rmbLastSlot = null;
        cir.setReturnValue(true);
    }
}

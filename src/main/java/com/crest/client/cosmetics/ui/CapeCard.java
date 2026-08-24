package com.crest.client.cosmetics.ui;

import com.crest.client.cosmetics.CapeLibrary;
import com.crest.client.ui.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

public class CapeCard implements Widget {
    private static final int CARD_H = 80;
    
    private final CapeLibrary.CapeEntry cape;
    private final Consumer<String> onSelect;
    private final Consumer<String> onFavorite;
    private final Consumer<String> onDelete;
    
    private int lastX, lastY, lastW;
    
    public CapeCard(CapeLibrary.CapeEntry cape, Consumer<String> onSelect, Consumer<String> onFavorite, Consumer<String> onDelete) {
        this.cape = cape;
        this.onSelect = onSelect;
        this.onFavorite = onFavorite;
        this.onDelete = onDelete;
    }
    
    @Override
    public int getWidth() {
        return 0;
    }
    
    @Override
    public int getHeight() {
        return CARD_H;
    }
    
    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        lastX = x;
        lastY = y;
        lastW = w;
        
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + CARD_H;
        boolean isActive = cape.id.equals(CapeLibrary.getActiveCapeId());
        
        int bg = isActive ? ColorUtil.withAlpha(Theme.ACCENT, 30) 
            : hover ? ColorUtil.withAlpha(Theme.SURFACE_VARIANT, 120) 
            : ColorUtil.withAlpha(Theme.BACKGROUND, 60);
        g.fill(x, y, x + w, y + CARD_H, bg);
        Panel.drawHollowRect(g, x, y, w, CARD_H, ColorUtil.withAlpha(isActive ? Theme.ACCENT : Theme.BORDER, isActive ? 200 : 100));
        
        if (hover || isActive) {
            g.fill(x, y, x + 3, y + CARD_H, Theme.ACCENT);
        }
        
        // Cape preview thumbnail
        int previewW = 44;
        int previewH = 64;
        int previewX = x + Spacing.S2;
        int previewY = y + (CARD_H - previewH) / 2;
        
        Identifier texture = CapeLibrary.getTexture(cape.id);
        if (texture != null) {
            g.fill(previewX - 1, previewY - 1, previewX + previewW + 1, previewY + previewH + 1, ColorUtil.withAlpha(Theme.BORDER, 150));
            g.blit(RenderPipelines.GUI_TEXTURED, texture, previewX, previewY, 0f, 0f, previewW, previewH, 64, 32, 0xFFFFFFFF);
        } else {
            g.fill(previewX, previewY, previewX + previewW, previewY + previewH, ColorUtil.withAlpha(Theme.MUTED, 100));
            g.centeredText(font, Component.literal("?"), previewX + previewW / 2, previewY + previewH / 2 - font.lineHeight / 2, Theme.MUTED_FOREGROUND);
        }
        
        // Cape info
        int infoX = previewX + previewW + Spacing.S3;
        int infoY = y + Spacing.S3;
        
        g.text(font, Component.literal(cape.name), infoX, infoY, Theme.FOREGROUND);
        
        String sourceText = cape.source != null && !cape.source.isEmpty() ? cape.source : "Custom";
        if (isActive) sourceText += " • Active";
        g.text(font, Component.literal(sourceText), infoX, infoY + font.lineHeight + 2, 
            ColorUtil.withAlpha(isActive ? Theme.ACCENT : Theme.MUTED_FOREGROUND, 200));
        
        // Action buttons
        int btnY = y + CARD_H - 28;
        int btnSize = 24;
        int btnSpacing = 4;
        
        int starX = x + w - Spacing.S2 - btnSize * 3 - btnSpacing * 2;
        int deleteX = starX + btnSize + btnSpacing;
        int applyX = deleteX + btnSize + btnSpacing;
        
        boolean starHover = mx >= starX && mx <= starX + btnSize && my >= btnY && my <= btnY + btnSize;
        boolean deleteHover = mx >= deleteX && mx <= deleteX + btnSize && my >= btnY && my <= btnY + btnSize;
        boolean applyHover = mx >= applyX && mx <= applyX + btnSize && my >= btnY && my <= btnY + btnSize;
        
        // Star/favorite button
        int starBg = cape.isFavorite ? ColorUtil.withAlpha(Theme.ACCENT, starHover ? 200 : 150) 
            : ColorUtil.withAlpha(Theme.SURFACE_VARIANT, starHover ? 180 : 120);
        g.fill(starX, btnY, starX + btnSize, btnY + btnSize, starBg);
        Panel.drawHollowRect(g, starX, btnY, btnSize, btnSize, Theme.BORDER);
        g.centeredText(font, Component.literal(cape.isFavorite ? "★" : "☆"), starX + btnSize / 2, btnY + 6, 
            cape.isFavorite ? 0xFFFFD700 : Theme.MUTED_FOREGROUND);
        
        // Delete button
        int deleteBg = ColorUtil.withAlpha(Theme.DESTRUCTIVE, deleteHover ? 200 : 100);
        g.fill(deleteX, btnY, deleteX + btnSize, btnY + btnSize, deleteBg);
        Panel.drawHollowRect(g, deleteX, btnY, btnSize, btnSize, Theme.BORDER);
        g.centeredText(font, Component.literal("×"), deleteX + btnSize / 2, btnY + 4, 0xFFFFFFFF);
        
        // Apply/Remove button
        String applyText = isActive ? "−" : "✓";
        int applyBg = ColorUtil.withAlpha(Theme.ACCENT, applyHover ? 220 : 180);
        g.fill(applyX, btnY, applyX + btnSize, btnY + btnSize, applyBg);
        Panel.drawHollowRect(g, applyX, btnY, btnSize, btnSize, Theme.ACCENT);
        g.centeredText(font, Component.literal(applyText), applyX + btnSize / 2, btnY + (isActive ? 3 : 5), 0xFFFFFFFF);
    }
    
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (my < lastY || my > lastY + CARD_H) return false;
        
        int btnY = lastY + CARD_H - 28;
        int btnSize = 24;
        int btnSpacing = 4;
        
        int starX = lastX + lastW - Spacing.S2 - btnSize * 3 - btnSpacing * 2;
        int deleteX = starX + btnSize + btnSpacing;
        int applyX = deleteX + btnSize + btnSpacing;
        
        if (mx >= starX && mx <= starX + btnSize && my >= btnY && my <= btnY + btnSize) {
            onFavorite.accept(cape.id);
            return true;
        }
        
        if (mx >= deleteX && mx <= deleteX + btnSize && my >= btnY && my <= btnY + btnSize) {
            onDelete.accept(cape.id);
            return true;
        }
        
        if (mx >= applyX && mx <= applyX + btnSize && my >= btnY && my <= btnY + btnSize) {
            boolean isActive = cape.id.equals(CapeLibrary.getActiveCapeId());
            if (isActive) {
                CapeLibrary.clearActiveCape();
            } else {
                onSelect.accept(cape.id);
            }
            return true;
        }
        
        if (mx >= lastX && mx <= lastX + lastW) {
            onSelect.accept(cape.id);
            return true;
        }
        
        return false;
    }
}

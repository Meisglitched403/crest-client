package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.SymbolChatModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChatScreen.class)
public class SymbolChatMixin {
    @Shadow
    protected EditBox input;

    @Unique
    private boolean crest$panelOpen;
    @Unique
    private int crest$selectedCategory;
    @Unique
    private int crest$scrollOffset;

    private static final List<String> CATEGORY_NAMES = List.of(
        "Faces", "Hearts", "Stars", "Arrows", "Shapes", "Weather", "Music", "Kaomoji"
    );

    private static final List<String> FACES = List.of("☺", "☻", "♥", "♦", "♣", "♠", "•", "◘", "○", "◙", "♂", "♀", "♪", "♫", "☼", "►", "◄", "↕", "‼", "¶", "§", "▬", "↨");
    private static final List<String> HEARTS = List.of("♥", "♡", "❤", "❥", "❦", "❧");
    private static final List<String> STARS = List.of("★", "☆", "✦", "✧", "⋆", "✶", "✷", "✸", "✹", "✺");
    private static final List<String> ARROWS = List.of("←", "↑", "↓", "→", "↔", "↕", "↵", "⇐", "⇑", "⇒", "⇓", "⇔", "➜", "⬅", "⬆", "⬇");
    private static final List<String> SHAPES = List.of("■", "□", "▪", "▫", "▲", "△", "▼", "▽", "◆", "◇", "○", "●", "◐", "◑", "◒", "◓");
    private static final List<String> WEATHER = List.of("☀", "☁", "☂", "☃", "☄", "⚡", "❄", "❅", "❆", "☽", "☾");
    private static final List<String> MUSIC = List.of("♪", "♫", "♩", "♬", "♭", "♮", "♯");
    private static final List<String> KAOMOJI = List.of(
        "(◕‿◕)", "¯\\_(ツ)_/¯", "(¬‿¬)", "(╯°□°)╯︵┻━┻",
        "┬─┬ノ( º _ ºノ)", "ʕ•ᴥ•ʔ", "ಠ_ಠ", "(╥﹏╥)",
        "♥‿♥", "⊙﹏⊙", "(ᵔᴥᵔ)", "(｡◕‿◕｡)",
        "(づ｡◕‿‿◕｡)づ", "♪┏(・o･)┛♪", "✿◠‿◠"
    );

    private static final List<List<String>> CATEGORY_SYMBOLS = List.of(FACES, HEARTS, STARS, ARROWS, SHAPES, WEATHER, MUSIC, KAOMOJI);

    private static final int CELL_SIZE = 22;
    private static final int CELL_GAP = 2;
    private static final int GRID_COLS = 8;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void crest$render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!CrestModules.isEnabled("symbol_chat")) return;
        ChatScreen screen = (ChatScreen) (Object) this;
        crest$renderToggleButton(graphics, mouseX, mouseY, screen);
        if (crest$panelOpen) {
            crest$renderPanel(graphics, mouseX, mouseY, screen);
        }
    }

    @Unique
    private void crest$renderToggleButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, ChatScreen screen) {
        int tx = screen.width - 14;
        int ty = screen.height - 14;
        if (mouseX >= tx && mouseX < tx + 12 && mouseY >= ty && mouseY < ty + 14) {
            graphics.fill(tx, ty, tx + 12, ty + 14, 0x44FFFFFF);
        }
        graphics.text(Minecraft.getInstance().font, "☺", tx + 2, ty + 3, 0xFFAAAAAA);
    }

    @Unique
    private void crest$renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, ChatScreen screen) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int panelW = screen.width - 4;
        int panelH = Math.min(SymbolChatModule.getPanelHeight(), screen.height - 24);
        int panelX = 2;
        int panelY = Math.max(10, screen.height - 14 - panelH);

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC000000);

        int tabY = panelY + 4;
        int tabH = 14;
        int tabX = panelX + 4;
        for (int i = 0; i < CATEGORY_NAMES.size(); i++) {
            String name = CATEGORY_NAMES.get(i);
            int tw = font.width(name) + 12;
            boolean hover = mouseX >= tabX && mouseX < tabX + tw && mouseY >= tabY && mouseY < tabY + tabH;
            int bg;
            if (i == crest$selectedCategory) {
                bg = 0xFF555555;
            } else if (hover) {
                bg = 0xFF444444;
            } else {
                bg = 0xFF333333;
            }
            graphics.fill(tabX, tabY, tabX + tw, tabY + tabH, bg);
            graphics.text(font, name, tabX + 6, tabY + 3, 0xFFFFFFFF);
            tabX += tw + 2;
        }

        List<String> symbols = CATEGORY_SYMBOLS.get(crest$selectedCategory);
        boolean isKaomoji = crest$selectedCategory == CATEGORY_NAMES.size() - 1;
        int contentY = panelY + 22;
        int contentH = panelY + panelH - contentY - 4;

        if (isKaomoji) {
            crest$renderKaomojiList(graphics, font, mouseX, mouseY, panelX, panelW, contentY, contentH, symbols);
        } else {
            crest$renderSymbolGrid(graphics, font, mouseX, mouseY, panelX, panelW, contentY, contentH, symbols);
        }
    }

    @Unique
    private void crest$renderSymbolGrid(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY,
                                        int panelX, int panelW, int gridY, int gridH, List<String> symbols) {
        int gridW = GRID_COLS * (CELL_SIZE + CELL_GAP) - CELL_GAP;
        int gridX = panelX + (panelW - gridW) / 2;
        int rows = (symbols.size() + GRID_COLS - 1) / GRID_COLS;
        int visibleRows = Math.max(1, gridH / (CELL_SIZE + CELL_GAP));
        int maxScroll = Math.max(0, rows - visibleRows);
        if (crest$scrollOffset > maxScroll) crest$scrollOffset = maxScroll;

        int startIdx = crest$scrollOffset * GRID_COLS;
        int endIdx = Math.min(symbols.size(), startIdx + visibleRows * GRID_COLS);

        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % GRID_COLS;
            int row = localIdx / GRID_COLS;
            int cx = gridX + col * (CELL_SIZE + CELL_GAP);
            int cy = gridY + row * (CELL_SIZE + CELL_GAP);
            if (mouseX >= cx && mouseX < cx + CELL_SIZE && mouseY >= cy && mouseY < cy + CELL_SIZE) {
                graphics.fill(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, 0x44FFFFFF);
            }
            String symbol = symbols.get(i);
            int sw = font.width(symbol);
            int sx = cx + (CELL_SIZE - sw) / 2;
            int sy = cy + (CELL_SIZE - font.lineHeight) / 2 + 1;
            graphics.text(font, symbol, sx, sy, 0xFFFFFFFF);
        }
    }

    @Unique
    private void crest$renderKaomojiList(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY,
                                         int panelX, int panelW, int listY, int listH, List<String> symbols) {
        int rowH = font.lineHeight + 4;
        int visibleRows = Math.max(1, listH / rowH);
        int maxScroll = Math.max(0, symbols.size() - visibleRows);
        if (crest$scrollOffset > maxScroll) crest$scrollOffset = maxScroll;

        int endIdx = Math.min(symbols.size(), crest$scrollOffset + visibleRows);
        for (int i = crest$scrollOffset; i < endIdx; i++) {
            int row = i - crest$scrollOffset;
            int ry = listY + row * rowH;
            int rx = panelX + 6;
            int rw = panelW - 12;
            if (mouseX >= rx && mouseX < rx + rw && mouseY >= ry && mouseY < ry + rowH) {
                graphics.fill(rx, ry, rx + rw, ry + rowH, 0x44FFFFFF);
            }
            graphics.text(font, symbols.get(i), rx + 4, ry + 2, 0xFFFFFFFF);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void crest$onClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!CrestModules.isEnabled("symbol_chat")) return;
        if (event.button() != 0) return;
        ChatScreen screen = (ChatScreen) (Object) this;
        int mx = (int) event.x();
        int my = (int) event.y();

        int tx = screen.width - 14;
        int ty = screen.height - 14;
        if (mx >= tx && mx < tx + 12 && my >= ty && my < ty + 14) {
            crest$panelOpen = !crest$panelOpen;
            crest$scrollOffset = 0;
            cir.setReturnValue(true);
            return;
        }

        if (!crest$panelOpen) return;

        Font font = Minecraft.getInstance().font;
        int panelW = screen.width - 4;
        int panelH = Math.min(SymbolChatModule.getPanelHeight(), screen.height - 24);
        int panelX = 2;
        int panelY = Math.max(10, screen.height - 14 - panelH);

        int tabY = panelY + 4;
        int tabH = 14;
        int tabX = panelX + 4;
        for (int i = 0; i < CATEGORY_NAMES.size(); i++) {
            String name = CATEGORY_NAMES.get(i);
            int tw = font.width(name) + 12;
            if (mx >= tabX && mx < tabX + tw && my >= tabY && my < tabY + tabH) {
                crest$selectedCategory = i;
                crest$scrollOffset = 0;
                cir.setReturnValue(true);
                return;
            }
            tabX += tw + 2;
        }

        List<String> symbols = CATEGORY_SYMBOLS.get(crest$selectedCategory);
        boolean isKaomoji = crest$selectedCategory == CATEGORY_NAMES.size() - 1;
        int contentY = panelY + 22;

        if (isKaomoji) {
            int rowH = font.lineHeight + 4;
            int rx = panelX + 6;
            int rw = panelW - 12;
            if (mx >= rx && mx < rx + rw && my >= contentY) {
                int row = (my - contentY) / rowH;
                int idx = crest$scrollOffset + row;
                if (idx >= 0 && idx < symbols.size()) {
                    input.insertText(symbols.get(idx));
                    crest$panelOpen = false;
                    cir.setReturnValue(true);
                    return;
                }
            }
        } else {
            int gridW = GRID_COLS * (CELL_SIZE + CELL_GAP) - CELL_GAP;
            int gridX = panelX + (panelW - gridW) / 2;
            if (mx >= gridX && mx < gridX + gridW && my >= contentY) {
                int col = (mx - gridX) / (CELL_SIZE + CELL_GAP);
                int row = (my - contentY) / (CELL_SIZE + CELL_GAP);
                if (col >= 0 && col < GRID_COLS && row >= 0) {
                    int idx = (crest$scrollOffset + row) * GRID_COLS + col;
                    if (idx >= 0 && idx < symbols.size()) {
                        input.insertText(symbols.get(idx));
                        crest$panelOpen = false;
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }

        if (mx < panelX || mx >= panelX + panelW || my < panelY || my >= panelY + panelH) {
            crest$panelOpen = false;
            return;
        }

        cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void crest$onScroll(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (!crest$panelOpen || !CrestModules.isEnabled("symbol_chat")) return;
        ChatScreen screen = (ChatScreen) (Object) this;
        int mx = (int) mouseX;
        int my = (int) mouseY;

        int panelW = screen.width - 4;
        int panelH = Math.min(SymbolChatModule.getPanelHeight(), screen.height - 24);
        int panelX = 2;
        int panelY = Math.max(10, screen.height - 14 - panelH);

        if (mx < panelX || mx >= panelX + panelW || my < panelY || my >= panelY + panelH) return;

        List<String> symbols = CATEGORY_SYMBOLS.get(crest$selectedCategory);
        boolean isKaomoji = crest$selectedCategory == CATEGORY_NAMES.size() - 1;
        int maxScroll;
        if (isKaomoji) {
            int rowH = Minecraft.getInstance().font.lineHeight + 4;
            int visibleRows = Math.max(1, panelH / rowH);
            maxScroll = Math.max(0, symbols.size() - visibleRows);
        } else {
            int rows = (symbols.size() + GRID_COLS - 1) / GRID_COLS;
            int visibleRows = Math.max(1, panelH / (CELL_SIZE + CELL_GAP));
            maxScroll = Math.max(0, rows - visibleRows);
        }
        int delta = (int) Math.signum(scrollY);
        if (delta == 0) return;
        crest$scrollOffset = (int) Math.max(0, Math.min(maxScroll, crest$scrollOffset + delta));
        cir.setReturnValue(true);
    }
}

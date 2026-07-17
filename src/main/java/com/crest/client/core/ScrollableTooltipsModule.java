package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: ScrollableTooltips. Caps tooltip width (wrapping long lines) and caps
 * the number of lines shown so huge tooltips stay on-screen. Applied by
 * ScreenTooltipMixin on Screen.getTooltipFromItem.
 */
public class ScrollableTooltipsModule implements CrestModule {
    public static final ScrollableTooltipsModule INSTANCE = new ScrollableTooltipsModule();

    private final BooleanSetting wrap = new BooleanSetting("Wrap To Width", true);
    private final IntegerSetting maxWidth = new IntegerSetting("Max Width", 80, 400, 220);
    private final BooleanSetting capLines = new BooleanSetting("Cap Lines", true);
    private final IntegerSetting maxLines = new IntegerSetting("Max Lines", 5, 60, 25);

    @Override public String getId() { return "scrollable_tooltips"; }
    @Override public String getName() { return "Scrollable Tooltips"; }
    @Override public String getDescription() { return "Wraps and caps tooltip size so long tooltips stay readable."; }
    @Override public String getCategory() { return "Utility"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return new ArrayList<>(List.of(wrap, maxWidth, capLines, maxLines));
    }

    public static List<Component> process(List<Component> lines) {
        if (!CrestModules.isEnabled("scrollable_tooltips")) return lines;
        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return lines;

        List<Component> out = new ArrayList<>();
        int w = INSTANCE.maxWidth.get();
        boolean wrap = INSTANCE.wrap.get();

        for (Component c : lines) {
            if (wrap && mc.font.width(c) > w) {
                for (var seq : mc.font.split(c, w)) {
                    out.add(Component.literal(seq.toString()));
                }
            } else {
                out.add(c);
            }
        }

        if (INSTANCE.capLines.get() && out.size() > INSTANCE.maxLines.get()) {
            out = new ArrayList<>(out.subList(0, INSTANCE.maxLines.get()));
            out.add(Component.literal("... (" + (lines.size() - INSTANCE.maxLines.get()) + " more)"));
        }
        return out;
    }
}

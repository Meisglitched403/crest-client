package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.Setting;

import java.util.List;

public class SymbolChatModule implements CrestModule {
    private final BooleanSetting enabled = new BooleanSetting("Enabled", true);
    private final IntegerSetting panelHeight = new IntegerSetting("Panel Height", 80, 300, 140);

    @Override public String getId() { return "symbol_chat"; }
    @Override public String getName() { return "Symbol Chat"; }
    @Override public String getDescription() { return "Adds a symbol/kaomoji panel to the chat screen."; }
    @Override public String getCategory() { return "Chat"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, panelHeight);
    }

    public static int getPanelHeight() {
        var m = CrestModules.get("symbol_chat");
        if (!(m instanceof SymbolChatModule mod)) return 140;
        return mod.panelHeight.get();
    }
}

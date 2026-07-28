package com.crest.client.core;

import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;

import java.util.List;

public class CrestNametagModule implements CrestModule {
    private static CrestNametagModule instance;
    private final ModeSetting position = new ModeSetting("Position", new String[]{"Left", "Right"}, 0);

    public CrestNametagModule() {
        instance = this;
    }

    @Override
    public String getId() { return "crest_nametag"; }
    @Override
    public String getName() { return "Crest Nametag"; }
    @Override
    public String getDescription() { return "Shows the crest-client logo next to players who also use crest-client"; }
    @Override
    public String getCategory() { return "Visual"; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(position);
    }

    public static boolean isLogoPositionLeft() {
        return instance == null || instance.position.get() == 0;
    }
}

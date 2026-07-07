package com.crest.client.core;

import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;

import java.util.List;

public class TimeChangerModule implements CrestModule {
    private final ModeSetting timeMode = new ModeSetting(
        "Time", new String[]{"Day", "Noon", "Sunset", "Night", "Midnight", "Custom"}, 0
    );
    private final IntegerSetting customTime = new IntegerSetting(
        "Custom Time (ticks)", 0, 24000, 12000
    );

    private static final long[] PRESET_TIMES = {1000, 6000, 12000, 14000, 18000};

    @Override public String getId() { return "time_changer"; }
    @Override public String getName() { return "Time Changer"; }
    @Override public String getDescription() { return "Overrides the world time"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(timeMode, customTime);
    }

    public long getOverrideTime(long originalTime) {
        int mode = timeMode.get();
        if (mode >= 0 && mode < PRESET_TIMES.length) {
            return PRESET_TIMES[mode];
        }
        return customTime.get();
    }
}

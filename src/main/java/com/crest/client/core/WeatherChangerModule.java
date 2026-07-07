package com.crest.client.core;

import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;

import java.util.List;

public class WeatherChangerModule implements CrestModule {
    private final ModeSetting weatherMode = new ModeSetting(
        "Weather", new String[]{"Clear", "Rain", "Thunder"}, 0
    );

    @Override public String getId() { return "weather_changer"; }
    @Override public String getName() { return "Weather Changer"; }
    @Override public String getDescription() { return "Overrides the world weather"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(weatherMode);
    }

    public float getRainLevel() {
        return switch (weatherMode.get()) {
            case 0 -> 0f;
            case 1 -> 1f;
            case 2 -> 1f;
            default -> 0f;
        };
    }

    public float getThunderLevel() {
        return switch (weatherMode.get()) {
            case 0 -> 0f;
            case 1 -> 0f;
            case 2 -> 1f;
            default -> 0f;
        };
    }
}

package com.crest.client.core;

public class NoFogModule implements CrestModule {
    @Override public String getId() { return "no_fog"; }
    @Override public String getName() { return "No Underwater Fog"; }
    @Override public String getDescription() { return "Removes underwater fog for better visibility"; }
    @Override public String getCategory() { return "Performance"; }
    @Override public boolean isEnabled() { return false; }
}

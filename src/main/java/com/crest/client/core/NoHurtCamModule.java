package com.crest.client.core;

public class NoHurtCamModule implements CrestModule {
    @Override public String getId() { return "no_hurt_cam"; }
    @Override public String getName() { return "No Hurt Cam"; }
    @Override public String getDescription() { return "Disables camera shake when taking damage"; }
    @Override public String getCategory() { return "Performance"; }
    @Override public boolean isEnabled() { return false; }
}

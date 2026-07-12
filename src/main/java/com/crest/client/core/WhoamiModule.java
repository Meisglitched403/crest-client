package com.crest.client.core;

public class WhoamiModule implements CrestModule {
    @Override public String getId() { return "whoami"; }
    @Override public String getName() { return "Who Am I"; }
    @Override public String getDescription() { return "Shows your own name tag in third person"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return false; }
}

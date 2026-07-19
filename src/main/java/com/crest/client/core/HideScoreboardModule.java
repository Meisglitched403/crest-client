package com.crest.client.core;

public class HideScoreboardModule implements CrestModule {
    @Override public String getId() { return "hide_scoreboard"; }
    @Override public String getName() { return "Hide Scoreboard"; }
    @Override public String getDescription() { return "Hides the vanilla in-game scoreboard sidebar."; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return false; }
}

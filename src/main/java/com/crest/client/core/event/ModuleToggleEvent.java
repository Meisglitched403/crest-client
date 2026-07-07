package com.crest.client.core.event;

import com.crest.client.core.CrestModule;

public class ModuleToggleEvent implements Event {
    private final CrestModule module;
    private final boolean enabled;

    public ModuleToggleEvent(CrestModule module, boolean enabled) {
        this.module = module;
        this.enabled = enabled;
    }

    public CrestModule getModule() { return module; }
    public boolean isEnabled() { return enabled; }
}

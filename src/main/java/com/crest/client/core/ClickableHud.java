package com.crest.client.core;

/**
 * ponytail: Optional interface for HUD modules that want to receive mouse clicks.
 * Click dispatch is driven from MouseInputMixin (frame-accurate edge detection),
 * so it is intentionally simple and cheap.
 */
public interface ClickableHud {
    /** Called when a click lands within this module's current bounds. */
    void onHudClick(int mouseX, int mouseY, int button);

    /** Current draw bounds (gui scaled), top-left + size. */
    int hudX();
    int hudY();
    int hudW();
    int hudH();
}

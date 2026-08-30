package com.crest.client.core;

/**
 * Implemented by Crest screens that capture keyboard text input (search boxes,
 * text fields, etc.). While such a screen reports {@link #isCapturingText()},
 * global action keybinds (open-music, HUD-edit, ...) are suppressed so they
 * don't fire while the user is typing.
 */
public interface TextCapturingScreen {
    boolean isCapturingText();
}

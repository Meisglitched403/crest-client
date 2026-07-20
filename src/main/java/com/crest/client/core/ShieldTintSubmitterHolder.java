package com.crest.client.core;

/** Holds the ShieldTintSubmitter captured from ShieldSpecialRenderer's constructor. */
public final class ShieldTintSubmitterHolder {
    private static ShieldTintSubmitter instance;

    public static void set(ShieldTintSubmitter submitter) {
        instance = submitter;
    }

    public static ShieldTintSubmitter get() {
        return instance;
    }
}

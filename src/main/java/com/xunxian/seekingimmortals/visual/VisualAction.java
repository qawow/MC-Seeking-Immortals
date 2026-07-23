package com.xunxian.seekingimmortals.visual;

import java.util.Locale;

/** The renderer-facing operation represented by one authored timeline event. */
public enum VisualAction {
    EMITTER,
    AURA,
    RIBBON,
    FLASH,
    DISSIPATE,
    BURST,
    SCREEN_OVERLAY,
    MODEL_ANIMATION,
    STATE_TRANSITION;

    public static VisualAction parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("blank visual action");
        }
        return valueOf(value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT));
    }
}

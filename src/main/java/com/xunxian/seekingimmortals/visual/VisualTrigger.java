package com.xunxian.seekingimmortals.visual;

import java.util.Locale;

/** A semantic point in an authored visual timeline. */
public enum VisualTrigger {
    TELEGRAPH,
    ANTICIPATION,
    FORMATION,
    RELEASE,
    IMPACT,
    DECAY,
    USE,
    STATE;

    public static VisualTrigger parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("blank visual trigger");
        }
        return valueOf(value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT));
    }
}

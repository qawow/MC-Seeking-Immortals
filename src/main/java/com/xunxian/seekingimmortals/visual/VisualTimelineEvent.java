package com.xunxian.seekingimmortals.visual;

import java.util.Objects;

/** One typed, deterministic event in a visual profile's timeline. */
public record VisualTimelineEvent(
        int ordinal,
        VisualTrigger trigger,
        int startTick,
        int durationTicks,
        VisualAction action,
        String anchor,
        String target,
        String state,
        String particle,
        String trail,
        double radius,
        int intensity,
        String condition,
        String source) {

    public VisualTimelineEvent {
        if (ordinal < 0) {
            throw new IllegalArgumentException("timeline ordinal must be non-negative");
        }
        trigger = Objects.requireNonNull(trigger, "trigger");
        action = Objects.requireNonNull(action, "action");
        if (startTick < 0) {
            throw new IllegalArgumentException("timeline start must be non-negative");
        }
        if (durationTicks < 1) {
            throw new IllegalArgumentException("timeline duration must be positive");
        }
        Math.addExact(startTick, durationTicks);
        if (!Double.isFinite(radius) || radius < 0.1D || radius > 8.0D) {
            throw new IllegalArgumentException("timeline radius must be between 0.1 and 8.0");
        }
        if (intensity < 1 || intensity > 64) {
            throw new IllegalArgumentException("timeline intensity must be between 1 and 64");
        }
        anchor = upper(anchor);
        target = upper(target);
        state = VisualDomain.normalizeId(state);
        particle = VisualDomain.normalizeId(particle);
        trail = VisualDomain.normalizeId(trail);
        condition = upper(condition);
        source = source == null ? "" : source.trim();
        if (anchor.isBlank() || target.isBlank() || particle.isBlank() || trail.isBlank() || condition.isBlank()) {
            throw new IllegalArgumentException("timeline references must not be blank");
        }
    }

    public int endTick() {
        return Math.addExact(startTick, durationTicks);
    }

    public int start() {
        return startTick;
    }

    public int duration() {
        return durationTicks;
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}

package com.xunxian.seekingimmortals.visual;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Immutable authored visual data shared by all visual domains. */
public record VisualProfile(
        String key,
        VisualDomain domain,
        String id,
        String runtimeId,
        String display,
        boolean authored,
        boolean fallback,
        boolean paletteFallback,
        String family,
        String motif,
        String shape,
        String particle,
        String trail,
        String colorProse,
        String paletteKey,
        long primaryArgb,
        boolean telegraphed,
        double radius,
        int intensity,
        List<VisualTimelineEvent> timeline,
        Map<String, VisualAction> states,
        Map<String, String> stateSources,
        Map<String, String> sources) {

    public VisualProfile {
        key = VisualDomain.normalizeKey(key);
        domain = Objects.requireNonNull(domain, "domain");
        id = VisualDomain.normalizeId(id);
        runtimeId = trim(runtimeId);
        display = trim(display);
        family = upper(family);
        motif = upper(motif);
        shape = trim(shape);
        particle = VisualDomain.normalizeId(particle);
        trail = VisualDomain.normalizeId(trail);
        colorProse = trim(colorProse);
        paletteKey = VisualDomain.normalizeId(paletteKey);
        if (key.isBlank() || id.isBlank() || runtimeId.isBlank() || family.isBlank() || motif.isBlank()
                || particle.isBlank() || trail.isBlank() || paletteKey.isBlank()) {
            throw new IllegalArgumentException("required visual profile fields must not be blank");
        }
        if (!key.equals(domain.qualify(id))) {
            throw new IllegalArgumentException("profile key does not match domain/id");
        }
        if (primaryArgb < 0L || primaryArgb > 0xffff_ffffL) {
            throw new IllegalArgumentException("ARGB value outside unsigned 32-bit range");
        }
        if (!Double.isFinite(radius) || radius < 0.1D || radius > 8.0D) {
            throw new IllegalArgumentException("profile radius must be between 0.1 and 8.0");
        }
        if (intensity < 1 || intensity > 64) {
            throw new IllegalArgumentException("profile intensity must be between 1 and 64");
        }
        List<VisualTimelineEvent> ordered = new ArrayList<>(timeline == null ? List.of() : timeline);
        ordered.sort(Comparator.comparingInt(VisualTimelineEvent::startTick)
                .thenComparingInt(VisualTimelineEvent::ordinal));
        timeline = List.copyOf(ordered);
        states = states == null || states.isEmpty()
                ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(states));
        stateSources = copyStrings(stateSources);
        sources = copyStrings(sources);
    }

    public String qualifiedKey() {
        return key;
    }

    public long argb() {
        return primaryArgb;
    }

    public int primaryArgbInt() {
        return (int) primaryArgb;
    }

    public List<VisualTimelineEvent> events() {
        return timeline;
    }

    public VisualAction state(String stateId) {
        return states.get(VisualDomain.normalizeId(stateId));
    }

    public boolean hasState(String stateId) {
        return states.containsKey(VisualDomain.normalizeId(stateId));
    }

    private static Map<String, String> copyStrings(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                copy.put(VisualDomain.normalizeId(entry.getKey()), trim(entry.getValue()));
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}

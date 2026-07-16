package com.xunxian.seekingimmortals.client;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Client mirror of M16 unlock records (bestiary / chronicle / timeline). */
public final class ClientLoreData {
    private static Set<String> bestiaryUnlocked = Set.of();
    private static Set<String> chronicleDiscovered = Set.of();
    private static Set<String> timelinePhases = Set.of();
    private static boolean synced;

    private ClientLoreData() {}

    public static void set(List<String> bestiary, List<String> chronicle, List<String> timeline) {
        bestiaryUnlocked = freeze(bestiary);
        chronicleDiscovered = freeze(chronicle);
        timelinePhases = freeze(timeline);
        synced = true;
    }

    public static void reset() {
        bestiaryUnlocked = Set.of();
        chronicleDiscovered = Set.of();
        timelinePhases = Set.of();
        synced = false;
    }

    public static boolean isSynced() {
        return synced;
    }

    public static boolean isBeastUnlocked(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return bestiaryUnlocked.contains(id.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean isChronicleDiscovered(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return chronicleDiscovered.contains(id.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean hasTimelinePhase(String phase) {
        if (phase == null || phase.isBlank()) {
            return false;
        }
        return timelinePhases.contains(phase.trim().toLowerCase(Locale.ROOT));
    }

    public static int bestiaryUnlockedCount() {
        return bestiaryUnlocked.size();
    }

    public static int chronicleDiscoveredCount() {
        return chronicleDiscovered.size();
    }

    public static int timelinePhaseCount() {
        return timelinePhases.size();
    }

    public static Set<String> bestiaryUnlocked() {
        return bestiaryUnlocked;
    }

    public static Set<String> chronicleDiscovered() {
        return chronicleDiscovered;
    }

    private static Set<String> freeze(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                set.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Collections.unmodifiableSet(set);
    }
}

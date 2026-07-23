package com.xunxian.seekingimmortals.visual;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic, renderer-neutral selection of authored timeline events.
 *
 * <p>The server packet still owns the anchor, duration, and trigger.  This
 * class only chooses the matching authored state/trigger window and rebases
 * it to the packet's local clock.  Keeping that policy outside the client
 * renderer makes it possible to test timeline semantics without a Minecraft
 * client and avoids putting author data on the wire.</p>
 */
public final class VisualTimelinePlan {
    private VisualTimelinePlan() {}

    /** Selects a state window first, then a typed trigger window. */
    public static Plan select(VisualProfile profile, String trigger, boolean looping) {
        if (profile == null || profile.timeline().isEmpty()) {
            return Plan.empty(looping);
        }
        String normalized = normalize(trigger);

        List<VisualTimelineEvent> selected = selectState(profile, normalized);
        if (selected.isEmpty() && "CAST".equals(normalized) && hasNoStates(profile)) {
            // Stateless technique/consumable storyboards are authored as one
            // cast sequence.  Persistent stateful profiles are never expanded
            // into an unrelated full sequence.
            selected = profile.timeline();
        }
        if (selected.isEmpty()) {
            selected = selectTrigger(profile, normalized);
        }
        return selected.isEmpty() ? Plan.empty(looping) : rebase(selected, looping);
    }

    private static List<VisualTimelineEvent> selectState(VisualProfile profile, String trigger) {
        if (trigger.isBlank() || profile.states().isEmpty()) {
            return List.of();
        }
        for (String candidate : stateCandidates(trigger)) {
            if (!profile.hasState(candidate)) {
                continue;
            }
            List<VisualTimelineEvent> selected = profile.timeline().stream()
                    .filter(event -> candidate.equals(event.state()))
                    .toList();
            if (!selected.isEmpty()) {
                return selected;
            }
        }
        return List.of();
    }

    private static List<VisualTimelineEvent> selectTrigger(VisualProfile profile, String trigger) {
        for (VisualTrigger candidate : triggerCandidates(trigger)) {
            List<VisualTimelineEvent> selected = profile.timeline().stream()
                    .filter(event -> event.trigger() == candidate)
                    .toList();
            if (!selected.isEmpty()) {
                return selected;
            }
        }
        return List.of();
    }

    private static Plan rebase(List<VisualTimelineEvent> events, boolean looping) {
        int origin = events.stream().mapToInt(VisualTimelineEvent::startTick).min().orElse(0);
        List<Entry> entries = new ArrayList<>(events.size());
        int duration = 0;
        for (VisualTimelineEvent event : events) {
            int start = Math.max(0, event.startTick() - origin);
            Entry entry = new Entry(start, event.durationTicks(), event);
            entries.add(entry);
            duration = Math.max(duration, entry.endTick());
        }
        entries.sort(java.util.Comparator.comparingInt(Entry::startTick)
                .thenComparingInt(entry -> entry.event().ordinal()));
        return new Plan(entries, duration, looping);
    }

    private static boolean hasNoStates(VisualProfile profile) {
        return profile.states().isEmpty()
                && profile.timeline().stream().allMatch(event -> event.state().isBlank());
    }

    private static List<String> stateCandidates(String trigger) {
        Set<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, trigger);
        switch (trigger) {
            case "APPLY" -> addCandidate(candidates, "applied");
            case "TICK", "PULSE" -> addCandidate(candidates, "tick_stack");
            case "DEPLOY", "START" -> addCandidate(candidates, "deploying");
            case "SPAWN", "JOIN" -> addCandidate(candidates, "idle");
            case "DESPAWN" -> addCandidate(candidates, "depart");
            case "ACTIVATE", "AWAKEN" -> addCandidate(candidates, "active");
            case "REPAIR" -> addCandidate(candidates, "repaired");
            case "DAMAGE", "HIT" -> addCandidate(candidates, "damaged");
            case "DESTROY" -> addCandidate(candidates, "broken");
            case "TRAIN" -> addCandidate(candidates, "train_enter");
            case "COMBAT" -> addCandidate(candidates, "combat_ready");
            default -> {
                // The canonical state is already present; no heuristic text
                // matching is needed beyond the small wire compatibility map.
            }
        }
        return List.copyOf(candidates);
    }

    private static void addCandidate(Set<String> candidates, String value) {
        String normalized = normalizeState(value);
        if (!normalized.isBlank()) {
            candidates.add(normalized);
        }
    }

    private static List<VisualTrigger> triggerCandidates(String trigger) {
        return switch (trigger) {
            case "TELEGRAPH", "ANNOUNCE", "P1", "P2", "P3" -> List.of(VisualTrigger.TELEGRAPH);
            case "FORMATION", "AURA" -> List.of(VisualTrigger.FORMATION, VisualTrigger.ANTICIPATION);
            case "PATH", "BEAM", "CONE", "SCAN", "RELEASE" -> List.of(VisualTrigger.RELEASE);
            case "IMPACT", "HIT", "COLLIDE" -> List.of(VisualTrigger.IMPACT);
            case "DISSIPATE", "STOP", "EXPIRE", "DEATH" -> List.of(VisualTrigger.DECAY);
            case "USE", "BURST", "OPEN", "ACTIVATE", "AWAKEN" -> List.of(VisualTrigger.USE);
            case "STATUS", "SUSTAIN", "TICK" -> List.of(VisualTrigger.STATE, VisualTrigger.ANTICIPATION);
            default -> parseTrigger(trigger);
        };
    }

    private static List<VisualTrigger> parseTrigger(String trigger) {
        try {
            return List.of(VisualTrigger.parse(trigger));
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
    }

    private static String normalizeState(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
    }

    /** One event window with a packet-local start tick. */
    public record Entry(int startTick, int durationTicks, VisualTimelineEvent event) {
        public Entry {
            if (startTick < 0 || durationTicks < 1 || event == null) {
                throw new IllegalArgumentException("invalid timeline plan entry");
            }
            Math.addExact(startTick, durationTicks);
        }

        public int endTick() {
            return Math.addExact(startTick, durationTicks);
        }

        public boolean activeAt(int age) {
            return age >= startTick && age < endTick();
        }
    }

    /** Immutable plan used by both one-shot and persistent client instances. */
    public record Plan(List<Entry> entries, int durationTicks, boolean looping) {
        public Plan {
            entries = entries == null ? List.of() : List.copyOf(entries);
            if (durationTicks < 0) {
                throw new IllegalArgumentException("timeline duration must not be negative");
            }
            if (entries.isEmpty() && durationTicks != 0) {
                throw new IllegalArgumentException("empty timeline plan must have zero duration");
            }
            if (!entries.isEmpty()) {
                int requiredDuration = entries.stream().mapToInt(Entry::endTick).max().orElse(0);
                if (durationTicks < requiredDuration) {
                    throw new IllegalArgumentException("timeline duration must contain every entry");
                }
            }
        }

        public static Plan empty(boolean looping) {
            return new Plan(List.of(), 0, looping);
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }

        public boolean expired(int age) {
            return !looping && (isEmpty() || age >= durationTicks);
        }

        /** Returns all authored events active at this local age. */
        public List<Entry> activeAt(int age) {
            if (isEmpty() || age < 0 || (!looping && age >= durationTicks)) {
                return List.of();
            }
            int localAge = looping ? Math.floorMod(age, durationTicks) : age;
            return entries.stream().filter(entry -> entry.activeAt(localAge)).toList();
        }
    }
}

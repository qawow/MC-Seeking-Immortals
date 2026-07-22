package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.SyncQuestTrackerPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client cache for quest tracker lines.
 * Wave457: structured parse of authority line format for UI buttons.
 */
public final class ClientQuestTrackerData {
    private static final List<String> LINES = new ArrayList<>();
    private static String selectedChainId = "";
    private static final Pattern CHAIN_LINE = Pattern.compile(
            "^(?<id>[a-z0-9_]+)\\s+(?<stage>\\d+)/(?<steps>\\d+)(?<done>\\s+DONE)?\\s+branch=(?<branch>[a-z_]+)"
                    + "(?:\\s+cost=(?<costItem>[a-z0-9_\\-]+):(?<costNeed>\\d+)\\s+own=(?<own>\\d+))?"
                    + "(?:\\s+LOCK=(?<lock>[01]))?(?:\\s+REW=(?<rew>[01]))?"
                    + "(?:\\s+STATE=(?<state>[A-Z]+))?(?:\\s+GATE=(?<gate>[A-Z]+))?",
            Pattern.CASE_INSENSITIVE);

    private ClientQuestTrackerData() {}

    public enum TrackerState {
        AVAILABLE,
        LOCKED,
        ACTIVE,
        DONE;

        static TrackerState parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            try {
                return valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    public enum StartGate {
        NONE,
        REALM,
        REGION,
        FACTION,
        PATH,
        RACE,
        PARENT,
        DATA;

        static StartGate parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            try {
                return valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    public record ChainLine(
            String id,
            int stage,
            int steps,
            boolean complete,
            String branch,
            String costItem,
            int costNeed,
            int owned,
            boolean branchLocked,
            boolean rewarded,
            TrackerState state,
            StartGate gate
    ) {
        /** Compatibility constructor for callers using the pre-state contract. */
        public ChainLine(String id, int stage, int steps, boolean complete, String branch,
                         String costItem, int costNeed, int owned, boolean branchLocked, boolean rewarded) {
            this(id, stage, steps, complete, branch, costItem, costNeed, owned, branchLocked, rewarded,
                    inferState(stage, steps, complete, branchLocked),
                    inferGate(stage, steps, complete, branchLocked));
        }

        public boolean isAvailable() {
            return state == TrackerState.AVAILABLE;
        }

        public boolean isLocked() {
            return state == TrackerState.LOCKED;
        }

        public boolean isActive() {
            return state == TrackerState.ACTIVE;
        }

        public boolean isDone() {
            return state == TrackerState.DONE;
        }

        public boolean available() {
            return isAvailable();
        }

        public boolean locked() {
            return isLocked();
        }

        public boolean active() {
            return isActive();
        }

        public boolean done() {
            return isDone();
        }

        public boolean canStart() {
            return isAvailable();
        }

        public boolean canAdvance() {
            return isActive() && !complete;
        }

        private static TrackerState inferState(int stage, int steps, boolean complete, boolean branchLocked) {
            if (complete || (steps > 0 && stage >= steps)) {
                return TrackerState.DONE;
            }
            if (stage > 0) {
                return TrackerState.ACTIVE;
            }
            return branchLocked ? TrackerState.LOCKED : TrackerState.AVAILABLE;
        }

        private static StartGate inferGate(int stage, int steps, boolean complete, boolean branchLocked) {
            return inferState(stage, steps, complete, branchLocked) == TrackerState.LOCKED
                    ? StartGate.DATA : StartGate.NONE;
        }
    }

    public static void set(SyncQuestTrackerPacket packet) {
        LINES.clear();
        if (packet != null && packet.lines() != null) {
            LINES.addAll(packet.lines());
        }
        selectedChainId = resolveSelectedChainId(selectedChainId, LINES);
    }

    public static void reset() {
        LINES.clear();
        selectedChainId = "";
    }

    public static List<String> lines() {
        return List.copyOf(LINES);
    }

    public static Optional<ChainLine> firstActiveChain() {
        // Historical method name retained; selection now prefers ACTIVE, then AVAILABLE,
        // then locked/done rows so an empty active list still has a useful default.
        return firstPreferredChain(LINES);
    }

    public static Optional<ChainLine> selectedChain() {
        Optional<ChainLine> selected = findChain(selectedChainId, LINES);
        return selected.isPresent() ? selected : firstPreferredChain(LINES);
    }

    public static String selectedChainId() {
        return selectedChainId;
    }

    public static boolean selectChain(String chainId) {
        Optional<ChainLine> selected = findChain(chainId, LINES);
        if (selected.isEmpty()) {
            return false;
        }
        selectedChainId = selected.get().id();
        return true;
    }

    static String resolveSelectedChainId(String preferredId, List<String> lines) {
        Optional<ChainLine> preferred = findChain(preferredId, lines);
        if (preferred.isPresent()) {
            return preferred.get().id();
        }
        return firstPreferredChain(lines).map(ChainLine::id).orElse("");
    }

    private static Optional<ChainLine> firstPreferredChain(List<String> lines) {
        if (lines == null) {
            return Optional.empty();
        }
        Optional<ChainLine> active = firstByState(lines, TrackerState.ACTIVE);
        if (active.isPresent()) {
            return active;
        }
        Optional<ChainLine> available = firstByState(lines, TrackerState.AVAILABLE);
        if (available.isPresent()) {
            return available;
        }
        for (String line : lines) {
            Optional<ChainLine> parsed = parseChainLine(line);
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        return Optional.empty();
    }

    private static Optional<ChainLine> firstByState(List<String> lines, TrackerState state) {
        for (String line : lines) {
            Optional<ChainLine> parsed = parseChainLine(line);
            if (parsed.isPresent() && parsed.get().state() == state) {
                return parsed;
            }
        }
        return Optional.empty();
    }

    private static Optional<ChainLine> findChain(String chainId, List<String> lines) {
        if (chainId == null || chainId.isBlank() || lines == null) {
            return Optional.empty();
        }
        String wanted = chainId.trim().toLowerCase(Locale.ROOT);
        for (String line : lines) {
            Optional<ChainLine> parsed = parseChainLine(line);
            if (parsed.isPresent() && parsed.get().id().equals(wanted)) {
                return parsed;
            }
        }
        return Optional.empty();
    }

    public static Optional<ChainLine> parseChainLine(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = CHAIN_LINE.matcher(line.trim());
        if (!matcher.find()) {
            return Optional.empty();
        }
        String id = matcher.group("id").toLowerCase(Locale.ROOT);
        int stage = parseInt(matcher.group("stage"), 0);
        int steps = parseInt(matcher.group("steps"), 0);
        boolean complete = matcher.group("done") != null || (steps > 0 && stage >= steps);
        String branch = matcher.group("branch") == null ? "neutral" : matcher.group("branch").toLowerCase(Locale.ROOT);
        String costItem = matcher.group("costItem") == null ? "-" : matcher.group("costItem");
        int costNeed = parseInt(matcher.group("costNeed"), 0);
        int owned = parseInt(matcher.group("own"), 0);
        boolean locked = "1".equals(matcher.group("lock")) || (!"neutral".equals(branch) && !branch.isBlank());
        boolean rewarded = "1".equals(matcher.group("rew"));
        TrackerState state = TrackerState.parse(matcher.group("state"));
        if (state == null) {
            state = ChainLine.inferState(stage, steps, complete, locked);
        }
        if (state == TrackerState.DONE) {
            complete = true;
        }
        StartGate gate = StartGate.parse(matcher.group("gate"));
        if (gate == null) {
            gate = state == TrackerState.LOCKED ? StartGate.DATA : StartGate.NONE;
        }
        return Optional.of(new ChainLine(id, stage, steps, complete, branch, costItem, costNeed, owned,
                locked, rewarded, state, gate));
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}

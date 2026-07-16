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
                    + "(?:\\s+LOCK=(?<lock>[01]))?(?:\\s+REW=(?<rew>[01]))?",
            Pattern.CASE_INSENSITIVE);

    private ClientQuestTrackerData() {}

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
            boolean rewarded
    ) {}

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
        return firstActiveChain(LINES);
    }

    public static Optional<ChainLine> selectedChain() {
        Optional<ChainLine> selected = findChain(selectedChainId, LINES);
        return selected.isPresent() ? selected : firstActiveChain();
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
        return firstActiveChain(lines).map(ChainLine::id).orElse("");
    }

    private static Optional<ChainLine> firstActiveChain(List<String> lines) {
        if (lines == null) {
            return Optional.empty();
        }
        for (String line : lines) {
            Optional<ChainLine> parsed = parseChainLine(line);
            if (parsed.isPresent() && !parsed.get().complete()) {
                return parsed;
            }
        }
        for (String line : lines) {
            Optional<ChainLine> parsed = parseChainLine(line);
            if (parsed.isPresent()) {
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
        return Optional.of(new ChainLine(id, stage, steps, complete, branch, costItem, costNeed, owned, locked, rewarded));
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

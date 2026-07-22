package com.xunxian.seekingimmortals.quest;

import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Native quest progress is authoritative. FTB custom tasks may mirror it, while
 * explicitly tagged FTB quest completions may request the normal native
 * start/advance path.
 */
public final class FtbNativeQuestSync {
    public static final String MIRROR_PREFIX = "si_native_";
    public static final String WRITE_PREFIX = "si_native_write_";

    private FtbNativeQuestSync() {}

    public record Target(String chainId, int stage) {}

    enum WriteAction {
        REJECT,
        SATISFIED,
        START,
        ADVANCE
    }

    enum GateRequirement {
        REJECT,
        NONE,
        BOUND_NPC
    }

    public static Optional<Target> parseMirrorTag(String raw) {
        String tag = normalize(raw);
        if (tag.startsWith(WRITE_PREFIX)) {
            return Optional.empty();
        }
        return parseTarget(tag, MIRROR_PREFIX);
    }

    public static Optional<Target> parseWriteTag(String raw) {
        return parseTarget(normalize(raw), WRITE_PREFIX);
    }

    public static List<Target> writeTargets(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Target> targets = new LinkedHashSet<>();
        for (String tag : tags) {
            String normalized = normalize(tag);
            if (!normalized.startsWith(WRITE_PREFIX)) {
                continue;
            }
            Optional<Target> target = parseWriteTag(normalized);
            if (target.isEmpty()) {
                return List.of();
            }
            targets.add(target.get());
        }
        return targets.size() == 1 ? List.copyOf(targets) : List.of();
    }

    static <T> Optional<T> singleOnlineMember(List<T> members) {
        if (members == null || members.size() != 1) {
            return Optional.empty();
        }
        return Optional.ofNullable(members.get(0));
    }

    public static boolean isSatisfied(ServerPlayer player, Target target) {
        if (player == null || target == null) {
            return false;
        }
        return isSatisfied(TextQuestChainService.progressOf(player, target.chainId()), target);
    }

    static boolean isSatisfied(TextQuestChainService.ChainProgress progress, Target target) {
        return progress != null && target != null
                && progress.id().equals(target.chainId())
                && progress.stepCount() >= target.stage()
                && progress.stage() >= target.stage();
    }

    static WriteAction writeAction(TextQuestChainService.ChainProgress progress, Target target) {
        if (progress == null || target == null
                || !progress.id().equals(target.chainId())
                || target.stage() <= 0
                || target.stage() > progress.stepCount()) {
            return WriteAction.REJECT;
        }
        if (progress.stage() >= target.stage()) {
            return WriteAction.SATISFIED;
        }
        if (progress.stage() == 0 && target.stage() == 1) {
            return WriteAction.START;
        }
        if (progress.stage() > 0 && progress.stage() + 1 == target.stage()) {
            return WriteAction.ADVANCE;
        }
        return WriteAction.REJECT;
    }

    static GateRequirement gateRequirement(WriteAction action, Optional<String> expectedHook) {
        if (action == null) {
            return GateRequirement.REJECT;
        }
        return switch (action) {
            case REJECT -> GateRequirement.REJECT;
            case SATISFIED -> GateRequirement.NONE;
            case START -> GateRequirement.BOUND_NPC;
            // Authored hooks are advanced by QuestHookRuntime; FTB may only observe
            // the resulting native stage and must never replay a persistent hook flag.
            case ADVANCE -> expectedHook != null && expectedHook.filter(hook -> !hook.isBlank()).isPresent()
                    ? GateRequirement.REJECT
                    : GateRequirement.BOUND_NPC;
        };
    }

    private static boolean hasNativeGate(ServerPlayer player, Target target, WriteAction action) {
        if (player == null) {
            return false;
        }
        Optional<String> expectedHook = Optional.empty();
        if (action == WriteAction.ADVANCE) {
            TextQuestChainService.ChainProgress progress =
                    TextQuestChainService.progressOf(player, target.chainId());
            expectedHook = TextQuestChainService.expectedHookForStage(target.chainId(), progress.stage());
        }
        return switch (gateRequirement(action, expectedHook)) {
            case REJECT -> false;
            case NONE -> true;
            case BOUND_NPC -> TextQuestNpcHookService.isNearBoundNpc(player, target.chainId());
        };
    }

    /**
     * Applies an explicit FTB completion through native authority APIs only.
     * No reward is issued here; finale rewards remain owned by advance().
     */
    public static boolean applyWrite(ServerPlayer player, Target target) {
        if (player == null || target == null) {
            return false;
        }
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, target.chainId());
        WriteAction action = writeAction(progress, target);
        if (!hasNativeGate(player, target, action)) {
            return action == WriteAction.SATISFIED;
        }
        boolean accepted = switch (action) {
            case SATISFIED -> true;
            case START -> TextQuestChainService.start(player, target.chainId());
            case ADVANCE -> TextQuestChainService.advance(player, target.chainId());
            case REJECT -> false;
        };
        return accepted && isSatisfied(TextQuestChainService.progressOf(player, target.chainId()), target);
    }

    private static Optional<Target> parseTarget(String tag, String prefix) {
        if (tag.isBlank() || !tag.startsWith(prefix)) {
            return Optional.empty();
        }
        String body = tag.substring(prefix.length());
        int split = body.lastIndexOf('_');
        if (split <= 0 || split >= body.length() - 1) {
            return Optional.empty();
        }
        String chainId = body.substring(0, split);
        int stage;
        try {
            stage = Integer.parseInt(body.substring(split + 1));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
        Optional<com.xunxian.seekingimmortals.catalog.ExtendedCatalogService.QuestChain> chain =
                TextQuestChainService.find(chainId);
        if (stage <= 0 || chain.isEmpty() || chain.get().stepCount() < stage) {
            return Optional.empty();
        }
        return Optional.of(new Target(chain.get().id(), stage));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

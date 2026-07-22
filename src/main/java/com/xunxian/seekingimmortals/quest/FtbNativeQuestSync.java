package com.xunxian.seekingimmortals.quest;

import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Native quest progress is authoritative. FTB custom tasks may mirror it, while
 * explicitly tagged FTB quest completions may request the normal native
 * start/advance path.
 */
public final class FtbNativeQuestSync {
    public static final String MIRROR_PREFIX = "si_native_";
    public static final String WRITE_PREFIX = "si_native_write_";
    public static final String READY_PREFIX = "si_native_ready_";

    private FtbNativeQuestSync() {}

    public record Target(String chainId, int stage) {}

    public record WriteIntent(Target target) {}

    public enum WriteIntentStatus {
        VALID,
        NO_WRITE_TAG,
        MALFORMED_WRITE_TAG,
        MULTIPLE_WRITE_TARGETS,
        MISSING_CHAIN_TAG,
        MULTIPLE_CHAIN_TAGS,
        CHAIN_MISMATCH
    }

    public record WriteIntentValidation(WriteIntentStatus status, WriteIntent intent) {
        public boolean valid() {
            return status == WriteIntentStatus.VALID && intent != null;
        }
    }

    enum WriteAction {
        REJECT,
        SATISFIED,
        START,
        ADVANCE
    }

    public static Optional<Target> parseMirrorTag(String raw) {
        String tag = normalize(raw);
        if (tag.startsWith(WRITE_PREFIX) || tag.startsWith(READY_PREFIX)) {
            return Optional.empty();
        }
        return parseTarget(tag, MIRROR_PREFIX);
    }

    public static Optional<Target> parseWriteTag(String raw) {
        return parseTarget(normalize(raw), WRITE_PREFIX);
    }

    public static Optional<Target> parseReadyTag(String raw) {
        return parseTarget(normalize(raw), READY_PREFIX);
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

    /**
     * Validates both the explicit write tag and the ordinary native-chain tag on
     * the containing FTB quest. A mismatched or ambiguous projection fails closed.
     */
    public static WriteIntentValidation validateWriteIntent(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return invalid(WriteIntentStatus.NO_WRITE_TAG);
        }
        boolean sawWriteTag = false;
        LinkedHashSet<Target> targets = new LinkedHashSet<>();
        for (String raw : tags) {
            String tag = normalize(raw);
            if (!tag.startsWith(WRITE_PREFIX)) {
                continue;
            }
            sawWriteTag = true;
            Optional<Target> target = parseWriteTag(tag);
            if (target.isEmpty()) {
                return invalid(WriteIntentStatus.MALFORMED_WRITE_TAG);
            }
            targets.add(target.get());
        }
        if (!sawWriteTag) {
            return invalid(WriteIntentStatus.NO_WRITE_TAG);
        }
        if (targets.size() != 1) {
            return invalid(WriteIntentStatus.MULTIPLE_WRITE_TARGETS);
        }

        Set<String> registeredChains = FtbQuestBridgeService.builtin().registeredChainIds();
        LinkedHashSet<String> chainTags = new LinkedHashSet<>();
        for (String raw : tags) {
            String tag = normalize(raw);
            if (registeredChains.contains(tag)) {
                chainTags.add(tag);
            }
        }
        if (chainTags.isEmpty()) {
            return invalid(WriteIntentStatus.MISSING_CHAIN_TAG);
        }
        if (chainTags.size() != 1) {
            return invalid(WriteIntentStatus.MULTIPLE_CHAIN_TAGS);
        }
        Target target = targets.iterator().next();
        if (!chainTags.iterator().next().equals(target.chainId())) {
            return invalid(WriteIntentStatus.CHAIN_MISMATCH);
        }
        return new WriteIntentValidation(WriteIntentStatus.VALID, new WriteIntent(target));
    }

    /** Requires one full team member and that same member to be the sole online member. */
    static Optional<UUID> singleAuthorityMember(Set<UUID> teamMembers, UUID implicitMember,
                                                 List<UUID> onlineMembers) {
        LinkedHashSet<UUID> fullMembers = new LinkedHashSet<>();
        if (teamMembers != null) {
            fullMembers.addAll(teamMembers);
        }
        if (implicitMember != null) {
            fullMembers.add(implicitMember);
        }
        fullMembers.remove(null);
        if (fullMembers.size() != 1 || onlineMembers == null || onlineMembers.size() != 1) {
            return Optional.empty();
        }
        UUID member = fullMembers.iterator().next();
        UUID online = onlineMembers.get(0);
        return member.equals(online) ? Optional.of(member) : Optional.empty();
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

    /** Pure readiness check used by the transactional FTB custom task. */
    public static boolean isWriteReady(ServerPlayer player, Target target) {
        if (player == null || target == null) {
            return false;
        }
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, target.chainId());
        WriteAction action = writeAction(progress, target);
        return action == WriteAction.SATISFIED
                || (action != WriteAction.REJECT
                && TextQuestChainService.canTransitionExact(player, target.chainId(), target.stage()));
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
        boolean accepted = switch (action) {
            case SATISFIED -> true;
            case START, ADVANCE -> TextQuestChainService.transitionExact(
                    player, target.chainId(), target.stage());
            case REJECT -> false;
        };
        return accepted && isSatisfied(TextQuestChainService.progressOf(player, target.chainId()), target);
    }

    private static WriteIntentValidation invalid(WriteIntentStatus status) {
        return new WriteIntentValidation(status, null);
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

package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.compat.ModCompat;
import com.xunxian.seekingimmortals.sect.SectWarService;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.CustomTaskEvent;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.quest.task.CustomTask;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Wave488: FTB CustomTaskEvent hooks for Seeking Immortals authority checks.
 *
 * Task tags (must match FTB {@code ^[a-z0-9_]*$}):
 * <ul>
 *   <li>{@code si_war_active} — complete while a sect war window is open</li>
 *   <li>{@code si_rep_<faction>_<min>} — require ReputationService.get(player, faction) &gt;= min</li>
 *   <li>{@code si_native_<chain>_<stage>} — mirror authoritative native text-quest progress</li>
 * </ul>
 * Multiple tags are AND-combined. Unknown {@code si_*} tags fail closed.
 */
public final class FtbCustomTaskHooks {
    public static final String TAG_WAR_ACTIVE = "si_war_active";
    public static final String TAG_REP_PREFIX = "si_rep_";

    private static boolean registered;

    private FtbCustomTaskHooks() {}

    public static void register() {
        if (registered || !ModCompat.FTB_QUESTS_LOADED) {
            return;
        }
        CustomTaskEvent.EVENT.register(event -> {
            CustomTask task = event.getTask();
            if (task == null) {
                return EventResult.pass();
            }
            List<Spec> specs = specsOf(task.getTags());
            if (specs.isEmpty()) {
                return EventResult.pass();
            }
            task.setCheckTimer(20); // once per second while incomplete
            task.setMaxProgress(1L);
            task.setCheck((data, player) -> {
                if (player == null || data == null) {
                    return;
                }
                if (evaluate(player, specs)) {
                    data.setProgress(1L);
                }
            });
            SeekingImmortalsMod.LOGGER.debug("Bound FTB custom task {} with {} SI specs",
                    task.getCodeString(), specs.size());
            return EventResult.pass();
        });
        ObjectCompletedEvent.QUEST.register(event -> {
            List<FtbNativeQuestSync.Target> targets = FtbNativeQuestSync.writeTargets(event.getQuest().getTags());
            if (targets.size() != 1) {
                return EventResult.pass();
            }
            FtbNativeQuestSync.singleOnlineMember(event.getOnlineMembers())
                    .ifPresent(player -> FtbNativeQuestSync.applyWrite(player, targets.get(0)));
            return EventResult.pass();
        });
        registered = true;
        SeekingImmortalsMod.LOGGER.info("Registered FTB custom-task and native quest-sync hooks for Seeking Immortals");
    }

    /** Pure evaluation used by runtime checks and unit tests. */
    public static boolean evaluate(ServerPlayer player, List<Spec> specs) {
        if (player == null || specs == null || specs.isEmpty()) {
            return false;
        }
        for (Spec spec : specs) {
            if (!spec.matches(player)) {
                return false;
            }
        }
        return true;
    }

    public static List<Spec> specsOf(Set<String> tags) {
        List<Spec> specs = new ArrayList<>();
        if (tags == null || tags.isEmpty()) {
            return specs;
        }
        for (String raw : tags) {
            Spec spec = parseTag(raw);
            if (spec != null) {
                specs.add(spec);
            }
        }
        return specs;
    }

    public static Spec parseTag(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String tag = raw.trim().toLowerCase(Locale.ROOT);
        if (TAG_WAR_ACTIVE.equals(tag)) {
            return Spec.warActive();
        }
        if (tag.startsWith(TAG_REP_PREFIX)) {
            String body = tag.substring(TAG_REP_PREFIX.length());
            int split = body.lastIndexOf('_');
            if (split <= 0 || split >= body.length() - 1) {
                return Spec.unknown(tag);
            }
            String faction = body.substring(0, split);
            String amount = body.substring(split + 1);
            if (faction.isBlank()) {
                return Spec.unknown(tag);
            }
            try {
                int min = Integer.parseInt(amount);
                if (min < 0) {
                    return Spec.unknown(tag);
                }
                return Spec.reputation(faction, min);
            } catch (NumberFormatException ignored) {
                return Spec.unknown(tag);
            }
        }
        if (tag.startsWith(FtbNativeQuestSync.MIRROR_PREFIX)) {
            return FtbNativeQuestSync.parseMirrorTag(tag)
                    .<Spec>map(target -> Spec.nativeStage(target.chainId(), target.stage()))
                    .orElseGet(() -> Spec.unknown(tag));
        }
        // Non-SI tags are ignored so packs can still use ordinary FTB tags.
        if (tag.startsWith("si_")) {
            return Spec.unknown(tag);
        }
        return null;
    }

    public sealed interface Spec {
        boolean matches(ServerPlayer player);

        static Spec warActive() {
            return new WarActive();
        }

        static Spec reputation(String faction, int min) {
            return new ReputationGate(faction, min);
        }

        static Spec nativeStage(String chainId, int stage) {
            return new NativeStage(chainId, stage);
        }

        static Spec unknown(String tag) {
            return new Unknown(tag);
        }

        record WarActive() implements Spec {
            @Override
            public boolean matches(ServerPlayer player) {
                return player != null && player.server != null && SectWarService.isActive(player.server);
            }
        }

        record ReputationGate(String faction, int min) implements Spec {
            @Override
            public boolean matches(ServerPlayer player) {
                return player != null && ReputationService.get(player, faction) >= min;
            }
        }

        record NativeStage(String chainId, int stage) implements Spec {
            @Override
            public boolean matches(ServerPlayer player) {
                return FtbNativeQuestSync.isSatisfied(player, new FtbNativeQuestSync.Target(chainId, stage));
            }
        }

        /** Fail closed for unrecognized si_* tags. */
        record Unknown(String tag) implements Spec {
            @Override
            public boolean matches(ServerPlayer player) {
                return false;
            }
        }
    }
}

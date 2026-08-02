package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.compat.ModCompat;
import com.xunxian.seekingimmortals.sect.SectWarService;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftblibrary.ui.CustomClickEvent;
import dev.ftb.mods.ftbquests.events.CustomTaskEvent;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.CustomTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Wave488: FTB CustomTaskEvent hooks for Seeking Immortals authority checks.
 *
 * Task tags (must match FTB {@code ^[a-z0-9_]*$}):
 * <ul>
 *   <li>{@code si_war_active} — complete while a sect war window is open</li>
 *   <li>{@code si_rep_<faction>_<min>} — require ReputationService.get(player, faction) &gt;= min</li>
 *   <li>{@code si_native_<chain>_<stage>} — mirror authoritative native text-quest progress</li>
 *   <li>{@code si_native_ready_<chain>_<stage>} — transactionally apply one explicit native transition</li>
 * </ul>
 * Multiple tags are AND-combined. Unknown {@code si_*} tags fail closed.
 */
public final class FtbCustomTaskHooks {
    public static final String TAG_WAR_ACTIVE = "si_war_active";
    public static final String TAG_REP_PREFIX = "si_rep_";
    private static final ResourceLocation GUIDE_BOOK = new ResourceLocation(
            SeekingImmortalsMod.MODID, "seeking_immortals_guide");
    private static final Set<String> GUIDE_ENTRIES = Set.of(
            "quest_native_main",
            "quest_native_chaotic_sea",
            "quest_native_dajin_kunwu",
            "quest_native_fallen_demon_yin",
            "quest_native_mulan_demonic",
            "quest_native_spirit_realm_service",
            "quest_native_tiannan_seven_sects",
            "quest_native_star_palace_inverse",
            "quest_native_ascension_border"
    );

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
            task.setCheck((data, player) -> checkCustomTask(data, player, specs));
            SeekingImmortalsMod.LOGGER.debug("Bound FTB custom task {} with {} SI specs",
                    task.getCodeString(), specs.size());
            return EventResult.pass();
        });
        ObjectCompletedEvent.QUEST.register(event -> {
            FtbNativeQuestSync.WriteIntentValidation validation =
                    FtbNativeQuestSync.validateWriteIntent(event.getQuest().getTags());
            if (validation.status() == FtbNativeQuestSync.WriteIntentStatus.NO_WRITE_TAG) {
                return EventResult.pass();
            }
            if (!validation.valid() || !isAuthoritySafeQuest(event.getQuest())) {
                SeekingImmortalsMod.LOGGER.warn("Rejected unsafe FTB native write fallback for quest {} ({})",
                        event.getQuest().getCodeString(), validation.status());
                return EventResult.pass();
            }
            singleAuthorityPlayer(event.getData(), event.getOnlineMembers()).ifPresent(player -> {
                FtbNativeQuestSync.Target target = validation.intent().target();
                if (!FtbNativeQuestSync.applyWrite(player, target)) {
                    SeekingImmortalsMod.LOGGER.warn("Rejected FTB native write fallback {}:{} for {}",
                            target.chainId(), target.stage(), player.getGameProfile().getName());
                }
            });
            return EventResult.pass();
        });
        if (FMLEnvironment.dist == Dist.CLIENT && ModCompat.PATCHOULI_LOADED) {
            CustomClickEvent.EVENT.register(FtbCustomTaskHooks::openPatchouliGuide);
        }
        registered = true;
        SeekingImmortalsMod.LOGGER.info("Registered FTB custom-task and native quest-sync hooks for Seeking Immortals");
    }

    private static EventResult openPatchouliGuide(CustomClickEvent event) {
        if (event == null || !isGuideEntry(event.id())) {
            return EventResult.pass();
        }
        try {
            Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Class<?> apiInterface = Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI");
            Object api = apiClass.getMethod("get").invoke(null);
            Method openEntry = apiInterface.getMethod("openBookEntry",
                    ResourceLocation.class, ResourceLocation.class, int.class);
            openEntry.invoke(api, GUIDE_BOOK, event.id(), 0);
            return EventResult.interruptTrue();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            SeekingImmortalsMod.LOGGER.error("Failed to open Patchouli entry {} from FTB Quests",
                    event.id(), exception);
            return EventResult.pass();
        }
    }

    static boolean isGuideEntry(ResourceLocation id) {
        return id != null && SeekingImmortalsMod.MODID.equals(id.getNamespace())
                && GUIDE_ENTRIES.contains(id.getPath());
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

    private static void checkCustomTask(CustomTask.Data data, ServerPlayer player, List<Spec> specs) {
        if (player == null || data == null || specs == null || specs.isEmpty() || !evaluate(player, specs)) {
            return;
        }
        List<Spec.NativeReady> ready = specs.stream()
                .filter(Spec.NativeReady.class::isInstance)
                .map(Spec.NativeReady.class::cast)
                .toList();
        if (ready.isEmpty()) {
            // Mirror/rep/war progress is player-scoped: a single member's earned state may
            // complete the shared task, but never grant team-wide FTB rewards from a quest
            // that carries rewards or consumes resources (authority-model asymmetry fix).
            if (isAuthoritySafeQuest(data.task().getQuest())) {
                data.setProgress(1L);
            }
            return;
        }
        if (ready.size() != 1 || !isAuthoritySafeQuest(data.task().getQuest())) {
            return;
        }
        FtbNativeQuestSync.Target readyTarget = new FtbNativeQuestSync.Target(
                ready.get(0).chainId(), ready.get(0).stage());
        FtbNativeQuestSync.WriteIntentValidation validation =
                FtbNativeQuestSync.validateWriteIntent(data.task().getQuest().getTags());
        if (!validation.valid() || !validation.intent().target().equals(readyTarget)) {
            return;
        }
        Optional<ServerPlayer> authority = singleAuthorityPlayer(data);
        if (authority.isEmpty() || !authority.get().getUUID().equals(player.getUUID())) {
            return;
        }
        if (FtbNativeQuestSync.applyWrite(player, readyTarget)) {
            data.setProgress(1L);
        }
    }

    private static boolean isAuthoritySafeQuest(Quest quest) {
        return quest != null && quest.getRewards().isEmpty()
                && quest.getTasks().stream().noneMatch(Task::consumesResources);
    }

    /**
     * Resolves the single online authority member of the task team. The online-members query is
     * only evaluated after the FTB Teams manager is confirmed loaded, so a not-yet-initialized
     * manager cannot throw before the guard (the check runs on the server tick path).
     */
    private static Optional<ServerPlayer> singleAuthorityPlayer(CustomTask.Data data) {
        if (data == null || data.teamData() == null || data.teamData().getTeamId() == null) {
            return Optional.empty();
        }
        try {
            if (!FTBTeamsAPI.api().isManagerLoaded()) {
                return Optional.empty();
            }
            TeamManager manager = FTBTeamsAPI.api().getManager();
            Collection<ServerPlayer> reportedOnline = data.teamData().getOnlineMembers();
            Optional<Team> optionalTeam = manager.getTeamByID(data.teamData().getTeamId());
            if (optionalTeam.isEmpty()) {
                return Optional.empty();
            }
            Team team = optionalTeam.get();
            if (!data.teamData().getTeamId().equals(team.getId()) || !team.isValid()
                    || (!team.isPlayerTeam() && !team.isPartyTeam())) {
                return Optional.empty();
            }
            List<ServerPlayer> online = reportedOnline.stream()
                    .filter(java.util.Objects::nonNull)
                    .toList();
            UUID implicitMember = team.isPlayerTeam() ? team.getId() : team.getOwner();
            Optional<UUID> authority = FtbNativeQuestSync.singleAuthorityMember(
                    team.getMembers(), implicitMember,
                    online.stream().map(ServerPlayer::getUUID).toList());
            if (authority.isEmpty()) {
                return Optional.empty();
            }
            ServerPlayer player = online.get(0);
            return manager.getTeamForPlayer(player)
                    .filter(effective -> effective.getId().equals(team.getId()))
                    .map(ignored -> player);
        } catch (RuntimeException | LinkageError exception) {
            SeekingImmortalsMod.LOGGER.error("FTB team authority lookup failed; native write rejected", exception);
            return Optional.empty();
        }
    }

    /** Object-completion fallback entry: online members are already resolved by the event. */
    private static Optional<ServerPlayer> singleAuthorityPlayer(TeamData data,
                                                                 Collection<ServerPlayer> reportedOnline) {
        if (data == null || reportedOnline == null || data.getTeamId() == null) {
            return Optional.empty();
        }
        try {
            if (!FTBTeamsAPI.api().isManagerLoaded()) {
                return Optional.empty();
            }
            TeamManager manager = FTBTeamsAPI.api().getManager();
            Optional<Team> optionalTeam = manager.getTeamByID(data.getTeamId());
            if (optionalTeam.isEmpty()) {
                return Optional.empty();
            }
            Team team = optionalTeam.get();
            if (!data.getTeamId().equals(team.getId()) || !team.isValid()
                    || (!team.isPlayerTeam() && !team.isPartyTeam())) {
                return Optional.empty();
            }
            List<ServerPlayer> online = reportedOnline.stream()
                    .filter(java.util.Objects::nonNull)
                    .toList();
            UUID implicitMember = team.isPlayerTeam() ? team.getId() : team.getOwner();
            Optional<UUID> authority = FtbNativeQuestSync.singleAuthorityMember(
                    team.getMembers(), implicitMember,
                    online.stream().map(ServerPlayer::getUUID).toList());
            if (authority.isEmpty()) {
                return Optional.empty();
            }
            ServerPlayer player = online.get(0);
            return manager.getTeamForPlayer(player)
                    .filter(effective -> effective.getId().equals(team.getId()))
                    .map(ignored -> player);
        } catch (RuntimeException | LinkageError exception) {
            SeekingImmortalsMod.LOGGER.error("FTB team authority lookup failed; native write rejected", exception);
            return Optional.empty();
        }
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
        if (tag.startsWith(FtbNativeQuestSync.READY_PREFIX)) {
            return FtbNativeQuestSync.parseReadyTag(tag)
                    .<Spec>map(target -> Spec.nativeReady(target.chainId(), target.stage()))
                    .orElseGet(() -> Spec.unknown(tag));
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

        static Spec nativeReady(String chainId, int stage) {
            return new NativeReady(chainId, stage);
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

        record NativeReady(String chainId, int stage) implements Spec {
            @Override
            public boolean matches(ServerPlayer player) {
                return FtbNativeQuestSync.isWriteReady(
                        player, new FtbNativeQuestSync.Target(chainId, stage));
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

package com.xunxian.seekingimmortals.region;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.worldpack.DailyEventEncounterService;
import com.xunxian.seekingimmortals.worldpack.DailyEventEffectCatalog;
import com.xunxian.seekingimmortals.worldpack.DailyEventEffectExecutor;
import com.xunxian.seekingimmortals.worldpack.WorldpackDataService;
import com.xunxian.seekingimmortals.worldpack.WorldpackSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import com.xunxian.seekingimmortals.cultivation.Realm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-side daily event scheduler.
 * Expands worldpack daily_events with multi-region text_material coverage and
 * exposes {@link #onDailyEvent} subscription for M08/M11.
 */
public final class DailyEventScheduler {
    private static final int AUTHORED_WEIGHT_SCALE = 1000;
    private static final List<DailyEventHook> HOOKS = new CopyOnWriteArrayList<>();
    private static final List<ExpandedEvent> EXPANDED = loadExpanded();
    private static long lastServerDay = Long.MIN_VALUE;

    private DailyEventScheduler() {}

    public static void registerHook(DailyEventHook hook) {
        if (hook != null) {
            HOOKS.add(hook);
        }
    }

    public static void clearHooks() {
        HOOKS.clear();
    }

    public static int hookCount() {
        return HOOKS.size();
    }

    public static int expandedEventCount() {
        return EXPANDED.size();
    }

    /**
     * Public subscription entry used by downstream modules.
     */
    public static void onDailyEvent(String regionId, String eventId) {
        if (regionId == null || eventId == null || regionId.isBlank() || eventId.isBlank()) {
            return;
        }
        for (DailyEventHook hook : HOOKS) {
            try {
                hook.onDailyEvent(regionId, eventId);
            } catch (Exception exception) {
                SeekingImmortalsMod.LOGGER.warn("Daily event hook failed for {}/{}", regionId, eventId, exception);
            }
        }
    }

    public static void serverTick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        if (!RegionEventConfig.isDailyEventsEnabled()) {
            // Force the first enabled tick to rehydrate the current day's roll.
            lastServerDay = Long.MIN_VALUE;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                clearPlayerEvent(player);
            }
            com.xunxian.seekingimmortals.sect.SectWarService.stopDailyEventWar(server);
            return;
        }
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        long day = overworld.getDayTime() / 24000L;
        if (day == lastServerDay) {
            return;
        }
        lastServerDay = day;
        rollAllRegions(overworld, true);
    }

    public static WorldpackSavedData.EventRoll ensurePlayerEvent(ServerPlayer player, String regionId) {
        if (player == null || player.server == null) {
            return new WorldpackSavedData.EventRoll(regionId == null ? "" : regionId, "", 0L);
        }
        if (!RegionEventConfig.isDailyEventsEnabled()) {
            clearPlayerEvent(player);
            return new WorldpackSavedData.EventRoll(regionId == null ? "" : regionId, "", 0L);
        }
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        WorldpackSavedData savedData = WorldpackSavedData.get(player.server.overworld());
        String resolvedRegion = regionId == null || regionId.isBlank()
                ? RegionRegistry.DEFAULT_REGION_ID
                : regionId;
        long gameTime = player.server.overworld().getGameTime();
        WorldpackSavedData.EventRoll previous = savedData.peekDailyEvent(resolvedRegion).orElse(null);
        WorldpackSavedData.EventRoll roll = savedData.getOrRollDailyEvent(
                resolvedRegion, snapshot, gameTime, player.getRandom(), expandedCandidates(resolvedRegion, snapshot));
        boolean changed = previous == null || !previous.eventId().equals(roll.eventId())
                || !previous.isActive(gameTime);
        if (!roll.eventId().isBlank()) {
            Optional<DailyEventEffectCatalog.Event> authored = DailyEventEffectCatalog.builtin().find(roll.eventId());
            authored.ifPresent(event ->
                    DailyEventEffectExecutor.apply(player, resolvedRegion, event, roll.untilTick(), changed));
            if (authored.isEmpty()) {
                // Legacy worldpack ids have no typed ownership; clear any
                // previous authored state before the compatibility multiplier
                // is restored by PlayerCultivation#setWorldpackDailyEvent.
                DailyEventEffectExecutor.expire(player);
                com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player)
                        .ifPresent(cultivation -> cultivation.setWorldpackDailyEvent(roll.eventId(), roll.untilTick()));
            }
            DailyEventEncounterService.maybeSpawn(player, resolvedRegion, roll.eventId(), roll.untilTick());
            if (changed) {
                onDailyEvent(resolvedRegion, roll.eventId());
            }
        } else {
            clearPlayerEvent(player);
        }
        com.xunxian.seekingimmortals.sect.FactionConflictEventService.onDailyEvent(
                player, resolvedRegion, roll.eventId(), roll.untilTick());
        return roll;
    }

    private static void clearPlayerEvent(ServerPlayer player) {
        if (!DailyEventEffectExecutor.hasState(player)
                && !com.xunxian.seekingimmortals.sect.FactionConflictEventService.hasActiveState(player)) {
            return;
        }
        DailyEventEffectExecutor.expire(player);
        com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player)
                .ifPresent(cultivation -> cultivation.setWorldpackDailyEvent("", 0L));
        String region = com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player)
                .map(cultivation -> cultivation.getWorldpackCurrentRegionId())
                .orElse(RegionRegistry.DEFAULT_REGION_ID);
        com.xunxian.seekingimmortals.sect.FactionConflictEventService.onDailyEvent(
                player, region, "", 0L);
    }

    public static void rollAllRegions(ServerLevel overworld, boolean notifyPlayers) {
        if (overworld == null || overworld.getServer() == null || !RegionEventConfig.isDailyEventsEnabled()) {
            return;
        }
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        WorldpackSavedData savedData = WorldpackSavedData.get(overworld);
        long gameTime = overworld.getGameTime();
        RandomSource random = overworld.getRandom();
        for (WorldpackDataService.RegionCard region : snapshot.regions()) {
            WorldpackSavedData.EventRoll previous = savedData.peekDailyEvent(region.id()).orElse(null);
            WorldpackSavedData.EventRoll roll = savedData.getOrRollDailyEvent(
                    region.id(), snapshot, gameTime, random, expandedCandidates(region.id(), snapshot));
            boolean changed = previous == null
                    || !previous.eventId().equals(roll.eventId())
                    || !previous.isActive(gameTime);
            if (changed) {
                if (!roll.eventId().isBlank()) {
                    onDailyEvent(region.id(), roll.eventId());
                }
            }
            if (notifyPlayers) {
                for (ServerPlayer player : overworld.getServer().getPlayerList().getPlayers()) {
                    com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player).ifPresent(cultivation -> {
                        if (region.id().equals(cultivation.getWorldpackCurrentRegionId())) {
                            if (roll.eventId().isBlank()) {
                                clearPlayerEvent(player);
                                return;
                            }
                            Optional<DailyEventEffectCatalog.Event> authored =
                                    DailyEventEffectCatalog.builtin().find(roll.eventId());
                            authored.ifPresent(event -> DailyEventEffectExecutor.apply(
                                    player, region.id(), event, roll.untilTick(), changed));
                            if (authored.isEmpty()) {
                                DailyEventEffectExecutor.expire(player);
                                cultivation.setWorldpackDailyEvent(roll.eventId(), roll.untilTick());
                            }
                            com.xunxian.seekingimmortals.sect.FactionConflictEventService.onDailyEvent(
                                    player, region.id(), roll.eventId(), roll.untilTick());
                            DailyEventEncounterService.maybeSpawn(player, region.id(), roll.eventId(), roll.untilTick());
                        }
                    });
                }
            }
        }
    }

    public static List<WorldpackDataService.DailyEvent> expandedCandidates(String regionId,
                                                                           WorldpackDataService.Snapshot snapshot) {
        List<WorldpackDataService.DailyEvent> base = new ArrayList<>();
        for (WorldpackDataService.DailyEvent event : snapshot.eventsForRegion(regionId)) {
            DailyEventEffectCatalog.Event authored = DailyEventEffectCatalog.builtin().find(event.id())
                    .filter(candidate -> candidate.matchesRegion(regionId))
                    .orElse(null);
            if (authored == null) {
                base.add(new WorldpackDataService.DailyEvent(
                        event.id(), event.regionId(), event.displayZh(), event.displayEn(),
                        scaledWeight(event.weight()), event.durationTicks(), event.effects()));
            } else {
                base.add(new WorldpackDataService.DailyEvent(
                        authored.id(), regionId,
                        authored.display().isBlank() ? event.displayZh() : authored.display(),
                        authored.display().isBlank() ? event.displayEn() : authored.display(),
                        authored.scaledWeight(AUTHORED_WEIGHT_SCALE), authored.durationTicks(),
                        authored.legacyEffects()));
            }
        }
        for (ExpandedEvent expanded : EXPANDED) {
            if (!expanded.matches(regionId)) {
                continue;
            }
            WorldpackDataService.DailyEvent projected = new WorldpackDataService.DailyEvent(
                    expanded.id(),
                    regionId,
                    expanded.displayZh(),
                    expanded.displayEn(),
                    expanded.weight(),
                    expanded.durationTicks(),
                    expanded.effects());
            int existingIndex = -1;
            for (int i = 0; i < base.size(); i++) {
                if (base.get(i).id().equals(expanded.id())) {
                    existingIndex = i;
                    break;
                }
            }
            if (existingIndex >= 0) {
                // Authored data wins over a legacy projection, including region aliases.
                base.set(existingIndex, projected);
            } else {
                base.add(projected);
            }
        }
        return List.copyOf(base);
    }

    private static List<ExpandedEvent> loadExpanded() {
        List<ExpandedEvent> events = new ArrayList<>();
        for (DailyEventEffectCatalog.Event event : DailyEventEffectCatalog.builtin().list()) {
            events.add(new ExpandedEvent(
                    event.id(),
                    event.regions(),
                    event.display(),
                    event.display(),
                    event.scaledWeight(AUTHORED_WEIGHT_SCALE),
                    event.durationTicks(),
                    event.legacyEffects()));
        }
        return List.copyOf(events);
    }

    private static int scaledWeight(int weight) {
        long scaled = (long) Math.max(1, weight) * AUTHORED_WEIGHT_SCALE;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, scaled));
    }

    /** Pure gate helper used by tests and player-scoped callers. */
    public static boolean meetsRealmMinimum(DailyEventEffectCatalog.Event event, Realm realm) {
        return event == null || DailyEventEffectExecutor.meetsRealmMinimum(realm, event.realmMin());
    }

    private record ExpandedEvent(String id, List<String> regions, String displayZh, String displayEn,
                                 int weight, int durationTicks, List<String> effects) {
        private boolean matches(String regionId) {
            if (regions == null || regions.isEmpty()) {
                return false;
            }
            for (String region : regions) {
                if (region == null) {
                    continue;
                }
                if ("*".equals(region) || "any".equalsIgnoreCase(region) || region.equalsIgnoreCase(regionId)) {
                    return true;
                }
                if (regionAliasMatches(region, regionId)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean regionAliasMatches(String authoredRegion, String runtimeRegion) {
            String authored = authoredRegion == null ? "" : authoredRegion.toLowerCase(java.util.Locale.ROOT);
            String runtime = runtimeRegion == null ? "" : runtimeRegion.toLowerCase(java.util.Locale.ROOT);
            return switch (authored) {
                case "tiannan_border" -> "tiannan".equals(runtime) || "wutu_border".equals(runtime);
                case "mulan" -> "mulan_grassland".equals(runtime);
                case "great_jin" -> "great_jin_central".equals(runtime) || "dajin".equals(runtime);
                case "wild_land" -> "spirit_realm_border".equals(runtime);
                case "diyuan" -> "spirit_fengyuan".equals(runtime);
                default -> false;
            };
        }
    }

}

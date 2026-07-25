package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;

import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Daily event encounter spawner (Wave51 content fidelity).
 * Spawns lightweight hostiles when certain daily event tokens are active.
 */
public final class DailyEventEncounterService {
    private static final String LEGACY_TAG_PREFIX = "seeking_immortals_daily_spawned_";
    private static final String LATCH_ROOT = "seeking_immortals_daily_encounter";
    private static final String LATCH_REGION = "Region";
    private static final String LATCH_EVENT = "Event";
    private static final String LATCH_UNTIL = "Until";
    private static final String LATCH_CLAIMS = "Claims";
    private static final int MAX_LATCH_CLAIMS = 32;

    private DailyEventEncounterService() {}

    /**
     * Authored-event entry point. The roll expiry is part of the latch key so a
     * recurring event can create a fresh encounter on a later daily roll.
     */
    public static void maybeSpawn(ServerPlayer player, String regionId, String eventId, long untilTick) {
        if (player == null || eventId == null || eventId.isBlank()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)
                || level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            return;
        }
        long gameTime = level.getGameTime();
        if (untilTick > 0L && untilTick <= gameTime) {
            return;
        }
        long effectiveUntil = untilTick > 0L ? untilTick : fallbackClaimUntil(gameTime);
        String id = normalize(eventId);
        Optional<DailyEventEffectCatalog.Event> authored = DailyEventEffectCatalog.builtin().find(id);
        if (authored.isPresent() && !DailyEventEffectExecutor.isRealmAllowed(player, authored.get())) {
            return;
        }
        EncounterPlan plan = authored.map(event -> plan(regionId, id, event))
                .orElseGet(() -> legacyPlan(regionId, id));
        if (plan.kind() == Kind.NONE) {
            return;
        }
        String claimRegion = plan.region().isBlank() ? normalize(regionId) : plan.region();
        if (isLatched(player, claimRegion, id, effectiveUntil, gameTime)) {
            return;
        }

        if (plan.kind() == Kind.BEAST) {
            String region = plan.region();
            if (!hasRegionSpawnTable(region)) {
                return;
            }
            int spawned = BeastSpawnTableService.spawnNearPlayerExact(player, region, plan.count(),
                    entity -> DailyEventRewardService.bindEncounter(
                            entity, player, claimRegion, id, effectiveUntil));
            if (spawned <= 0) {
                return;
            }
            latch(player, claimRegion, id, effectiveUntil);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.daily_event.spawn", displayName(id), spawned), true);
            return;
        } else if (plan.kind() == Kind.SHELL) {
            int spawned = 0;
            for (int i = 0; i < plan.count(); i++) {
                Mob mob = TrialCombatShellService.spawnHostile(
                        level,
                        player.blockPosition().offset(i, 0, i + 1),
                        player.getYRot(),
                        plan.shellId(),
                        20.0D + plan.combatTier() * 12.0D,
                        3.0D + plan.combatTier() * 2.0D,
                        plan.archetype());
                if (mob == null) {
                    continue;
                }
                DailyEventRewardService.bindEncounter(mob, player, claimRegion, id, effectiveUntil);
                if (mob instanceof Monster monster && !player.isCreative() && !player.isSpectator()) {
                    monster.setTarget(player);
                }
                spawned++;
            }
            if (spawned <= 0) {
                return;
            }
            latch(player, claimRegion, id, effectiveUntil);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.daily_event.spawn", displayName(id), spawned), true);
            return;
        } else {
            int spawned = 0;
            for (int i = 0; i < plan.count(); i++) {
                Mob mob = plan.entityType().create(level);
                if (mob == null) {
                    continue;
                }
                mob.moveTo(player.getX() + (i - 1), player.getY(), player.getZ() + 1.5D + i * 0.4D,
                        player.getYRot(), 0.0F);
                if (!level.noCollision(mob)) {
                    continue;
                }
                if (mob instanceof Monster monster && !player.isCreative() && !player.isSpectator()) {
                    monster.setTarget(player);
                }
                DailyEventRewardService.bindEncounter(mob, player, claimRegion, id, effectiveUntil);
                mob.setPersistenceRequired();
                if (level.addFreshEntity(mob)) {
                    spawned++;
                }
            }
            if (spawned <= 0) {
                return;
            }
            latch(player, claimRegion, id, effectiveUntil);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.daily_event.spawn", displayName(id), spawned), true);
        }
    }

    private static EncounterPlan plan(String regionId, String id, DailyEventEffectCatalog.Event event) {
        String region = normalize(regionId);
        if (region.isBlank()) {
            region = legacyRegion(id);
        }
        if (authoredTrue(event, "spawn_elite")) {
            return new EncounterPlan(
                    Kind.SHELL, region, 1, null, id + "_elite", SummonedServitorEntity.Archetype.BEAST,
                    Math.max(3, event.combatTier()));
        }
        if (authoredTrue(event, "spawn_beast_wave") || event.hasToken("sea_spawn_boost")
                || event.rawFields().containsKey("spawn_multiplier")
                && (id.contains("beast") || id.contains("migration") || id.contains("tide"))) {
            int count = 3;
            if (authoredTrue(event, "spawn_beast_wave")) {
                count = 5;
            }
            if (event.hasToken("sea_spawn_boost")) {
                count = Math.max(count, 4);
            }
            if (event.rawFields().containsKey("spawn_multiplier")) {
                count = (int) Math.ceil(count * Math.max(1.0D, event.spawnMultiplier()));
            }
            return new EncounterPlan(Kind.BEAST, region, Math.max(1, Math.min(6, count)), null,
                    "", null, 0);
        }
        String spawn = event.spawn();
        if (!spawn.isBlank()) {
            if (spawn.contains("wraith") || spawn.contains("ghost") || spawn.contains("demon")) {
                return new EncounterPlan(
                        Kind.SHELL, region, Math.max(1, 1 + event.combatTier()), null, spawn,
                        SummonedServitorEntity.Archetype.GHOST, Math.max(1, event.combatTier()));
            }
            // Unknown authored spawn ids are preserved by the catalog but fail closed here.
            return none(region);
        }
        if (event.combatTier() > 0) {
            return new EncounterPlan(
                    Kind.SHELL, region, 1, null, id,
                    TrialCombatShellService.archetypeFor(id), event.combatTier());
        }
        if (event.hasToken("random_ambush_low")) {
            return new EncounterPlan(Kind.SHELL, region, 1, null, id + "_ambush",
                    SummonedServitorEntity.Archetype.GENERIC, 1);
        }
        if (!event.warPhase().isBlank() || id.contains("ambush") || id.contains("duel")
                || id.contains("patrol") || id.contains("war_merit_muster")) {
            SummonedServitorEntity.Archetype archetype = id.contains("soul_array")
                    ? SummonedServitorEntity.Archetype.GHOST
                    : SummonedServitorEntity.Archetype.GENERIC;
            return new EncounterPlan(Kind.SHELL, region, 1, null, id + "_encounter",
                    archetype, Math.max(1, event.combatTier()));
        }
        return none(region);
    }

    /** Whether this authored roll must settle through an owned encounter kill. */
    public static boolean hasCombatPlan(String regionId, DailyEventEffectCatalog.Event event) {
        if (event == null) {
            return false;
        }
        return plan(regionId, event.id(), event).kind() != Kind.NONE;
    }

    private static EncounterPlan legacyPlan(String regionId, String id) {
        String region = normalize(regionId);
        if (region.isBlank()) {
            region = legacyRegion(id);
        }
        if (id.contains("bandit") || id.contains("rogue") || id.contains("raid")) {
            return new EncounterPlan(Kind.VANILLA, region, 2, EntityType.PILLAGER,
                    "", null, 0);
        }
        if (id.contains("beast") || id.contains("migration") || id.contains("tide")) {
            return new EncounterPlan(Kind.BEAST, region, 3, null, "", null, 0);
        }
        if (id.contains("demon") || id.contains("qi") || id.contains("corruption")) {
            return new EncounterPlan(Kind.VANILLA, region, 2, EntityType.VEX,
                    "", null, 0);
        }
        return none(region);
    }

    private static EncounterPlan none(String region) {
        return new EncounterPlan(Kind.NONE, normalize(region), 0, null, "", null, 0);
    }

    private static boolean authoredTrue(DailyEventEffectCatalog.Event event, String token) {
        return event.tokenValue(token).map(value -> {
            try {
                JsonElement parsed = JsonParser.parseString(value);
                if (parsed.isJsonPrimitive()) {
                    if (parsed.getAsJsonPrimitive().isBoolean()) {
                        return parsed.getAsBoolean();
                    }
                    if (parsed.getAsJsonPrimitive().isNumber()) {
                        return parsed.getAsDouble() != 0.0D;
                    }
                    return !"false".equalsIgnoreCase(parsed.getAsString());
                }
            } catch (Exception ignored) {
                // Preserve presence semantics for legacy scalar/array tokens.
            }
            return !"false".equalsIgnoreCase(value);
        }).orElse(false);
    }

    private static boolean isLatched(ServerPlayer player, String region, String event, long until, long now) {
        CompoundTag data = player.getPersistentData();
        CompoundTag latch = data.getCompound(LATCH_ROOT).copy();
        boolean migratedLegacyClaim = migrateLegacyBooleanClaims(data, latch, region, event, until, now);
        boolean claimed = migratedLegacyClaim || hasEncounterClaim(latch, region, event, until, now);
        if (latch.isEmpty()) {
            data.remove(LATCH_ROOT);
        } else {
            data.put(LATCH_ROOT, latch);
        }
        return claimed;
    }

    static boolean migrateLegacyBooleanClaims(CompoundTag data, CompoundTag latch, String region,
                                              String event, long until, long now) {
        if (data == null || latch == null) {
            return false;
        }
        boolean migrated = false;
        // Migrate the unbounded pre-0.2.155 keys before removing them. The
        // prefix is owned by this service and cannot contain other gameplay data.
        for (String key : List.copyOf(data.getAllKeys())) {
            if (key.startsWith(LEGACY_TAG_PREFIX)) {
                String legacyEvent = normalize(key.substring(LEGACY_TAG_PREFIX.length()));
                if (data.getBoolean(key) && normalize(event).equals(legacyEvent)) {
                    recordEncounterClaim(latch, region, event, until, now);
                    migrated = hasEncounterClaim(latch, region, event, until, now);
                }
                data.remove(key);
            }
        }
        return migrated;
    }

    private static void latch(ServerPlayer player, String region, String event, long until) {
        CompoundTag latch = player.getPersistentData().getCompound(LATCH_ROOT).copy();
        recordEncounterClaim(latch, region, event, until, player.level().getGameTime());
        latch.putString(LATCH_REGION, normalize(region));
        latch.putString(LATCH_EVENT, normalize(event));
        latch.putLong(LATCH_UNTIL, Math.max(0L, until));
        player.getPersistentData().put(LATCH_ROOT, latch);
    }

    static String encounterClaimKey(String region, String event, long until) {
        return normalize(region) + "|" + normalize(event) + "|" + Math.max(0L, until);
    }

    static boolean hasEncounterClaim(CompoundTag latch, String region, String event,
                                     long until, long gameTime) {
        long effectiveUntil = until > 0L ? until : fallbackClaimUntil(gameTime);
        String key = encounterClaimKey(region, event, effectiveUntil);
        return readEncounterClaims(latch, gameTime).contains(key);
    }

    static void recordEncounterClaim(CompoundTag latch, String region, String event,
                                     long until, long gameTime) {
        long effectiveUntil = until > 0L ? until : fallbackClaimUntil(gameTime);
        if (effectiveUntil <= gameTime) {
            return;
        }
        String key = encounterClaimKey(region, event, effectiveUntil);
        List<String> claims = new ArrayList<>(readEncounterClaims(latch, gameTime));
        claims.remove(key);
        claims.add(key);
        writeEncounterClaims(latch, claims);
    }

    static int encounterClaimCount(CompoundTag latch, long gameTime) {
        return readEncounterClaims(latch, gameTime).size();
    }

    private static List<String> readEncounterClaims(CompoundTag latch, long gameTime) {
        LinkedHashSet<String> claims = new LinkedHashSet<>();
        ListTag stored = latch.getList(LATCH_CLAIMS, Tag.TAG_STRING);
        for (int i = 0; i < stored.size(); i++) {
            String claim = stored.getString(i);
            if (isCanonicalClaim(claim) && !isExpiredClaim(claim, gameTime)) {
                claims.add(claim);
            }
        }

        String legacyRegion = normalize(latch.getString(LATCH_REGION));
        String legacyEvent = normalize(latch.getString(LATCH_EVENT));
        long legacyUntil = latch.getLong(LATCH_UNTIL);
        if (!legacyEvent.isBlank() && legacyUntil > gameTime) {
            claims.add(encounterClaimKey(legacyRegion, legacyEvent, legacyUntil));
        } else if (!legacyEvent.isBlank()) {
            latch.remove(LATCH_REGION);
            latch.remove(LATCH_EVENT);
            latch.remove(LATCH_UNTIL);
        }

        List<String> ordered = new ArrayList<>(claims);
        int from = Math.max(0, ordered.size() - MAX_LATCH_CLAIMS);
        List<String> bounded = List.copyOf(ordered.subList(from, ordered.size()));
        writeEncounterClaims(latch, bounded);
        return bounded;
    }

    private static void writeEncounterClaims(CompoundTag latch, List<String> claims) {
        if (claims == null || claims.isEmpty()) {
            latch.remove(LATCH_CLAIMS);
            return;
        }
        ListTag stored = new ListTag();
        int from = Math.max(0, claims.size() - MAX_LATCH_CLAIMS);
        for (int i = from; i < claims.size(); i++) {
            String claim = claims.get(i);
            if (claim != null && !claim.isBlank()) {
                stored.add(StringTag.valueOf(claim));
            }
        }
        latch.put(LATCH_CLAIMS, stored);
    }

    private static boolean isExpiredClaim(String claim, long gameTime) {
        int separator = claim == null ? -1 : claim.lastIndexOf('|');
        if (separator < 0 || separator >= claim.length() - 1) {
            return true;
        }
        try {
            long until = Long.parseLong(claim.substring(separator + 1));
            return until <= 0L || gameTime >= until;
        } catch (NumberFormatException ignored) {
            return true;
        }
    }

    private static boolean isCanonicalClaim(String claim) {
        if (claim == null) {
            return false;
        }
        int first = claim.indexOf('|');
        int last = claim.lastIndexOf('|');
        if (first < 0 || last <= first || last >= claim.length() - 1) {
            return false;
        }
        try {
            return Long.parseLong(claim.substring(last + 1)) > 0L;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    static long fallbackClaimUntil(long gameTime) {
        long normalizedTime = Math.max(0L, gameTime);
        long dayStart = normalizedTime - normalizedTime % 24000L;
        return dayStart > Long.MAX_VALUE - 24000L ? Long.MAX_VALUE : dayStart + 24000L;
    }

    private static boolean hasRegionSpawnTable(String region) {
        String wanted = normalize(region);
        return !wanted.isBlank() && BeastSpawnTableService.tables().stream()
                .anyMatch(table -> {
                    String tableRegion = normalize(table.region());
                    if (wanted.equals(tableRegion)) {
                        return true;
                    }
                    return ("wutu_border".equals(wanted) || "mulan_border".equals(wanted))
                            && ("mulan".equals(tableRegion) || "mulan_border".equals(tableRegion));
                });
    }

    private static String legacyRegion(String id) {
        if (id.contains("mulan")) {
            return "mulan";
        }
        if (id.contains("sea") || id.contains("chaotic")) {
            return "chaotic_sea";
        }
        if (id.contains("dajin") || id.contains("kunwu")) {
            return "dajin";
        }
        return "tiannan";
    }

    private enum Kind { NONE, BEAST, VANILLA, SHELL }

    private record EncounterPlan(Kind kind, String region, int count, EntityType<? extends Mob> entityType,
                                 String shellId, SummonedServitorEntity.Archetype archetype,
                                 int combatTier) {}

    public static void maybeSpawn(ServerPlayer player, String eventId) {
        if (player == null || eventId == null || eventId.isBlank()) {
            return;
        }
        String id = normalize(eventId);
        String region = CultivationHelper.get(player)
                .map(cultivation -> cultivation.getWorldpackCurrentRegionId())
                .orElseGet(() -> legacyRegion(id));
        long until = CultivationHelper.get(player)
                .filter(cultivation -> id.equals(cultivation.getWorldpackActiveDailyEventId()))
                .map(cultivation -> cultivation.getWorldpackActiveDailyEventUntilTick())
                .filter(activeUntil -> activeUntil > player.level().getGameTime())
                .orElseGet(() -> fallbackClaimUntil(player.level().getGameTime()));
        maybeSpawn(player, region, id, until);
    }

    /** Event ids are retained for scheduling; only authored Chinese names reach chat. */
    private static Component displayName(String eventId) {
        String id = normalize(eventId);
        String authored = DailyEventEffectCatalog.builtin().find(id)
                .map(DailyEventEffectCatalog.Event::display)
                .filter(value -> !value.isBlank())
                .orElseGet(() -> WorldpackDataService.builtin().findDailyEvent(id)
                .map(WorldpackDataService.DailyEvent::displayZh)
                .orElse(""));
        if (PlayerDisplayText.isSafe(authored)) {
            return Component.literal(authored.trim());
        }
        if (id.contains("merchant") || id.contains("caravan")) {
            return Component.literal("商队异象");
        }
        if (id.contains("beast") || id.contains("migration") || id.contains("tide")) {
            return Component.literal("灵兽异动");
        }
        if (id.contains("bandit") || id.contains("rogue") || id.contains("raid")) {
            return Component.literal("劫修来袭");
        }
        if (id.contains("demon") || id.contains("qi") || id.contains("corruption")) {
            return Component.literal("魔气侵染");
        }
        return Component.literal("未知异象");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

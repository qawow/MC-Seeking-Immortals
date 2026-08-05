package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.beast.BeastTierService;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Y-A-2: consumes {@link SecretRealmCatalogService.LayerDef#spawns()} so authored per-layer
 * rosters become live spawns instead of parsed-but-unused data.
 *
 * <p>Fail-closed per entry: an authored spawn id that does not resolve to a bestiary entry is
 * skipped and logged, never substituted with a guess. A layer whose whole roster is unresolvable
 * simply contributes nothing, so partially-authored realms stay playable.</p>
 *
 * <p>There is no per-session spawn budget in {@link SecretRealmProgressSavedData.Session}, so this
 * service caps each request the way {@link BeastSpawnTableService} does and latches per
 * session+layer to keep re-entry from stacking rosters.</p>
 */
public final class SecretRealmLayerSpawnService {
    /** Hard ceiling per layer request; mirrors BeastSpawnTableService.MAX_SPAWN_PER_REQUEST. */
    public static final int MAX_LAYER_SPAWNS_PER_REQUEST = 4;
    /** Hard ceiling across one realm entry so deep realms cannot flood the arena. */
    public static final int MAX_LAYER_SPAWNS_PER_ENTRY = 10;
    private static final String LAYER_SPAWN_ROOT = "seeking_immortals_secret_realm_layer_spawns";
    private static final String ENCOUNTER_PREFIX = "trial:layer:";

    private SecretRealmLayerSpawnService() {}

    /** Resolution outcome for one authored spawn entry. */
    public record Resolved(String authoredId, String beastId, int weight, boolean resolvable) {}

    /**
     * Resolves an authored layer roster without touching the world.
     * Unresolvable ids are returned with {@code resolvable=false} so callers (and tests) can
     * audit authored drift instead of silently spawning something else.
     */
    public static List<Resolved> resolveRoster(SecretRealmCatalogService.LayerDef layer) {
        if (layer == null || layer.spawns().isEmpty()) {
            return List.of();
        }
        List<Resolved> out = new ArrayList<>();
        for (SecretRealmCatalogService.SpawnDef spawn : layer.spawns()) {
            String authored = spawn.id() == null ? "" : spawn.id().trim();
            if (authored.isBlank()) {
                continue;
            }
            Optional<BeastBestiaryService.BeastEntry> entry = BeastBestiaryService.find(authored);
            boolean usable = entry.isPresent() && !BeastSpawnTableService.isBanned(entry.get().id());
            out.add(new Resolved(
                    authored,
                    usable ? entry.get().id() : "",
                    Math.max(1, spawn.weight()),
                    usable));
        }
        return List.copyOf(out);
    }

    /** Authored ids in this layer that cannot be spawned; empty means the roster is fully wired. */
    public static List<String> unresolvedIds(SecretRealmCatalogService.LayerDef layer) {
        return resolveRoster(layer).stream()
                .filter(resolved -> !resolved.resolvable())
                .map(Resolved::authoredId)
                .toList();
    }

    /**
     * Spawns the authored roster for every layer of a realm once per session.
     *
     * @return number of mobs actually spawned
     */
    public static int spawnRealmLayers(ServerPlayer player, String realmId) {
        if (player == null || realmId == null || realmId.isBlank()) {
            return 0;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        String id = realmId.trim().toLowerCase(Locale.ROOT);
        SecretRealmCatalogService.RealmDef realm = SecretRealmCatalogService.find(id).orElse(null);
        if (realm == null || realm.layers().isEmpty()) {
            return 0;
        }
        SecretRealmProgressSavedData.Session session =
                SecretRealmSessionService.activeSession(player, id).orElse(null);
        if (session == null) {
            return 0;
        }
        int spawned = 0;
        int skipped = 0;
        BlockPos base = player.blockPosition().above();
        List<SecretRealmCatalogService.LayerDef> layers = realm.layers();
        for (int i = 0; i < layers.size() && spawned < MAX_LAYER_SPAWNS_PER_ENTRY; i++) {
            SecretRealmCatalogService.LayerDef layer = layers.get(i);
            // Spread layer rosters so they do not collapse onto one spot.
            BlockPos center = base.offset((i % 3) * 4 - 4, 0, (i / 3) * 4 + 4);
            int budget = Math.min(MAX_LAYER_SPAWNS_PER_REQUEST, MAX_LAYER_SPAWNS_PER_ENTRY - spawned);
            LayerResult result = spawnLayer(level, player, session, realm, layer, center, budget);
            spawned += result.spawned();
            skipped += result.skipped();
        }
        if (spawned > 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.layer_roster_spawned",
                    realmDisplay(realm), spawned));
        }
        if (skipped > 0) {
            SeekingImmortalsMod.LOGGER.warn(
                    "M09 layer roster for {} skipped {} unresolvable authored spawn id(s)", id, skipped);
        }
        return spawned;
    }

    private record LayerResult(int spawned, int skipped) {}

    private static LayerResult spawnLayer(ServerLevel level, ServerPlayer player,
                                          SecretRealmProgressSavedData.Session session,
                                          SecretRealmCatalogService.RealmDef realm,
                                          SecretRealmCatalogService.LayerDef layer,
                                          BlockPos center, int budget) {
        if (budget <= 0) {
            return new LayerResult(0, 0);
        }
        List<Resolved> roster = resolveRoster(layer);
        if (roster.isEmpty()) {
            return new LayerResult(0, 0);
        }
        int skipped = (int) roster.stream().filter(resolved -> !resolved.resolvable()).count();
        List<Resolved> usable = roster.stream().filter(Resolved::resolvable).toList();
        if (usable.isEmpty()) {
            return new LayerResult(0, skipped);
        }
        String layerId = layer.id() == null ? "" : layer.id().trim().toLowerCase(Locale.ROOT);
        String encounterId = ENCOUNTER_PREFIX + layerId;
        CompoundTag root = player.getPersistentData().getCompound(LAYER_SPAWN_ROOT).copy();
        String sessionKey = session.sessionId() + "|" + realm.id() + "|" + encounterId;
        if (root.getBoolean(sessionKey)) {
            return new LayerResult(0, skipped);
        }
        RandomSource random = level.random;
        int spawned = 0;
        for (int slot = 0; slot < budget; slot++) {
            Resolved pick = pickWeighted(usable, random);
            if (pick == null) {
                break;
            }
            int tier = layerTier(layer, pick.beastId(), random);
            BeastTierService.ScaledStats stats = BeastTierService.scaleStats(tier);
            SummonedServitorEntity.Archetype archetype =
                    TrialCombatShellService.archetypeFor(pick.beastId());
            BlockPos pos = center.offset(slot % 2 == 0 ? slot : -slot, 1, slot % 3);
            Mob mob = TrialCombatShellService.spawnHostile(
                    level, pos, player.getYRot(), pick.beastId(),
                    stats.health(), stats.damage(), archetype);
            if (mob == null) {
                continue;
            }
            mob.setCustomNameVisible(true);
            mob.setTarget(player);
            // Bind to the session so only the owner can claim kill gates (same rule as patrols).
            SecretRealmTrialService.tagTrial(mob, player, session,
                    SecretRealmTrialService.KIND_PATROL, realm.id(), encounterId);
            spawned++;
        }
        if (spawned <= 0) {
            return new LayerResult(0, skipped);
        }
        root.putBoolean(sessionKey, true);
        player.getPersistentData().put(LAYER_SPAWN_ROOT, root);
        return new LayerResult(spawned, skipped);
    }

    /** Authored threat band drives tier; the bestiary tier is the floor so elites stay elite. */
    private static int layerTier(SecretRealmCatalogService.LayerDef layer, String beastId,
                                RandomSource random) {
        int min = Math.max(1, layer.threatMin());
        int max = Math.max(min, layer.threatMax());
        int band = min == max ? min : min + random.nextInt(max - min + 1);
        int beastTier = BeastBestiaryService.find(beastId)
                .map(BeastBestiaryService.BeastEntry::tier)
                .orElse(band);
        return BeastTierService.clampTier(Math.max(band, beastTier));
    }

    private static Resolved pickWeighted(List<Resolved> usable, RandomSource random) {
        int total = 0;
        for (Resolved resolved : usable) {
            total += Math.max(1, resolved.weight());
        }
        if (total <= 0) {
            return null;
        }
        int pick = random.nextInt(total);
        int cursor = 0;
        for (Resolved resolved : usable) {
            cursor += Math.max(1, resolved.weight());
            if (pick < cursor) {
                return resolved;
            }
        }
        return usable.get(usable.size() - 1);
    }

    private static Component realmDisplay(SecretRealmCatalogService.RealmDef realm) {
        String display = realm.display();
        return display == null || display.isBlank()
                ? Component.literal(realm.id())
                : Component.literal(display);
    }
}

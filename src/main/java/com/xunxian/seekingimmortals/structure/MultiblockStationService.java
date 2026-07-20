package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * M07 stable station formed API for M04/M08/M13 (and M06 region events).
 * Large structures use TTL cache + dirty invalidation to avoid per-tick full scans.
 */
public final class MultiblockStationService {
    private static final long SMALL_TTL_TICKS = 10L;
    private static final long LARGE_TTL_TICKS = 40L;

    private static final Map<CacheKey, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private MultiblockStationService() {}

    /**
     * Stable public API: whether the multiblock workstation/station is formed at origin.
     * {@code stationId} matches multiblock structure index ids (e.g. alchemy_furnace_g1, sect_formation_hub).
     */
    public static boolean isStationFormed(LevelReader level, String stationId, BlockPos origin) {
        return check(level, stationId, origin).formed();
    }

    public static StationCheckResult check(LevelReader level, String stationId, BlockPos origin) {
        if (level == null || origin == null || stationId == null || stationId.isBlank()) {
            return StationCheckResult.unknown(stationId, origin);
        }
        Optional<MultiblockStructureCatalog.StructureEntry> entryOpt = MultiblockStructureCatalog.builtin().find(stationId);
        if (entryOpt.isEmpty()) {
            return StationCheckResult.unknown(stationId, origin);
        }
        MultiblockStructureCatalog.StructureEntry entry = entryOpt.get();
        long gameTime = level instanceof Level l ? l.getGameTime() : 0L;
        String dim = level instanceof Level l ? l.dimension().location().toString() : "reader";
        CacheKey key = new CacheKey(dim, entry.id(), origin.asLong());
        CacheEntry cached = CACHE.get(key);
        long ttl = entry.large() ? LARGE_TTL_TICKS : SMALL_TTL_TICKS;
        if (cached != null && !cached.dirty && gameTime - cached.checkedAt <= ttl) {
            return new StationCheckResult(entry.id(), origin.immutable(), cached.formed, entry, cached.detail);
        }

        ValidateOutcome outcome = validateLive(level, entry, origin);
        CACHE.put(key, new CacheEntry(outcome.formed(), gameTime, false, outcome.detail()));
        return new StationCheckResult(entry.id(), origin.immutable(), outcome.formed(), entry, outcome.detail());
    }

    /** Mark nearby station cache dirty after block changes (optional hook). */
    public static void markDirty(Level level, BlockPos pos) {
        if (level == null || pos == null || level.isClientSide) {
            return;
        }
        String dim = level.dimension().location().toString();
        long packed = pos.asLong();
        for (Map.Entry<CacheKey, CacheEntry> e : CACHE.entrySet()) {
            CacheKey key = e.getKey();
            if (!dim.equals(key.dimensionId())) {
                continue;
            }
            BlockPos core = BlockPos.of(key.packedPos());
            // Conservative: invalidate cores within 12 blocks of the changed pos.
            if (core.distManhattan(pos) <= 12) {
                CacheEntry old = e.getValue();
                e.setValue(new CacheEntry(old.formed, old.checkedAt, true, old.detail));
            } else if (key.packedPos() == packed) {
                e.setValue(new CacheEntry(e.getValue().formed, e.getValue().checkedAt, true, e.getValue().detail));
            }
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static int cacheSize() {
        return CACHE.size();
    }

    public static int structureCount() {
        return MultiblockStructureCatalog.builtin().size();
    }

    private static ValidateOutcome validateLive(LevelReader level, MultiblockStructureCatalog.StructureEntry entry, BlockPos origin) {
        MultiblockStructureCatalog.StationPattern pattern = entry.pattern();
        String validator = pattern.validator();
        try {
            return switch (validator) {
                case "alchemy_furnace_shell" -> {
                    int tier = pattern.tier() > 0 ? pattern.tier() : guessAlchemyTier(entry.id());
                    boolean ok = AlchemyFurnaceShellStructure.isComplete(level, origin, tier);
                    yield new ValidateOutcome(ok, "alchemy_shell:t" + tier);
                }
                case "sect_earth_fire_room" -> {
                    boolean ok = SectEarthFireRoomMultiblock.isComplete(level, origin);
                    yield new ValidateOutcome(ok, "earth_fire_room");
                }
                case "refinement_forge" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    boolean ok = RefinementForgeStructure.validate(live, origin,
                            ModBlocks.REFINEMENT_FORGE.get(), ModBlocks.SPIRIT_ORE.get()).complete();
                    yield new ValidateOutcome(ok, "refinement_forge");
                }
                case "refinement_forge_g2" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    boolean ok = RefinementForgeG2Structure.validate(live, origin,
                            ModBlocks.REFINEMENT_FORGE_G2.get(), ModBlocks.SPIRIT_ORE.get()).complete();
                    yield new ValidateOutcome(ok, "refinement_forge_g2");
                }
                case "refinement_forge_g3" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    boolean ok = RefinementForgeG3Structure.validate(live, origin,
                            ModBlocks.REFINEMENT_FORGE_G3.get(), ModBlocks.SPIRIT_ORE.get()).complete();
                    yield new ValidateOutcome(ok, "refinement_forge_g3");
                }
                case "talisman_table" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    boolean ok = TalismanTableStructure.validate(live, origin,
                            ModBlocks.TALISMAN_TABLE.get(), ModBlocks.SPIRIT_ORE.get()).complete();
                    yield new ValidateOutcome(ok, "talisman_table");
                }
                case "puppet_assembly_bench" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    boolean ok = PuppetAssemblyBenchStructure.validate(live, origin,
                            ModBlocks.PUPPET_ASSEMBLY_BENCH.get(), ModBlocks.SPIRIT_ORE.get()).complete();
                    yield new ValidateOutcome(ok, "puppet_assembly_bench");
                }
                case "spirit_herb_planter" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    boolean ok = SpiritHerbPlanterStructure.validate(live, origin,
                            ModBlocks.SPIRIT_GATHERING_ARRAY.get()).complete();
                    yield new ValidateOutcome(ok, "spirit_herb_planter");
                }
                case "spirit_gathering_formation" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    int radius = pattern.radius() > 0 ? pattern.radius() : 2;
                    boolean ok = radius == SpiritGatheringFormationStructure.RING_RADIUS
                            ? SpiritGatheringFormationStructure.validate(live, origin, ModBlocks.SPIRIT_GATHERING_ARRAY.get()).complete()
                            : RingFormationStructure.validate(live, origin, ModBlocks.SPIRIT_GATHERING_ARRAY.get(), radius).complete();
                    yield new ValidateOutcome(ok, "spirit_gather_r" + radius);
                }
                case "fixed_teleport_array" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    boolean ok = FixedTeleportArrayStructure.validate(live, origin, ModBlocks.TELEPORT_ARRAY_PEDESTAL.get()).complete();
                    yield new ValidateOutcome(ok, "fixed_teleport");
                }
                case "long_range_teleport_array" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    Block ring = ModBlocks.LONG_RANGE_TELEPORT_ARRAY.get();
                    Block frame = ModBlocks.SPIRIT_ORE.get();
                    boolean ok = LongRangeTeleportArrayStructure.validate(live, origin, ring, frame).complete();
                    yield new ValidateOutcome(ok, "long_range_teleport");
                }
                case "blood_sacrifice_altar" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    boolean ok = BloodSacrificeAltarStructure.validate(live, origin,
                            ModBlocks.BLOOD_SACRIFICE_ALTAR.get(), ModBlocks.SPIRIT_ORE.get()).complete();
                    yield new ValidateOutcome(ok, "blood_sacrifice");
                }
                case "thunder_tribulation_altar" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    boolean ok = ThunderTribulationAltarStructure.validate(live, origin,
                            ModBlocks.THUNDER_TRIBULATION_ALTAR.get(), ModBlocks.SPIRIT_ORE.get()).complete();
                    yield new ValidateOutcome(ok, "thunder_altar");
                }
                case "single_core" -> {
                    BlockState state = level.getBlockState(origin);
                    boolean ok = !state.isAir() && state.getBlock() != Blocks.CAVE_AIR && state.getBlock() != Blocks.VOID_AIR;
                    yield new ValidateOutcome(ok, "single_core");
                }
                case "ring" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    int radius = pattern.radius() > 0 ? pattern.radius() : Math.max(1, entry.radius());
                    Block ringBlock = resolveRingBlock(pattern.ringRole());
                    boolean ok = RingFormationStructure.validate(live, origin, ringBlock, radius).complete();
                    yield new ValidateOutcome(ok, "ring_r" + radius + ":" + pattern.ringRole());
                }
                default -> {
                    // Conservative fallback: ring of spirit ore at catalog radius (capped).
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    int radius = Math.min(4, Math.max(1, entry.radius()));
                    boolean ok = RingFormationStructure.validate(live, origin, ModBlocks.SPIRIT_ORE.get(), radius).complete();
                    yield new ValidateOutcome(ok, "fallback_ring_r" + radius);
                }
            };
        } catch (Throwable t) {
            // Missing blocks / incomplete registry during unit tests → not formed.
            return new ValidateOutcome(false, "error:" + t.getClass().getSimpleName());
        }
    }

    private static int guessAlchemyTier(String id) {
        if (id == null) {
            return 1;
        }
        if (id.contains("g3") || id.contains("_3")) {
            return 3;
        }
        if (id.contains("g2") || id.contains("_2")) {
            return 2;
        }
        if (id.contains("g4") || id.contains("_4") || id.contains("infant") || id.contains("immortal")) {
            return 4;
        }
        return 1;
    }

    private static Block resolveRingBlock(String role) {
        String r = role == null ? "" : role.trim().toLowerCase(java.util.Locale.ROOT);
        if (r.contains("gather") || r.contains("array")) {
            return ModBlocks.SPIRIT_GATHERING_ARRAY.get();
        }
        return ModBlocks.SPIRIT_ORE.get();
    }

    public record StationCheckResult(
            String stationId,
            BlockPos origin,
            boolean formed,
            MultiblockStructureCatalog.StructureEntry entry,
            String detail
    ) {
        public static StationCheckResult unknown(String stationId, BlockPos origin) {
            return new StationCheckResult(
                    stationId == null ? "" : stationId,
                    origin == null ? BlockPos.ZERO : origin.immutable(),
                    false,
                    null,
                    "unknown_station");
        }
    }

    private record ValidateOutcome(boolean formed, String detail) {}

    private record CacheKey(String dimensionId, String stationId, long packedPos) {
        private CacheKey {
            Objects.requireNonNull(dimensionId);
            Objects.requireNonNull(stationId);
        }
    }

    private static final class CacheEntry {
        private final boolean formed;
        private final long checkedAt;
        private final boolean dirty;
        private final String detail;

        private CacheEntry(boolean formed, long checkedAt, boolean dirty, String detail) {
            this.formed = formed;
            this.checkedAt = checkedAt;
            this.dirty = dirty;
            this.detail = detail == null ? "" : detail;
        }
    }
}

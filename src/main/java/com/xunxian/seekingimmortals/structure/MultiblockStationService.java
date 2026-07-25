package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * M07 stable station formed API for M04/M08/M13 (and M06 region events).
 * Large structures use TTL cache + dirty invalidation to avoid per-tick full scans.
 */
public final class MultiblockStationService {
    private static final long SMALL_TTL_TICKS = 10L;
    private static final long LARGE_TTL_TICKS = 40L;

    private static final Map<CacheKey, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> SINGLE_CORE_BLOCK_IDS = Map.of(
            "low_spirit_iron_ore", new ResourceLocation(SeekingImmortalsMod.MODID, "low_spirit_iron_ore"),
            "yin_essence_ore_block", new ResourceLocation(SeekingImmortalsMod.MODID, "yin_essence_ore"));
    private static final Set<String> SPECIALIZED_VALIDATORS = Set.of(
            "alchemy_furnace_shell",
            "altar",
            "array_hub",
            "blood_sacrifice_altar",
            "fixed_teleport_array",
            "flying_boat_dock",
            "long_range_teleport_array",
            "platform",
            "puppet_assembly_bench",
            "puppet_core_forge",
            "refinement_forge",
            "refinement_forge_g2",
            "refinement_forge_g3",
            "refinement_forge_g4",
            "refinement_forge_g5",
            "refinement_forge_g6",
            "ring",
            "sect_earth_fire_room",
            "single_core",
            "spirit_beast_evolution_pool",
            "spirit_gathering_formation",
            "spirit_herb_planter",
            "talisman_table",
            "teleport_gate",
            "thunder_tribulation_altar");
    private static final Set<String> IMPLEMENTED_VALIDATORS = implementedValidatorsInternal();
    private static final Set<String> FAIL_CLOSED_VALIDATORS = Set.of();
    private static final Set<String> SUPPORTED_VALIDATORS = mergeValidators();

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

    /** Every validator authored by the shipped station-pattern catalog. */
    public static Set<String> supportedValidators() {
        return SUPPORTED_VALIDATORS;
    }

    /** Validators backed by a concrete geometry check. */
    public static Set<String> implementedValidators() {
        return IMPLEMENTED_VALIDATORS;
    }

    /** Recognized authored validators that deliberately reject until implemented. */
    public static Set<String> failClosedValidators() {
        return FAIL_CLOSED_VALIDATORS;
    }

    private static ValidateOutcome validateLive(LevelReader level, MultiblockStructureCatalog.StructureEntry entry, BlockPos origin) {
        MultiblockStructureCatalog.StationPattern pattern = entry.pattern();
        String validator = pattern.validator();
        if (CatalogStationGeometry.supports(validator)) {
            CatalogStationGeometry.Validation validation =
                    CatalogStationGeometry.validate(level, entry, origin);
            return new ValidateOutcome(validation.formed(), validation.detail());
        }
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
                    boolean legacy = RefinementForgeG3Structure.validate(live, origin,
                            ModBlocks.REFINEMENT_FORGE_G3.get(), ModBlocks.SPIRIT_ORE.get()).complete();
                    boolean furnace = RefinementFurnaceStructure.validate(live, origin,
                            ModBlocks.REFINEMENT_FORGE_G3.get(), ModBlocks.SPIRIT_ORE.get(), Blocks.LAVA).complete();
                    yield new ValidateOutcome(legacy || furnace,
                            furnace ? "refinement_furnace" : "refinement_forge_g3");
                }
                case "refinement_forge_g4" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    boolean ok = RefinementForgeHighStructure.validate(live, origin,
                            ModBlocks.REFINEMENT_FORGE_G4.get(), ModBlocks.SPIRIT_ORE.get(), 4).complete();
                    yield new ValidateOutcome(ok, "refinement_forge_g4");
                }
                case "refinement_forge_g5" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    boolean ok = RefinementForgeHighStructure.validate(live, origin,
                            ModBlocks.REFINEMENT_FORGE_G5.get(), ModBlocks.SPIRIT_ORE.get(), 5).complete();
                    yield new ValidateOutcome(ok, "refinement_forge_g5");
                }
                case "refinement_forge_g6" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    boolean ok = RefinementForgeHighStructure.validate(live, origin,
                            ModBlocks.REFINEMENT_FORGE_G6.get(), ModBlocks.SPIRIT_ORE.get(), 6).complete();
                    yield new ValidateOutcome(ok, "refinement_forge_g6");
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
                    boolean standard = radius == SpiritGatheringFormationStructure.RING_RADIUS
                            ? SpiritGatheringFormationStructure.validate(live, origin, ModBlocks.SPIRIT_GATHERING_ARRAY.get()).complete()
                            : RingFormationStructure.validate(live, origin, ModBlocks.SPIRIT_GATHERING_ARRAY.get(), radius).complete();
                    boolean advanced = AdvancedSpiritGatheringArrayStructure.validate(
                            live,
                            origin,
                            ModBlocks.SPIRIT_ORE.get(),
                            ModBlocks.SPIRIT_GATHERING_FORMATION_CORE.get()).complete();
                    yield new ValidateOutcome(standard || advanced,
                            advanced ? "spirit_gather_advanced" : "spirit_gather_r" + radius);
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
                    boolean legacy = LongRangeTeleportArrayStructure.validate(
                            live,
                            origin,
                            ModBlocks.LONG_RANGE_TELEPORT_ARRAY.get(),
                            ModBlocks.SPIRIT_ORE.get()).complete();
                    boolean layered = TeleportationArrayStructure.validate(
                            live,
                            origin,
                            ModBlocks.SPIRIT_ORE.get(),
                            ModBlocks.SPIRIT_GATHERING_ARRAY.get(),
                            ModBlocks.LONG_RANGE_TELEPORT_ARRAY.get(),
                            ModBlocks.TELEPORT_ARRAY_PEDESTAL.get()).complete();
                    yield new ValidateOutcome(legacy || layered,
                            layered ? "teleportation_array_layered" : "long_range_teleport");
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
                    boolean altar = ThunderTribulationAltarStructure.validate(live, origin,
                            ModBlocks.THUNDER_TRIBULATION_ALTAR.get(), ModBlocks.SPIRIT_ORE.get()).complete();
                    boolean platform = TribulationPlatformStructure.validate(
                            live,
                            origin,
                            ModBlocks.SPIRIT_ORE.get(),
                            ModBlocks.THUNDER_TRIBULATION_ALTAR.get(),
                            Blocks.LIGHTNING_ROD,
                            ModBlocks.SPIRIT_GATHERING_ARRAY.get()).complete();
                    yield new ValidateOutcome(altar || platform,
                            platform ? "tribulation_platform" : "thunder_altar");
                }
                case "single_core" -> {
                    Optional<ResourceLocation> blockIdOpt = singleCoreBlockId(entry.id());
                    if (blockIdOpt.isEmpty()) {
                        yield new ValidateOutcome(false, "missing_core_mapping:" + entry.id());
                    }
                    ResourceLocation blockId = blockIdOpt.get();
                    if (!ForgeRegistries.BLOCKS.containsKey(blockId)) {
                        yield new ValidateOutcome(false, "missing_core_block:" + blockId);
                    }
                    Block expected = ForgeRegistries.BLOCKS.getValue(blockId);
                    boolean ok = expected != null && level.getBlockState(origin).is(expected);
                    yield new ValidateOutcome(ok, "single_core:" + blockId);
                }
                case "array_hub" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    if ("kill_array_hub".equals(entry.id())) {
                        boolean ok = ArrayHubStructure.validateKillHub(live, origin).complete();
                        yield new ValidateOutcome(ok, "array_hub:kill");
                    }
                    if ("illusion_array_hub".equals(entry.id())) {
                        boolean ok = ArrayHubStructure.validateIllusionHub(live, origin).complete();
                        yield new ValidateOutcome(ok, "array_hub:illusion");
                    }
                    yield new ValidateOutcome(false, "unsupported_array_hub:" + entry.id());
                }
                case "flying_boat_dock" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    // Match FlightVehicleService dock geometry: gathering-array platform + spirit-ore masts.
                    boolean ok = FlyingBoatDockStructure.validate(
                            live, origin,
                            ModBlocks.SPIRIT_GATHERING_ARRAY.get(),
                            ModBlocks.SPIRIT_ORE.get()).complete();
                    yield new ValidateOutcome(ok, "flying_boat_dock");
                }
                case "puppet_core_forge" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    // Core heart: puppet assembly bench if present, else spirit ore; ring is spirit ore.
                    Block core = ModBlocks.PUPPET_ASSEMBLY_BENCH.get();
                    boolean ok = PuppetCoreForgeStructure.validate(
                            live, origin, core, ModBlocks.SPIRIT_ORE.get()).complete();
                    yield new ValidateOutcome(ok, "puppet_core_forge");
                }
                case "spirit_beast_evolution_pool" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    boolean ok = SpiritBeastEvolutionPoolStructure.validate(
                            live, origin,
                            ModBlocks.SPIRIT_GATHERING_ARRAY.get(),
                            ModBlocks.SPIRIT_ORE.get()).complete();
                    yield new ValidateOutcome(ok, "spirit_beast_evolution_pool");
                }
                case "teleport_gate" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    Block ring = ModBlocks.SPIRIT_GATHERING_ARRAY.get();
                    Block pillar = ModBlocks.SPIRIT_ORE.get();
                    boolean ok = TeleportGateStructure.validate(live, origin, ring, pillar).complete();
                    yield new ValidateOutcome(ok, "teleport_gate");
                }
                case "platform" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    Block base = ModBlocks.SPIRIT_GATHERING_ARRAY.get();
                    Block decoration = ModBlocks.SPIRIT_ORE.get();
                    boolean ok = PlatformStructure.validate(live, origin, base, decoration).complete();
                    yield new ValidateOutcome(ok, "platform");
                }
                case "altar" -> {
                    if (!(level instanceof Level live)) {
                        yield new ValidateOutcome(false, "needs_level");
                    }
                    Block base = ModBlocks.SPIRIT_GATHERING_ARRAY.get();
                    Block offering = ModBlocks.SPIRIT_ORE.get();
                    boolean ok = AltarStructure.validate(live, origin, base, offering).complete();
                    yield new ValidateOutcome(ok, "altar");
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
                    yield new ValidateOutcome(false, "unrecognized_validator:" + validator);
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

    static Optional<ResourceLocation> singleCoreBlockId(String stationId) {
        if (stationId == null || stationId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(SINGLE_CORE_BLOCK_IDS.get(stationId));
    }

    private static Set<String> mergeValidators() {
        Set<String> validators = new HashSet<>(IMPLEMENTED_VALIDATORS);
        validators.addAll(FAIL_CLOSED_VALIDATORS);
        return Set.copyOf(validators);
    }

    private static Set<String> implementedValidatorsInternal() {
        Set<String> validators = new HashSet<>(SPECIALIZED_VALIDATORS);
        validators.addAll(CatalogStationGeometry.validators());
        return Set.copyOf(validators);
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

package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Compiles authored catalog dimensions into bounded, validator-specific build geometry. */
public final class CatalogStationGeometry {
    private static final String MOD_PREFIX = SeekingImmortalsMod.MODID + ":";
    private static final String SPIRIT_ORE = MOD_PREFIX + "spirit_ore";
    private static final String SPIRIT_ARRAY = MOD_PREFIX + "spirit_gathering_array";
    private static final String AIR = "minecraft:air";
    private static final String GLASS = "minecraft:glass";
    private static final String OAK_FENCE = "minecraft:oak_fence";
    private static final String CHEST = "minecraft:chest";
    private static final String CRAFTING_TABLE = "minecraft:crafting_table";
    private static final String MAGMA = "minecraft:magma_block";
    private static final String PACKED_ICE = "minecraft:packed_ice";
    private static final String CAMPFIRE = "minecraft:campfire";

    private static final Set<String> VALIDATORS = Set.of(
            "blood_pool",
            "brazier",
            "cultivation_chamber",
            "defense_wall",
            "furnace_safety_array",
            "grand_hall",
            "greenhouse",
            "immortal_alchemy_cauldron",
            "infant_fire_alchemy_room",
            "kunwu_frost_forge",
            "pedestal",
            "rift_core_small",
            "rift_large",
            "rift_world_seed",
            "seal_pillar",
            "secret_realm_gate",
            "sect_formation_hub",
            "small_array_3x3",
            "small_array_post",
            "small_array_single",
            "small_control_2x2",
            "small_control_post",
            "small_control_trading",
            "spirit_beast_pen",
            "storage",
            "time_acceleration_array",
            "tower",
            "trap_corridor",
            "trial_ring",
            "warehouse_loading_bay",
            "well",
            "workshop");

    private CatalogStationGeometry() {}

    public static Set<String> validators() {
        return VALIDATORS;
    }

    public static boolean supports(String validator) {
        return VALIDATORS.contains(normalize(validator));
    }

    public static Geometry compile(MultiblockStructureCatalog.StructureEntry entry) {
        if (entry == null || !supports(entry.pattern().validator())) {
            return new Geometry("", List.of(), List.of());
        }
        Builder builder = new Builder(entry.id());
        int radiusX = boundedRadius(entry.sizeW(), entry.pattern().radius(), 5);
        int radiusZ = boundedRadius(entry.sizeD(), entry.pattern().radius(), 7);
        int height = Math.max(1, Math.min(8, entry.sizeH()));
        String validator = normalize(entry.pattern().validator());
        String ring = ringBlock(entry.pattern().ringRole());

        switch (validator) {
            case "small_array_single" -> builder.core(entry, SPIRIT_ARRAY);
            case "small_array_3x3", "furnace_safety_array" -> {
                builder.core(entry, SPIRIT_ARRAY);
                builder.ring(Math.max(1, radiusX), SPIRIT_ARRAY);
            }
            case "time_acceleration_array", "trial_ring" -> {
                builder.core(entry, SPIRIT_ARRAY);
                builder.ring(Math.max(2, radiusX), SPIRIT_ARRAY);
                builder.cardinals(Math.max(1, radiusX - 1), SPIRIT_ORE);
            }
            case "small_array_post" -> {
                builder.core(entry, SPIRIT_ARRAY);
                builder.column(1, height - 1, SPIRIT_ARRAY);
            }
            case "seal_pillar" -> {
                builder.core(entry, SPIRIT_ORE);
                builder.column(1, height - 1, SPIRIT_ORE);
                builder.cardinals(1, ring);
            }
            case "small_control_post" -> {
                builder.core(entry, SPIRIT_ORE);
                builder.required(new BlockPos(0, 1, 0), CRAFTING_TABLE);
                builder.cardinals(1, ring);
            }
            case "brazier" -> {
                builder.core(entry, SPIRIT_ORE);
                builder.required(new BlockPos(0, 1, 0), CAMPFIRE);
                builder.cardinals(1, SPIRIT_ARRAY);
            }
            case "pedestal" -> {
                builder.core(entry, SPIRIT_ORE);
                builder.squareBase(Math.max(1, radiusX), ring);
                builder.required(new BlockPos(0, 1, 0), SPIRIT_ORE);
            }
            case "small_control_2x2", "small_control_trading" -> {
                builder.core(entry, CRAFTING_TABLE);
                builder.rectangleBase(radiusX, Math.max(1, Math.min(2, radiusZ)), SPIRIT_ORE);
                builder.airColumn(1, Math.max(1, height - 1));
            }
            case "sect_formation_hub" -> {
                builder.core(entry, SPIRIT_ARRAY);
                builder.ring(Math.max(2, radiusX), SPIRIT_ARRAY);
                builder.cornerPosts(Math.max(2, radiusX), Math.min(3, height), SPIRIT_ORE);
            }
            case "secret_realm_gate" -> builder.gate(entry, radiusX, height, ring);
            case "rift_large" -> {
                builder.core(entry, SPIRIT_ARRAY);
                builder.ring(Math.max(2, radiusX), SPIRIT_ORE);
                builder.cornerPosts(Math.max(2, radiusX), height, SPIRIT_ORE);
                builder.airColumn(1, height - 1);
            }
            case "rift_core_small" -> {
                builder.core(entry, SPIRIT_ORE);
                builder.ring(1, SPIRIT_ARRAY);
            }
            case "rift_world_seed" -> {
                builder.core(entry, SPIRIT_ORE);
                builder.cubeCorners(Math.max(1, radiusX), Math.max(2, height), SPIRIT_ARRAY);
            }
            case "blood_pool", "well" -> {
                builder.core(entry, SPIRIT_ARRAY);
                builder.ring(Math.max(1, radiusX), SPIRIT_ORE);
                builder.cornerPosts(Math.max(1, radiusX), Math.min(2, height), SPIRIT_ORE);
            }
            case "spirit_beast_pen" -> {
                builder.core(entry, SPIRIT_ARRAY);
                builder.fencePerimeter(Math.max(1, radiusX), Math.max(1, radiusZ), OAK_FENCE);
                builder.airInterior(Math.max(1, radiusX - 1), Math.max(1, radiusZ - 1), 1, 2);
            }
            case "greenhouse" -> {
                builder.core(entry, SPIRIT_ARRAY);
                builder.rectanglePerimeter(radiusX, radiusZ, SPIRIT_ORE, 0);
                builder.rectanglePerimeter(radiusX, radiusZ, GLASS, 1);
                builder.rectanglePerimeter(radiusX, radiusZ, GLASS, Math.max(2, height - 1));
                builder.airInterior(Math.max(1, radiusX - 1), Math.max(1, radiusZ - 1), 1, height - 1);
            }
            case "storage" -> {
                builder.core(entry, CHEST);
                builder.rectanglePerimeter(radiusX, radiusZ, SPIRIT_ORE, 0);
                builder.cornerPosts(radiusX, height, SPIRIT_ORE);
            }
            case "warehouse_loading_bay" -> {
                builder.core(entry, CHEST);
                builder.rectangleBase(radiusX, radiusZ, SPIRIT_ARRAY);
                builder.cornerPosts(radiusX, Math.min(2, height), SPIRIT_ORE);
            }
            case "workshop" -> {
                builder.core(entry, CRAFTING_TABLE);
                builder.rectanglePerimeter(radiusX, radiusZ, SPIRIT_ORE, 0);
                builder.cornerPosts(radiusX, Math.min(3, height), SPIRIT_ORE);
            }
            case "kunwu_frost_forge" -> {
                builder.core(entry, SPIRIT_ORE);
                builder.squareBase(Math.max(1, radiusX), SPIRIT_ORE);
                builder.cornerPosts(Math.max(1, radiusX), Math.min(2, height), PACKED_ICE);
            }
            case "immortal_alchemy_cauldron" -> {
                builder.core(entry, SPIRIT_ORE);
                builder.ring(Math.max(2, radiusX), SPIRIT_ORE);
                builder.cornerPosts(Math.max(2, radiusX), height, SPIRIT_ARRAY);
                builder.required(new BlockPos(0, -1, 0), MAGMA);
                builder.airColumn(1, height - 1);
            }
            case "infant_fire_alchemy_room" -> {
                builder.room(entry, radiusX, radiusZ, height, SPIRIT_ORE);
                builder.required(new BlockPos(0, -1, 0), MAGMA);
            }
            case "cultivation_chamber", "grand_hall" -> builder.room(
                    entry, radiusX, radiusZ, height, SPIRIT_ORE);
            case "tower" -> {
                builder.core(entry, SPIRIT_ORE);
                builder.rectanglePerimeter(radiusX, radiusZ, SPIRIT_ORE, 0);
                builder.cornerPosts(radiusX, height, SPIRIT_ORE);
                for (int y = 2; y < height; y += 2) {
                    builder.cardinalsAt(Math.max(1, radiusX), y, SPIRIT_ARRAY);
                }
                builder.airColumn(1, height - 1);
            }
            case "defense_wall" -> builder.wall(entry, radiusX, radiusZ, height);
            case "trap_corridor" -> builder.corridor(entry, radiusX, radiusZ, height);
            default -> throw new IllegalStateException("Unhandled catalog validator " + validator);
        }
        return builder.build();
    }

    public static Validation validate(LevelReader level, MultiblockStructureCatalog.StructureEntry entry,
                                      BlockPos origin) {
        if (level == null || entry == null || origin == null) {
            return new Validation(false, 0, 0, "missing_context");
        }
        Geometry geometry = compile(entry);
        int missing = 0;
        String firstMissing = "";
        ResourceLocation exactCoreId = new ResourceLocation(SeekingImmortalsMod.MODID, entry.id());
        boolean exactCoreExists = ForgeRegistries.BLOCKS.containsKey(exactCoreId);
        for (Cell cell : geometry.cells()) {
            BlockPos worldPos = origin.offset(cell.offset());
            BlockState state = level.getBlockState(worldPos);
            boolean matches;
            if (cell.airRequired()) {
                matches = state.isAir();
            } else {
                ResourceLocation actual = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                String actualId = actual == null ? "" : actual.toString();
                if (cell.offset().equals(BlockPos.ZERO) && exactCoreExists) {
                    matches = exactCoreId.toString().equals(actualId);
                } else {
                    matches = cell.acceptedBlockIds().contains(actualId);
                }
            }
            if (!matches) {
                missing++;
                if (firstMissing.isBlank()) {
                    firstMissing = cell.offset().toShortString();
                }
            }
        }
        return new Validation(missing == 0, missing, geometry.cells().size(),
                missing == 0 ? "catalog_geometry:" + entry.pattern().validator()
                        : "catalog_geometry_missing:" + missing + "@" + firstMissing);
    }

    public record Geometry(String stationId, List<Cell> cells, List<Integer> layers) {
        public Geometry {
            stationId = normalize(stationId);
            cells = List.copyOf(cells == null ? List.of() : cells);
            layers = List.copyOf(layers == null ? List.of() : layers);
        }
    }

    public record Cell(BlockPos offset, String displayBlockId, List<String> acceptedBlockIds,
                       boolean airRequired) {
        public Cell {
            offset = offset == null ? BlockPos.ZERO : offset.immutable();
            displayBlockId = displayBlockId == null ? AIR : displayBlockId;
            acceptedBlockIds = List.copyOf(acceptedBlockIds == null ? List.of() : acceptedBlockIds);
        }
    }

    public record Validation(boolean formed, int missingCells, int totalCells, String detail) {}

    private static int boundedRadius(int size, int authoredRadius, int max) {
        int fromSize = Math.max(1, Math.max(1, size) / 2);
        return Math.min(max, authoredRadius > 0 ? authoredRadius : fromSize);
    }

    private static String ringBlock(String role) {
        String normalized = normalize(role);
        return normalized.contains("array") || normalized.contains("gather") ? SPIRIT_ARRAY : SPIRIT_ORE;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Builder {
        private final String stationId;
        private final Map<BlockPos, Cell> cells = new LinkedHashMap<>();

        private Builder(String stationId) {
            this.stationId = normalize(stationId);
        }

        private void core(MultiblockStructureCatalog.StructureEntry entry, String fallback) {
            String exact = MOD_PREFIX + entry.id();
            requiredAny(BlockPos.ZERO, fallback, exact, fallback);
        }

        private void required(BlockPos pos, String blockId) {
            requiredAny(pos, blockId, blockId);
        }

        private void requiredAny(BlockPos pos, String display, String... accepted) {
            LinkedHashSet<String> ids = new LinkedHashSet<>();
            if (accepted != null) {
                for (String id : accepted) {
                    if (id != null && !id.isBlank()) ids.add(id);
                }
            }
            put(new Cell(pos, display, List.copyOf(ids), false));
        }

        private void air(BlockPos pos) {
            put(new Cell(pos, AIR, List.of(), true));
        }

        private void ring(int radius, String block) {
            rectanglePerimeter(radius, radius, block, 0);
        }

        private void cardinals(int radius, String block) {
            cardinalsAt(radius, 0, block);
        }

        private void cardinalsAt(int radius, int y, String block) {
            required(new BlockPos(radius, y, 0), block);
            required(new BlockPos(-radius, y, 0), block);
            required(new BlockPos(0, y, radius), block);
            required(new BlockPos(0, y, -radius), block);
        }

        private void column(int fromY, int toY, String block) {
            for (int y = Math.max(0, fromY); y <= Math.max(fromY, toY); y++) {
                required(new BlockPos(0, y, 0), block);
            }
        }

        private void squareBase(int radius, String block) {
            rectangleBase(radius, radius, block);
        }

        private void rectangleBase(int radiusX, int radiusZ, String block) {
            for (int x = -radiusX; x <= radiusX; x++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    if (x != 0 || z != 0) required(new BlockPos(x, 0, z), block);
                }
            }
        }

        private void rectanglePerimeter(int radiusX, int radiusZ, String block, int y) {
            for (int x = -radiusX; x <= radiusX; x++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    if (Math.abs(x) == radiusX || Math.abs(z) == radiusZ) {
                        required(new BlockPos(x, y, z), block);
                    }
                }
            }
        }

        private void cornerPosts(int radius, int height, String block) {
            int[] sides = {-radius, radius};
            for (int y = 1; y < Math.max(2, height); y++) {
                for (int x : sides) for (int z : sides) required(new BlockPos(x, y, z), block);
            }
        }

        private void cubeCorners(int radius, int height, String block) {
            int top = Math.max(1, height - 1);
            int[] sides = {-radius, radius};
            for (int x : sides) for (int z : sides) {
                required(new BlockPos(x, 0, z), block);
                required(new BlockPos(x, top, z), block);
            }
        }

        private void airColumn(int fromY, int toY) {
            for (int y = Math.max(1, fromY); y <= Math.max(fromY, toY); y++) air(new BlockPos(0, y, 0));
        }

        private void airInterior(int radiusX, int radiusZ, int fromY, int toY) {
            for (int y = Math.max(1, fromY); y <= Math.max(fromY, toY); y++) {
                for (int x = -radiusX; x <= radiusX; x++) {
                    for (int z = -radiusZ; z <= radiusZ; z++) air(new BlockPos(x, y, z));
                }
            }
        }

        private void fencePerimeter(int radiusX, int radiusZ, String block) {
            rectanglePerimeter(radiusX, radiusZ, block, 1);
        }

        private void gate(MultiblockStructureCatalog.StructureEntry entry, int radius, int height, String block) {
            core(entry, SPIRIT_ARRAY);
            for (int x = -radius; x <= radius; x++) required(new BlockPos(x, 0, 0), block);
            for (int y = 1; y < height; y++) {
                required(new BlockPos(-radius, y, 0), SPIRIT_ORE);
                required(new BlockPos(radius, y, 0), SPIRIT_ORE);
            }
            int top = Math.max(1, height - 1);
            for (int x = -radius; x <= radius; x++) required(new BlockPos(x, top, 0), SPIRIT_ORE);
            for (int y = 1; y < top; y++) for (int x = -radius + 1; x < radius; x++) air(new BlockPos(x, y, 0));
        }

        private void room(MultiblockStructureCatalog.StructureEntry entry, int radiusX, int radiusZ,
                          int height, String block) {
            core(entry, SPIRIT_ARRAY);
            rectanglePerimeter(radiusX, radiusZ, block, 0);
            cornerPosts(Math.max(radiusX, radiusZ), height, block);
            rectanglePerimeter(radiusX, radiusZ, block, Math.max(1, height - 1));
            airColumn(1, height - 2);
        }

        private void wall(MultiblockStructureCatalog.StructureEntry entry, int radiusX, int radiusZ, int height) {
            core(entry, SPIRIT_ORE);
            boolean alongX = radiusX >= radiusZ;
            int span = Math.max(radiusX, radiusZ);
            for (int y = 0; y < height; y++) {
                for (int p = -span; p <= span; p++) {
                    BlockPos pos = alongX ? new BlockPos(p, y, 0) : new BlockPos(0, y, p);
                    if (!pos.equals(BlockPos.ZERO)) required(pos, y % 2 == 0 ? SPIRIT_ORE : SPIRIT_ARRAY);
                }
            }
        }

        private void corridor(MultiblockStructureCatalog.StructureEntry entry, int radiusX, int radiusZ, int height) {
            core(entry, SPIRIT_ORE);
            boolean alongZ = radiusZ >= radiusX;
            int length = Math.max(radiusX, radiusZ);
            int halfWidth = Math.max(1, Math.min(2, Math.min(radiusX, radiusZ)));
            for (int p = -length; p <= length; p++) {
                BlockPos left = alongZ ? new BlockPos(-halfWidth, 0, p) : new BlockPos(p, 0, -halfWidth);
                BlockPos right = alongZ ? new BlockPos(halfWidth, 0, p) : new BlockPos(p, 0, halfWidth);
                required(left, SPIRIT_ORE);
                required(right, SPIRIT_ORE);
                if (p % 4 == 0) {
                    required(left.above(Math.max(1, height - 1)), SPIRIT_ARRAY);
                    required(right.above(Math.max(1, height - 1)), SPIRIT_ARRAY);
                }
            }
            airColumn(1, height - 1);
        }

        private void put(Cell cell) {
            Cell old = cells.get(cell.offset());
            if (old == null || old.equals(cell)) {
                cells.put(cell.offset(), cell);
                return;
            }
            if (old.airRequired() && !cell.airRequired()) {
                cells.put(cell.offset(), cell);
                return;
            }
            if (!old.airRequired() && cell.airRequired()) {
                return;
            }
            LinkedHashSet<String> accepted = new LinkedHashSet<>(old.acceptedBlockIds());
            accepted.addAll(cell.acceptedBlockIds());
            cells.put(cell.offset(), new Cell(cell.offset(), old.displayBlockId(), List.copyOf(accepted), false));
        }

        private Geometry build() {
            if (!cells.containsKey(BlockPos.ZERO)) {
                throw new IllegalStateException("Catalog geometry lacks origin for " + stationId);
            }
            TreeSet<Integer> layers = new TreeSet<>();
            cells.keySet().forEach(pos -> layers.add(pos.getY()));
            return new Geometry(stationId, new ArrayList<>(cells.values()), new ArrayList<>(layers));
        }
    }
}

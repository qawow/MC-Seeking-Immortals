package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Client-neutral construction geometry for placeable multiblock controllers.
 * Block ids are kept as strings so tests and dedicated servers never need a live client registry.
 */
public final class MultiblockProjectionCatalog {
    private static final String MOD_PREFIX = SeekingImmortalsMod.MODID + ":";
    private static final String SPIRIT_ORE = mod("spirit_ore");
    private static final String SPIRIT_ARRAY = mod("spirit_gathering_array");
    private static final Map<String, Projection> PROJECTIONS = buildProjections();

    private MultiblockProjectionCatalog() {}

    public static Optional<Projection> find(String controllerId) {
        if (controllerId == null || controllerId.isBlank()) {
            return Optional.empty();
        }
        String normalized = controllerId.indexOf(':') >= 0
                ? controllerId.trim()
                : mod(controllerId.trim());
        return Optional.ofNullable(PROJECTIONS.get(normalized));
    }

    public static Collection<Projection> all() {
        return PROJECTIONS.values();
    }

    public static Set<String> supportedControllerIds() {
        return PROJECTIONS.keySet();
    }

    private static Map<String, Projection> buildProjections() {
        Map<String, Projection> projections = new LinkedHashMap<>();

        add(projections, alchemyFurnace("alchemy_furnace", 1, "alchemy_lid_low"));
        add(projections, alchemyFurnace("alchemy_furnace_tier_2", 2, "alchemy_lid_mid"));
        add(projections, alchemyFurnace("alchemy_furnace_tier_3", 3, "alchemy_lid_high"));
        add(projections, alchemyFurnace("alchemy_furnace_tier_4", 4, "alchemy_lid_tier_4"));
        add(projections, alchemyFurnace("alchemy_furnace_tier_5", 5, "alchemy_lid_tier_5"));

        ProjectionBuilder earthFire = builder("sect_earth_fire_room");
        earthFire.required(BlockPos.ZERO, mod("sect_earth_fire_room"));
        earthFire.required(new BlockPos(1, 0, 0), SPIRIT_ARRAY);
        earthFire.required(new BlockPos(-1, 0, 0), SPIRIT_ARRAY);
        earthFire.required(new BlockPos(0, 0, 1), SPIRIT_ARRAY);
        earthFire.required(new BlockPos(0, 0, -1), SPIRIT_ARRAY);
        earthFire.required(new BlockPos(0, -1, 0), vanilla("magma_block"));
        add(projections, earthFire.build());

        ProjectionBuilder portalArray = builder("spirit_gathering_array");
        portalArray.required(BlockPos.ZERO, SPIRIT_ARRAY);
        portalArray.squareBase(6, SPIRIT_ARRAY);
        portalArray.cornerFrame(6, 10, SPIRIT_ORE);
        portalArray.airAperture(5, 9);
        add(projections, portalArray.build());

        add(projections, baseAndFrame("refinement_forge", 2));
        add(projections, baseAndFrame("refinement_forge_g2", 1));
        add(projections, ringFrameAperture("refinement_forge_g3", 2, 3, 1, 3));
        add(projections, ringFrameAperture("refinement_forge_g4", 3, 4, 2, 4));
        add(projections, ringFrameAperture("refinement_forge_g5", 4, 5, 3, 5));
        add(projections, ringFrameAperture("refinement_forge_g6", 5, 6, 4, 6));
        add(projections, baseAndFrame("talisman_table", 2));
        add(projections, baseAndFrame("puppet_assembly_bench", 2));

        ProjectionBuilder herbPlanter = builder("spirit_herb_planter");
        herbPlanter.required(BlockPos.ZERO, mod("spirit_herb_planter"));
        herbPlanter.ringAny(1, vanilla("dirt"),
                SPIRIT_ARRAY,
                vanilla("dirt"),
                vanilla("grass_block"),
                vanilla("farmland"),
                vanilla("moss_block"),
                vanilla("rooted_dirt"),
                vanilla("podzol"),
                vanilla("coarse_dirt"),
                vanilla("mycelium"),
                vanilla("mud"),
                vanilla("muddy_mangrove_roots"));
        add(projections, herbPlanter.build());

        add(projections, simpleRing("spirit_gathering_formation_core", 2, SPIRIT_ARRAY));
        add(projections, simpleRing("defense_formation_core", 2, SPIRIT_ORE));
        add(projections, simpleRing("seal_demon_formation_core", 2, SPIRIT_ORE));
        add(projections, arrayHub("kill_sword_formation_core", SPIRIT_ORE, SPIRIT_ARRAY));
        add(projections, arrayHub("illusion_maze_formation_core", SPIRIT_ARRAY, SPIRIT_ORE));
        add(projections, simpleRing("five_elements_mountain_formation_core", 2, SPIRIT_ORE));
        add(projections, simpleRing("nine_dragon_flame_barrier_formation_core", 4, SPIRIT_ORE));
        add(projections, simpleRing("inverted_five_elements_formation_core", 3, SPIRIT_ORE));
        add(projections, simpleRing("vajra_prison_formation_core", 4, SPIRIT_ORE));
        add(projections, simpleRing("mulan_wind_ride_formation_core", 3, SPIRIT_ORE));
        add(projections, simpleRing("barrier_sect_protection_formation_core", 2, SPIRIT_ORE));
        add(projections, simpleRing("spirit_gathering_minor_formation_core", 2, SPIRIT_ARRAY));
        add(projections, simpleRing("demon_seal_pillar_formation_core", 4, SPIRIT_ORE));
        add(projections, simpleRing("sword_array_bagua_formation_core", 4, SPIRIT_ORE));
        add(projections, simpleRing("thunder_tribulation_array_formation_core", 4, SPIRIT_ORE));

        ProjectionBuilder fixedTeleport = builder("teleport_array_pedestal");
        fixedTeleport.required(BlockPos.ZERO, mod("teleport_array_pedestal"));
        fixedTeleport.ring(2, mod("teleport_array_pedestal"));
        fixedTeleport.airAperture(1, 3);
        add(projections, fixedTeleport.build());
        add(projections, ringFrameAperture("long_range_teleport_array", 4, 5, 3, 5));

        add(projections, ringFrameAperture("sect_gate_array", 3, 4, 2, 4));
        add(projections, ringFrameAperture("ascension_gate", 3, 5, 2, 5));
        add(projections, ringFrameAperture("blood_forbidden_gate", 3, 4, 2, 4));
        add(projections, ringFrameAperture("ancient_rift_gate", 3, 4, 2, 4));
        add(projections, ringFrameAperture("cycle_gate", 4, 5, 3, 5));
        add(projections, ringFrameAperture("hidden_rift_gate", 1, 2, 0, 2));
        add(projections, ringFrameAperture("king_territory_gate", 2, 3, 1, 3));
        add(projections, ringFrameAperture("nether_ferry_gate", 2, 3, 1, 3));

        ProjectionBuilder bloodAltar = builder("blood_sacrifice_altar");
        bloodAltar.required(BlockPos.ZERO, mod("blood_sacrifice_altar"));
        bloodAltar.squareBase(1, mod("blood_sacrifice_altar"));
        bloodAltar.cornerFrame(1, 2, SPIRIT_ORE);
        bloodAltar.airColumn(2);
        add(projections, bloodAltar.build());

        ProjectionBuilder thunderAltar = builder("thunder_tribulation_altar");
        thunderAltar.required(BlockPos.ZERO, mod("thunder_tribulation_altar"));
        thunderAltar.ring(2, mod("thunder_tribulation_altar"));
        thunderAltar.cornerFrame(2, 3, SPIRIT_ORE);
        thunderAltar.airAperture(1, 3);
        add(projections, thunderAltar.build());

        addCatalogProjections(projections);
        return Collections.unmodifiableMap(projections);
    }

    private static void addCatalogProjections(Map<String, Projection> projections) {
        for (MultiblockStructureCatalog.StructureEntry entry
                : MultiblockStructureCatalog.builtin().structures().values()) {
            if (!CatalogStationGeometry.supports(entry.pattern().validator())) {
                continue;
            }
            CatalogStationGeometry.Geometry geometry = CatalogStationGeometry.compile(entry);
            List<Cell> cells = geometry.cells().stream()
                    .map(cell -> new Cell(
                            cell.offset(),
                            cell.airRequired() ? "" : cell.displayBlockId(),
                            cell.acceptedBlockIds(),
                            cell.airRequired()))
                    .toList();
            add(projections, new Projection(mod(geometry.stationId()), cells, geometry.layers()));
        }
    }

    private static Projection alchemyFurnace(String controllerPath, int tier, String lidPath) {
        ProjectionBuilder projection = builder(controllerPath);
        projection.required(BlockPos.ZERO, mod(controllerPath));
        projection.requiredAny(
                new BlockPos(0, 1, 0),
                mod(lidPath),
                mod("alchemy_lid_low"),
                mod("alchemy_lid_mid"),
                mod("alchemy_lid_high"),
                mod("alchemy_lid_tier_4"),
                mod("alchemy_lid_tier_5"));
        if (tier >= 3) {
            projection.ringAny(2, mod("alchemy_furnace_array_node"), SPIRIT_ARRAY);
        }
        if (tier >= 4) {
            projection.required(new BlockPos(0, -1, 0), vanilla("magma_block"));
        }
        return projection.build();
    }

    private static Projection baseAndFrame(String controllerPath, int frameHeight) {
        ProjectionBuilder projection = builder(controllerPath);
        String controller = mod(controllerPath);
        projection.required(BlockPos.ZERO, controller);
        projection.squareBase(1, controller);
        projection.cornerFrame(1, frameHeight, SPIRIT_ORE);
        return projection.build();
    }

    private static Projection simpleRing(String controllerPath, int radius, String ringBlock) {
        ProjectionBuilder projection = builder(controllerPath);
        projection.required(BlockPos.ZERO, mod(controllerPath));
        projection.ring(radius, ringBlock);
        return projection.build();
    }

    private static Projection arrayHub(String controllerPath, String cornerBlock, String edgeBlock) {
        ProjectionBuilder projection = builder(controllerPath);
        projection.required(BlockPos.ZERO, mod(controllerPath));
        int[] sides = {-1, 1};
        for (int x : sides) {
            for (int z : sides) {
                projection.required(new BlockPos(x, 0, z), cornerBlock);
            }
        }
        projection.required(new BlockPos(-1, 0, 0), edgeBlock);
        projection.required(new BlockPos(1, 0, 0), edgeBlock);
        projection.required(new BlockPos(0, 0, -1), edgeBlock);
        projection.required(new BlockPos(0, 0, 1), edgeBlock);
        return projection.build();
    }

    private static Projection ringFrameAperture(
            String controllerPath,
            int ringRadius,
            int frameHeight,
            int apertureRadius,
            int apertureHeight) {
        ProjectionBuilder projection = builder(controllerPath);
        projection.required(BlockPos.ZERO, mod(controllerPath));
        projection.ring(ringRadius, mod(controllerPath));
        projection.cornerFrame(ringRadius, frameHeight, SPIRIT_ORE);
        projection.airAperture(apertureRadius, apertureHeight);
        return projection.build();
    }

    private static ProjectionBuilder builder(String controllerPath) {
        return new ProjectionBuilder(mod(controllerPath));
    }

    private static void add(Map<String, Projection> projections, Projection projection) {
        Projection duplicate = projections.put(projection.controllerId(), projection);
        if (duplicate != null) {
            throw new IllegalStateException("Duplicate multiblock projection " + projection.controllerId());
        }
    }

    private static String mod(String path) {
        return MOD_PREFIX + path;
    }

    private static String vanilla(String path) {
        return "minecraft:" + path;
    }

    public record Projection(String controllerId, List<Cell> cells, List<Integer> layers) {
        public Projection {
            controllerId = Objects.requireNonNull(controllerId);
            cells = List.copyOf(cells);
            layers = List.copyOf(layers);
        }

        public String displayKey() {
            int separator = controllerId.indexOf(':');
            String namespace = separator >= 0 ? controllerId.substring(0, separator) : SeekingImmortalsMod.MODID;
            String path = separator >= 0 ? controllerId.substring(separator + 1) : controllerId;
            return "block." + namespace + "." + path;
        }

        public List<Cell> cellsForLayer(Integer relativeY) {
            if (relativeY == null) {
                return cells;
            }
            return cells.stream().filter(cell -> cell.offset().getY() == relativeY).toList();
        }
    }

    public record Cell(BlockPos offset, String displayBlockId, List<String> acceptedBlockIds, boolean airRequired) {
        public Cell {
            offset = Objects.requireNonNull(offset).immutable();
            displayBlockId = displayBlockId == null ? "" : displayBlockId;
            acceptedBlockIds = List.copyOf(acceptedBlockIds);
            if (airRequired && (!displayBlockId.isBlank() || !acceptedBlockIds.isEmpty())) {
                throw new IllegalArgumentException("Air projection cells cannot require block ids");
            }
            if (!airRequired && (displayBlockId.isBlank() || acceptedBlockIds.isEmpty())) {
                throw new IllegalArgumentException("Block projection cells need a display and accepted block ids");
            }
        }

        public boolean matches(String actualBlockId, boolean actualAir) {
            return airRequired ? actualAir : acceptedBlockIds.contains(actualBlockId);
        }
    }

    private static final class ProjectionBuilder {
        private final String controllerId;
        private final Map<BlockPos, Cell> cells = new LinkedHashMap<>();

        private ProjectionBuilder(String controllerId) {
            this.controllerId = controllerId;
        }

        private void required(BlockPos offset, String blockId) {
            requiredAny(offset, blockId, blockId);
        }

        private void requiredAny(BlockPos offset, String displayBlockId, String... acceptedBlockIds) {
            LinkedHashSet<String> accepted = new LinkedHashSet<>();
            if (displayBlockId != null && !displayBlockId.isBlank()) {
                accepted.add(displayBlockId);
            }
            for (String blockId : acceptedBlockIds) {
                if (blockId != null && !blockId.isBlank()) {
                    accepted.add(blockId);
                }
            }
            put(new Cell(offset, displayBlockId, List.copyOf(accepted), false));
        }

        private void air(BlockPos offset) {
            put(new Cell(offset, "", List.of(), true));
        }

        private void squareBase(int radius, String blockId) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x != 0 || z != 0) {
                        required(new BlockPos(x, 0, z), blockId);
                    }
                }
            }
        }

        private void ring(int radius, String blockId) {
            ringAny(radius, blockId, blockId);
        }

        private void ringAny(int radius, String displayBlockId, String... acceptedBlockIds) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if ((x != 0 || z != 0) && (Math.abs(x) == radius || Math.abs(z) == radius)) {
                        requiredAny(new BlockPos(x, 0, z), displayBlockId, acceptedBlockIds);
                    }
                }
            }
        }

        private void cornerFrame(int radius, int height, String blockId) {
            int[] corners = {-radius, radius};
            for (int y = 1; y <= height; y++) {
                for (int x : corners) {
                    for (int z : corners) {
                        required(new BlockPos(x, y, z), blockId);
                    }
                }
            }
        }

        private void airColumn(int height) {
            for (int y = 1; y <= height; y++) {
                air(new BlockPos(0, y, 0));
            }
        }

        private void airAperture(int radius, int height) {
            for (int y = 1; y <= height; y++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        air(new BlockPos(x, y, z));
                    }
                }
            }
        }

        private void put(Cell cell) {
            Cell duplicate = cells.putIfAbsent(cell.offset(), cell);
            if (duplicate != null && !duplicate.equals(cell)) {
                throw new IllegalStateException(
                        "Conflicting projection cells for " + controllerId + " at " + cell.offset());
            }
        }

        private Projection build() {
            if (!cells.containsKey(BlockPos.ZERO)) {
                throw new IllegalStateException("Projection lacks controller cell: " + controllerId);
            }
            TreeSet<Integer> layers = new TreeSet<>();
            cells.keySet().forEach(offset -> layers.add(offset.getY()));
            return new Projection(controllerId, new ArrayList<>(cells.values()), new ArrayList<>(layers));
        }
    }
}

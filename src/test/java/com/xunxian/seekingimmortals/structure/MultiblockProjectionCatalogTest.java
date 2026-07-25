package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import com.xunxian.seekingimmortals.block.PortalArrayStructure;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockProjectionCatalogTest {
    private static final String MOD_PREFIX = "seeking_immortals:";

    @Test
    void coversEveryPlaceableMultiblockController() {
        Set<String> expected = namespaced(Set.of(
                "alchemy_furnace",
                "alchemy_furnace_tier_2",
                "alchemy_furnace_tier_3",
                "alchemy_furnace_tier_4",
                "alchemy_furnace_tier_5",
                "sect_earth_fire_room",
                "spirit_gathering_array",
                "refinement_forge",
                "refinement_forge_g2",
                "refinement_forge_g3",
                "refinement_forge_g4",
                "refinement_forge_g5",
                "refinement_forge_g6",
                "talisman_table",
                "puppet_assembly_bench",
                "spirit_herb_planter",
                "spirit_gathering_formation_core",
                "defense_formation_core",
                "seal_demon_formation_core",
                "kill_sword_formation_core",
                "illusion_maze_formation_core",
                "five_elements_mountain_formation_core",
                "nine_dragon_flame_barrier_formation_core",
                "inverted_five_elements_formation_core",
                "vajra_prison_formation_core",
                "mulan_wind_ride_formation_core",
                "barrier_sect_protection_formation_core",
                "spirit_gathering_minor_formation_core",
                "demon_seal_pillar_formation_core",
                "sword_array_bagua_formation_core",
                "thunder_tribulation_array_formation_core",
                "teleport_array_pedestal",
                "long_range_teleport_array",
                "sect_gate_array",
                "ascension_gate",
                "blood_forbidden_gate",
                "ancient_rift_gate",
                "cycle_gate",
                "hidden_rift_gate",
                "king_territory_gate",
                "nether_ferry_gate",
                "blood_sacrifice_altar",
                "thunder_tribulation_altar"));
        expected.addAll(catalogProjectionIds());

        assertEquals(expected.size(), MultiblockProjectionCatalog.all().size());
        assertEquals(expected, MultiblockProjectionCatalog.supportedControllerIds());
        for (String id : expected) {
            assertTrue(MultiblockProjectionCatalog.find(id).isPresent(), id);
            assertTrue(MultiblockProjectionCatalog.find(id.substring(MOD_PREFIX.length())).isPresent(), id);
        }
        assertTrue(MultiblockProjectionCatalog.find(null).isEmpty());
        assertTrue(MultiblockProjectionCatalog.find(" ").isEmpty());
        assertTrue(MultiblockProjectionCatalog.find("minecraft:stone").isEmpty());
    }

    @Test
    void projectionsHaveUniqueCellsAndExactLayerIndexes() {
        for (MultiblockProjectionCatalog.Projection projection : MultiblockProjectionCatalog.all()) {
            Set<BlockPos> offsets = projection.cells().stream()
                    .map(MultiblockProjectionCatalog.Cell::offset)
                    .collect(Collectors.toSet());
            assertEquals(projection.cells().size(), offsets.size(), projection.controllerId());

            List<Integer> expectedLayers = projection.cells().stream()
                    .map(cell -> cell.offset().getY())
                    .distinct()
                    .sorted()
                    .toList();
            assertEquals(expectedLayers, projection.layers(), projection.controllerId());

            MultiblockProjectionCatalog.Cell controller = cellAt(projection, BlockPos.ZERO);
            assertFalse(controller.airRequired(), projection.controllerId());
            assertFalse(controller.displayBlockId().isBlank(), projection.controllerId());
            assertTrue(controller.acceptedBlockIds().contains(projection.controllerId()), projection.controllerId());
        }
    }

    @Test
    void catalogStationsReuseTheExactRuntimeGeometryForProjection() {
        for (MultiblockStructureCatalog.StructureEntry entry
                : MultiblockStructureCatalog.builtin().structures().values()) {
            if (!CatalogStationGeometry.supports(entry.pattern().validator())) {
                continue;
            }
            CatalogStationGeometry.Geometry geometry = CatalogStationGeometry.compile(entry);
            MultiblockProjectionCatalog.Projection projection = projection(entry.id());
            assertEquals(geometry.layers(), projection.layers(), entry.id());
            assertEquals(geometry.cells().size(), projection.cells().size(), entry.id());
            for (CatalogStationGeometry.Cell expected : geometry.cells()) {
                MultiblockProjectionCatalog.Cell actual = cellAt(projection, expected.offset());
                assertEquals(expected.airRequired(), actual.airRequired(), entry.id());
                assertEquals(expected.airRequired() ? "" : expected.displayBlockId(),
                        actual.displayBlockId(), entry.id());
                assertEquals(expected.acceptedBlockIds(), actual.acceptedBlockIds(), entry.id());
            }
        }
    }

    @Test
    void clientPreviewAcceptsCatalogStationCarriersAsReachableControllers() throws Exception {
        String renderer = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/client/MultiblockProjectionRenderer.java"));
        assertTrue(renderer.contains("stack.getItem() instanceof CatalogCarrierItem carrier"));
        assertTrue(renderer.contains("controllerId = carrier.catalogId();"));
        assertTrue(renderer.contains("MultiblockProjectionCatalog.find(controllerId)"));
    }

    @Test
    void alchemyFurnacesMirrorRuntimeShellGeometry() {
        for (int tier = 1; tier <= 5; tier++) {
            String path = tier == 1 ? "alchemy_furnace" : "alchemy_furnace_tier_" + tier;
            MultiblockProjectionCatalog.Projection projection = projection(path);
            Set<BlockPos> expected = new HashSet<>(AlchemyFurnaceShellStructure.requiredOffsets(tier));
            expected.add(BlockPos.ZERO);

            assertEquals(expected, offsets(projection, false), path);
            assertTrue(offsets(projection, true).isEmpty(), path);

            MultiblockProjectionCatalog.Cell lid = cellAt(projection, AlchemyFurnaceShellStructure.LID_OFFSET);
            assertEquals(5, lid.acceptedBlockIds().size(), path);
            if (tier >= 3) {
                assertEquals(Set.of(
                                MOD_PREFIX + "alchemy_furnace_array_node",
                                MOD_PREFIX + "spirit_gathering_array"),
                        new HashSet<>(cellAt(projection, new BlockPos(2, 0, 0)).acceptedBlockIds()), path);
            }
            if (tier >= 4) {
                assertEquals(List.of("minecraft:magma_block"),
                        cellAt(projection, new BlockPos(0, -1, 0)).acceptedBlockIds(), path);
            }
        }
    }

    @Test
    void frameAndApertureStructuresMatchRuntimeOffsets() {
        assertGeometry("refinement_forge_g3",
                merge(Set.of(BlockPos.ZERO), RefinementForgeG3Structure.ringOffsets(),
                        RefinementForgeG3Structure.frameOffsets()),
                new HashSet<>(RefinementForgeG3Structure.apertureOffsets()));
        assertGeometry("refinement_forge_g6",
                merge(Set.of(BlockPos.ZERO), RefinementForgeHighStructure.ringOffsets(5),
                        RefinementForgeHighStructure.frameOffsets(5, 6)),
                new HashSet<>(RefinementForgeHighStructure.apertureOffsets(4, 6)));
        assertGeometry("teleport_array_pedestal",
                merge(Set.of(BlockPos.ZERO), FixedTeleportArrayStructure.ringOffsets()),
                new HashSet<>(FixedTeleportArrayStructure.apertureOffsets()));
        assertGeometry("sect_gate_array",
                merge(Set.of(BlockPos.ZERO), SectGateStructure.ringOffsets(), SectGateStructure.frameOffsets()),
                new HashSet<>(SectGateStructure.apertureOffsets()));
        assertGeometry("blood_sacrifice_altar",
                merge(Set.of(BlockPos.ZERO), BloodSacrificeAltarStructure.baseOffsets(),
                        BloodSacrificeAltarStructure.pillarOffsets()),
                new HashSet<>(BloodSacrificeAltarStructure.apertureOffsets()));
        assertGeometry("thunder_tribulation_altar",
                merge(Set.of(BlockPos.ZERO), ThunderTribulationAltarStructure.ringOffsets(),
                        ThunderTribulationAltarStructure.pillarOffsets()),
                new HashSet<>(ThunderTribulationAltarStructure.apertureOffsets()));
    }

    @Test
    void spiritGatheringArrayProjectsItsPortalValidator() {
        MultiblockProjectionCatalog.Projection projection = projection("spirit_gathering_array");
        Set<BlockPos> required = new HashSet<>();
        for (int x = -PortalArrayStructure.BASE_RADIUS; x <= PortalArrayStructure.BASE_RADIUS; x++) {
            for (int z = -PortalArrayStructure.BASE_RADIUS; z <= PortalArrayStructure.BASE_RADIUS; z++) {
                required.add(new BlockPos(x, 0, z));
            }
        }
        int[] corners = {-PortalArrayStructure.BASE_RADIUS, PortalArrayStructure.BASE_RADIUS};
        for (int y = 1; y <= PortalArrayStructure.FRAME_HEIGHT; y++) {
            for (int x : corners) {
                for (int z : corners) {
                    required.add(new BlockPos(x, y, z));
                }
            }
        }
        Set<BlockPos> air = new HashSet<>();
        for (int y = 1; y <= PortalArrayStructure.APERTURE_HEIGHT; y++) {
            for (int x = -PortalArrayStructure.APERTURE_RADIUS;
                 x <= PortalArrayStructure.APERTURE_RADIUS; x++) {
                for (int z = -PortalArrayStructure.APERTURE_RADIUS;
                     z <= PortalArrayStructure.APERTURE_RADIUS; z++) {
                    air.add(new BlockPos(x, y, z));
                }
            }
        }

        assertEquals(required, offsets(projection, false));
        assertEquals(air, offsets(projection, true));
        assertEquals(209, required.size());
    }

    @Test
    void herbPlanterProjectsEveryVanillaSoilAcceptedByItsValidator() {
        MultiblockProjectionCatalog.Projection projection = projection("spirit_herb_planter");
        Set<BlockPos> expected = new HashSet<>(SpiritHerbPlanterStructure.ringOffsets());
        expected.add(BlockPos.ZERO);
        assertEquals(expected, offsets(projection, false));
        assertTrue(offsets(projection, true).isEmpty());

        Set<String> acceptedSoils = Set.of(
                MOD_PREFIX + "spirit_gathering_array",
                "minecraft:dirt",
                "minecraft:grass_block",
                "minecraft:farmland",
                "minecraft:moss_block",
                "minecraft:rooted_dirt",
                "minecraft:podzol",
                "minecraft:coarse_dirt",
                "minecraft:mycelium",
                "minecraft:mud",
                "minecraft:muddy_mangrove_roots");
        for (BlockPos offset : SpiritHerbPlanterStructure.ringOffsets()) {
            assertEquals(acceptedSoils, new HashSet<>(cellAt(projection, offset).acceptedBlockIds()));
        }
    }

    private static MultiblockProjectionCatalog.Projection projection(String path) {
        return MultiblockProjectionCatalog.find(path).orElseThrow();
    }

    private static MultiblockProjectionCatalog.Cell cellAt(
            MultiblockProjectionCatalog.Projection projection, BlockPos offset) {
        return projection.cells().stream()
                .filter(cell -> cell.offset().equals(offset))
                .findFirst()
                .orElseThrow(() -> new AssertionError(projection.controllerId() + " lacks " + offset));
    }

    private static Set<BlockPos> offsets(MultiblockProjectionCatalog.Projection projection, boolean airRequired) {
        return projection.cells().stream()
                .filter(cell -> cell.airRequired() == airRequired)
                .map(MultiblockProjectionCatalog.Cell::offset)
                .collect(Collectors.toSet());
    }

    @SafeVarargs
    private static Set<BlockPos> merge(Set<BlockPos> initial, List<BlockPos>... additions) {
        Set<BlockPos> merged = new HashSet<>(initial);
        for (List<BlockPos> addition : additions) {
            merged.addAll(addition);
        }
        return merged;
    }

    private static void assertGeometry(String path, Set<BlockPos> required, Set<BlockPos> air) {
        MultiblockProjectionCatalog.Projection projection = projection(path);
        assertEquals(required, offsets(projection, false), path + " required blocks");
        assertEquals(air, offsets(projection, true), path + " air aperture");
    }

    private static Set<String> namespaced(Set<String> paths) {
        return paths.stream()
                .map(path -> MOD_PREFIX + path)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> catalogProjectionIds() {
        return MultiblockStructureCatalog.builtin().structures().values().stream()
                .filter(entry -> CatalogStationGeometry.supports(entry.pattern().validator()))
                .map(entry -> MOD_PREFIX + entry.id())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

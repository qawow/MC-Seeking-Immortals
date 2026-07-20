package com.xunxian.seekingimmortals.structure;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M07 acceptance: 86-structure index parse + pattern dimensions + public API surface.
 */
class M07FormationsMultiblockTest {

    @Test
    void structureIndexLoadsEightySixEntries() {
        assertEquals(87, MultiblockStructureCatalog.builtin().size());
        assertEquals(87, FormationApi.structureIndexSize());
        assertTrue(MultiblockStructureCatalog.builtin().find("immortal_teleport_grand_array").isPresent());
        assertTrue(MultiblockStructureCatalog.builtin().find("sect_formation_hub").isPresent());
        assertTrue(MultiblockStructureCatalog.builtin().find("flying_boat_dock").isPresent());
        assertTrue(MultiblockStructureCatalog.builtin().find("capture_point_obelisk").isPresent());
        assertTrue(MultiblockStructureCatalog.builtin().find("war_banner_pole").isPresent());
    }

    @Test
    void patternDimensionsAlignWithIndex() {
        MultiblockStructureCatalog.StructureEntry teleport =
                MultiblockStructureCatalog.builtin().find("immortal_teleport_grand_array").orElseThrow();
        assertEquals(9, teleport.sizeW());
        assertEquals(2, teleport.sizeH());
        assertEquals(9, teleport.sizeD());
        assertEquals("long_range_teleport_array", teleport.pattern().validator());

        MultiblockStructureCatalog.StructureEntry hub =
                MultiblockStructureCatalog.builtin().find("sect_formation_hub").orElseThrow();
        assertEquals(5, hub.sizeW());
        assertEquals(3, hub.sizeH());
        assertEquals(5, hub.sizeD());
        assertEquals("ring", hub.pattern().validator());

        MultiblockStructureCatalog.StructureEntry dock =
                MultiblockStructureCatalog.builtin().find("flying_boat_dock").orElseThrow();
        assertEquals(3, dock.sizeW());
        assertEquals(2, dock.sizeH());
        assertEquals(5, dock.sizeD());

        MultiblockStructureCatalog.StructureEntry furnace =
                MultiblockStructureCatalog.builtin().find("alchemy_furnace_g1").orElseThrow();
        assertEquals("alchemy_furnace_shell", furnace.pattern().validator());
        assertEquals(1, furnace.pattern().tier());
    }

    @Test
    void dataDrivenRingPatternOffsetsMatchRadius() {
        MultiblockStructureCatalog.StructureEntry hub =
                MultiblockStructureCatalog.builtin().find("sect_formation_hub").orElseThrow();
        // Without live registry suppliers, only assert catalog-built radius offsets via RingFormationStructure.
        int radius = hub.pattern().radius() > 0 ? hub.pattern().radius() : hub.radius();
        assertEquals(2, radius);
        assertEquals(16, RingFormationStructure.ringOffsets(radius).size());
        assertFalse(RingFormationStructure.ringOffsets(radius).contains(net.minecraft.core.BlockPos.ZERO));
    }

    @Test
    void formationFieldCatalogExpandsBeyondBuiltinKinds() {
        assertTrue(FormationFieldCatalog.builtin().size() >= 20);
        assertTrue(FormationApi.formationFieldCatalogSize() >= 20);
        Optional<FormationFieldCatalog.FieldParams> spirit = FormationFieldCatalog.builtin().find("spirit_gather");
        assertTrue(spirit.isPresent());
        assertEquals(FormationFieldService.FieldKind.SPIRIT_GATHER, spirit.get().kind());
        assertTrue(spirit.get().radius() >= 1);
        assertTrue(spirit.get().auraBonus() > 0);

        Optional<FormationFieldCatalog.FieldParams> array = FormationFieldCatalog.builtin().find("juling_zhen");
        assertTrue(array.isPresent());
        assertEquals(FormationFieldService.FieldKind.SPIRIT_GATHER, array.get().kind());
    }

    @Test
    void formationItemsAndMpSequencesLoad() {
        assertEquals(14, FormationItemService.builtin().size());
        assertEquals(14, FormationApi.formationItemBehaviorSize());
        assertTrue(FormationItemService.builtin().find("portable_spirit_gather_disk").isPresent());
        assertEquals("spirit_gathering_array",
                FormationItemService.builtin().find("portable_spirit_gather_disk").orElseThrow().placeBlock());
        assertEquals("inspect_only",
                FormationItemService.builtin().find("array_blueprint_scroll").orElseThrow().action());
        assertEquals(10,
                FormationItemService.builtin().find("portable_spirit_gather_disk").orElseThrow().uses());

        assertTrue(MultiblockSequenceDisplayCatalog.builtin().size() >= 7);
        assertTrue(FormationApi.mpSequenceDisplaySize() >= 7);
        assertTrue(MultiblockSequenceDisplayCatalog.builtin().find("seq_teleport_interrupt").isPresent());
        assertFalse(MultiblockSequenceDisplayCatalog.builtin().forStructure("immortal_teleport_grand_array").isEmpty());
    }

    @Test
    void unknownStationIsNotFormed() {
        MultiblockStationService.clearCache();
        MultiblockStationService.StationCheckResult result =
                MultiblockStationService.check(null, "not_a_real_station", net.minecraft.core.BlockPos.ZERO);
        assertFalse(result.formed());
        assertEquals("unknown_station", result.detail());
    }

    @Test
    void fieldServiceQueryApiExistsAndStartsEmpty() {
        FormationFieldService.clearAll();
        assertEquals(0, FormationFieldService.activeCount());
        assertTrue(FormationFieldService.getActiveFieldEffects((net.minecraft.world.level.Level) null,
                net.minecraft.core.BlockPos.ZERO).isEmpty());
    }

    @Test
    void sizeParserHandlesChineseDimensions() {
        int[] size = MultiblockStructureCatalog.parseSize("9×9×2");
        assertEquals(9, size[0]);
        assertEquals(2, size[1]);
        assertEquals(9, size[2]);
        int[] size2 = MultiblockStructureCatalog.parseSize("1×1×2（宽×深×高）");
        assertEquals(1, size2[0]);
        assertEquals(2, size2[1]);
        assertEquals(1, size2[2]);
    }

    @Test
    void savedDataRecordKeepsNewFormationFields() {
        FormationFieldSavedData.StoredField field = new FormationFieldSavedData.StoredField(
                "minecraft:overworld",
                net.minecraft.core.BlockPos.ZERO,
                "SPIRIT_GATHER",
                100,
                "spirit_gather",
                3,
                50,
                "cultivation_speed_1.2",
                false);
        assertEquals("spirit_gather", field.formationId());
        assertEquals(3, field.radius());
        assertEquals(50, field.auraBonus());
        assertFalse(field.freeField());
    }
}

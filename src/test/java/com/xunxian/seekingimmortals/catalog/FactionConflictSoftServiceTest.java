package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionConflictSoftServiceTest {
    @Test
    void loadsConflictsFromFactionCatalog() {
        assertTrue(FactionConflictSoftService.count() >= 21);
        assertFalse(FactionConflictSoftService.sample(5).isEmpty());
    }

    @Test
    void sampleShowsMappingColumn() {
        boolean anyMapped = FactionConflictSoftService.sample(21).stream()
                .anyMatch(line -> line.contains("->") && !line.contains("-> -"));
        assertTrue(anyMapped);
    }

    @Test
    void mapsCoreConflictsToRealChains() {
        assertEquals("mulan_tianlan_war",
                FactionConflictSoftService.mappedChainId("mulan_tianlan_campaign").orElse(""));
        assertEquals("star_palace_internal_politics",
                FactionConflictSoftService.mappedChainId("star_palace_inverse_war").orElse(""));
        assertTrue(TextQuestChainService.find(
                FactionConflictSoftService.mappedChainId("huangfeng_yanyue_rivalry").orElseThrow()).isPresent());
    }

    @Test
    void unknownConflictStaysUnmapped() {
        assertTrue(FactionConflictSoftService.mappedChainId("totally_unknown_conflict_xyz").isEmpty());
    }

    @Test
    void mappedSidesAreBranchCompatible() {
        Optional<FactionConflictSoftService.Sides> sides =
                FactionConflictSoftService.mappedSides("mulan_tianlan_campaign");
        assertTrue(sides.isPresent());
        assertFalse(sides.get().sideA().isBlank());
        assertFalse(sides.get().sideB().isBlank());
        assertTrue(sides.get().branchA().matches("righteous|neutral|demonic"));
        assertTrue(sides.get().branchB().matches("righteous|neutral|demonic"));
    }

    @Test
    void wave479AllCatalogConflictsMapToChainsAndSides() {
        int mapped = 0;
        for (String line : FactionConflictSoftService.sample(50)) {
            String id = line.split("\\|")[0].trim();
            assertTrue(FactionConflictSoftService.mappedChainId(id).isPresent(), "missing chain for " + id);
            assertTrue(FactionConflictSoftService.mappedSides(id).isPresent(), "missing sides for " + id);
            mapped++;
        }
        assertTrue(mapped >= 21);
    }
}

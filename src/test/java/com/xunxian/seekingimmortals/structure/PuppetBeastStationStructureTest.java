package com.xunxian.seekingimmortals.structure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuppetBeastStationStructureTest {

    @Test
    void puppetCoreForgeOffsetsExcludeCenter() {
        assertEquals(8, PuppetCoreForgeStructure.ringOffsets().size());
        assertFalse(PuppetCoreForgeStructure.ringOffsets().contains(net.minecraft.core.BlockPos.ZERO));
        assertTrue(new PuppetCoreForgeStructure.CheckResult(0, 0).complete());
        assertFalse(new PuppetCoreForgeStructure.CheckResult(1, 0).complete());
        assertFalse(new PuppetCoreForgeStructure.CheckResult(0, 1).complete());
    }

    @Test
    void spiritBeastPoolHasRingPostsAndDeck() {
        assertEquals(16, SpiritBeastEvolutionPoolStructure.ringOffsets().size());
        assertEquals(4, SpiritBeastEvolutionPoolStructure.postOffsets().size());
        assertEquals(9, SpiritBeastEvolutionPoolStructure.deckAirOffsets().size());
        assertTrue(new SpiritBeastEvolutionPoolStructure.CheckResult(0, 0, 0).complete());
        assertFalse(new SpiritBeastEvolutionPoolStructure.CheckResult(1, 0, 0).complete());
    }

    @Test
    void catalogPatternsUseSpecializedValidators() {
        assertEquals("puppet_core_forge",
                MultiblockStructureCatalog.builtin().find("puppet_core_forge").orElseThrow().pattern().validator());
        assertEquals("spirit_beast_evolution_pool",
                MultiblockStructureCatalog.builtin().find("spirit_beast_evolution_pool").orElseThrow().pattern().validator());
    }
}

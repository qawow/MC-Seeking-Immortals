package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.SpiritualRootAttribute;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritStoneItemTest {
    @Test
    void passiveBonusAttributeRulesMatchRuntimeAndSyncBehavior() {
        assertTrue(SpiritStonePassiveBonus.matchesAttribute(SpiritualRootAttribute.METAL, SpiritualRootAttribute.METAL));
        assertFalse(SpiritStonePassiveBonus.matchesAttribute(SpiritualRootAttribute.METAL, SpiritualRootAttribute.WATER));
        assertFalse(SpiritStonePassiveBonus.matchesAttribute(null, SpiritualRootAttribute.METAL));

        assertTrue(SpiritStonePassiveBonus.matchesAttribute(SpiritualRootAttribute.METAL, SpiritualRootAttribute.THUNDER));
        assertTrue(SpiritStonePassiveBonus.matchesAttribute(null, SpiritualRootAttribute.THUNDER));
        assertTrue(SpiritStonePassiveBonus.matchesAttribute(SpiritualRootAttribute.EARTH, SpiritualRootAttribute.IMMORTAL));
    }
}

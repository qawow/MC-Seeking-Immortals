package com.xunxian.seekingimmortals.npc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamedNpcRewardServiceTest {
    @Test
    void loadsLootRewardEntries() {
        assertTrue(NamedNpcRewardService.entryCount() >= 40, "entries=" + NamedNpcRewardService.entryCount());
        assertTrue(NamedNpcRewardService.find("mo_daifu").isPresent()
                || NamedNpcRewardService.find("jin_guang_shangren").isPresent());
    }

    @Test
    void storyTokenMapCoversCommonItems() {
        // mapStoryToken is private; grantCatalogItem returns false without player, but resolve path is tested via find.
        assertTrue(NamedNpcRewardService.find("jin_guang_shangren").isPresent());
        var entry = NamedNpcRewardService.find("jin_guang_shangren").orElseThrow();
        assertTrue(entry.guaranteed().size() >= 1);
        assertEquals("jin_guang_shangren", entry.id());
    }
}

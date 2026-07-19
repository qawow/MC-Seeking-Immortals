package com.xunxian.seekingimmortals.structure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationItemServiceTest {
    @Test
    void spiritSandExtendsOnlySuccessfulFieldActivationDuration() {
        assertEquals(20 * 90, FormationItemService.fieldDurationTicks(false));
        assertEquals(20 * 150, FormationItemService.fieldDurationTicks(true));
    }

    @Test
    void shippedSpiritGatheringDiskIsSingleUseFieldActivator() {
        FormationItemService.ItemBehavior behavior = FormationItemService.builtin()
                .find("spirit_gathering_array_disk").orElseThrow();
        assertEquals("spirit_gather", behavior.formationId());
        assertEquals(1, behavior.uses());
        assertEquals("activate_free_field", behavior.action());
        assertTrue(FormationItemService.builtin().size() >= 14);
    }
}

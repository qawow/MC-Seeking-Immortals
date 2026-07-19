package com.xunxian.seekingimmortals.structure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormationItemServiceTest {
    @Test
    void spiritSandExtendsOnlySuccessfulFieldActivationDuration() {
        assertEquals(20 * 90, FormationItemService.fieldDurationTicks(false));
        assertEquals(20 * 150, FormationItemService.fieldDurationTicks(true));
    }
}

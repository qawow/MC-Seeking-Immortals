package com.xunxian.seekingimmortals.sect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionConflictEventServiceTest {
    @Test
    void loadsConflictEventsFromCorpus() {
        FactionConflictEventService.Snapshot snapshot = FactionConflictEventService.builtin();
        assertTrue(snapshot.count() >= 15, "expected faction_conflict_events volume, got " + snapshot.count());
        assertTrue(FactionConflictEventService.find("huangfeng_yanyue_rivalry").isPresent()
                || snapshot.events().values().stream().anyMatch(e -> e.id().contains("huangfeng")));
    }

    @Test
    void regionQueryReturnsEvents() {
        assertFalse(FactionConflictEventService.eventsForRegion("tiannan").isEmpty()
                || FactionConflictEventService.eventsForRegion("chaotic_sea").isEmpty()
                || FactionConflictEventService.builtin().count() == 0);
        assertTrue(FactionConflictEventService.eventsForRegion("tiannan").size()
                + FactionConflictEventService.eventsForRegion("mulan").size()
                + FactionConflictEventService.eventsForRegion("").size()
                >= 1);
    }
}

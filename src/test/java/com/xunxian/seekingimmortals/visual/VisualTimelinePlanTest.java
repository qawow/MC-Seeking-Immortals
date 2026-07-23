package com.xunxian.seekingimmortals.visual;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualTimelinePlanTest {

    @Test
    void persistentFormationSelectsOnlyTheNamedStateAndLoopsFromZero() {
        VisualProfile profile = AuthoredVisualCatalog.resolve("formation:spirit_gather").orElseThrow();

        VisualTimelinePlan.Plan plan = VisualTimelinePlan.select(profile, "ACTIVE", true);

        assertTrue(plan.looping());
        assertEquals(4, plan.durationTicks());
        assertEquals(1, plan.entries().size());
        VisualTimelinePlan.Entry active = plan.entries().get(0);
        assertEquals(0, active.startTick());
        assertEquals(4, active.durationTicks());
        assertEquals(2, active.event().ordinal());
        assertEquals("active", active.event().state());
        assertEquals(VisualAction.EMITTER, active.event().action());
        assertEquals(List.of(active), plan.activeAt(0));
        assertEquals(List.of(active), plan.activeAt(4));
        assertFalse(plan.expired(400));
    }

    @Test
    void statusApplyUsesTheAppliedStateAlias() {
        VisualProfile profile = AuthoredVisualCatalog.resolve("status:burn").orElseThrow();

        VisualTimelinePlan.Plan plan = VisualTimelinePlan.select(profile, "APPLY", false);

        assertEquals(1, plan.entries().size());
        VisualTimelinePlan.Entry applied = plan.entries().get(0);
        assertEquals(0, applied.startTick());
        assertEquals("applied", applied.event().state());
        assertEquals(VisualAction.AURA, applied.event().action());
    }

    @Test
    void bossPhasesAndTribulationWaveSelectDistinctAuthoredStates() {
        VisualProfile boss = AuthoredVisualCatalog.resolve("boss:abyss_jiao").orElseThrow();

        VisualTimelinePlan.Entry p1 = onlyEntry(VisualTimelinePlan.select(boss, "P1", true));
        VisualTimelinePlan.Entry p2 = onlyEntry(VisualTimelinePlan.select(boss, "P2", true));
        VisualTimelinePlan.Entry p3 = onlyEntry(VisualTimelinePlan.select(boss, "P3", true));

        assertEquals(List.of("p1", "p2", "p3"), List.of(
                p1.event().state(), p2.event().state(), p3.event().state()));
        assertEquals(List.of(0, 1, 2), List.of(
                p1.event().ordinal(), p2.event().ordinal(), p3.event().ordinal()));
        assertEquals(List.of(0.9D, 1.15D, 1.4D), List.of(
                p1.event().radius(), p2.event().radius(), p3.event().radius()));

        VisualProfile tribulation = AuthoredVisualCatalog.resolve(
                "tribulation:final_ascension").orElseThrow();
        VisualTimelinePlan.Entry wave = onlyEntry(
                VisualTimelinePlan.select(tribulation, "WAVE", true));
        assertEquals("wave", wave.event().state());
        assertEquals(1, wave.event().ordinal());
        assertEquals(VisualAction.EMITTER, wave.event().action());
    }

    @Test
    void statelessCastKeepsTheCompleteAuthoredStoryboard() {
        VisualProfile profile = AuthoredVisualCatalog.resolve("technique:fireball").orElseThrow();

        VisualTimelinePlan.Plan plan = VisualTimelinePlan.select(profile, "CAST", false);

        assertFalse(plan.looping());
        assertEquals(27, plan.durationTicks());
        assertEquals(List.of(0, 5, 13, 21, 23), plan.entries().stream()
                .map(VisualTimelinePlan.Entry::startTick).toList());
        assertEquals(List.of(0, 1, 2, 3, 4), plan.entries().stream()
                .map(entry -> entry.event().ordinal()).toList());
        assertFalse(plan.expired(26));
        assertTrue(plan.expired(27));
        assertTrue(plan.activeAt(27).isEmpty());
    }

    @Test
    void packetTriggerSelectsAndRebasesOneStoryboardWindow() {
        VisualProfile profile = AuthoredVisualCatalog.resolve("technique:fireball").orElseThrow();

        VisualTimelinePlan.Plan plan = VisualTimelinePlan.select(profile, "IMPACT", false);

        assertEquals(2, plan.durationTicks());
        assertEquals(1, plan.entries().size());
        VisualTimelinePlan.Entry impact = plan.entries().get(0);
        assertEquals(0, impact.startTick());
        assertEquals(3, impact.event().ordinal());
        assertEquals(21, impact.event().startTick());
        assertEquals(VisualAction.FLASH, impact.event().action());
        assertEquals(List.of(impact), plan.activeAt(1));
        assertTrue(plan.activeAt(2).isEmpty());
        assertTrue(plan.expired(2));
    }

    @Test
    void planRejectsDurationsThatCannotContainItsEntries() {
        VisualProfile profile = AuthoredVisualCatalog.resolve("status:burn").orElseThrow();
        VisualTimelineEvent event = profile.timeline().get(0);
        VisualTimelinePlan.Entry entry = new VisualTimelinePlan.Entry(0, 4, event);

        assertThrows(IllegalArgumentException.class,
                () -> new VisualTimelinePlan.Plan(List.of(entry), 0, true));
        assertThrows(IllegalArgumentException.class,
                () -> new VisualTimelinePlan.Plan(List.of(entry), 3, false));
    }

    private static VisualTimelinePlan.Entry onlyEntry(VisualTimelinePlan.Plan plan) {
        assertEquals(1, plan.entries().size());
        return plan.entries().get(0);
    }
}

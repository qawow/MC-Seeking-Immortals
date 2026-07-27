package com.xunxian.seekingimmortals.client;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VfxBudgetPlanTest {
    @Test
    void namedCopiesAreStratifiedAndAllAppearAcrossPhases() {
        Set<Integer> seen = new HashSet<>();
        for (int phase = 0; phase < 18; phase++) {
            List<Integer> sampled = VfxBudgetPlan.sampledCopies(72, 8, 2, phase);
            assertEquals(4, sampled.size());
            assertEquals(4, new HashSet<>(sampled).size());
            seen.addAll(sampled);
        }
        assertEquals(72, seen.size());
        assertEquals(List.of(0, 18, 36, 54),
                VfxBudgetPlan.sampledCopies(72, 8, 2, 0));
    }

    @Test
    void coreComponentsKeepTwoSamplesBeforeDetailsRotate() {
        Set<Integer> seenDetails = new HashSet<>();
        for (int phase = 0; phase < 5; phase++) {
            List<VfxBudgetPlan.Allocation> plan = VfxBudgetPlan.components(8, 2, 5, phase);
            assertEquals(8, plan.stream().mapToInt(VfxBudgetPlan.Allocation::particles).sum());
            assertTrue(particlesFor(plan, 0) >= 2);
            assertTrue(particlesFor(plan, 1) >= 2);
            plan.stream().map(VfxBudgetPlan.Allocation::componentIndex)
                    .filter(index -> index >= 2).forEach(seenDetails::add);
        }
        assertEquals(Set.of(2, 3, 4, 5, 6), seenDetails);
    }

    @Test
    void scarceBudgetsStayBoundedAndRotateCoreCoverage() {
        Set<Integer> seenCore = new HashSet<>();
        for (int phase = 0; phase < 3; phase++) {
            List<VfxBudgetPlan.Allocation> plan = VfxBudgetPlan.components(1, 3, 4, phase);
            assertEquals(1, plan.stream().mapToInt(VfxBudgetPlan.Allocation::particles).sum());
            assertEquals(1, plan.size());
            seenCore.add(plan.get(0).componentIndex());
        }
        assertEquals(Set.of(0, 1, 2), seenCore);

        List<VfxBudgetPlan.Allocation> threeParticles = VfxBudgetPlan.components(3, 2, 4, 0);
        assertEquals(3, threeParticles.stream()
                .mapToInt(VfxBudgetPlan.Allocation::particles).sum());
        assertTrue(threeParticles.stream().allMatch(allocation -> allocation.particles() > 0));
    }

    private static int particlesFor(List<VfxBudgetPlan.Allocation> plan, int component) {
        return plan.stream().filter(allocation -> allocation.componentIndex() == component)
                .mapToInt(VfxBudgetPlan.Allocation::particles).sum();
    }
}

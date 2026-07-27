package com.xunxian.seekingimmortals.client;

import java.util.ArrayList;
import java.util.List;

/** Pure allocation rules shared by the client renderer and its budget contracts. */
final class VfxBudgetPlan {
    private VfxBudgetPlan() {}

    static List<Integer> sampledCopies(int copies, int budget, int minimumPerCopy, int phase) {
        if (copies <= 0 || budget <= 0) {
            return List.of();
        }
        int slots = Math.min(copies, Math.max(1, budget / Math.max(1, minimumPerCopy)));
        int shift = Math.floorMod(phase, copies);
        List<Integer> sampled = new ArrayList<>(slots);
        for (int slot = 0; slot < slots; slot++) {
            sampled.add(Math.floorMod(slot * copies / slots + shift, copies));
        }
        return List.copyOf(sampled);
    }

    static List<Allocation> components(int budget, int coreCount, int detailCount, int phase) {
        if (coreCount < 0 || detailCount < 0) {
            throw new IllegalArgumentException("negative component count");
        }
        int componentCount = coreCount + detailCount;
        if (budget <= 0 || componentCount == 0) {
            return List.of();
        }

        int[] quotas = new int[componentCount];
        if (budget < coreCount) {
            for (int core : sampledCopies(coreCount, budget, 1, phase)) {
                quotas[core] = 1;
            }
            return allocations(quotas);
        }

        int remaining = budget;
        for (int core = 0; core < coreCount; core++) {
            quotas[core] = 1;
            remaining--;
        }
        if (coreCount > 0 && remaining >= coreCount) {
            for (int core = 0; core < coreCount; core++) {
                quotas[core]++;
                remaining--;
            }
        }

        List<Integer> selectedDetails = List.of();
        if (detailCount > 0 && remaining > 0) {
            int detailSlots = Math.min(detailCount, remaining);
            selectedDetails = sampledCopies(detailCount, detailSlots, 1, phase);
            for (int detail : selectedDetails) {
                quotas[coreCount + detail] = 1;
                remaining--;
            }
        }

        List<Integer> expansionTargets = new ArrayList<>();
        if (coreCount > 0) {
            for (int core = 0; core < coreCount; core++) {
                expansionTargets.add(core);
            }
        } else {
            for (int detail : selectedDetails) {
                expansionTargets.add(coreCount + detail);
            }
        }
        for (int index = 0; remaining > 0 && !expansionTargets.isEmpty(); index++, remaining--) {
            quotas[expansionTargets.get(index % expansionTargets.size())]++;
        }
        return allocations(quotas);
    }

    private static List<Allocation> allocations(int[] quotas) {
        List<Allocation> result = new ArrayList<>(quotas.length);
        for (int component = 0; component < quotas.length; component++) {
            if (quotas[component] > 0) {
                result.add(new Allocation(component, quotas[component]));
            }
        }
        return List.copyOf(result);
    }

    record Allocation(int componentIndex, int particles) {
        Allocation {
            if (componentIndex < 0 || particles <= 0) {
                throw new IllegalArgumentException("invalid component allocation");
            }
        }
    }
}

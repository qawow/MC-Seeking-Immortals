package com.xunxian.seekingimmortals.artifact;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * M15 草案对账：moditems_artifacts_draft ↔ 实际可解析注册物品差异报告。
 * 不修改注册表，仅提供权威查询。纯 JSON 侧可在单元测试中运行（不触碰 Forge 注册表）。
 */
public final class ArtifactDraftReconcileService {
    private ArtifactDraftReconcileService() {}

    public record DiffReport(
            int draftCount,
            int catalogCount,
            int draftPresent,
            int draftMissing,
            List<String> missingFromRegistry,
            List<String> draftOnly,
            List<String> catalogUnregisteredHint,
            List<String> uniqueRestricted
    ) {
        public DiffReport {
            missingFromRegistry = List.copyOf(missingFromRegistry);
            draftOnly = List.copyOf(draftOnly);
            catalogUnregisteredHint = List.copyOf(catalogUnregisteredHint);
            uniqueRestricted = List.copyOf(uniqueRestricted);
        }

        public boolean isClean() {
            return draftMissing == 0;
        }
    }

    public static DiffReport reconcile() {
        return reconcile(knownRegistryIds());
    }

    public static DiffReport reconcile(Set<String> registryIds) {
        ArtifactDataService.Snapshot snap = ArtifactDataService.builtin();
        Set<String> known = registryIds == null ? Set.of() : registryIds;

        List<String> missing = new ArrayList<>();
        List<String> present = new ArrayList<>();
        for (ArtifactDataService.DraftItem draft : snap.draftItems()) {
            String id = draft.registry() == null ? "" : draft.registry().toLowerCase(Locale.ROOT);
            if (id.isBlank()) {
                continue;
            }
            if (isResolvable(id, known, snap)) {
                present.add(id);
            } else {
                missing.add(id);
            }
        }

        List<String> draftOnly = new ArrayList<>();
        for (String id : present) {
            if (snap.findArtifact(id).isEmpty()) {
                draftOnly.add(id);
            }
        }

        List<String> unregistered = new ArrayList<>();
        List<String> uniques = new ArrayList<>();
        for (ArtifactDataService.ArtifactDefinition def : snap.artifacts().values()) {
            if (snap.isUniqueRestricted(def.id())) {
                uniques.add(def.id());
            }
            if (!isResolvable(def.id(), known, snap)) {
                unregistered.add(def.id());
            }
        }

        return new DiffReport(
                snap.draftItems().size(),
                snap.artifacts().size(),
                present.size(),
                missing.size(),
                missing,
                draftOnly,
                unregistered,
                uniques
        );
    }

    /**
     * Known ids without touching Forge DeferredRegister static init:
     * full catalog + special carriers. Bulk carriers are optional at runtime.
     */
    public static Set<String> knownRegistryIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (String id : ArtifactDataService.builtin().artifacts().keySet()) {
            ids.add(id.toLowerCase(Locale.ROOT));
        }
        // special non-catalog / alias carriers
        ids.add("flying_sword");
        ids.add("flying_artifact");
        ids.add("natal_artifact_embryo");
        ids.add("artifact_repair_kit");
        ids.add("wanbao_pavilion_coupon");
        // best-effort bulk (may be empty outside game bootstrap)
        try {
            for (String id : com.xunxian.seekingimmortals.registry.ModBulkItems.ids()) {
                if (id != null) {
                    ids.add(id.toLowerCase(Locale.ROOT));
                }
            }
        } catch (Throwable ignored) {
            // unit tests: Forge registries not bootstrapped
        }
        return ids;
    }

    private static boolean isResolvable(String id, Set<String> known, ArtifactDataService.Snapshot snap) {
        if (id == null || id.isBlank()) {
            return false;
        }
        String key = id.toLowerCase(Locale.ROOT);
        if (known.contains(key)) {
            return true;
        }
        // catalog authority: draft entries that exist in catalog are considered registered
        // via dedicated registerArtifact / bulk carrier (M03 closed missing=0).
        return snap.findArtifact(key).isPresent();
    }
}

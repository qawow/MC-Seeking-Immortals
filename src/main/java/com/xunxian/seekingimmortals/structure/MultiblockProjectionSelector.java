package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.item.CatalogItemDescriptionService;

import java.util.Locale;
import java.util.Optional;

/** Resolves the two kinds of client-side projection selectors. */
public final class MultiblockProjectionSelector {
    private MultiblockProjectionSelector() {}

    /** A placed controller block can always identify its own projection. */
    public static Optional<MultiblockProjectionCatalog.Projection> fromControllerBlock(String blockId) {
        return MultiblockProjectionCatalog.find(blockId);
    }

    /**
     * Resolves a bulk structure token without treating ordinary catalog carriers as controllers.
     * Both a bare catalog path and a namespaced mod id are accepted for resource-facing callers.
     */
    public static Optional<MultiblockProjectionCatalog.Projection> fromStructureToken(String itemId) {
        String path = catalogPath(itemId);
        if (path.isBlank() || !CatalogItemDescriptionService.isStructureTokenCarrier(path)) {
            return Optional.empty();
        }
        return MultiblockProjectionCatalog.find(path);
    }

    private static String catalogPath(String itemId) {
        if (itemId == null) {
            return "";
        }
        String key = itemId.trim().toLowerCase(Locale.ROOT);
        if (key.isBlank()) {
            return "";
        }
        int separator = key.indexOf(':');
        if (separator < 0) {
            return key;
        }
        if (!SeekingImmortalsMod.MODID.equals(key.substring(0, separator))) {
            return "";
        }
        return key.substring(separator + 1);
    }
}

package com.xunxian.seekingimmortals.region;

import java.util.List;

/**
 * Unified region definition for M06.
 * Authoritative region_id surface for M07/M08/M09/M10/M11/M13.
 */
public record RegionDefinition(
        String id,
        String displayZh,
        String displayEn,
        double auraMultiplier,
        String minRealm,
        String travelAnchor,
        String dimensionId,
        String climate,
        List<String> biomes,
        List<String> factions,
        List<String> tags,
        List<String> dailyEventIds,
        boolean hasCard,
        boolean hasWorldpack) {

    public RegionDefinition {
        id = id == null ? "" : id.trim();
        displayZh = displayZh == null ? "" : displayZh;
        displayEn = displayEn == null ? "" : displayEn;
        auraMultiplier = auraMultiplier <= 0.0D ? 1.0D : auraMultiplier;
        minRealm = minRealm == null || minRealm.isBlank() ? "qi_refining" : minRealm;
        travelAnchor = travelAnchor == null ? "" : travelAnchor;
        dimensionId = dimensionId == null || dimensionId.isBlank() ? "minecraft:overworld" : dimensionId;
        climate = climate == null ? "" : climate;
        biomes = biomes == null ? List.of() : List.copyOf(biomes);
        factions = factions == null ? List.of() : List.copyOf(factions);
        tags = tags == null ? List.of() : List.copyOf(tags);
        dailyEventIds = dailyEventIds == null ? List.of() : List.copyOf(dailyEventIds);
    }

    public String display() {
        return !displayZh.isBlank() ? displayZh : (!displayEn.isBlank() ? displayEn : id);
    }
}

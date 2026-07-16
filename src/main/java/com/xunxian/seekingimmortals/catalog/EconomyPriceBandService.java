package com.xunxian.seekingimmortals.catalog;

import java.util.Locale;
import java.util.Optional;

/**
 * Soft price guidance from economy_price_bands.
 * Returns a midpoint multiplier hint for shop cost display/tuning.
 */
public final class EconomyPriceBandService {
    private EconomyPriceBandService() {}

    public static int bandCount() {
        return ExtendedCatalogService.builtin().priceBands().size();
    }

    public static Optional<ExtendedCatalogService.PriceBand> find(String bandId) {
        return ExtendedCatalogService.builtin().findBand(bandId);
    }

    public static int suggestedCost(String bandId, int fallback) {
        return find(bandId).map(band -> {
            if (band.suggested() > 0) {
                return band.suggested();
            }
            if (band.min() > 0 || band.max() > 0) {
                return Math.max(1, (band.min() + Math.max(band.max(), band.min())) / 2);
            }
            return Math.max(1, fallback);
        }).orElse(Math.max(1, fallback));
    }

    public static double clampToBand(String bandId, double cost) {
        return find(bandId).map(band -> {
            double value = cost;
            if (band.min() > 0) {
                value = Math.max(value, band.min());
            }
            if (band.max() > 0) {
                value = Math.min(value, band.max());
            }
            return Math.max(1.0D, value);
        }).orElse(Math.max(1.0D, cost));
    }

    public static Optional<String> guessBandForItem(String itemId) {
        String id = itemId == null ? "" : itemId.toLowerCase(Locale.ROOT);
        if (id.contains("talisman") || id.contains("符")) {
            return Optional.of("talisman_low");
        }
        if (id.contains("pill") || id.contains("丹")) {
            return Optional.of("pill_qi");
        }
        if (id.contains("herb") || id.contains("grass") || id.contains("mushroom") || id.contains("ginseng")) {
            return Optional.of("herb_common");
        }
        if (id.contains("ticket") || id.contains("permit")) {
            return Optional.of("travel_ticket");
        }
        boolean oreToken = id.equals("ore") || id.startsWith("ore_")
                || id.endsWith("_ore") || id.contains("_ore_");
        if (oreToken || id.contains("iron") || id.contains("jade") || id.contains("crystal")) {
            return Optional.of("ore_rare");
        }
        return Optional.empty();
    }
}

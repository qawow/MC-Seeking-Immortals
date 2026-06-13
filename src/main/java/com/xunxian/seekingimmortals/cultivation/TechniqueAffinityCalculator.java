package com.xunxian.seekingimmortals.cultivation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Unified affinity calculator for cultivation techniques and talisman-like skills.
 *
 * <p>The first recognized spiritual-root attribute in an expression is treated as the primary
 * attribute; later recognized attributes are treated as secondary attributes. Unknown technique
 * tags such as sword arts, demon arts, blood arts or movement styles are kept data-friendly and
 * simply do not contribute a root affinity bonus.</p>
 */
public final class TechniqueAffinityCalculator {
    private TechniqueAffinityCalculator() {}

    public static AffinityResult calculate(PlayerCultivation cultivation, TechniqueDataManager.TechniqueEntry technique) {
        if (technique == null) {
            return calculate(cultivation, "");
        }
        return calculate(cultivation, technique.attribute());
    }

    public static AffinityResult calculate(PlayerCultivation cultivation, String attributeExpression) {
        return calculate(cultivation, parseAttributes(attributeExpression));
    }

    public static AffinityResult calculate(PlayerCultivation cultivation, SpiritualRootAttribute primary, SpiritualRootAttribute... secondary) {
        return calculate(cultivation, ParsedAttributes.of(primary, secondary));
    }

    public static int getDurationBonusTicks(PlayerCultivation cultivation, int baseDurationTicks, String attributeExpression) {
        double multiplier = calculate(cultivation, attributeExpression).multiplier();
        return Math.max(0, (int) Math.round(baseDurationTicks * (multiplier - 1.0D)));
    }

    public static int getEffectAmplifierBonus(PlayerCultivation cultivation, String attributeExpression) {
        return calculate(cultivation, attributeExpression).multiplier() >= 1.25D ? 1 : 0;
    }

    public static ParsedAttributes parseAttributes(String attributeExpression) {
        String raw = attributeExpression == null ? "" : attributeExpression.trim();
        if (raw.isBlank() || raw.contains(SpiritualRootAttribute.NONE.getDisplayName())) {
            return new ParsedAttributes(raw, true, null, Set.of());
        }

        SpiritualRootAttribute primary = null;
        LinkedHashSet<SpiritualRootAttribute> secondary = new LinkedHashSet<>();
        for (String part : raw.split("[/、,，\\s]+")) {
            SpiritualRootAttribute matched = parseAttributeToken(part);
            if (matched == null || matched == SpiritualRootAttribute.NONE) continue;
            if (primary == null) {
                primary = matched;
            } else if (matched != primary) {
                secondary.add(matched);
            }
        }
        return new ParsedAttributes(raw, primary == null, primary, Collections.unmodifiableSet(secondary));
    }

    private static AffinityResult calculate(PlayerCultivation cultivation, ParsedAttributes parsed) {
        if (cultivation == null || parsed.neutral() || parsed.primary() == null) {
            return AffinityResult.inactive(parsed, 0, false, false);
        }

        boolean awakened = cultivation.isSpiritualRootAwakened();
        boolean tested = cultivation.isSpiritualRootTested();
        int purity = Math.max(1, Math.min(100, cultivation.getSpiritualRootPurity()));
        Set<SpiritualRootAttribute> playerAttributes = cultivation.getSpiritualRootAttributes();
        if (!awakened || !tested || playerAttributes.isEmpty()) {
            return new AffinityResult(1.0D, false, parsed.raw(), parsed.primary(), parsed.secondary(), Set.of(), purity, awakened, tested);
        }

        double purityFactor = purity / 100.0D;
        double bonus = 0.0D;
        LinkedHashSet<SpiritualRootAttribute> matched = new LinkedHashSet<>();
        if (playerAttributes.contains(parsed.primary())) {
            bonus = Math.max(bonus, 0.18D + 0.22D * purityFactor);
            matched.add(parsed.primary());
        }
        for (SpiritualRootAttribute attribute : parsed.secondary()) {
            if (playerAttributes.contains(attribute)) {
                bonus = Math.max(bonus, 0.08D + 0.12D * purityFactor);
                matched.add(attribute);
            }
        }
        return new AffinityResult(1.0D + bonus, bonus > 0.0D, parsed.raw(), parsed.primary(), parsed.secondary(),
                Collections.unmodifiableSet(matched), purity, awakened, tested);
    }

    private static SpiritualRootAttribute parseAttributeToken(String token) {
        if (token == null || token.isBlank()) return null;
        String trimmed = token.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        for (SpiritualRootAttribute attribute : SpiritualRootAttribute.values()) {
            if (trimmed.equals(attribute.getDisplayName()) || normalized.equals(attribute.name().toLowerCase(Locale.ROOT))) {
                return attribute;
            }
        }
        for (SpiritualRootAttribute attribute : orderedAttributesForLooseMatch()) {
            if (!attribute.getDisplayName().isBlank() && trimmed.contains(attribute.getDisplayName())) {
                return attribute;
            }
            if (normalized.contains(attribute.name().toLowerCase(Locale.ROOT))) {
                return attribute;
            }
        }
        return null;
    }

    private static SpiritualRootAttribute[] orderedAttributesForLooseMatch() {
        return new SpiritualRootAttribute[] {
                SpiritualRootAttribute.HIDDEN_THUNDER,
                SpiritualRootAttribute.HIDDEN_DARK,
                SpiritualRootAttribute.IMMORTAL,
                SpiritualRootAttribute.THUNDER,
                SpiritualRootAttribute.METAL,
                SpiritualRootAttribute.WOOD,
                SpiritualRootAttribute.WATER,
                SpiritualRootAttribute.FIRE,
                SpiritualRootAttribute.EARTH,
                SpiritualRootAttribute.WIND,
                SpiritualRootAttribute.ICE,
                SpiritualRootAttribute.DARK,
                SpiritualRootAttribute.NONE
        };
    }

    public record AffinityResult(double multiplier,
                                 boolean active,
                                 String attributeExpression,
                                 SpiritualRootAttribute primary,
                                 Set<SpiritualRootAttribute> secondary,
                                 Set<SpiritualRootAttribute> matchedAttributes,
                                 int purity,
                                 boolean awakened,
                                 boolean tested) {
        private static AffinityResult inactive(ParsedAttributes parsed, int purity, boolean awakened, boolean tested) {
            return new AffinityResult(1.0D, false, parsed.raw(), parsed.primary(), parsed.secondary(), Set.of(), purity, awakened, tested);
        }
    }

    public record ParsedAttributes(String raw, boolean neutral, SpiritualRootAttribute primary, Set<SpiritualRootAttribute> secondary) {
        public static ParsedAttributes of(SpiritualRootAttribute primary, SpiritualRootAttribute... secondary) {
            LinkedHashSet<SpiritualRootAttribute> secondarySet = new LinkedHashSet<>();
            if (secondary != null) {
                for (SpiritualRootAttribute attribute : secondary) {
                    if (attribute != null && attribute != primary) {
                        secondarySet.add(attribute);
                    }
                }
            }
            return new ParsedAttributes(primary == null ? "" : primary.getDisplayName(), primary == null, primary,
                    Collections.unmodifiableSet(secondarySet));
        }
    }
}

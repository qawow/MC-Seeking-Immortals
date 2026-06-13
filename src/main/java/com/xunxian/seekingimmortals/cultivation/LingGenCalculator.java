package com.xunxian.seekingimmortals.cultivation;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class LingGenCalculator {
    private LingGenCalculator() {}

    public record Result(SpiritualRoot root, List<SpiritualRootAttribute> attributes, int purity, boolean awakened) {
        public String attributeNames() {
            return attributes.stream().map(SpiritualRootAttribute::getDisplayName).reduce((a, b) -> a + "/" + b).orElse("未知");
        }
    }

    public static Result roll(RandomSource random, double bonusChance) {
        double heavenlyThreshold = clamp(0.003D + bonusChance * 0.10D, 0.001D, 0.005D);
        double mutatedThreshold = heavenlyThreshold + clamp(0.020D + bonusChance * 0.25D, 0.010D, 0.030D);
        double trueRootThreshold = mutatedThreshold + clamp(0.300D + bonusChance * 0.35D, 0.200D, 0.400D);

        double roll = random.nextDouble();
        SpiritualRoot root;
        if (roll < heavenlyThreshold) {
            root = SpiritualRoot.HEAVENLY;
        } else if (roll < mutatedThreshold) {
            root = SpiritualRoot.MUTATED;
        } else if (roll < trueRootThreshold) {
            root = random.nextBoolean() ? SpiritualRoot.DUAL : SpiritualRoot.TRIPLE;
        } else {
            root = random.nextInt(100) < 78 ? SpiritualRoot.PSEUDO : SpiritualRoot.FIVE_ELEMENTS;
        }

        List<SpiritualRootAttribute> attributes = rollAttributes(random, root);
        int purity = rollPurity(random, root, bonusChance);
        boolean awakened = root != SpiritualRoot.HIDDEN;
        return new Result(root, attributes, purity, awakened);
    }

    public static Result rollAfterPurifying(RandomSource random, int currentPurity) {
        double bonus = clamp(currentPurity / 1000.0D + 0.08D, 0.08D, 0.18D);
        Result result = roll(random, bonus);
        return new Result(result.root(), result.attributes(), Math.max(currentPurity, result.purity()), result.awakened());
    }

    private static List<SpiritualRootAttribute> rollAttributes(RandomSource random, SpiritualRoot root) {
        List<SpiritualRootAttribute> result = new ArrayList<>();
        if (root == SpiritualRoot.HIDDEN) {
            result.add(SpiritualRootAttribute.randomHidden(random));
            return result;
        }
        if (root == SpiritualRoot.MUTATED) {
            result.add(SpiritualRootAttribute.randomMutated(random));
            return result;
        }

        EnumSet<SpiritualRootAttribute> excluded = EnumSet.noneOf(SpiritualRootAttribute.class);
        while (result.size() < root.getAttributeCount()) {
            SpiritualRootAttribute attribute = SpiritualRootAttribute.randomFiveElement(random, excluded);
            excluded.add(attribute);
            result.add(attribute);
        }
        return result;
    }

    private static int rollPurity(RandomSource random, SpiritualRoot root, double bonusChance) {
        int base = switch (root) {
            case HEAVENLY -> 88;
            case HIDDEN -> 90;
            case MUTATED -> 78;
            case DUAL -> 65;
            case TRIPLE -> 52;
            case PSEUDO -> 34;
            case FIVE_ELEMENTS -> 22;
        };
        int spread = switch (root) {
            case HEAVENLY, HIDDEN -> 12;
            case MUTATED -> 18;
            case DUAL, TRIPLE -> 24;
            case PSEUDO, FIVE_ELEMENTS -> 30;
        };
        int bonus = (int)Math.round(bonusChance * 100.0D);
        return Math.max(1, Math.min(100, base + random.nextInt(spread + 1) + bonus));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

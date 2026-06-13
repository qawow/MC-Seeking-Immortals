package com.xunxian.seekingimmortals.cultivation;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class LingGenCalculator {
    private LingGenCalculator() {}

    public record Result(SpiritualRoot root, List<SpiritualRootAttribute> attributes, boolean awakened) {
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
            root = random.nextInt(100) < 78 ? SpiritualRoot.FALSE_ROOT : SpiritualRoot.MIXED;
        }

        List<SpiritualRootAttribute> attributes = rollAttributes(random, root);
        boolean awakened = root != SpiritualRoot.HIDDEN;
        return new Result(root, attributes, awakened);
    }

    public static Result rollAfterPurifying(RandomSource random, int currentPurity) {
        double bonus = clamp(currentPurity / 1000.0D + 0.08D, 0.08D, 0.18D);
        return roll(random, bonus);
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

package com.xunxian.seekingimmortals.cultivation;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * 灵根随机生成器。
 * <p>权重对齐 {@code spirit_roots_catalog.json} generation：
 * mortal_awakening_chance=0.08；multi_root_weights single=0.55 dual=0.28 triple=0.12 mutant=0.05。</p>
 * <p>single 再按品阶拆为天灵/上品真根/下品伪杂；mutant 覆盖变异与隐灵根。</p>
 */
public final class LingGenCalculator {
    /** 凡人觉醒灵根基础概率（测灵前未觉醒时的设定参考）。 */
    public static final double MORTAL_AWAKENING_CHANCE = 0.08D;
    public static final double WEIGHT_SINGLE = 0.55D;
    public static final double WEIGHT_DUAL = 0.28D;
    public static final double WEIGHT_TRIPLE = 0.12D;
    public static final double WEIGHT_MUTANT = 0.05D;

    private LingGenCalculator() {}

    public record Result(SpiritualRoot root, List<SpiritualRootAttribute> attributes, boolean awakened) {
        public String attributeNames() {
            return attributes.stream().map(SpiritualRootAttribute::getDisplayName).reduce((a, b) -> a + "/" + b).orElse("未知");
        }
    }

    public static Result roll(RandomSource random, double bonusChance) {
        double bonus = clamp(bonusChance, 0.0D, 0.25D);
        // bonus 轻微抬高单/异灵根权重，压低伪杂。
        double single = clamp(WEIGHT_SINGLE + bonus * 0.20D, 0.40D, 0.70D);
        double dual = clamp(WEIGHT_DUAL + bonus * 0.05D, 0.15D, 0.35D);
        double triple = clamp(WEIGHT_TRIPLE, 0.05D, 0.20D);
        double mutant = clamp(WEIGHT_MUTANT + bonus * 0.08D, 0.03D, 0.12D);
        double sum = single + dual + triple + mutant;
        single /= sum;
        dual /= sum;
        triple /= sum;
        mutant /= sum;

        double roll = random.nextDouble();
        SpiritualRoot root;
        if (roll < single) {
            // single 内：天灵根稀有、上品真根、下品伪/杂
            double grade = random.nextDouble();
            if (grade < 0.03D + bonus * 0.04D) {
                root = SpiritualRoot.HEAVENLY;
            } else if (grade < 0.55D) {
                // 单属性真根在现有分类里以 HEAVENLY 的弱化形态不存在；
                // 用 TRIPLE 中的单属性不可表达，保守映射为 DUAL 的单属性例外 → 仍归 HEAVENLY 低档用 FALSE 区分。
                // 为避免破坏下游“天灵=单纯”语义：上品单属 → HEAVENLY；凡品/下品单属 → FALSE_ROOT 单属性。
                root = grade < 0.18D + bonus * 0.05D ? SpiritualRoot.HEAVENLY : SpiritualRoot.FALSE_ROOT;
            } else {
                root = random.nextInt(100) < 70 ? SpiritualRoot.FALSE_ROOT : SpiritualRoot.MIXED;
            }
        } else if (roll < single + dual) {
            root = SpiritualRoot.DUAL;
        } else if (roll < single + dual + triple) {
            root = SpiritualRoot.TRIPLE;
        } else {
            // mutant: 85% 变异，15% 隐
            root = random.nextDouble() < 0.15D ? SpiritualRoot.HIDDEN : SpiritualRoot.MUTATED;
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
        if (root == SpiritualRoot.HEAVENLY) {
            result.add(SpiritualRootAttribute.randomFiveElement(random, EnumSet.noneOf(SpiritualRootAttribute.class)));
            return result;
        }

        EnumSet<SpiritualRootAttribute> excluded = EnumSet.noneOf(SpiritualRootAttribute.class);
        int count = Math.max(1, root.getAttributeCount());
        // 伪灵根/杂灵根保持 4/5 属性；若被映射为 FALSE 的“单属凡品”则只 1 属性。
        if (root == SpiritualRoot.FALSE_ROOT && random.nextDouble() < 0.35D) {
            count = 1;
        }
        while (result.size() < count) {
            SpiritualRootAttribute attribute = SpiritualRootAttribute.randomFiveElement(random, excluded);
            excluded.add(attribute);
            result.add(attribute);
            if (excluded.size() >= 5) break;
        }
        return result;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

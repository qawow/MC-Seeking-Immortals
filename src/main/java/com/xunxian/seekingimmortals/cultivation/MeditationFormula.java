package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.item.SpiritStoneItem;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import net.minecraft.world.item.ItemStack;

public final class MeditationFormula {
    public static final double CUSHION_BONUS = 1.0D;
    public static final double NO_CUSHION_BONUS = 0.5D;

    private MeditationFormula() {}

    public static Breakdown calculate(PlayerCultivation cultivation, SpiritualAuraManager.AuraInfo auraInfo, boolean onCushion, double techniqueMultiplier, ItemStack bonusStone, int stoneBonus) {
        double basePerSecond = RealmStageConfig.getCultivationGainBase(cultivation.getRealm());
        double rootMultiplier = cultivation.getSpiritualRootCultivationSpeedCoefficient();
        double physiqueMultiplier = cultivation.getPhysiqueCultivationSpeedMultiplier();
        double meditationBonus = onCushion ? CUSHION_BONUS : NO_CUSHION_BONUS;
        double auraMultiplier = Math.sqrt(auraInfo.concentration() / (double) SpiritualAuraManager.BASE_AURA);
        double heldStoneBonus = getStoneBonusPerSecond(bonusStone, stoneBonus);
        double perSecond = basePerSecond * rootMultiplier * physiqueMultiplier * meditationBonus * auraMultiplier * techniqueMultiplier + heldStoneBonus;
        return new Breakdown(basePerSecond, rootMultiplier, physiqueMultiplier, meditationBonus, auraMultiplier, techniqueMultiplier, heldStoneBonus, Math.max(0.0D, perSecond));
    }

    public static double getStoneBonusPerSecond(ItemStack stack, int stoneBonus) {
        if (stoneBonus <= 0 || stack.isEmpty() || SpiritStoneItem.getStoredPower(stack) <= 0) return 0.0D;
        return stoneBonus;
    }

    public record Breakdown(double basePerSecond, double rootMultiplier, double physiqueMultiplier, double meditationBonus,
                            double auraMultiplier, double techniqueMultiplier, double heldStoneBonus, double totalPerSecond) {
        public double perTick() {
            return totalPerSecond / 20.0D;
        }
    }
}

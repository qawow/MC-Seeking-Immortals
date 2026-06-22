package com.xunxian.seekingimmortals.client;

public final class ClientCultivationData {
    private static Snapshot snapshot = Snapshot.empty();
    private static boolean synced;
    private static Boolean pendingMeditating;

    private ClientCultivationData() {}

    public static void setSnapshot(Snapshot value) {
        snapshot = value;
        synced = true;
        pendingMeditating = null;
    }

    public static void reset() {
        snapshot = Snapshot.empty();
        synced = false;
        pendingMeditating = null;
    }

    public static Snapshot getSnapshot() {
        return snapshot;
    }

    public static boolean isSynced() {
        return synced;
    }

    public static void setPendingMeditating(boolean value) {
        pendingMeditating = value;
    }

    public static boolean effectiveMeditating() {
        return pendingMeditating != null ? pendingMeditating : snapshot.meditating();
    }

    public static boolean hasPendingMeditating() {
        return pendingMeditating != null;
    }

    public record Snapshot(
            String realm,
            String stage,
            int spiritualPower,
            int maxSpiritualPower,
            int cultivationExp,
            int cultivation,
            long cultivationMax,
            int mana,
            int manaMax,
            int divSense,
            int bodyRef,
            int qiDevRisk,
            int tribRes,
            int lifespanYears,
            int ageYears,
            int remainingLifespanYears,
            String spiritualRoot,
            String spiritualRootAttributes,
            int spiritualRootPurity,
            boolean spiritualRootAwakened,
            boolean spiritualRootTested,
            String specialPhysique,
            int learnedTechniqueCount,
            boolean meditating,
            boolean severeInjury,
            int heartDemonLevel,
            boolean shatteredCore,
            int realmFallScars,
            double cultivationSpeedMultiplier,
            double rootCultivationSpeedCoefficient,
            double physiqueCultivationSpeedMultiplier,
            double baseAttack,
            double baseDefense,
            double critChance,
            double critDamage,
            double dodgeChance,
            double accuracy,
            int auraConcentration,
            String auraNature,
            double breakthroughChance,
            double breakthroughPillBonus,
            double breakthroughSpiritEyeBonus,
            double breakthroughTechniqueQualityBonus,
            double breakthroughObsessionBonus,
            int failedBreakthroughs,
            double meditationBasePerSecond,
            double meditationRootMultiplier,
            double meditationPhysiqueMultiplier,
            double meditationBonus,
            double meditationAuraMultiplier,
            double meditationTechniqueMultiplier,
            double meditationStoneBonus,
            double meditationTotalPerSecond) {
        public static Snapshot empty() {
            return new Snapshot("未同步", "", 0, 100, 0, 0, 1L, 0, 100, 0, 0, 0, 0, 0, 0, 0, "未检测", "未知", 0, false, false, "无", 0, false, false, 0, false, 0, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.5D, 0.0D, 0.0D, 100, "天地灵气", 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D);
        }
    }
}

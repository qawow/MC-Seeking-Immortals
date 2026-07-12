package com.xunxian.seekingimmortals.cultivation;

public enum GoldCoreGrade {
    NONE("未结丹", 0x777777, 1.0f),
    PSEUDO("伪丹", 0x8B6F47, 0.85f),
    LOW("下品金丹", 0xA0A0A0, 1.0f),
    MIDDLE("中品金丹", 0xC0C0C0, 1.2f),
    HIGH("上品金丹", 0xFFD700, 1.5f),
    PERFECT("极品金丹", 0x9370DB, 2.0f);

    private final String displayName;
    private final int color;
    private final float attributeMultiplier;

    GoldCoreGrade(String displayName, int color, float attributeMultiplier) {
        this.displayName = displayName;
        this.color = color;
        this.attributeMultiplier = attributeMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    public float getAttributeMultiplier() {
        return attributeMultiplier;
    }

    public boolean isFormed() {
        return this != NONE;
    }

    public static GoldCoreGrade fromScore(int score) {
        if (score < 35) return PSEUDO;
        if (score < 55) return LOW;
        if (score < 75) return MIDDLE;
        if (score < 90) return HIGH;
        return PERFECT;
    }

    public static int calculateScore(SpiritualRoot root, int purity, SpecialPhysique physique,
                                     double pillBonus, double techniqueQualityBonus,
                                     boolean spiritEye, int bodyRefinement, int qiDeviationRisk) {
        int rootScore = switch (root == null ? SpiritualRoot.TRIPLE : root) {
            case HEAVENLY -> 24;
            case HIDDEN -> 23;
            case MUTATED -> 21;
            case DUAL -> 17;
            case TRIPLE -> 13;
            case FALSE_ROOT -> 8;
            case MIXED -> 6;
        };
        int purityScore = (int)Math.round(clamp(purity, 1, 100) / 100.0D * 18.0D);
        int physiqueScore = getPhysiqueScore(physique);
        int pillScore = (int)Math.round(clamp01(pillBonus / 0.20D) * 20.0D);
        int techniqueScore = (int)Math.round(clamp01(techniqueQualityBonus / 0.10D) * 10.0D);
        int spiritEyeScore = spiritEye ? 8 : 0;
        int bodyScore = Math.min(10, (int)Math.round(Math.sqrt(Math.max(0, bodyRefinement)) / 2.0D));
        int riskPenalty = Math.min(20, Math.max(0, qiDeviationRisk) / 5);
        return Math.max(0, rootScore + purityScore + physiqueScore + pillScore + techniqueScore
                + spiritEyeScore + bodyScore - riskPenalty);
    }

    public static GoldCoreGrade calculateGrade(double cultivationProgress, SpiritualRoot root, boolean usedPerfectPill) {
        int progressScore = (int)Math.round(clamp01(cultivationProgress) * 18.0D);
        int score = calculateScore(root, progressScore * 5, SpecialPhysique.NONE,
                usedPerfectPill ? 0.20D : 0.05D, 0.0D, false, 0, 0);
        return fromScore(score);
    }

    private static int getPhysiqueScore(SpecialPhysique physique) {
        return switch (physique == null ? SpecialPhysique.NONE : physique) {
            case NONE -> 0;
            case DRAGON_CHANT_BODY -> -2;
            case ICE_MARROW_BODY -> 5;
            case HIDDEN_THUNDER_ROOT, JADE_PHOENIX_MARROW, GOLD_FORGING_BODY, MOLTEN_GOLD_BODY,
                    THREE_YANG_BODY, CHARMING_BODY -> 4;
            case FIVE_THUNDER_BODY, NINE_SPIRIT_SWORD_BODY, HEAVENLY_YIN_BODY, MYSTIC_YIN_BODY -> 7;
            case SEVEN_STAR_MOON_BODY, CHASTE_YIN_BODY -> 6;
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}

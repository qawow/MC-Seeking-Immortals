package com.xunxian.seekingimmortals.skill;

import net.minecraft.server.level.ServerPlayer;

/**
 * Wave490/492: special-skill authority helpers reusing LifeSkillService + SkillType.SPECIAL.
 * MULTI_CASTING: honest dual-cast (main + free secondary slots) with shared cost, plus CD scale.
 */
public final class SpecialSkillService {
    private SpecialSkillService() {}

    public static final SkillType[] SPECIALS = {
            SkillType.FLYING_SWORD_BEGINNER,
            SkillType.FLYING_SWORD_ADVANCED,
            SkillType.DIVINE_SENSE_EXPANSION,
            SkillType.FORMATION_SENSE,
            SkillType.BEAST_TAMING,
            SkillType.PUPPET_CONTROL,
            SkillType.MULTI_CASTING
    };

    public static int level(ServerPlayer player, SkillType type) {
        return LifeSkillService.level(player, type);
    }

    public static void practice(ServerPlayer player, SkillType type, int xp, int proficiency) {
        if (type == null || type.getCategory() != SkillCategory.SPECIAL) {
            return;
        }
        LifeSkillService.grantPractice(player, type, xp, proficiency);
    }

    public static double multiCastCooldownScale(ServerPlayer player) {
        int lv = level(player, SkillType.MULTI_CASTING);
        if (lv <= 0) {
            return 1.0D;
        }
        return Math.max(0.55D, 1.0D - lv * 0.07D);
    }

    /** Wave492: dual-cast unlocks at MULTI_CASTING L1; extra free slot count scales slowly. */
    public static int dualCastExtraSlots(ServerPlayer player) {
        int lv = level(player, SkillType.MULTI_CASTING);
        if (lv <= 0) {
            return 0;
        }
        return Math.min(2, 1 + (lv - 1) / 2);
    }

    public static boolean canDualCast(ServerPlayer player) {
        return dualCastExtraSlots(player) > 0;
    }

    public static void practiceMultiCast(ServerPlayer player) {
        practice(player, SkillType.MULTI_CASTING, 10, 4);
    }

    public static void practiceSense(ServerPlayer player) {
        practice(player, SkillType.DIVINE_SENSE_EXPANSION, 8, 3);
        practice(player, SkillType.FORMATION_SENSE, 6, 2);
    }
}

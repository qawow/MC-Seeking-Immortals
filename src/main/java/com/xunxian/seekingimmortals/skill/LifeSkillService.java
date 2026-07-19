package com.xunxian.seekingimmortals.skill;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.network.SyncSkillDataPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Wave489: shared life/special craft-skill authority helpers.
 * Reuses PlayerCultivation skill map + CultivationSkill level/proficiency.
 */
public final class LifeSkillService {
    public static final double BONUS_PER_LEVEL = 0.02D;
    public static final double BONUS_MAX = 0.20D;
    /** Full proficiency (10000) grants this much extra success chance. */
    public static final double PROFICIENCY_BONUS_MAX = 0.10D;
    public static final int DEFAULT_XP = 20;
    public static final int DEFAULT_PROFICIENCY = 8;

    private LifeSkillService() {}

    public static int level(ServerPlayer player, SkillType type) {
        if (player == null || type == null) {
            return 0;
        }
        return CultivationHelper.get(player)
                .map(c -> {
                    CultivationSkill skill = c.getSkill(type);
                    return skill == null || !skill.isUnlocked() ? 0 : skill.getLevel();
                })
                .orElse(0);
    }

    public static int proficiency(ServerPlayer player, SkillType type) {
        if (player == null || type == null) {
            return 0;
        }
        return CultivationHelper.get(player)
                .map(c -> {
                    CultivationSkill skill = c.getSkill(type);
                    return skill == null || !skill.isUnlocked() ? 0 : Math.max(0, skill.getProficiency());
                })
                .orElse(0);
    }

    public static double successBonus(ServerPlayer player, SkillType type) {
        int lv = level(player, type);
        double levelBonus = lv <= 0 ? 0.0D : Math.min(BONUS_MAX, lv * BONUS_PER_LEVEL);
        double proficiencyBonus = proficiencyBonus(proficiency(player, type));
        return Math.min(BONUS_MAX + PROFICIENCY_BONUS_MAX, levelBonus + proficiencyBonus);
    }

    /** Pure helper for tests and callers without a live player. */
    public static double proficiencyBonus(int proficiency) {
        int clamped = Math.max(0, Math.min(10000, proficiency));
        return PROFICIENCY_BONUS_MAX * (clamped / 10000.0D);
    }

    public static double adjustedSuccessRate(ServerPlayer player, SkillType type, double baseRate) {
        double base = Math.max(0.0D, Math.min(1.0D, baseRate));
        return Math.max(0.03D, Math.min(0.95D, base + successBonus(player, type)));
    }

    public static boolean meetsLevel(ServerPlayer player, SkillType type, int required) {
        if (player != null && player.getAbilities().instabuild) {
            return true;
        }
        if (player == null || type == null) {
            return false;
        }
        return CultivationHelper.get(player)
                .map(cultivation -> {
                    CultivationSkill skill = cultivation.getSkill(type);
                    return skill != null && skill.isUnlocked()
                            && skill.getLevel() >= Math.max(0, required);
                })
                .orElse(false);
    }

    /** Unlock (if eligible) then grant XP/proficiency and sync. */
    public static void grantPractice(ServerPlayer player, SkillType type, int xp, int proficiency) {
        if (player == null || type == null) {
            return;
        }
        CultivationHelper.get(player).ifPresent(cultivation -> {
            CultivationSkill before = cultivation.getSkill(type);
            boolean wasUnlocked = before != null && before.isUnlocked();
            int previousLevel = before == null ? 0 : before.getLevel();
            int previousExperience = before == null ? 0 : before.getExperience();
            int previousProficiency = before == null ? 0 : before.getProficiency();
            if (!cultivation.hasSkill(type)) {
                if (!cultivation.unlockSkill(type)) {
                    return;
                }
            }
            if (xp > 0) {
                cultivation.addSkillExperience(type, xp);
            }
            if (proficiency > 0) {
                cultivation.addSkillProficiency(type, proficiency);
            }
            CultivationSkill after = cultivation.getSkill(type);
            boolean changed = after != null && (after.isUnlocked() != wasUnlocked
                    || after.getLevel() != previousLevel
                    || after.getExperience() != previousExperience
                    || after.getProficiency() != previousProficiency);
            if (changed) {
                SyncCultivationDataPacket.send(player, cultivation);
                SyncSkillDataPacket.send(player, cultivation);
            }
        });
    }

    public static void grantPractice(ServerPlayer player, SkillType type) {
        grantPractice(player, type, DEFAULT_XP, DEFAULT_PROFICIENCY);
    }

    public static String summaryLine(PlayerCultivation cultivation, SkillType type) {
        if (cultivation == null || type == null) {
            return type == null ? "-" : type.getDisplayName() + " L0";
        }
        CultivationSkill skill = cultivation.getSkill(type);
        if (skill == null || !skill.isUnlocked()) {
            return type.getDisplayName() + " L0";
        }
        return type.getDisplayName() + " L" + skill.getLevel()
                + " (" + skill.getExperience() + "/" + skill.getExpForNextLevel() + ")"
                + " P" + skill.getProficiency();
    }
}

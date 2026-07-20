package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.skill.LifeSkillService;
import com.xunxian.seekingimmortals.skill.SkillType;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

/**
 * Server-authoritative gameplay projection of the authored sect specialty map.
 */
public final class SectSpecialtyGameplayService {
    private static final int MAX_DISCOUNT_PERCENT = 25;

    private SectSpecialtyGameplayService() {}

    public static int discountPercent(String sectId, int stage) {
        if (stage < SectContributionService.STAGE_OUTER_DISCIPLE) {
            return 0;
        }
        int authored = SectMasterDataService.specialty(sectId)
                .map(SectMasterDataService.Specialty::shopDiscountPercent)
                .orElse(0);
        return Math.min(MAX_DISCOUNT_PERCENT, authored + rankIncrement(stage));
    }

    public static int contributionCost(String sectId, String shopId, int stage, int baseCost) {
        int safeBase = Math.max(1, baseCost);
        if (!isOwnContributionHall(sectId, shopId)) {
            return safeBase;
        }
        int discount = discountPercent(sectId, stage);
        if (discount <= 0) {
            return safeBase;
        }
        return Math.max(1, (safeBase * (100 - discount) + 99) / 100);
    }

    public static int missionContributionReward(String sectId, int stage, int baseReward) {
        int safeBase = Math.max(0, baseReward);
        if (stage < SectContributionService.STAGE_OUTER_DISCIPLE) {
            return safeBase;
        }
        int authored = SectMasterDataService.specialty(sectId)
                .map(SectMasterDataService.Specialty::missionContributionBonus)
                .orElse(0);
        return Math.max(0, safeBase + authored + rankIncrement(stage));
    }

    public static Optional<SkillType> missionSkill(String sectId) {
        String skillId = SectMasterDataService.specialty(sectId)
                .map(SectMasterDataService.Specialty::missionSkill)
                .orElse("");
        if (skillId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(SkillType.valueOf(skillId.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static void grantMissionPractice(ServerPlayer player, String sectId, int stage) {
        missionSkill(sectId).ifPresent(skill -> LifeSkillService.grantPractice(
                player, skill, 12 + Math.max(0, stage) * 2, 4 + Math.max(0, stage)));
    }

    static int rankIncrement(int stage) {
        return switch (Math.max(0, stage)) {
            case SectContributionService.STAGE_FOUNDATION_DILEMMA -> 2;
            case SectContributionService.STAGE_INNER_DISCIPLE -> 5;
            case SectContributionService.STAGE_PHASE10_COMPLETE -> 8;
            default -> 0;
        };
    }

    private static boolean isOwnContributionHall(String sectId, String shopId) {
        String normalizedShop = normalize(shopId);
        return SectDefinitionService.find(sectId)
                .map(SectDefinitionService.SectDefinition::shopId)
                .map(SectSpecialtyGameplayService::normalize)
                .filter(normalizedShop::equals)
                .isPresent();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

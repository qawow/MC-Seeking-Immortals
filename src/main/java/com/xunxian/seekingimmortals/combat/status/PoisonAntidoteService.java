package com.xunxian.seekingimmortals.combat.status;

import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;
import java.util.Optional;

/**
 * 十绝毒 → 状态 id 映射，以及解药驱散（效果归 M14，物品注册归 M03）。
 */
public final class PoisonAntidoteService {
    private PoisonAntidoteService() {}

    public static Optional<String> statusIdForPoisonVariant(String poisonVariantId) {
        if (poisonVariantId == null || poisonVariantId.isBlank()) {
            return Optional.empty();
        }
        String key = poisonVariantId.trim().toLowerCase(Locale.ROOT);
        for (StatusCatalogService.PoisonVariant variant : StatusCatalogService.builtin().poisonVariants()) {
            if (variant.id().equals(key)) {
                return Optional.of(variant.statusId());
            }
        }
        return Optional.empty();
    }

    /**
     * 施加毒系变体对应的战斗状态。
     */
    public static boolean applyPoisonVariant(LivingEntity target, String poisonVariantId, int level, int durationTicks) {
        return statusIdForPoisonVariant(poisonVariantId)
                .map(statusId -> StatusRegistry.applyStatus(target, statusId, level, durationTicks))
                .orElse(false);
    }

    public static boolean applyPoisonVariant(LivingEntity target,
                                             LivingEntity caster,
                                             String poisonVariantId,
                                             int level,
                                             int durationTicks) {
        return statusIdForPoisonVariant(poisonVariantId)
                .map(statusId -> StatusRegistry.applyStatus(target, caster, statusId, level, durationTicks))
                .orElse(false);
    }

    /**
     * 按解药 id 驱散目标上匹配的状态。
     *
     * @return 是否至少清除了一个状态
     */
    public static boolean applyAntidote(LivingEntity target, String antidoteId) {
        if (target == null || target.level().isClientSide || antidoteId == null || antidoteId.isBlank()) {
            return false;
        }
        String key = antidoteId.trim().toLowerCase(Locale.ROOT);
        StatusCatalogService.AntidoteClear rule = null;
        for (StatusCatalogService.AntidoteClear antidote : StatusCatalogService.builtin().antidotes()) {
            if (antidote.id().equals(key)) {
                rule = antidote;
                break;
            }
        }
        if (rule == null) {
            return false;
        }

        boolean cleared = false;
        // 直接按 status id 清
        for (String statusId : rule.clearsStatusIds()) {
            if (StatusRegistry.clearStatus(target, statusId)) {
                cleared = true;
            }
        }
        // 按毒变体映射到 status
        for (String variant : rule.clearsVariants()) {
            Optional<String> statusId = statusIdForPoisonVariant(variant);
            if (statusId.isPresent() && StatusRegistry.clearStatus(target, statusId.get())) {
                cleared = true;
            }
        }
        // 按 family 清（emergency 或 family 列表）
        if (!rule.clearsFamilies().isEmpty() || rule.emergency()) {
            for (StatusCatalogService.StatusDefinition def : StatusCatalogService.builtin().effects()) {
                boolean familyMatch = rule.clearsFamilies().contains(def.family());
                if ((familyMatch || rule.emergency()) && StatusRegistry.hasStatus(target, def.id())) {
                    // fails_on 阻止特定变体对应状态被普通解毒清掉
                    if (!rule.emergency() && isFailedBy(rule, def.id())) {
                        continue;
                    }
                    if (StatusRegistry.clearStatus(target, def.id())) {
                        cleared = true;
                    }
                }
            }
        }
        return cleared;
    }

    private static boolean isFailedBy(StatusCatalogService.AntidoteClear rule, String statusId) {
        for (String fail : rule.failsOn()) {
            Optional<String> mapped = statusIdForPoisonVariant(fail);
            if (mapped.isPresent() && mapped.get().equals(statusId)) {
                return true;
            }
            if (fail.equals(statusId)) {
                return true;
            }
        }
        return false;
    }
}

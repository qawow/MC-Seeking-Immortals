package com.xunxian.seekingimmortals.combat.status;

import net.minecraft.world.effect.MobEffectCategory;

import java.util.ArrayList;
import java.util.List;

public final class StatusRegistryTestSupport {
    private StatusRegistryTestSupport() {}

    public static double outgoingDamageMultiplier(String... statusIds) {
        return StatusRegistry.outgoingDamageMultiplierForEffects(activeEffects(statusIds));
    }

    public static boolean blocksTechnique(String... statusIds) {
        return StatusRegistry.blocksTechniqueForEffects(activeEffects(statusIds));
    }

    public static boolean hidesRealm(String... statusIds) {
        return StatusRegistry.hidesRealmForEffects(activeEffects(statusIds));
    }

    private static List<SeekingStatusEffect> activeEffects(String... statusIds) {
        List<SeekingStatusEffect> effects = new ArrayList<>();
        if (statusIds == null) {
            return effects;
        }
        for (String statusId : statusIds) {
            StatusCatalogService.StatusDefinition def = StatusCatalogService.builtin()
                    .find(statusId)
                    .orElseThrow();
            effects.add(new SeekingStatusEffect(
                    def.id(),
                    def.beneficial() ? MobEffectCategory.BENEFICIAL : MobEffectCategory.HARMFUL,
                    def.colorRgb(),
                    0.0D,
                    0.0D,
                    20,
                    1.0D,
                    def.outgoingDamageMul(),
                    1.0D,
                    def.blocksTechnique(),
                    def.hidesRealm()));
        }
        return effects;
    }
}

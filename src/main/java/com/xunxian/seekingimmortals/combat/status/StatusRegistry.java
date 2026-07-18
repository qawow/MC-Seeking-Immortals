package com.xunxian.seekingimmortals.combat.status;

import com.xunxian.seekingimmortals.cultivation.ConstitutionCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.registry.ModMobEffects;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * M14 统一状态 id → MobEffect 映射与施加入口。
 * <p>跨模块（M02/M10/M15）应只通过本类字符串 id API 引用状态，不直接依赖具体 MobEffect 常量。</p>
 * <p>边界：心魔/重伤/碎丹/跌境等长时伤势归 M01 {@code ImmortalAffliction}；本类仅处理短时战斗状态。</p>
 */
public final class StatusRegistry {
    private StatusRegistry() {}

    public static Collection<String> allIds() {
        return StatusCatalogService.builtin().ids();
    }

    public static Optional<StatusCatalogService.StatusDefinition> definition(String statusId) {
        return StatusCatalogService.builtin().find(statusId);
    }

    public static Optional<MobEffect> resolve(String statusId) {
        StatusCatalogService.StatusDefinition def = StatusCatalogService.builtin().find(statusId).orElse(null);
        if (def == null) {
            return Optional.empty();
        }
        RegistryObject<MobEffect> object = ModMobEffects.get(def.id());
        if (object == null || !object.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(object.get());
    }

    public static boolean isKnown(String statusId) {
        return StatusCatalogService.builtin().find(statusId).isPresent();
    }

    /**
     * 对目标施加状态。无 caster 时跳过境界差命中检定（视为必然命中，仍受体质抗性衰减 duration）。
     *
     * @return true 若成功写入 MobEffectInstance
     */
    public static boolean applyStatus(LivingEntity target, String statusId, int level, int durationTicks) {
        return applyStatus(target, null, statusId, level, durationTicks, null);
    }

    public static boolean applyStatus(LivingEntity target,
                                      @Nullable LivingEntity caster,
                                      String statusId,
                                      int level,
                                      int durationTicks) {
        return applyStatus(target, caster, statusId, level, durationTicks, null);
    }

    public static boolean applyStatus(LivingEntity target,
                                      @Nullable LivingEntity caster,
                                      String statusId,
                                      int level,
                                      int durationTicks,
                                      @Nullable RandomSource random) {
        return applyStatusInternal(target, caster, statusId, level, durationTicks, random, true);
    }

    /** Applies an authoritative encounter status while retaining its caster as the effect source. */
    public static boolean applyGuaranteedStatus(LivingEntity target,
                                                @Nullable LivingEntity caster,
                                                String statusId,
                                                int level,
                                                int durationTicks) {
        return applyStatusInternal(target, caster, statusId, level, durationTicks, null, false);
    }

    private static boolean applyStatusInternal(LivingEntity target,
                                               @Nullable LivingEntity caster,
                                               String statusId,
                                               int level,
                                               int durationTicks,
                                               @Nullable RandomSource random,
                                               boolean checkHit) {
        if (target == null || target.level().isClientSide || statusId == null || statusId.isBlank()) {
            return false;
        }
        Optional<StatusCatalogService.StatusDefinition> defOpt = definition(statusId);
        if (defOpt.isEmpty()) {
            return false;
        }
        StatusCatalogService.StatusDefinition def = defOpt.get();
        Optional<MobEffect> effectOpt = resolve(def.id());
        if (effectOpt.isEmpty()) {
            return false;
        }

        int amplifier = Mth.clamp(level, 0, Math.max(0, def.maxAmplifier()));
        int duration = durationTicks > 0 ? durationTicks : def.defaultDurationTicks();

        if (!def.beneficial()) {
            if (checkHit && caster != null) {
                double hitChance = computeHitChance(caster, target);
                RandomSource rng = random != null ? random
                        : (target.level().getRandom() != null ? target.level().getRandom() : RandomSource.create());
                if (rng.nextDouble() > hitChance) {
                    return false;
                }
            }
            // 体质 debuff_resist 缩短有害状态时长
            double resist = targetDebuffResist(target);
            if (resist > 0.0D) {
                duration = resistedDuration(duration, resist);
            }
        }

        MobEffectInstance existing = target.getEffect(effectOpt.get());
        MobEffectInstance next;
        if (existing != null) {
            int nextAmp = Math.max(existing.getAmplifier(), amplifier);
            int nextDur = Math.max(existing.getDuration(), duration);
            next = new MobEffectInstance(effectOpt.get(), nextDur, nextAmp, false, true, true);
        } else {
            next = new MobEffectInstance(effectOpt.get(), duration, amplifier, false, true, true);
        }
        return caster == null ? target.addEffect(next) : target.addEffect(next, caster);
    }

    static int resistedDuration(int requestedDuration, double resist) {
        int requested = Math.max(1, requestedDuration);
        if (resist <= 0.0D) {
            return requested;
        }
        double reduction = 1.0D - Mth.clamp(resist, 0.0D, 0.75D);
        return Mth.clamp((int) Math.round(requested * reduction), 1, requested);
    }

    public static boolean clearStatus(LivingEntity target, String statusId) {
        if (target == null || target.level().isClientSide) {
            return false;
        }
        return resolve(statusId).map(target::removeEffect).orElse(false);
    }

    public static boolean hasStatus(LivingEntity target, String statusId) {
        if (target == null) {
            return false;
        }
        return resolve(statusId).map(target::hasEffect).orElse(false);
    }

    public static double outgoingDamageMultiplier(LivingEntity target) {
        return target == null
                ? 1.0D
                : outgoingDamageMultiplier(target.getActiveEffects(), StatusRegistry::seekingEffectOf);
    }

    static double outgoingDamageMultiplierForEffects(Iterable<SeekingStatusEffect> effects) {
        return outgoingDamageMultiplier(effects, Function.identity());
    }

    public static double accuracyDelta(LivingEntity target) {
        return target == null
                ? 0.0D
                : accuracyDelta(target.getActiveEffects(), StatusRegistry::seekingEffectOf);
    }

    static double accuracyDeltaForEffects(Iterable<SeekingStatusEffect> effects) {
        return accuracyDelta(effects, Function.identity());
    }

    private static <T> double accuracyDelta(Iterable<T> activeStatuses,
                                            Function<T, SeekingStatusEffect> mapper) {
        if (activeStatuses == null) {
            return 0.0D;
        }
        double delta = 0.0D;
        for (T activeStatus : activeStatuses) {
            SeekingStatusEffect effect = mapper.apply(activeStatus);
            if (effect != null) {
                delta += effect.getAccuracyDelta();
            }
        }
        return delta;
    }

    private static <T> double outgoingDamageMultiplier(Iterable<T> activeStatuses,
                                                       Function<T, SeekingStatusEffect> mapper) {
        if (activeStatuses == null) {
            return 1.0D;
        }
        double multiplier = 1.0D;
        for (T activeStatus : activeStatuses) {
            SeekingStatusEffect effect = mapper.apply(activeStatus);
            if (effect != null) {
                multiplier *= effect.getOutgoingDamageMul();
            }
        }
        return multiplier;
    }

    public static boolean blocksTechnique(LivingEntity target) {
        return target != null && hasStatusFlag(target.getActiveEffects(), StatusRegistry::seekingEffectOf,
                SeekingStatusEffect::blocksTechnique);
    }

    static boolean blocksTechniqueForEffects(Iterable<SeekingStatusEffect> effects) {
        return hasStatusFlag(effects, Function.identity(), SeekingStatusEffect::blocksTechnique);
    }

    public static boolean hidesRealm(LivingEntity target) {
        return target != null && hasStatusFlag(target.getActiveEffects(), StatusRegistry::seekingEffectOf,
                SeekingStatusEffect::hidesRealm);
    }

    static boolean hidesRealmForEffects(Iterable<SeekingStatusEffect> effects) {
        return hasStatusFlag(effects, Function.identity(), SeekingStatusEffect::hidesRealm);
    }

    private static <T> boolean hasStatusFlag(Iterable<T> activeStatuses,
                                             Function<T, SeekingStatusEffect> mapper,
                                             Predicate<SeekingStatusEffect> predicate) {
        if (activeStatuses == null) {
            return false;
        }
        for (T activeStatus : activeStatuses) {
            SeekingStatusEffect effect = mapper.apply(activeStatus);
            if (effect != null && predicate.test(effect)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static SeekingStatusEffect seekingEffectOf(MobEffectInstance instance) {
        if (instance != null && instance.getEffect() instanceof SeekingStatusEffect effect) {
            return effect;
        }
        return null;
    }

    /**
     * 命中率 = clamp(0.05, 0.95, 0.70 + 0.08 * realmDelta - targetDebuffResist)
     */
    public static double computeHitChance(@Nullable LivingEntity caster, LivingEntity target) {
        int casterIndex = realmIndex(caster);
        int targetIndex = realmIndex(target);
        double realmDelta = casterIndex - targetIndex;
        double resist = targetDebuffResist(target);
        double chance = 0.70D + 0.08D * realmDelta - resist;
        return Mth.clamp(chance, 0.05D, 0.95D);
    }

    public static double targetDebuffResist(LivingEntity target) {
        if (!(target instanceof Player player)) {
            return 0.0D;
        }
        return CultivationHelper.get(player)
                .map(StatusRegistry::debuffResistOf)
                .orElse(0.0D);
    }

    private static double debuffResistOf(PlayerCultivation cultivation) {
        String constitutionId = cultivation.getConstitutionId();
        return ConstitutionCatalogService.builtin()
                .find(constitutionId)
                .map(ConstitutionCatalogService.ConstitutionEntry::debuffResist)
                .orElse(0.0D);
    }

    private static int realmIndex(@Nullable LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return Realm.MORTAL.ordinal();
        }
        return CultivationHelper.get(player)
                .map(c -> c.getRealm().ordinal())
                .orElse(Realm.MORTAL.ordinal());
    }

    public static String normalizeId(String statusId) {
        if (statusId == null) {
            return "";
        }
        return StatusCatalogService.builtin().find(statusId)
                .map(StatusCatalogService.StatusDefinition::id)
                .orElse(statusId.trim().toLowerCase(Locale.ROOT));
    }
}

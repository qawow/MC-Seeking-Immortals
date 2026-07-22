package com.xunxian.seekingimmortals.combat.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * 修仙短时战斗状态（M14）。
 * <p>与 M01 {@code ImmortalAffliction} 分离：本类仅承载可被 {@link StatusRegistry} 按字符串 id 引用的短时效果。</p>
 */
public class SeekingStatusEffect extends MobEffect {
    private static final int AMBIENT_VFX_INTERVAL_TICKS = 60;
    private final String statusId;
    private final double tickDamage;
    private final double tickHeal;
    private final int tickInterval;
    private final double movementMul;
    private final double accuracyDelta;
    private final double outgoingDamageMul;
    private final double defenseMul;
    private final boolean blocksTechnique;
    private final boolean hidesRealm;

    public SeekingStatusEffect(String statusId,
                               MobEffectCategory category,
                               int color,
                               double tickDamage,
                               double tickHeal,
                               int tickInterval,
                               double movementMul,
                               double accuracyDelta,
                               double outgoingDamageMul,
                               double defenseMul,
                               boolean blocksTechnique,
                               boolean hidesRealm) {
        super(category, color);
        this.statusId = statusId;
        this.tickDamage = tickDamage;
        this.tickHeal = tickHeal;
        this.tickInterval = Math.max(1, tickInterval);
        this.movementMul = movementMul;
        this.accuracyDelta = accuracyDelta;
        this.outgoingDamageMul = outgoingDamageMul;
        this.defenseMul = defenseMul;
        this.blocksTechnique = blocksTechnique;
        this.hidesRealm = hidesRealm;

        if (movementMul >= 0.0D && movementMul < 1.0D) {
            double amount = movementMul - 1.0D;
            this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                    UUID.nameUUIDFromBytes(("seeking_immortals:status:" + statusId + ":move").getBytes()).toString(),
                    amount,
                    AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
        if (defenseMul > 0.0D && defenseMul != 1.0D) {
            double amount = defenseMul - 1.0D;
            this.addAttributeModifier(Attributes.ARMOR,
                    UUID.nameUUIDFromBytes(("seeking_immortals:status:" + statusId + ":armor").getBytes()).toString(),
                    amount,
                    AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
    }

    public String getStatusId() {
        return statusId;
    }

    public double getOutgoingDamageMul() {
        return outgoingDamageMul <= 0.0D ? 1.0D : outgoingDamageMul;
    }

    public double getAccuracyDelta() {
        return accuracyDelta;
    }

    public double getDefenseMul() {
        return defenseMul <= 0.0D ? 1.0D : defenseMul;
    }

    public boolean blocksTechnique() {
        return blocksTechnique;
    }

    public boolean hidesRealm() {
        return hidesRealm;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        int amp = Math.max(0, amplifier);
        if (tickDamage > 0.0D) {
            float amount = (float) (tickDamage * (1.0D + amp * 0.35D));
            entity.hurt(entity.damageSources().magic(), amount);
        }
        if (tickHeal > 0.0D && entity.getHealth() < entity.getMaxHealth()) {
            entity.heal((float) (tickHeal * (1.0D + amp * 0.25D)));
        }
        StatusVfxService.emitPulse(entity, this, amp);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % (tickDamage > 0.0D || tickHeal > 0.0D
                ? tickInterval
                : AMBIENT_VFX_INTERVAL_TICKS) == 0;
    }
}

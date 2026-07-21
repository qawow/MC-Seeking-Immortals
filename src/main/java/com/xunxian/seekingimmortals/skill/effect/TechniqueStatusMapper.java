package com.xunxian.seekingimmortals.skill.effect;

import com.xunxian.seekingimmortals.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * 将术法元素家族映射到自定义 ModMobEffects 状态效果，完全替代原版药水效果。
 * 所有法术视觉效果使用自定义粒子和自定义状态，不依赖原版 MobEffects。
 */
public final class TechniqueStatusMapper {
    private TechniqueStatusMapper() {}

    public static class StatusPair {
        public final Supplier<MobEffect> primary;
        public final Supplier<MobEffect> secondary;
        public final Supplier<MobEffect> buffPrimary;
        public final Supplier<MobEffect> buffSecondary;

        public StatusPair(Supplier<MobEffect> primary, Supplier<MobEffect> secondary,
                         Supplier<MobEffect> buffPrimary, Supplier<MobEffect> buffSecondary) {
            this.primary = primary;
            this.secondary = secondary;
            this.buffPrimary = buffPrimary;
            this.buffSecondary = buffSecondary;
        }

        public MobEffect primaryDebuff() {
            return primary != null ? primary.get() : null;
        }

        public MobEffect secondaryDebuff() {
            return secondary != null ? secondary.get() : null;
        }

        public MobEffect primaryBuff() {
            return buffPrimary != null ? buffPrimary.get() : null;
        }

        public MobEffect secondaryBuff() {
            return buffSecondary != null ? buffSecondary.get() : null;
        }
    }

    public static StatusPair forFamily(TechniqueVfxPalette.Family family) {
        return switch (family) {
            case FIRE -> new StatusPair(
                    safeGet("burn"),
                    safeGet("qi_disorder"),
                    safeGet("berserk"),
                    safeGet("shield"));
            case WATER -> new StatusPair(
                    safeGet("frozen"),
                    safeGet("qi_disorder"),
                    safeGet("heal_hot"),
                    safeGet("shield"));
            case METAL -> new StatusPair(
                    safeGet("bleed"),
                    safeGet("qi_disorder"),
                    safeGet("sword_intent"),
                    safeGet("shield"));
            case WOOD -> new StatusPair(
                    safeGet("poison"),
                    safeGet("marrow_drain"),
                    safeGet("heal_hot"),
                    safeGet("shield"));
            case EARTH -> new StatusPair(
                    safeGet("stun"),
                    safeGet("qi_disorder"),
                    safeGet("shield"),
                    safeGet("heal_hot"));
            case WIND -> new StatusPair(
                    safeGet("qi_disorder"),
                    safeGet("frozen"),
                    safeGet("berserk"),
                    safeGet("heal_hot"));
            case ICE -> new StatusPair(
                    safeGet("frozen"),
                    safeGet("stun"),
                    safeGet("shield"),
                    safeGet("heal_hot"));
            case THUNDER -> new StatusPair(
                    safeGet("stun"),
                    safeGet("qi_disorder"),
                    safeGet("berserk"),
                    safeGet("sword_intent"));
            case LIGHT -> new StatusPair(
                    safeGet("fear"),
                    safeGet("illusion"),
                    safeGet("shield"),
                    safeGet("heal_hot"));
            case DARK -> new StatusPair(
                    safeGet("soul_wound"),
                    safeGet("fear"),
                    safeGet("conceal_qi"),
                    safeGet("berserk"));
            case SOUL -> new StatusPair(
                    safeGet("soul_shock"),
                    safeGet("soul_wound"),
                    safeGet("conceal_qi"),
                    safeGet("shield"));
            case BLOOD -> new StatusPair(
                    safeGet("marrow_drain"),
                    safeGet("bleed"),
                    safeGet("berserk"),
                    safeGet("heal_hot"));
            case VOID -> new StatusPair(
                    safeGet("array_bind"),
                    safeGet("illusion"),
                    safeGet("conceal_qi"),
                    safeGet("shield"));
            case ILLUSION -> new StatusPair(
                    safeGet("illusion"),
                    safeGet("fear"),
                    safeGet("conceal_qi"),
                    safeGet("heal_hot"));
            case NEUTRAL -> new StatusPair(
                    safeGet("qi_disorder"),
                    safeGet("foundation_unstable"),
                    safeGet("shield"),
                    safeGet("heal_hot"));
        };
    }

    private static Supplier<MobEffect> safeGet(String statusId) {
        RegistryObject<MobEffect> obj = ModMobEffects.get(statusId);
        return obj != null ? obj : () -> null;
    }
}

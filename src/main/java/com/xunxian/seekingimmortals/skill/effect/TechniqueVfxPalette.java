package com.xunxian.seekingimmortals.skill.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Locale;

/**
 * Shared element → particle/sound/effect palette for generic technique runtime.
 * Keeps authored element/attribute strings visually distinct without per-spell bespoke classes.
 */
public final class TechniqueVfxPalette {
    private TechniqueVfxPalette() {}

    public enum Family {
        FIRE, WATER, METAL, WOOD, EARTH, WIND, ICE, THUNDER,
        LIGHT, DARK, SOUL, BLOOD, VOID, ILLUSION, NEUTRAL
    }

    public record Profile(
            Family family,
            DustParticleOptions core,
            DustParticleOptions edge,
            ParticleOptions accent,
            SoundEvent castSound,
            float castPitch,
            SoundEvent impactSound,
            float impactPitch,
            MobEffect primaryDebuff,
            MobEffect secondaryDebuff,
            MobEffect buffPrimary,
            MobEffect buffSecondary
    ) {
        public void burst(ServerLevel level, Vec3 center, double radius, int density) {
            if (level == null || center == null) {
                return;
            }
            int count = Math.max(8, Math.min(96, density));
            double spread = Math.max(0.35D, radius);
            level.sendParticles(core, center.x, center.y + 0.45D, center.z,
                    count / 2, spread * 0.35D, 0.35D, spread * 0.35D, 0.02D);
            level.sendParticles(edge, center.x, center.y + 0.55D, center.z,
                    count / 3, spread * 0.45D, 0.4D, spread * 0.45D, 0.01D);
            level.sendParticles(accent, center.x, center.y + 0.35D, center.z,
                    Math.max(4, count / 6), spread * 0.25D, 0.3D, spread * 0.25D, 0.02D);
        }

        public void castAt(ServerLevel level, LivingEntity caster) {
            if (level == null || caster == null) {
                return;
            }
            Vec3 eye = caster.getEyePosition();
            Vec3 look = caster.getLookAngle().normalize();
            Vec3 origin = eye.add(look.scale(0.55D));
            level.sendParticles(core, origin.x, origin.y, origin.z, 10, 0.08D, 0.08D, 0.08D, 0.01D);
            level.sendParticles(accent, origin.x, origin.y, origin.z, 4, 0.05D, 0.05D, 0.05D, 0.01D);
            level.playSound(null, caster.blockPosition(), castSound, SoundSource.PLAYERS, 0.55F, castPitch);
        }

        public void impactAt(ServerLevel level, Vec3 center) {
            if (level == null || center == null) {
                return;
            }
            level.sendParticles(core, center.x, center.y + 0.2D, center.z, 18, 0.25D, 0.2D, 0.25D, 0.02D);
            level.sendParticles(accent, center.x, center.y + 0.25D, center.z, 8, 0.15D, 0.15D, 0.15D, 0.01D);
            level.playSound(null, center.x, center.y, center.z, impactSound, SoundSource.PLAYERS, 0.7F, impactPitch);
        }
    }

    public static Family familyOf(String element) {
        String key = normalize(element);
        if (key.isBlank() || "neutral".equals(key) || "elemental".equals(key)) {
            return Family.NEUTRAL;
        }
        if (contains(key, "fire", "flame", "lava", "yang")) {
            return Family.FIRE;
        }
        if (contains(key, "water", "rain", "mist", "ocean")) {
            return Family.WATER;
        }
        if (contains(key, "metal", "gold", "sword", "blade")) {
            return Family.METAL;
        }
        if (contains(key, "wood", "plant", "poison", "vine")) {
            return Family.WOOD;
        }
        if (contains(key, "earth", "sand", "stone", "rock")) {
            return Family.EARTH;
        }
        if (contains(key, "wind", "air", "gale", "cloud")) {
            return Family.WIND;
        }
        if (contains(key, "ice", "frost", "snow", "cold")) {
            return Family.ICE;
        }
        if (contains(key, "thunder", "lightning", "bolt")) {
            return Family.THUNDER;
        }
        if (contains(key, "light", "holy", "buddha", "radiant")) {
            return Family.LIGHT;
        }
        if (contains(key, "dark", "shadow", "night", "yin") && !contains(key, "yang")) {
            return Family.DARK;
        }
        if (contains(key, "soul", "spirit", "ghost", "wraith")) {
            return Family.SOUL;
        }
        if (contains(key, "blood", "demon", "demonic", "curse", "flesh")) {
            return Family.BLOOD;
        }
        if (contains(key, "void", "space", "spatial", "time", "rift")) {
            return Family.VOID;
        }
        if (contains(key, "illusion", "mirage", "dream", "phantasm")) {
            return Family.ILLUSION;
        }
        return Family.NEUTRAL;
    }

    public static Profile profile(String element) {
        return switch (familyOf(element)) {
            case FIRE -> profile(
                    Family.FIRE,
                    dust(1.00F, 0.28F, 0.05F, 0.95F), dust(1.00F, 0.78F, 0.18F, 0.55F),
                    ParticleTypes.FLAME, SoundEvents.BLAZE_SHOOT, 1.15F,
                    SoundEvents.GENERIC_EXPLODE, 1.25F,
                    MobEffects.WEAKNESS, MobEffects.MOVEMENT_SLOWDOWN,
                    MobEffects.DAMAGE_BOOST, MobEffects.FIRE_RESISTANCE);
            case WATER -> profile(
                    Family.WATER,
                    dust(0.20F, 0.55F, 1.00F, 0.72F), dust(0.78F, 0.92F, 1.00F, 0.40F),
                    ParticleTypes.SPLASH, SoundEvents.BUCKET_EMPTY, 1.25F,
                    SoundEvents.TRIDENT_HIT, 1.15F,
                    MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS,
                    MobEffects.DOLPHINS_GRACE, MobEffects.REGENERATION);
            case METAL -> profile(
                    Family.METAL,
                    dust(0.78F, 0.84F, 0.96F, 0.80F), dust(1.00F, 1.00F, 1.00F, 0.42F),
                    ParticleTypes.CRIT, SoundEvents.ANVIL_LAND, 1.55F,
                    SoundEvents.ARROW_HIT_PLAYER, 1.40F,
                    MobEffects.WEAKNESS, MobEffects.DIG_SLOWDOWN,
                    MobEffects.DAMAGE_BOOST, MobEffects.DAMAGE_RESISTANCE);
            case WOOD -> profile(
                    Family.WOOD,
                    dust(0.22F, 0.72F, 0.28F, 0.78F), dust(0.62F, 0.95F, 0.48F, 0.42F),
                    ParticleTypes.HAPPY_VILLAGER, SoundEvents.GRASS_BREAK, 1.20F,
                    SoundEvents.AZALEA_LEAVES_BREAK, 1.05F,
                    MobEffects.POISON, MobEffects.MOVEMENT_SLOWDOWN,
                    MobEffects.REGENERATION, MobEffects.ABSORPTION);
            case EARTH -> profile(
                    Family.EARTH,
                    dust(0.62F, 0.42F, 0.18F, 0.82F), dust(0.90F, 0.74F, 0.38F, 0.48F),
                    ParticleTypes.CLOUD, SoundEvents.STONE_BREAK, 0.85F,
                    SoundEvents.GRAVEL_BREAK, 0.90F,
                    MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SLOWDOWN,
                    MobEffects.DAMAGE_RESISTANCE, MobEffects.ABSORPTION);
            case WIND -> profile(
                    Family.WIND,
                    dust(0.70F, 0.95F, 0.86F, 0.68F), dust(0.92F, 1.00F, 0.96F, 0.36F),
                    ParticleTypes.CLOUD, SoundEvents.TRIDENT_RIPTIDE_1, 1.55F,
                    SoundEvents.ELYTRA_FLYING, 1.35F,
                    MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS,
                    MobEffects.MOVEMENT_SPEED, MobEffects.JUMP);
            case ICE -> profile(
                    Family.ICE,
                    dust(0.52F, 0.86F, 1.00F, 0.72F), dust(0.92F, 0.98F, 1.00F, 0.40F),
                    ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK, 1.45F,
                    SoundEvents.PLAYER_HURT_FREEZE, 1.20F,
                    MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SLOWDOWN,
                    MobEffects.DAMAGE_RESISTANCE, MobEffects.ABSORPTION);
            case THUNDER -> profile(
                    Family.THUNDER,
                    dust(0.70F, 0.90F, 1.00F, 0.82F), dust(0.30F, 0.48F, 1.00F, 0.48F),
                    ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.65F,
                    SoundEvents.TRIDENT_THUNDER, 1.40F,
                    MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS,
                    MobEffects.DAMAGE_BOOST, MobEffects.MOVEMENT_SPEED);
            case LIGHT -> profile(
                    Family.LIGHT,
                    dust(1.00F, 0.90F, 0.35F, 0.80F), dust(1.00F, 1.00F, 0.88F, 0.42F),
                    ParticleTypes.END_ROD, SoundEvents.AMETHYST_BLOCK_CHIME, 1.45F,
                    SoundEvents.BEACON_ACTIVATE, 1.25F,
                    MobEffects.GLOWING, MobEffects.WEAKNESS,
                    MobEffects.DAMAGE_RESISTANCE, MobEffects.REGENERATION);
            case DARK -> profile(
                    Family.DARK,
                    dust(0.18F, 0.04F, 0.28F, 0.82F), dust(0.58F, 0.12F, 0.82F, 0.46F),
                    ParticleTypes.SMOKE, SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.95F,
                    SoundEvents.WARDEN_HEARTBEAT, 0.90F,
                    MobEffects.WITHER, MobEffects.BLINDNESS,
                    MobEffects.DAMAGE_RESISTANCE, MobEffects.NIGHT_VISION);
            case SOUL -> profile(
                    Family.SOUL,
                    dust(0.20F, 0.82F, 0.86F, 0.78F), dust(0.08F, 0.42F, 0.48F, 0.44F),
                    ParticleTypes.SOUL, SoundEvents.SOUL_ESCAPE, 1.10F,
                    SoundEvents.WARDEN_HEARTBEAT, 1.00F,
                    MobEffects.WITHER, MobEffects.WEAKNESS,
                    MobEffects.NIGHT_VISION, MobEffects.ABSORPTION);
            case BLOOD -> profile(
                    Family.BLOOD,
                    dust(0.72F, 0.05F, 0.08F, 0.88F), dust(0.42F, 0.02F, 0.04F, 0.50F),
                    ParticleTypes.CRIMSON_SPORE, SoundEvents.WARDEN_HEARTBEAT, 0.85F,
                    SoundEvents.RAVAGER_ROAR, 0.95F,
                    MobEffects.WITHER, MobEffects.HUNGER,
                    MobEffects.DAMAGE_BOOST, MobEffects.ABSORPTION);
            case VOID -> profile(
                    Family.VOID,
                    dust(0.34F, 0.08F, 0.58F, 0.86F), dust(0.72F, 0.42F, 1.00F, 0.48F),
                    ParticleTypes.PORTAL, SoundEvents.ENDERMAN_TELEPORT, 0.80F,
                    SoundEvents.END_PORTAL_FRAME_FILL, 0.90F,
                    MobEffects.LEVITATION, MobEffects.BLINDNESS,
                    MobEffects.SLOW_FALLING, MobEffects.NIGHT_VISION);
            case ILLUSION -> profile(
                    Family.ILLUSION,
                    dust(0.78F, 0.42F, 0.96F, 0.72F), dust(0.96F, 0.78F, 1.00F, 0.40F),
                    ParticleTypes.ENCHANT, SoundEvents.ILLUSIONER_CAST_SPELL, 1.25F,
                    SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.15F,
                    MobEffects.CONFUSION, MobEffects.BLINDNESS,
                    MobEffects.INVISIBILITY, MobEffects.MOVEMENT_SPEED);
            case NEUTRAL -> profile(
                    Family.NEUTRAL,
                    dust(0.72F, 0.62F, 0.92F, 0.70F), dust(0.92F, 0.88F, 1.00F, 0.38F),
                    ParticleTypes.ENCHANT, SoundEvents.ENCHANTMENT_TABLE_USE, 1.15F,
                    SoundEvents.AMETHYST_BLOCK_CHIME, 1.05F,
                    MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS,
                    MobEffects.DAMAGE_RESISTANCE, MobEffects.ABSORPTION);
        };
    }

    private static Profile profile(Family family,
                                   DustParticleOptions core, DustParticleOptions edge, ParticleOptions accent,
                                   SoundEvent castSound, float castPitch,
                                   SoundEvent impactSound, float impactPitch,
                                   MobEffect primaryDebuff, MobEffect secondaryDebuff,
                                   MobEffect buffPrimary, MobEffect buffSecondary) {
        return new Profile(family, core, edge, accent, castSound, castPitch, impactSound, impactPitch,
                primaryDebuff, secondaryDebuff, buffPrimary, buffSecondary);
    }

    private static DustParticleOptions dust(float r, float g, float b, float scale) {
        return new DustParticleOptions(new Vector3f(r, g, b), scale);
    }

    private static boolean contains(String value, String... parts) {
        for (String part : parts) {
            if (value.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

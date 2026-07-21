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
            float impactPitch
    ) {
        public MobEffect primaryDebuff() {
            return TechniqueStatusMapper.forFamily(family).primaryDebuff();
        }

        public MobEffect secondaryDebuff() {
            return TechniqueStatusMapper.forFamily(family).secondaryDebuff();
        }

        public MobEffect buffPrimary() {
            return TechniqueStatusMapper.forFamily(family).primaryBuff();
        }

        public MobEffect buffSecondary() {
            return TechniqueStatusMapper.forFamily(family).secondaryBuff();
        }
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
            Vec3 look = caster.getLookAngle();
            look = look.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : look.normalize();
            Vec3 origin = eye.add(look.scale(0.55D));
            level.sendParticles(core, origin.x, origin.y, origin.z, 10, 0.08D, 0.08D, 0.08D, 0.01D);
            level.sendParticles(accent, origin.x, origin.y, origin.z, 4, 0.05D, 0.05D, 0.05D, 0.01D);
            ring(level, caster.position().add(0.0D, 0.12D, 0.0D), 0.72D, 16, edge);
            level.playSound(null, caster.blockPosition(), castSound, SoundSource.PLAYERS, 0.55F, castPitch);
        }

        public void path(ServerLevel level, Vec3 start, Vec3 end, int density) {
            if (level == null || start == null || end == null) {
                return;
            }
            int points = Math.max(4, Math.min(64, density));
            Vec3 step = end.subtract(start).scale(1.0D / points);
            for (int i = 0; i <= points; i++) {
                Vec3 point = start.add(step.scale(i));
                ParticleOptions particle = i % 3 == 0 ? edge : core;
                level.sendParticles(particle, point.x, point.y, point.z,
                        1, 0.025D, 0.025D, 0.025D, 0.0D);
                if (i % 6 == 0) {
                    level.sendParticles(accent, point.x, point.y, point.z,
                            1, 0.015D, 0.015D, 0.015D, 0.0D);
                }
            }
        }

        public void trailAt(ServerLevel level, Vec3 center, Vec3 movement) {
            if (level == null || center == null) {
                return;
            }
            Vec3 motion = movement == null ? Vec3.ZERO : movement;
            if (motion.lengthSqr() < 0.001D) {
                level.sendParticles(core, center.x, center.y, center.z,
                        2, 0.035D, 0.035D, 0.035D, 0.0D);
                return;
            }
            double length = Math.max(0.35D, Math.min(1.15D, motion.length()));
            Vec3 tail = center.subtract(motion.normalize().scale(length));
            Vec3 middle = tail.lerp(center, 0.5D);
            level.sendParticles(core, center.x, center.y, center.z,
                    2, 0.045D, 0.045D, 0.045D, 0.0D);
            level.sendParticles(edge, middle.x, middle.y, middle.z,
                    2, length * 0.18D, length * 0.08D, length * 0.18D, 0.0D);
        }

        public void auraAt(ServerLevel level, LivingEntity entity, double radius, int density) {
            if (level == null || entity == null) {
                return;
            }
            int points = Math.max(12, Math.min(64, density));
            double safeRadius = Math.max(0.45D, radius);
            Vec3 base = entity.position().add(0.0D, 0.12D, 0.0D);
            ring(level, base, safeRadius, points, core);
            for (int i = 0; i < Math.max(8, points / 2); i++) {
                double angle = i * 2.399963229728653D;
                double height = 0.18D + entity.getBbHeight() * (i % 9) / 9.0D;
                double spiralRadius = safeRadius * (0.45D + (i % 4) * 0.12D);
                level.sendParticles(i % 4 == 0 ? accent : edge,
                        entity.getX() + Math.cos(angle) * spiralRadius,
                        entity.getY() + height,
                        entity.getZ() + Math.sin(angle) * spiralRadius,
                        1, 0.02D, 0.025D, 0.02D, 0.0D);
            }
        }

        public void scanAt(ServerLevel level, Vec3 center, double radius, int density) {
            if (level == null || center == null) {
                return;
            }
            double safeRadius = Math.max(1.0D, radius);
            int points = Math.max(20, Math.min(64, density));
            level.sendParticles(core, center.x, center.y + 0.12D, center.z,
                    points / 2, safeRadius * 0.22D, 0.025D, safeRadius * 0.22D, 0.0D);
            level.sendParticles(edge, center.x, center.y + 0.18D, center.z,
                    points, safeRadius * 0.48D, 0.035D, safeRadius * 0.48D, 0.0D);
            level.sendParticles(core, center.x, center.y + 0.24D, center.z,
                    points, safeRadius * 0.70D, 0.045D, safeRadius * 0.70D, 0.0D);
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2.0D * i / 8.0D;
                level.sendParticles(edge,
                        center.x + Math.cos(angle) * safeRadius,
                        center.y + 0.24D,
                        center.z + Math.sin(angle) * safeRadius,
                        2, 0.04D, 0.02D, 0.04D, 0.0D);
            }
            level.sendParticles(accent, center.x, center.y + 0.65D, center.z,
                    Math.max(8, points / 6), safeRadius * 0.2D, 0.3D, safeRadius * 0.2D, 0.01D);
        }

        public void beamAt(ServerLevel level, Vec3 start, Vec3 end, double radius) {
            if (level == null || start == null || end == null || start.distanceToSqr(end) < 0.001D) {
                return;
            }
            int points = Math.max(8, Math.min(28, (int) Math.ceil(start.distanceTo(end) * 1.4D)));
            Vec3 step = end.subtract(start).scale(1.0D / points);
            double spread = Math.max(0.025D, Math.min(0.18D, radius * 0.12D));
            for (int i = 0; i <= points; i++) {
                Vec3 point = start.add(step.scale(i));
                level.sendParticles(i % 4 == 0 ? edge : core, point.x, point.y, point.z,
                        i % 4 == 0 ? 2 : 1, spread, spread, spread, 0.0D);
            }
            level.sendParticles(accent, end.x, end.y, end.z,
                    10, Math.max(0.12D, radius * 0.35D), 0.15D, Math.max(0.12D, radius * 0.35D), 0.01D);
        }

        public void coneAt(ServerLevel level, Vec3 start, Vec3 direction, double range, double endRadius) {
            if (level == null || start == null || direction == null || direction.lengthSqr() < 0.001D) {
                return;
            }
            Vec3 normalized = direction.normalize();
            double safeRange = Math.max(1.0D, range);
            double safeRadius = Math.max(0.75D, endRadius);
            for (int slice = 1; slice <= 6; slice++) {
                double progress = slice / 6.0D;
                Vec3 center = start.add(normalized.scale(safeRange * progress));
                double spread = safeRadius * progress;
                level.sendParticles(slice % 2 == 0 ? edge : core,
                        center.x, center.y, center.z,
                        4 + slice * 2, spread * 0.45D, spread * 0.34D, spread * 0.45D, 0.01D);
            }
            Vec3 end = start.add(normalized.scale(safeRange));
            level.sendParticles(accent, end.x, end.y, end.z,
                    12, safeRadius * 0.55D, safeRadius * 0.38D, safeRadius * 0.55D, 0.015D);
        }

        public void impactAt(ServerLevel level, Vec3 center) {
            if (level == null || center == null) {
                return;
            }
            level.sendParticles(core, center.x, center.y + 0.2D, center.z, 18, 0.25D, 0.2D, 0.25D, 0.02D);
            level.sendParticles(accent, center.x, center.y + 0.25D, center.z, 8, 0.15D, 0.15D, 0.15D, 0.01D);
            ring(level, center.add(0.0D, 0.08D, 0.0D), 0.72D, 20, edge);
            level.playSound(null, center.x, center.y, center.z, impactSound, SoundSource.PLAYERS, 0.7F, impactPitch);
        }

        private void ring(ServerLevel level, Vec3 center, double radius, int points, ParticleOptions particle) {
            int count = Math.max(8, Math.min(96, points));
            for (int i = 0; i < count; i++) {
                double angle = Math.PI * 2.0D * i / count;
                level.sendParticles(particle,
                        center.x + Math.cos(angle) * radius,
                        center.y,
                        center.z + Math.sin(angle) * radius,
                        1, 0.012D, 0.012D, 0.012D, 0.0D);
            }
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
                    SoundEvents.GENERIC_EXPLODE, 1.25F);
            case WATER -> profile(
                    Family.WATER,
                    dust(0.20F, 0.55F, 1.00F, 0.72F), dust(0.78F, 0.92F, 1.00F, 0.40F),
                    ParticleTypes.SPLASH, SoundEvents.BUCKET_EMPTY, 1.25F,
                    SoundEvents.TRIDENT_HIT, 1.15F);
            case METAL -> profile(
                    Family.METAL,
                    dust(0.78F, 0.84F, 0.96F, 0.80F), dust(1.00F, 1.00F, 1.00F, 0.42F),
                    ParticleTypes.CRIT, SoundEvents.ANVIL_LAND, 1.55F,
                    SoundEvents.ARROW_HIT_PLAYER, 1.40F);
            case WOOD -> profile(
                    Family.WOOD,
                    dust(0.22F, 0.72F, 0.28F, 0.78F), dust(0.62F, 0.95F, 0.48F, 0.42F),
                    ParticleTypes.HAPPY_VILLAGER, SoundEvents.GRASS_BREAK, 1.20F,
                    SoundEvents.AZALEA_LEAVES_BREAK, 1.05F);
            case EARTH -> profile(
                    Family.EARTH,
                    dust(0.62F, 0.42F, 0.18F, 0.82F), dust(0.90F, 0.74F, 0.38F, 0.48F),
                    ParticleTypes.CLOUD, SoundEvents.STONE_BREAK, 0.85F,
                    SoundEvents.GRAVEL_BREAK, 0.90F);
            case WIND -> profile(
                    Family.WIND,
                    dust(0.70F, 0.95F, 0.86F, 0.68F), dust(0.92F, 1.00F, 0.96F, 0.36F),
                    ParticleTypes.CLOUD, SoundEvents.TRIDENT_RIPTIDE_1, 1.55F,
                    SoundEvents.ELYTRA_FLYING, 1.35F);
            case ICE -> profile(
                    Family.ICE,
                    dust(0.52F, 0.86F, 1.00F, 0.72F), dust(0.92F, 0.98F, 1.00F, 0.40F),
                    ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK, 1.45F,
                    SoundEvents.PLAYER_HURT_FREEZE, 1.20F);
            case THUNDER -> profile(
                    Family.THUNDER,
                    dust(0.70F, 0.90F, 1.00F, 0.82F), dust(0.30F, 0.48F, 1.00F, 0.48F),
                    ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.65F,
                    SoundEvents.TRIDENT_THUNDER, 1.40F);
            case LIGHT -> profile(
                    Family.LIGHT,
                    dust(1.00F, 0.90F, 0.35F, 0.80F), dust(1.00F, 1.00F, 0.88F, 0.42F),
                    ParticleTypes.END_ROD, SoundEvents.AMETHYST_BLOCK_CHIME, 1.45F,
                    SoundEvents.BEACON_ACTIVATE, 1.25F);
            case DARK -> profile(
                    Family.DARK,
                    dust(0.18F, 0.04F, 0.28F, 0.82F), dust(0.58F, 0.12F, 0.82F, 0.46F),
                    ParticleTypes.SMOKE, SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.95F,
                    SoundEvents.WARDEN_HEARTBEAT, 0.90F);
            case SOUL -> profile(
                    Family.SOUL,
                    dust(0.20F, 0.82F, 0.86F, 0.78F), dust(0.08F, 0.42F, 0.48F, 0.44F),
                    ParticleTypes.SOUL, SoundEvents.SOUL_ESCAPE, 1.10F,
                    SoundEvents.WARDEN_HEARTBEAT, 1.00F);
            case BLOOD -> profile(
                    Family.BLOOD,
                    dust(0.72F, 0.05F, 0.08F, 0.88F), dust(0.42F, 0.02F, 0.04F, 0.50F),
                    ParticleTypes.CRIMSON_SPORE, SoundEvents.WARDEN_HEARTBEAT, 0.85F,
                    SoundEvents.RAVAGER_ROAR, 0.95F);
            case VOID -> profile(
                    Family.VOID,
                    dust(0.34F, 0.08F, 0.58F, 0.86F), dust(0.72F, 0.42F, 1.00F, 0.48F),
                    ParticleTypes.PORTAL, SoundEvents.ENDERMAN_TELEPORT, 0.80F,
                    SoundEvents.END_PORTAL_FRAME_FILL, 0.90F);
            case ILLUSION -> profile(
                    Family.ILLUSION,
                    dust(0.78F, 0.42F, 0.96F, 0.72F), dust(0.96F, 0.78F, 1.00F, 0.40F),
                    ParticleTypes.ENCHANT, SoundEvents.ILLUSIONER_CAST_SPELL, 1.25F,
                    SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.15F);
            case NEUTRAL -> profile(
                    Family.NEUTRAL,
                    dust(0.72F, 0.62F, 0.92F, 0.70F), dust(0.92F, 0.88F, 1.00F, 0.38F),
                    ParticleTypes.ENCHANT, SoundEvents.ENCHANTMENT_TABLE_USE, 1.15F,
                    SoundEvents.AMETHYST_BLOCK_CHIME, 1.05F);
        };
    }

    private static Profile profile(Family family,
                                   DustParticleOptions core, DustParticleOptions edge, ParticleOptions accent,
                                   SoundEvent castSound, float castPitch,
                                   SoundEvent impactSound, float impactPitch) {
        return new Profile(family, core, edge, accent, castSound, castPitch, impactSound, impactPitch);
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

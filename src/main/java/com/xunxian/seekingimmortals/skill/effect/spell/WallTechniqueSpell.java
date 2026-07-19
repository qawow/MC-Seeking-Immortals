package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.block.EarthWallBlock;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Dedicated wall family: temporary barrier along the caster look path + path damage/slow.
 */
public class WallTechniqueSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final String element;
    private final String successKey;

    public WallTechniqueSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                              String element, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = Math.max(4.0D, range);
        this.radius = Math.max(1.5D, radius);
        this.element = element == null ? "earth" : element;
        this.successKey = successKey == null || successKey.isBlank()
                ? "message.seeking_immortals.spell.generic_wall.success"
                : successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        Direction facing = player.getDirection();
        Direction side = facing.getClockWise();
        BlockPos origin = player.blockPosition().relative(facing);
        int length = Math.min(9, Math.max(4, (int) Math.round(radius * 2.0D) + skill.getLevel() / 4));
        int height = Math.min(5, 2 + skill.getLevel() / 3);
        int halfWidth = Math.min(2, Math.max(0, (int) Math.floor(radius / 3.0D)));

        BlockState wall = ModBlocks.EARTH_WALL.get().defaultBlockState();
        Set<BlockPos> placed = new HashSet<>();
        int blocks = 0;
        for (int i = 0; i < length; i++) {
            BlockPos column = origin.relative(facing, i);
            for (int w = -halfWidth; w <= halfWidth; w++) {
                BlockPos base = column.relative(side, w);
                for (int y = 0; y < height; y++) {
                    BlockPos pos = base.above(y);
                    if (!level.getBlockState(pos).isAir()) {
                        continue;
                    }
                    level.setBlock(pos, wall, 3);
                    level.scheduleTick(pos, ModBlocks.EARTH_WALL.get(), EarthWallBlock.REMOVAL_TICKS);
                    placed.add(pos.immutable());
                    blocks++;
                }
            }
        }

        double damage = Math.max(4.0D, calculateDamage(skill.getLevel(), skill.getProficiency()));
        AABB scan = player.getBoundingBox()
                .expandTowards(facing.getStepX() * length, height, facing.getStepZ() * length)
                .inflate(radius * 0.45D + halfWidth, 0.5D, radius * 0.45D + halfWidth);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, scan,
                entity -> canAffect(player, entity));
        int hitCount = 0;
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().indirectMagic(player, player), (float) damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    80 + skill.getLevel() * 4, 2, false, true));
            if (isEarthy()) {
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,
                        70 + skill.getLevel() * 3, 1, false, true));
            } else if (isIcy()) {
                target.setTicksFrozen(Math.min(200, target.getTicksFrozen() + 60));
            } else if (isFirey()) {
                target.setSecondsOnFire(3);
            }
            hitCount++;
        }

        spawnVisual(level, player.position().add(facing.getStepX() * 2.0D, 0.4D, facing.getStepZ() * 2.0D));
        level.playSound(null, player.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 0.85F, 0.75F);
        player.displayClientMessage(Component.translatable(successKey, blocks, hitCount), true);
        return true;
    }

    private void spawnVisual(ServerLevel level, Vec3 center) {
        DustParticleOptions dust = isIcy()
                ? new DustParticleOptions(new Vector3f(0.70F, 0.90F, 1.00F), 0.70F)
                : isFirey()
                ? new DustParticleOptions(new Vector3f(1.00F, 0.45F, 0.12F), 0.70F)
                : new DustParticleOptions(new Vector3f(0.72F, 0.55F, 0.28F), 0.78F);
        level.sendParticles(dust, center.x, center.y + 0.8D, center.z, 48, radius * 0.35D, 0.8D, radius * 0.35D, 0.02D);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.2D, center.z, 18, 0.6D, 0.2D, 0.6D, 0.02D);
    }

    private boolean isEarthy() {
        String e = element.toLowerCase(Locale.ROOT);
        return e.contains("earth") || e.contains("metal") || e.contains("rock");
    }

    private boolean isIcy() {
        String e = element.toLowerCase(Locale.ROOT);
        return e.contains("ice") || e.contains("water") || e.contains("frost");
    }

    private boolean isFirey() {
        String e = element.toLowerCase(Locale.ROOT);
        return e.contains("fire") || e.contains("lava") || e.contains("flame");
    }
}

package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

public class DetectionSpell extends SpellEffect {
    private static final int ENTITY_HIGHLIGHT_TICKS = 10 * 20;
    private static final int MAX_ENTITY_HIGHLIGHTS = 64;
    private static final int MAX_BLOCK_MATCHES = 32;

    public DetectionSpell() {
        super(5, 200, 0);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        double baseRange = Math.max(8.0D, cultivation.getDivSense());
        // Wave492: divine-sense expansion multiplies detection range.
        double multi = com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseExpansionPassive
                .senseRangeMultiplier(player);
        double range = baseRange * multi;
        AABB area = new AABB(player.blockPosition()).inflate(range);

        if (context.getLevel() instanceof ServerLevel serverLevel) {
            List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != player && entity.isAlive() && !entity.isSpectator());
            entities.sort(Comparator.comparingDouble(entity -> player.distanceToSqr(entity)));
            int highlightedEntities = highlightDetectedEntities(serverLevel, entities);
            int highlightedBlocks = 0;
            int blockRange = Math.min(16, (int)Math.ceil(range));
            int yRange = Math.min(8, blockRange);
            BlockPos origin = player.blockPosition();
            outer:
            for (int x = -blockRange; x <= blockRange; x++) {
                for (int y = -yRange; y <= yRange; y++) {
                    for (int z = -blockRange; z <= blockRange; z++) {
                        if (x * x + y * y + z * z > blockRange * blockRange) continue;
                        BlockPos pos = origin.offset(x, y, z);
                        if (!serverLevel.isLoaded(pos)) continue;
                        BlockState state = serverLevel.getBlockState(pos);
                        if (state.is(Blocks.AIR)) continue;
                        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                        if (path.contains("spirit") || path.contains("ore") || path.contains("grass") || path.contains("mushroom")) {
                            if (highlightedBlocks < MAX_BLOCK_MATCHES) {
                                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                        pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D,
                                        1, 0.25D, 0.25D, 0.25D, 0.01D);
                            }
                            if (++highlightedBlocks >= MAX_BLOCK_MATCHES) break outer;
                        }
                    }
                }
            }
            player.displayClientMessage(
                    Component.literal("神识探测：发现" + entities.size() + "个生灵，已高亮" + highlightedEntities + "个，" + highlightedBlocks + "处灵物波动"),
                    true
            );
            return true;
        }

        player.displayClientMessage(
            Component.literal("神识探测：未能展开探查"),
            true
        );

        return true;
    }

    private static int highlightDetectedEntities(ServerLevel level, List<LivingEntity> entities) {
        int highlighted = 0;
        for (LivingEntity entity : entities) {
            if (highlighted >= MAX_ENTITY_HIGHLIGHTS) break;
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, ENTITY_HIGHLIGHT_TICKS, 0, false, true));
            level.sendParticles(ParticleTypes.END_ROD,
                    entity.getX(), entity.getY() + entity.getBbHeight() / 2.0D, entity.getZ(),
                    8, 0.35D, 0.45D, 0.35D, 0.025D);
            highlighted++;
        }
        return highlighted;
    }
}

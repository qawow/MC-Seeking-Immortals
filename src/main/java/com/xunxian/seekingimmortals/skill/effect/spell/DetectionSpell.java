package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class DetectionSpell extends SpellEffect {
    public DetectionSpell() {
        super(5, 200, 0);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        double range = Math.max(8.0D, cultivation.getDivSense());
        AABB area = new AABB(player.blockPosition()).inflate(range);
        List<LivingEntity> entities = context.getLevel().getEntitiesOfClass(LivingEntity.class, area, e -> e != player);

        if (context.getLevel() instanceof ServerLevel serverLevel) {
            for (LivingEntity entity : entities) {
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, true));
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                    5, 0.3, 0.3, 0.3, 0.02);
            }
            int highlightedBlocks = 0;
            int maxBlockMatches = 32;
            int blockRange = Math.min(16, (int)Math.ceil(range));
            int yRange = Math.min(8, blockRange);
            net.minecraft.core.BlockPos origin = player.blockPosition();
            outer:
            for (int x = -blockRange; x <= blockRange; x++) {
                for (int y = -yRange; y <= yRange; y++) {
                    for (int z = -blockRange; z <= blockRange; z++) {
                        if (x * x + y * y + z * z > blockRange * blockRange) continue;
                        net.minecraft.core.BlockPos pos = origin.offset(x, y, z);
                        if (!serverLevel.isLoaded(pos)) continue;
                        BlockState state = serverLevel.getBlockState(pos);
                        if (state.is(Blocks.AIR)) continue;
                        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                        if (path.contains("spirit") || path.contains("ore") || path.contains("grass") || path.contains("mushroom")) {
                            if (highlightedBlocks < maxBlockMatches) {
                                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                        pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D,
                                        1, 0.25D, 0.25D, 0.25D, 0.01D);
                            }
                            if (++highlightedBlocks >= maxBlockMatches) break outer;
                        }
                    }
                }
            }
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("灵气探测：发现" + entities.size() + "个生灵，" + highlightedBlocks + "处灵物波动"),
                    true
            );
            return true;
        }

        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("灵气探测：发现" + entities.size() + "个生灵"),
            true
        );

        return true;
    }
}

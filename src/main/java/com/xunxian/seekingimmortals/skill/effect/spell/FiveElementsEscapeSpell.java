package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.SpiritualRootAttribute;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class FiveElementsEscapeSpell extends SpellEffect {
    public FiveElementsEscapeSpell() {
        super(60, 400, 0.0D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        if (!hasFiveElementRoot(cultivation.getSpiritualRootAttributes())) {
            player.displayClientMessage(Component.literal("灵根五行不稳，无法施展五行遁术。"), true);
            return false;
        }

        Vec3 flat = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (flat.lengthSqr() < 0.001D) return false;
        flat = flat.normalize();

        ServerLevel level = player.serverLevel();
        Vec3 origin = player.position();
        for (double distance = 20.0D; distance >= 4.0D; distance -= 1.0D) {
            Vec3 target = origin.add(flat.scale(distance));
            BlockPos base = BlockPos.containing(target);
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos feet = base.offset(0, dy, 0);
                if (!level.isLoaded(feet)) continue;
                if (canStandAt(level, feet)) {
                    level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.5D, player.getZ(), 18, 0.4D, 0.4D, 0.4D, 0.04D);
                    player.teleportTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.8D, player.getZ(), 18, 0.4D, 0.5D, 0.4D, 0.02D);
                    level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.5F);
                    player.displayClientMessage(Component.literal("五行遁术穿行二十格。"), true);
                    return true;
                }
            }
        }
        player.displayClientMessage(Component.literal("前方五行阻滞，遁术失败。"), true);
        return false;
    }

    private static boolean hasFiveElementRoot(Set<SpiritualRootAttribute> attributes) {
        return attributes.contains(SpiritualRootAttribute.METAL)
                || attributes.contains(SpiritualRootAttribute.WOOD)
                || attributes.contains(SpiritualRootAttribute.WATER)
                || attributes.contains(SpiritualRootAttribute.FIRE)
                || attributes.contains(SpiritualRootAttribute.EARTH);
    }

    private static boolean canStandAt(ServerLevel level, BlockPos feet) {
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(feet.above());
        BlockState belowState = level.getBlockState(feet.below());
        return belowState.isSolidRender(level, feet.below())
                && feetState.getCollisionShape(level, feet).isEmpty()
                && headState.getCollisionShape(level, feet.above()).isEmpty();
    }
}

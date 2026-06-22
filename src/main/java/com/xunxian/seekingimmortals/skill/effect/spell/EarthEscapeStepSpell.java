package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EarthEscapeStepSpell extends SpellEffect {
    public EarthEscapeStepSpell() {
        super(15, 100, 0.0D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 0.001D) return false;
        flat = flat.normalize();
        ServerLevel level = player.serverLevel();
        Vec3 origin = player.position();
        for (double distance = 4.0D; distance >= 1.5D; distance -= 0.5D) {
            Vec3 target = origin.add(flat.scale(distance));
            if (canStandAt(level, BlockPos.containing(target))) {
                player.teleportTo(target.x, target.y, target.z);
                level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.3F);
                player.displayClientMessage(Component.literal("土遁步穿行数步。"), true);
                return true;
            }
        }
        player.displayClientMessage(Component.literal("前方地脉紊乱，土遁步失败。"), true);
        return false;
    }

    private boolean canStandAt(ServerLevel level, BlockPos feet) {
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(feet.above());
        BlockState belowState = level.getBlockState(feet.below());
        return belowState.isSolidRender(level, feet.below())
                && feetState.getCollisionShape(level, feet).isEmpty()
                && headState.getCollisionShape(level, feet.above()).isEmpty();
    }
}

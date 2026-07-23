package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
            if (canStandAt(level, BlockPos.containing(target)) && isPathClear(level, origin, target)) {
                TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile("earth");
                vfx.castAt(level, player);
                vfx.path(level, origin.add(0.0D, 0.35D, 0.0D), target.add(0.0D, 0.35D, 0.0D),
                        Math.max(10, (int) Math.ceil(distance * 5.0D)));
                player.teleportTo(target.x, target.y, target.z);
                vfx.impactAt(level, target);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.earth_escape_step.success"), true);
                return true;
            }
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.earth_escape_step.failed"), true);
        return false;
    }

    /** 沿起止点之间的身体通道逐格检查无实心阻挡，避免穿薄墙（M7）。 */
    private boolean isPathClear(ServerLevel level, Vec3 origin, Vec3 target) {
        Vec3 step = target.subtract(origin);
        int segments = Math.max(1, (int)Math.ceil(step.length() * 2.0D));
        Vec3 delta = step.scale(1.0D / segments);
        Vec3 cursor = origin;
        for (int i = 0; i <= segments; i++) {
            BlockPos body = BlockPos.containing(cursor);
            BlockState state = level.getBlockState(body);
            if (!state.getCollisionShape(level, body).isEmpty()) {
                return false;
            }
            cursor = cursor.add(delta);
        }
        return true;
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

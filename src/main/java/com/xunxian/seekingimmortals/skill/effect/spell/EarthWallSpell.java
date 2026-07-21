package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.block.EarthWallBlock;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EarthWallSpell extends SpellEffect {
    public EarthWallSpell() {
        super(40, 150, 0);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        if (cultivation.getSpiritualPower() < getSpiritualPowerCost(skill.getLevel())) {
            return false;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) return false;

        BlockPos base = player.blockPosition().relative(player.getDirection());
        int height = 2 + skill.getLevel() / 2;
        int placed = 0;
        BlockState wall = ModBlocks.EARTH_WALL.get().defaultBlockState();
        TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile("earth");

        for (int y = 0; y < Math.min(height, 5); y++) {
            BlockPos wallPos = base.above(y);
            if (level.getBlockState(wallPos).isAir()) {
                level.setBlock(wallPos, wall, 3);
                level.scheduleTick(wallPos, ModBlocks.EARTH_WALL.get(), EarthWallBlock.REMOVAL_TICKS);
                placed++;
            }
        }
        if (placed <= 0) {
            return false;
        }
        vfx.castAt(level, player);
        vfx.path(level, Vec3.atCenterOf(base), Vec3.atCenterOf(base.above(Math.min(height, 5) - 1)),
                Math.max(8, placed * 4));
        vfx.impactAt(level, Vec3.atCenterOf(base));

        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("土墙术！生成" + placed + "格土墙，" + (EarthWallBlock.REMOVAL_TICKS / 20) + "秒后消散"),
            true
        );

        return true;
    }
}

package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

public class EarthWallSpell extends SpellEffect {
    public EarthWallSpell() {
        super(40, 150, 0);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        if (cultivation.getSpiritualPower() < getSpiritualPowerCost(skill.getLevel())) {
            return false;
        }

        BlockPos pos = player.blockPosition().relative(player.getDirection());
        int height = 2 + skill.getLevel() / 2;

        for (int y = 0; y < Math.min(height, 5); y++) {
            BlockPos wallPos = pos.above(y);
            if (context.getLevel().getBlockState(wallPos).isAir()) {
                context.getLevel().setBlock(wallPos, Blocks.STONE.defaultBlockState(), 3);
            }
        }

        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("土墙术！生成" + height + "格高土墙"),
            true
        );

        return true;
    }
}

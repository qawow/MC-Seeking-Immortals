package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class DetectionSpell extends SpellEffect {
    public DetectionSpell() {
        super(15, 60, 0);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        if (cultivation.getSpiritualPower() < getSpiritualPowerCost(skill.getLevel())) {
            return false;
        }

        double range = 10.0 + skill.getLevel() * 2.0;
        AABB area = new AABB(player.blockPosition()).inflate(range);
        List<LivingEntity> entities = context.getLevel().getEntitiesOfClass(LivingEntity.class, area, e -> e != player);

        if (context.getLevel() instanceof ServerLevel serverLevel) {
            for (LivingEntity entity : entities) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                    5, 0.3, 0.3, 0.3, 0.02);
            }
        }

        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("探测术！发现" + entities.size() + "个生物"),
            true
        );

        return true;
    }
}
